package com.gridops.config;

import com.gridops.repository.AnomalyRepository;
import com.gridops.repository.MaintenanceIncidentRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@RequiredArgsConstructor
public class MetricsConfig {

    private final MeterRegistry meterRegistry;
    private final MaintenanceIncidentRepository incidentRepository;
    private final AnomalyRepository anomalyRepository;

    @PostConstruct
    public void registerCustomMetrics() {
        // Custom Gauge: Total active maintenance incidents
        Gauge.builder("gridops.incidents.active.count", incidentRepository, MaintenanceIncidentRepository::countActiveIncidents)
                .description("Number of currently active maintenance incidents (OPEN, ASSIGNED, IN_PROGRESS, PENDING_VERIFICATION)")
                .register(meterRegistry);

        // Custom Gauge: Total open anomalies
        Gauge.builder("gridops.anomalies.open.count", anomalyRepository, repo -> repo.count())
                .description("Total number of tracked anomaly records in the platform")
                .register(meterRegistry);
    }
}
