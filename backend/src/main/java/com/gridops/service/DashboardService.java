package com.gridops.service;

import com.gridops.domain.Asset;
import com.gridops.domain.MaintenanceIncident;
import com.gridops.domain.TelemetryReading;
import com.gridops.dto.OperationalDtos.*;
import com.gridops.repository.AssetRepository;
import com.gridops.repository.MaintenanceIncidentRepository;
import com.gridops.repository.SiteRepository;
import com.gridops.repository.TelemetryReadingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SiteRepository siteRepository;
    private final AssetRepository assetRepository;
    private final MaintenanceIncidentRepository incidentRepository;
    private final TelemetryReadingRepository telemetryRepository;
    private final MaintenanceIncidentService incidentService;

    @Cacheable(value = "fleetSummary", key = "'latest'")
    public DashboardSummaryDto getFleetSummary() {
        long totalSites = siteRepository.count();
        List<Asset> allAssets = assetRepository.findAll();
        long totalAssets = allAssets.size();
        long underperformingCount = allAssets.stream()
                .filter(a -> a.getStatus() == Asset.AssetStatus.UNDERPERFORMING || a.getStatus() == Asset.AssetStatus.DEGRADED)
                .count();

        long activeIncidents = incidentRepository.countActiveIncidents();

        BigDecimal currentActualKw = BigDecimal.ZERO;
        BigDecimal currentExpectedKw = BigDecimal.ZERO;

        for (Asset asset : allAssets) {
            List<TelemetryReading> latest = telemetryRepository.findByAssetIdOrderByTimestampDesc(asset.getId(), PageRequest.of(0, 1));
            if (!latest.isEmpty()) {
                TelemetryReading r = latest.get(0);
                currentActualKw = currentActualKw.add(r.getActualOutputKw());
                currentExpectedKw = currentExpectedKw.add(r.getExpectedOutputKw());
            }
        }

        BigDecimal lossKw = currentExpectedKw.subtract(currentActualKw).max(BigDecimal.ZERO);
        BigDecimal prPct = BigDecimal.ZERO;
        if (currentExpectedKw.compareTo(BigDecimal.ZERO) > 0) {
            prPct = currentActualKw.divide(currentExpectedKw, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100.0));
        }

        List<IncidentResponseDto> recentCritical = incidentService.getAllActiveIncidents().stream()
                .filter(i -> i.getPriority() == MaintenanceIncident.IncidentPriority.P1_CRITICAL || i.getPriority() == MaintenanceIncident.IncidentPriority.P2_HIGH)
                .limit(5)
                .toList();

        return DashboardSummaryDto.builder()
                .totalSites(totalSites)
                .totalAssets(totalAssets)
                .underperformingAssetsCount(underperformingCount)
                .activeIncidentsCount(activeIncidents)
                .currentFleetOutputKw(currentActualKw.setScale(2, RoundingMode.HALF_UP))
                .currentFleetExpectedKw(currentExpectedKw.setScale(2, RoundingMode.HALF_UP))
                .totalProductionLossKw(lossKw.setScale(2, RoundingMode.HALF_UP))
                .fleetPerformanceRatioPct(prPct.setScale(2, RoundingMode.HALF_UP))
                .recentCriticalIncidents(recentCritical)
                .build();
    }
}
