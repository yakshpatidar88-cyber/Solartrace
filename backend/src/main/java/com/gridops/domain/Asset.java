package com.gridops.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "assets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    @JsonIgnore
    private Site site;

    @Column(name = "site_id", insertable = false, updatable = false)
    private UUID siteId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false)
    private AssetType assetType;

    @Column(name = "serial_number", nullable = false, unique = true)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AssetStatus status = AssetStatus.OPTIMAL;

    @Column(name = "rated_power_kw", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratedPowerKw;

    @Column(name = "criticality_weight", nullable = false, precision = 3, scale = 1)
    @Builder.Default
    private BigDecimal criticalityWeight = BigDecimal.valueOf(3.0);

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public enum AssetType {
        INVERTER, SOLAR_STRING, TRANSFORMER, BATTERY
    }

    public enum AssetStatus {
        OPTIMAL, UNDERPERFORMING, DEGRADED, MAINTENANCE, OFFLINE
    }
}
