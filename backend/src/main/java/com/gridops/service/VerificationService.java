package com.gridops.service;

import com.gridops.domain.Asset;
import com.gridops.domain.MaintenanceIncident;
import com.gridops.domain.TelemetryReading;
import com.gridops.repository.AssetRepository;
import com.gridops.repository.MaintenanceIncidentRepository;
import com.gridops.repository.TelemetryReadingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VerificationService {

    private final MaintenanceIncidentRepository incidentRepository;
    private final TelemetryReadingRepository telemetryRepository;
    private final AssetRepository assetRepository;
    private final AuditEventService auditEventService;

    @Value("${verification.recovery-threshold-pct:95.0}")
    private double recoveryThresholdPct;

    /**
     * Periodically inspects incidents currently in PENDING_VERIFICATION state.
     * Compares post-repair telemetry against baseline to certify recovery.
     */
    @Scheduled(fixedRate = 60000) // Runs every 60 seconds
    @Transactional
    public void evaluatePendingVerifications() {
        List<MaintenanceIncident> pendingList = incidentRepository.findByStatus(MaintenanceIncident.IncidentStatus.PENDING_VERIFICATION);
        if (pendingList.isEmpty()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (MaintenanceIncident incident : pendingList) {
            if (incident.getVerificationWindowStart() == null) {
                continue;
            }

            // Fetch post-repair telemetry readings within the window
            List<TelemetryReading> postRepairReadings = telemetryRepository.findByAssetIdAndTimestampBetween(
                    incident.getAssetId(),
                    incident.getVerificationWindowStart(),
                    now
            );

            // Require at least 3 post-repair daylight readings to evaluate recovery
            List<TelemetryReading> daylightReadings = postRepairReadings.stream()
                    .filter(r -> r.getExpectedOutputKw().compareTo(BigDecimal.valueOf(10.0)) > 0)
                    .toList();

            if (daylightReadings.size() < 3) {
                log.debug("Incident {} awaiting more daylight telemetry for verification (found {})", incident.getId(), daylightReadings.size());
                continue;
            }

            // Calculate mean actual vs expected ratio
            double totalActual = 0.0;
            double totalExpected = 0.0;
            for (TelemetryReading r : daylightReadings) {
                totalActual += r.getActualOutputKw().doubleValue();
                totalExpected += r.getExpectedOutputKw().doubleValue();
            }

            double recoveryRatioPct = totalExpected > 0 ? (totalActual / totalExpected) * 100.0 : 0.0;
            Asset asset = incident.getAsset();

            if (recoveryRatioPct >= recoveryThresholdPct) {
                // Recovery PASSED -> Close incident
                incident.setStatus(MaintenanceIncident.IncidentStatus.CLOSED);
                incident.setVerificationPassed(true);
                incident.setVerificationNotes(String.format("Verification PASSED: Asset output reached %.1f%% of expected physical baseline over %d samples.", recoveryRatioPct, daylightReadings.size()));
                incidentRepository.save(incident);

                // Return asset to OPTIMAL
                if (asset != null) {
                    asset.setStatus(Asset.AssetStatus.OPTIMAL);
                    assetRepository.save(asset);
                }

                auditEventService.recordEvent(
                        "INCIDENT",
                        incident.getId(),
                        "VERIFICATION_PASSED",
                        "VERIFICATION_ENGINE",
                        String.format("Post-repair verification successful (Recovery: %.1f%%). Incident closed.", recoveryRatioPct)
                );
                log.info("Incident {} successfully verified and closed (Recovery: {:.1f}%)", incident.getId(), recoveryRatioPct);
            } else if (now.isAfter(incident.getVerificationWindowEnd())) {
                // Verification window expired and recovery failed -> Reopen incident
                incident.setStatus(MaintenanceIncident.IncidentStatus.IN_PROGRESS);
                incident.setVerificationPassed(false);
                incident.setVerificationNotes(String.format("Verification FAILED: Output only reached %.1f%% of baseline (required >= %.1f%%). Reopening for technician re-inspection.", recoveryRatioPct, recoveryThresholdPct));
                incidentRepository.save(incident);

                if (asset != null) {
                    asset.setStatus(Asset.AssetStatus.UNDERPERFORMING);
                    assetRepository.save(asset);
                }

                auditEventService.recordEvent(
                        "INCIDENT",
                        incident.getId(),
                        "VERIFICATION_FAILED",
                        "VERIFICATION_ENGINE",
                        String.format("Verification failed (Output was only %.1f%%). Incident reopened.", recoveryRatioPct)
                );
                log.warn("Incident {} verification failed (Recovery: {:.1f}%). Reopened.", incident.getId(), recoveryRatioPct);
            }
        }
    }
}
