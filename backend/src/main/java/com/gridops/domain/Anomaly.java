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
@Table(name = "anomalies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    @JsonIgnore
    private Asset asset;

    @Column(name = "asset_id", insertable = false, updatable = false)
    private UUID assetId;

    @Column(name = "detected_at", nullable = false)
    private OffsetDateTime detectedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnomalySeverity severity;

    @Column(name = "deviation_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal deviationPct;

    @Column(name = "estimated_loss_kw", nullable = false, precision = 10, scale = 2)
    private BigDecimal estimatedLossKw;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AnomalyStatus status = AnomalyStatus.OPEN;

    @Column(name = "root_cause_candidate")
    private String rootCauseCandidate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "confidence_score", nullable = false, precision = 4, scale = 2)
    @Builder.Default
    private BigDecimal confidenceScore = BigDecimal.valueOf(0.85);

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public enum AnomalySeverity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum AnomalyStatus {
        OPEN, INCIDENT_CREATED, RESOLVED, SUPPRESSED
    }
}
