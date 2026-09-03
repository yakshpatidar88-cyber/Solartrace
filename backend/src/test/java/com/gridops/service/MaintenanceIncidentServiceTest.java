package com.gridops.service;

import com.gridops.domain.Anomaly;
import com.gridops.domain.Asset;
import com.gridops.domain.MaintenanceIncident;
import com.gridops.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaintenanceIncidentServiceTest {

    @Mock
    private MaintenanceIncidentRepository incidentRepository;
    @Mock
    private WorkOrderRepository workOrderRepository;
    @Mock
    private TechnicianRepository technicianRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AnomalyRepository anomalyRepository;
    @Mock
    private AuditEventService auditEventService;

    private MaintenanceIncidentService incidentService;

    @BeforeEach
    void setUp() {
        incidentService = new MaintenanceIncidentService(
                incidentRepository,
                workOrderRepository,
                technicianRepository,
                assetRepository,
                anomalyRepository,
                auditEventService,
                Optional.empty()
        );
    }

    @Test
    void testPriorityCalculation_CriticalScenario() {
        // High loss (150 kW), 2 hours duration, Criticality 5.0, Critical urgency
        BigDecimal score = incidentService.calculatePriorityScore(
                BigDecimal.valueOf(150.0),
                120,
                BigDecimal.valueOf(5.0),
                Anomaly.AnomalySeverity.CRITICAL
        );

        MaintenanceIncident.IncidentPriority tier = incidentService.determinePriorityTier(score, BigDecimal.valueOf(150.0));

        assertTrue(score.compareTo(BigDecimal.valueOf(75.0)) >= 0);
        assertEquals(MaintenanceIncident.IncidentPriority.P1_CRITICAL, tier);
    }

    @Test
    void testPriorityCalculation_LowScenario() {
        // Low loss (10 kW), 15 mins duration, Criticality 2.0, Low urgency
        BigDecimal score = incidentService.calculatePriorityScore(
                BigDecimal.valueOf(10.0),
                15,
                BigDecimal.valueOf(2.0),
                Anomaly.AnomalySeverity.LOW
        );

        MaintenanceIncident.IncidentPriority tier = incidentService.determinePriorityTier(score, BigDecimal.valueOf(10.0));
        assertEquals(MaintenanceIncident.IncidentPriority.P4_LOW, tier);
    }

    @Test
    void testCreateIncidentFromAnomaly_Success() {
        UUID assetId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(assetId)
                .name("Inverter INV-01")
                .ratedPowerKw(BigDecimal.valueOf(500.0))
                .criticalityWeight(BigDecimal.valueOf(4.0))
                .build();

        Anomaly anomaly = Anomaly.builder()
                .id(UUID.randomUUID())
                .asset(asset)
                .assetId(assetId)
                .severity(Anomaly.AnomalySeverity.HIGH)
                .estimatedLossKw(BigDecimal.valueOf(80.0))
                .durationMinutes(30)
                .rootCauseCandidate("Inverter Phase Fault")
                .build();

        when(incidentRepository.findTopByAssetIdAndStatusNotIn(eq(assetId), any())).thenReturn(Optional.empty());
        when(incidentRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        MaintenanceIncident incident = incidentService.createOrUpdateIncidentFromAnomaly(anomaly);

        assertNotNull(incident);
        assertEquals(MaintenanceIncident.IncidentStatus.OPEN, incident.getStatus());
        assertEquals(anomaly.getId(), incident.getAnomalyId());
        verify(anomalyRepository, times(1)).save(anomaly);
        verify(auditEventService, times(1)).recordEvent(eq("INCIDENT"), any(), eq("INCIDENT_CREATED"), any(), any());
    }
}
