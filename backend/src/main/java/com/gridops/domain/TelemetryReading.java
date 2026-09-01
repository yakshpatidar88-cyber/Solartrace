package com.gridops.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "telemetry_readings", uniqueConstraints = {
    @UniqueConstraint(name = "uq_asset_timestamp", columnNames = {"asset_id", "timestamp"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TelemetryReading {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    @JsonIgnore
    private Asset asset;

    @Column(name = "asset_id", insertable = false, updatable = false)
    private UUID assetId;

    @Column(nullable = false)
    private OffsetDateTime timestamp;

    @Column(name = "actual_output_kw", nullable = false, precision = 10, scale = 2)
    private BigDecimal actualOutputKw;

    @Column(name = "expected_output_kw", nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedOutputKw;

    @Column(name = "irradiance_w_m2", nullable = false, precision = 10, scale = 2)
    private BigDecimal irradianceWM2;

    @Column(name = "ambient_temp_c", nullable = false, precision = 5, scale = 2)
    private BigDecimal ambientTempC;

    @Column(name = "module_temp_c", precision = 5, scale = 2)
    private BigDecimal moduleTempC;

    @Column(name = "efficiency_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal efficiencyPct;

    @Column(name = "is_simulated", nullable = false)
    @Builder.Default
    private Boolean isSimulated = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;
}
