package com.gridops.dto;

import com.gridops.domain.Asset;
import com.gridops.domain.MaintenanceIncident;
import com.gridops.domain.WorkOrder;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class OperationalDtos {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssignWorkOrderRequest {
        @NotNull(message = "Technician ID is required")
        private UUID technicianId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompleteWorkOrderRequest {
        @NotNull(message = "Repair notes are required")
        private String repairNotes;
        private String rootCauseCategory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncidentResponseDto {
        private UUID id;
        private UUID anomalyId;
        private UUID assetId;
        private String assetName;
        private String siteName;
        private String title;
        private MaintenanceIncident.IncidentPriority priority;
        private BigDecimal priorityScore;
        private MaintenanceIncident.IncidentStatus status;
        private OffsetDateTime verificationWindowStart;
        private OffsetDateTime verificationWindowEnd;
        private Boolean verificationPassed;
        private String verificationNotes;
        private List<WorkOrderDto> workOrders;
        private OffsetDateTime createdAt;
        private OffsetDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkOrderDto {
        private UUID id;
        private UUID incidentId;
        private UUID technicianId;
        private String technicianName;
        private WorkOrder.WorkOrderStatus status;
        private OffsetDateTime assignedAt;
        private OffsetDateTime completedAt;
        private String repairNotes;
        private String rootCauseCategory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DashboardSummaryDto {
        private long totalSites;
        private long totalAssets;
        private long underperformingAssetsCount;
        private long activeIncidentsCount;
        private BigDecimal currentFleetOutputKw;
        private BigDecimal currentFleetExpectedKw;
        private BigDecimal totalProductionLossKw;
        private BigDecimal fleetPerformanceRatioPct;
        private List<IncidentResponseDto> recentCriticalIncidents;
    }
}
