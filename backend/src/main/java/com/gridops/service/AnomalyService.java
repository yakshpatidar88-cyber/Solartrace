package com.gridops.service;

import com.gridops.client.AnalyticsClient;
import com.gridops.domain.Anomaly;
import com.gridops.domain.Asset;
import com.gridops.domain.TelemetryReading;
import com.gridops.dto.AnalyticsDtos.*;
import com.gridops.repository.AnomalyRepository;
import com.gridops.repository.AssetRepository;
import com.gridops.repository.TelemetryReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyService {

    private final AnomalyRepository anomalyRepository;
    private final AssetRepository assetRepository;
    private final TelemetryReadingRepository telemetryRepository;
    private final AnalyticsClient analyticsClient;
    private final AuditEventService auditEventService;
    private final Optional<SimpMessagingTemplate> messagingTemplate;

    @Async
    @Transactional
    public void evaluateAssetPerformanceAsync(UUID assetId) {
        evaluateAssetPerformance(assetId);
    }

    @Transactional
    public Optional<Anomaly> evaluateAssetPerformance(UUID assetId) {
        Asset asset = assetRepository.findById(assetId).orElse(null);
        if (asset == null) {
            return Optional.empty();
        }

        // Fetch last 10 readings for window evaluation
        List<TelemetryReading> recentReadings = telemetryRepository.findByAssetIdOrderByTimestampDesc(assetId, PageRequest.of(0, 10));
        if (recentReadings.size() < 3) {
            return Optional.empty();
        }

        // Reverse to chronological order
        Collections.reverse(recentReadings);

        List<TelemetryPointDto> points = recentReadings.stream()
                .map(r -> TelemetryPointDto.builder()
                        .timestamp(r.getTimestamp())
                        .actualOutputKw(r.getActualOutputKw())
                        .expectedOutputKw(r.getExpectedOutputKw())
                        .irradianceWM2(r.getIrradianceWM2())
                        .ambientTempC(r.getAmbientTempC())
                        .moduleTempC(r.getModuleTempC())
                        .build())
                .collect(Collectors.toList());

        AnomalyDetectionRequestDto req = AnomalyDetectionRequestDto.builder()
                .assetId(asset.getId().toString())
                .assetName(asset.getName())
                .assetType(asset.getAssetType().name())
                .ratedCapacityKw(asset.getRatedPowerKw())
                .criticalityWeight(asset.getCriticalityWeight())
                .consecutiveThreshold(3)
                .deviationPercentageThreshold(BigDecimal.valueOf(15.0))
                .readings(points)
                .build();

        AnomalyResponseDto response = analyticsClient.evaluateWindow(req);

        if (Boolean.TRUE.equals(response.getIsAnomaly())) {
            return handleDetectedAnomaly(asset, response);
        } else {
            // Check if there was an active anomaly that is now resolved
            handleRecoveryIfAny(asset);
            return Optional.empty();
        }
    }

    private Optional<Anomaly> handleDetectedAnomaly(Asset asset, AnomalyResponseDto dto) {
        // Check for active open anomaly to prevent duplicates (Idempotency check)
        List<Anomaly.AnomalyStatus> activeStatuses = List.of(Anomaly.AnomalyStatus.OPEN, Anomaly.AnomalyStatus.INCIDENT_CREATED);
        Optional<Anomaly> existingActive = anomalyRepository.findTopByAssetIdAndStatusInOrderByDetectedAtDesc(asset.getId(), activeStatuses);

        if (existingActive.isPresent()) {
            Anomaly existing = existingActive.get();
            // Update duration and loss estimates on existing anomaly
            existing.setDurationMinutes(dto.getDurationMinutes());
            existing.setEstimatedLossKw(dto.getEstimatedLossKw());
            existing.setDeviationPct(dto.getDeviationPct());
            existing.setExplanation(dto.getExplanation());
            Anomaly updated = anomalyRepository.save(existing);
            log.info("Updated existing active anomaly {} for asset {}", updated.getId(), asset.getName());
            return Optional.of(updated);
        }

        // Create new Anomaly record
        Anomaly.AnomalySeverity severity = Anomaly.AnomalySeverity.valueOf(dto.getSeverity() != null ? dto.getSeverity() : "MEDIUM");
        Anomaly anomaly = Anomaly.builder()
                .asset(asset)
                .assetId(asset.getId())
                .detectedAt(OffsetDateTime.now())
                .severity(severity)
                .deviationPct(dto.getDeviationPct())
                .estimatedLossKw(dto.getEstimatedLossKw())
                .durationMinutes(dto.getDurationMinutes() != null ? dto.getDurationMinutes() : 15)
                .status(Anomaly.AnomalyStatus.OPEN)
                .rootCauseCandidate(dto.getRootCauseCandidate())
                .explanation(dto.getExplanation())
                .confidenceScore(dto.getConfidenceScore() != null ? dto.getConfidenceScore() : BigDecimal.valueOf(0.85))
                .build();

        Anomaly saved = anomalyRepository.save(anomaly);

        // Update asset status
        asset.setStatus(severity == Anomaly.AnomalySeverity.CRITICAL ? Asset.AssetStatus.DEGRADED : Asset.AssetStatus.UNDERPERFORMING);
        assetRepository.save(asset);

        // Audit Event
        auditEventService.recordEvent(
                "ANOMALY",
                saved.getId(),
                "ANOMALY_DETECTED",
                "ANOMALY_ENGINE",
                "Flagged sustained deviation: " + saved.getExplanation()
        );

        // Notify WebSocket subscribers
        messagingTemplate.ifPresent(template -> template.convertAndSend("/topic/anomalies", saved));

        return Optional.of(saved);
    }

    private void handleRecoveryIfAny(Asset asset) {
        // If asset was underperforming and now within normal limits without open maintenance, mark optimal
        if (asset.getStatus() == Asset.AssetStatus.UNDERPERFORMING) {
            asset.setStatus(Asset.AssetStatus.OPTIMAL);
            assetRepository.save(asset);
        }
    }
}
