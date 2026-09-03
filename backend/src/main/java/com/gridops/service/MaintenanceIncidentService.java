package com.gridops.service;

import com.gridops.domain.*;
import com.gridops.dto.OperationalDtos.*;
import com.gridops.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaintenanceIncidentService {

    private final MaintenanceIncidentRepository incidentRepository;
    private final WorkOrderRepository workOrderRepository;
    private final TechnicianRepository technicianRepository;
    private final AssetRepository assetRepository;
    private final AnomalyRepository anomalyRepository;
    private final AuditEventService auditEventService;
    private final Optional<SimpMessagingTemplate> messagingTemplate;

    /**
     * Converts a detected anomaly into a prioritized Maintenance Incident
     */
    @Transactional
    public MaintenanceIncident createOrUpdateIncidentFromAnomaly(Anomaly anomaly) {
        Asset asset = anomaly.getAsset() != null ? anomaly.getAsset() : assetRepository.findById(anomaly.getAssetId()).orElseThrow();

        // Check if there is already an active incident for this asset to avoid duplicates
        Optional<MaintenanceIncident> activeIncidentOpt = incidentRepository.findTopByAssetIdAndStatusNotIn(
                asset.getId(),
                List.of(MaintenanceIncident.IncidentStatus.RESOLVED, MaintenanceIncident.IncidentStatus.CLOSED)
        );

        if (activeIncidentOpt.isPresent()) {
            MaintenanceIncident existing = activeIncidentOpt.get();
            // Re-calculate dynamic priority
            BigDecimal updatedScore = calculatePriorityScore(anomaly.getEstimatedLossKw(), anomaly.getDurationMinutes(), asset.getCriticalityWeight(), anomaly.getSeverity());
            existing.setPriorityScore(updatedScore);
            existing.setPriority(determinePriorityTier(updatedScore, anomaly.getEstimatedLossKw()));
            return incidentRepository.save(existing);
        }

        // Calculate Priority Score
        BigDecimal score = calculatePriorityScore(
                anomaly.getEstimatedLossKw(),
                anomaly.getDurationMinutes(),
                asset.getCriticalityWeight(),
                anomaly.getSeverity()
        );
        MaintenanceIncident.IncidentPriority priorityTier = determinePriorityTier(score, anomaly.getEstimatedLossKw());

        MaintenanceIncident incident = MaintenanceIncident.builder()
                .anomaly(anomaly)
                .anomalyId(anomaly.getId())
                .asset(asset)
                .assetId(asset.getId())
                .title(String.format("[%s] %s underperformance on %s", priorityTier.name(), anomaly.getRootCauseCandidate() != null ? anomaly.getRootCauseCandidate() : "Abnormal deviation", asset.getName()))
                .priority(priorityTier)
                .priorityScore(score)
                .status(MaintenanceIncident.IncidentStatus.OPEN)
                .build();

        MaintenanceIncident saved = incidentRepository.save(incident);

        // Update anomaly status
        anomaly.setStatus(Anomaly.AnomalyStatus.INCIDENT_CREATED);
        anomalyRepository.save(anomaly);

        // Audit Trail
        auditEventService.recordEvent(
                "INCIDENT",
                saved.getId(),
                "INCIDENT_CREATED",
                "INCIDENT_ENGINE",
                String.format("Created %s incident for %s (Score: %s, Loss: %s kW)", priorityTier, asset.getName(), score, anomaly.getEstimatedLossKw())
        );

        // Broadcast via WebSocket
        messagingTemplate.ifPresent(template -> template.convertAndSend("/topic/incidents", toResponseDto(saved)));

        return saved;
    }

    /**
     * Priority Formula:
     * Score = (Loss_kW * 0.40) + (Duration_Hours * 0.20) + (Criticality * 20 * 0.25) + (Urgency * 0.15)
     */
    public BigDecimal calculatePriorityScore(BigDecimal estimatedLossKw, int durationMinutes, BigDecimal criticalityWeight, Anomaly.AnomalySeverity severity) {
        double loss = estimatedLossKw != null ? estimatedLossKw.doubleValue() : 0.0;
        double durationHours = durationMinutes / 60.0;
        double criticality = criticalityWeight != null ? criticalityWeight.doubleValue() : 3.0; // 1-5 scale

        double urgencyMultiplier = switch (severity) {
            case CRITICAL -> 100.0;
            case HIGH -> 75.0;
            case MEDIUM -> 50.0;
            case LOW -> 25.0;
        };

        double score = (loss * 0.40) + (durationHours * 5.0 * 0.20) + (criticality * 20.0 * 0.25) + (urgencyMultiplier * 0.15);
        return BigDecimal.valueOf(Math.min(100.0, Math.max(1.0, score))).setScale(2, RoundingMode.HALF_UP);
    }

    public MaintenanceIncident.IncidentPriority determinePriorityTier(BigDecimal score, BigDecimal estimatedLossKw) {
        if (score.compareTo(BigDecimal.valueOf(75.0)) >= 0 || (estimatedLossKw != null && estimatedLossKw.compareTo(BigDecimal.valueOf(100.0)) >= 0)) {
            return MaintenanceIncident.IncidentPriority.P1_CRITICAL;
        } else if (score.compareTo(BigDecimal.valueOf(50.0)) >= 0) {
            return MaintenanceIncident.IncidentPriority.P2_HIGH;
        } else if (score.compareTo(BigDecimal.valueOf(25.0)) >= 0) {
            return MaintenanceIncident.IncidentPriority.P3_MEDIUM;
        } else {
            return MaintenanceIncident.IncidentPriority.P4_LOW;
        }
    }

    @Transactional
    public WorkOrder assignTechnician(UUID incidentId, UUID technicianId) {
        MaintenanceIncident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));
        Technician technician = technicianRepository.findById(technicianId)
                .orElseThrow(() -> new IllegalArgumentException("Technician not found: " + technicianId));

        WorkOrder workOrder = WorkOrder.builder()
                .incident(incident)
                .incidentId(incident.getId())
                .technician(technician)
                .technicianId(technician.getId())
                .status(WorkOrder.WorkOrderStatus.DISPATCHED)
                .assignedAt(OffsetDateTime.now())
                .build();

        WorkOrder savedWo = workOrderRepository.save(workOrder);

        // Update incident state
        incident.setStatus(MaintenanceIncident.IncidentStatus.ASSIGNED);
        incidentRepository.save(incident);

        // Update technician availability
        technician.setIsAvailable(false);
        technicianRepository.save(technician);

        // Update asset state
        Asset asset = incident.getAsset();
        asset.setStatus(Asset.AssetStatus.MAINTENANCE);
        assetRepository.save(asset);

        auditEventService.recordEvent(
                "WORK_ORDER",
                savedWo.getId(),
                "TECHNICIAN_ASSIGNED",
                "OPERATIONS_DISPATCHER",
                String.format("Assigned technician %s to incident %s", technician.getName(), incident.getTitle())
        );

        return savedWo;
    }

    @Transactional
    public MaintenanceIncident completeWorkOrderAndStartVerification(UUID workOrderId, CompleteWorkOrderRequest request) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new IllegalArgumentException("Work order not found: " + workOrderId));
        
        workOrder.setStatus(WorkOrder.WorkOrderStatus.WORK_COMPLETED);
        workOrder.setCompletedAt(OffsetDateTime.now());
        workOrder.setRepairNotes(request.getRepairNotes());
        workOrder.setRootCauseCategory(request.getRootCauseCategory());
        workOrderRepository.save(workOrder);

        // Free technician
        if (workOrder.getTechnician() != null) {
            Technician tech = workOrder.getTechnician();
            tech.setIsAvailable(true);
            technicianRepository.save(tech);
        }

        // Move incident to PENDING_VERIFICATION window
        MaintenanceIncident incident = workOrder.getIncident();
        OffsetDateTime now = OffsetDateTime.now();
        incident.setStatus(MaintenanceIncident.IncidentStatus.PENDING_VERIFICATION);
        incident.setVerificationWindowStart(now);
        incident.setVerificationWindowEnd(now.plusMinutes(60)); // 60-min verification window
        incident.setVerificationNotes("Technician completed repair. Telemetry verification window active (evaluating 60 mins).");
        MaintenanceIncident updated = incidentRepository.save(incident);

        auditEventService.recordEvent(
                "INCIDENT",
                incident.getId(),
                "PENDING_VERIFICATION",
                "TECHNICIAN",
                "Repair reported complete. Post-repair telemetry verification started."
        );

        return updated;
    }

    public List<IncidentResponseDto> getAllActiveIncidents() {
        return incidentRepository.findByStatusInOrderByPriorityScoreDesc(
                List.of(MaintenanceIncident.IncidentStatus.OPEN, MaintenanceIncident.IncidentStatus.ASSIGNED,
                        MaintenanceIncident.IncidentStatus.IN_PROGRESS, MaintenanceIncident.IncidentStatus.PENDING_VERIFICATION)
        ).stream().map(this::toResponseDto).collect(Collectors.toList());
    }

    public IncidentResponseDto toResponseDto(MaintenanceIncident incident) {
        return IncidentResponseDto.builder()
                .id(incident.getId())
                .anomalyId(incident.getAnomalyId())
                .assetId(incident.getAssetId())
                .assetName(incident.getAsset() != null ? incident.getAsset().getName() : "Unknown Asset")
                .siteName(incident.getAsset() != null && incident.getAsset().getSite() != null ? incident.getAsset().getSite().getName() : "Unknown Site")
                .title(incident.getTitle())
                .priority(incident.getPriority())
                .priorityScore(incident.getPriorityScore())
                .status(incident.getStatus())
                .verificationWindowStart(incident.getVerificationWindowStart())
                .verificationWindowEnd(incident.getVerificationWindowEnd())
                .verificationPassed(incident.getVerificationPassed())
                .verificationNotes(incident.getVerificationNotes())
                .workOrders(incident.getWorkOrders().stream().map(wo -> WorkOrderDto.builder()
                        .id(wo.getId())
                        .incidentId(wo.getIncidentId())
                        .technicianId(wo.getTechnicianId())
                        .technicianName(wo.getTechnician() != null ? wo.getTechnician().getName() : "Unassigned")
                        .status(wo.getStatus())
                        .assignedAt(wo.getAssignedAt())
                        .completedAt(wo.getCompletedAt())
                        .repairNotes(wo.getRepairNotes())
                        .rootCauseCategory(wo.getRootCauseCategory())
                        .build()).collect(Collectors.toList()))
                .createdAt(incident.getCreatedAt())
                .updatedAt(incident.getUpdatedAt())
                .build();
    }
}
