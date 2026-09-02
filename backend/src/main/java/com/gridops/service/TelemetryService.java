package com.gridops.service;

import com.gridops.client.AnalyticsClient;
import com.gridops.domain.Asset;
import com.gridops.domain.TelemetryReading;
import com.gridops.dto.AnalyticsDtos.*;
import com.gridops.dto.TelemetryIngestRequest;
import com.gridops.repository.AssetRepository;
import com.gridops.repository.TelemetryReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TelemetryService {

    private final TelemetryReadingRepository telemetryRepository;
    private final AssetRepository assetRepository;
    private final AnalyticsClient analyticsClient;
    private final AnomalyService anomalyService;
    private final MaintenanceIncidentService incidentService;
    private final Optional<SimpMessagingTemplate> messagingTemplate;

    @Transactional
    public TelemetryReading ingestReading(TelemetryIngestRequest req) {
        Asset asset = assetRepository.findById(req.getAssetId())
                .orElseThrow(() -> new IllegalArgumentException("Asset not found with ID: " + req.getAssetId()));

        // Prevent duplicate readings at identical timestamp
        Optional<TelemetryReading> existing = telemetryRepository.findByAssetIdAndTimestamp(req.getAssetId(), req.getTimestamp());
        if (existing.isPresent()) {
            return existing.get();
        }

        // Calculate expected output via analytics baseline if omitted in payload
        BigDecimal expectedOutput = req.getExpectedOutputKw();
        if (expectedOutput == null) {
            BaselineRequestDto baselineReq = BaselineRequestDto.builder()
                    .ratedCapacityKw(asset.getRatedPowerKw())
                    .irradianceWM2(req.getIrradianceWM2())
                    .ambientTempC(req.getAmbientTempC())
                    .moduleTempC(req.getModuleTempC())
                    .build();
            BaselineResponseDto baselineRes = analyticsClient.calculateBaseline(baselineReq);
            expectedOutput = baselineRes.getExpectedOutputKw();
        }

        // Calculate efficiency %
        BigDecimal efficiency = BigDecimal.ZERO;
        if (asset.getRatedPowerKw().compareTo(BigDecimal.ZERO) > 0) {
            efficiency = req.getActualOutputKw()
                    .divide(asset.getRatedPowerKw(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100.0));
        }

        TelemetryReading reading = TelemetryReading.builder()
                .asset(asset)
                .assetId(asset.getId())
                .timestamp(req.getTimestamp())
                .actualOutputKw(req.getActualOutputKw())
                .expectedOutputKw(expectedOutput)
                .irradianceWM2(req.getIrradianceWM2())
                .ambientTempC(req.getAmbientTempC())
                .moduleTempC(req.getModuleTempC())
                .efficiencyPct(efficiency)
                .isSimulated(req.getIsSimulated() != null ? req.getIsSimulated() : true)
                .build();

        TelemetryReading saved = telemetryRepository.save(reading);

        // Run anomaly evaluation asynchronously
        anomalyService.evaluateAssetPerformance(asset.getId()).ifPresent(anomaly -> {
            incidentService.createOrUpdateIncidentFromAnomaly(anomaly);
        });

        // Broadcast live telemetry point
        messagingTemplate.ifPresent(template -> template.convertAndSend("/topic/telemetry/" + asset.getId(), saved));

        return saved;
    }
}
