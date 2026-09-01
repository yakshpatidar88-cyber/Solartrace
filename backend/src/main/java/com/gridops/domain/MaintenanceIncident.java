package com.gridops.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "maintenance_incidents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaintenanceIncident {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "anomaly_id")
    @JsonIgnore
    private Anomaly anomaly;

    @Column(name = "anomaly_id", insertable = false, updatable = false)
    private UUID anomalyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    @JsonIgnore
    private Asset asset;

    @Column(name = "asset_id", insertable = false, updatable = false)
    private UUID assetId;

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentPriority priority;

    @Column(name = "priority_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal priorityScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private IncidentStatus status = IncidentStatus.OPEN;

    @Column(name = "verification_window_start")
    private OffsetDateTime verificationWindowStart;

    @Column(name = "verification_window_end")
    private OffsetDateTime verificationWindowEnd;

    @Column(name = "verification_passed")
    private Boolean verificationPassed;

    @Column(name = "verification_notes", columnDefinition = "TEXT")
    private String verificationNotes;

    @OneToMany(mappedBy = "incident", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkOrder> workOrders = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public enum IncidentPriority {
        P1_CRITICAL, P2_HIGH, P3_MEDIUM, P4_LOW
    }

    public enum IncidentStatus {
        OPEN, ASSIGNED, IN_PROGRESS, PENDING_VERIFICATION, RESOLVED, CLOSED
    }
}
