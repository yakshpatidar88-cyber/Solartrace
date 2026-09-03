package com.gridops.service;

import com.gridops.domain.Asset;
import com.gridops.domain.MaintenanceIncident;
import com.gridops.domain.TelemetryReading;
import com.gridops.repository.AssetRepository;
import com.gridops.repository.MaintenanceIncidentRepository;
import com.gridops.repository.TelemetryReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VerificationServiceTest {

    @Mock
    private MaintenanceIncidentRepository incidentRepository;
    @Mock
    private TelemetryReadingRepository telemetryRepository;
    @Mock
    private AssetRepository assetRepository;
    @Mock
    private AuditEventService auditEventService;

    private VerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new VerificationService(
                incidentRepository,
                telemetryRepository,
                assetRepository,
                auditEventService
        );
        ReflectionTestUtils.setField(verificationService, "recoveryThresholdPct", 95.0);
    }

    @Test
    void testEvaluatePendingVerifications_PassesRecoveryThreshold() {
        UUID assetId = UUID.randomUUID();
        Asset asset = Asset.builder()
                .id(assetId)
                .name("Central Inverter INV-01")
                .status(Asset.AssetStatus.MAINTENANCE)
                .build();

        MaintenanceIncident incident = MaintenanceIncident.builder()
                .id(UUID.randomUUID())
                .assetId(assetId)
                .asset(asset)
                .status(MaintenanceIncident.IncidentStatus.PENDING_VERIFICATION)
                .verificationWindowStart(OffsetDateTime.now().minusMinutes(30))
                .build();

        when(incidentRepository.findByStatus(MaintenanceIncident.IncidentStatus.PENDING_VERIFICATION))
                .thenReturn(List.of(incident));
        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));

        // 3 readings showing 98 kW actual vs 100 kW expected (98% recovery > 95%)
        TelemetryReading r1 = TelemetryReading.builder().actualOutputKw(BigDecimal.valueOf(98.0)).expectedOutputKw(BigDecimal.valueOf(100.0)).irradianceWM2(BigDecimal.valueOf(800.0)).build();
        TelemetryReading r2 = TelemetryReading.builder().actualOutputKw(BigDecimal.valueOf(97.0)).expectedOutputKw(BigDecimal.valueOf(100.0)).irradianceWM2(BigDecimal.valueOf(800.0)).build();
        TelemetryReading r3 = TelemetryReading.builder().actualOutputKw(BigDecimal.valueOf(99.0)).expectedOutputKw(BigDecimal.valueOf(100.0)).irradianceWM2(BigDecimal.valueOf(800.0)).build();

        when(telemetryRepository.findByAssetIdAndTimestampBetween(any(), any(), any()))
                .thenReturn(List.of(r1, r2, r3));

        verificationService.evaluatePendingVerifications();

        verify(incidentRepository, atLeastOnce()).save(any(MaintenanceIncident.class));
        verify(assetRepository, atLeastOnce()).save(any(Asset.class));
        verify(auditEventService, atLeastOnce()).recordEvent(any(), any(), eq("VERIFICATION_PASSED"), any(), any());
    }
}
