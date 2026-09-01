package com.gridops.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "work_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "incident_id", nullable = false)
    @JsonIgnore
    private MaintenanceIncident incident;

    @Column(name = "incident_id", insertable = false, updatable = false)
    private UUID incidentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "technician_id")
    @JsonIgnore
    private Technician technician;

    @Column(name = "technician_id", insertable = false, updatable = false)
    private UUID technicianId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private WorkOrderStatus status = WorkOrderStatus.DISPATCHED;

    @Column(name = "assigned_at", nullable = false)
    @Builder.Default
    private OffsetDateTime assignedAt = OffsetDateTime.now();

    @Column(name = "completed_at")
    private OffsetDateTime completedAt;

    @Column(name = "repair_notes", columnDefinition = "TEXT")
    private String repairNotes;

    @Column(name = "root_cause_category")
    private String rootCauseCategory;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    public enum WorkOrderStatus {
        DISPATCHED, ON_SITE, WORK_COMPLETED, CANCELLED
    }
}
