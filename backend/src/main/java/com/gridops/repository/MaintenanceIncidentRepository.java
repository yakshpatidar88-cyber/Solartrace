package com.gridops.repository;

import com.gridops.domain.MaintenanceIncident;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MaintenanceIncidentRepository extends JpaRepository<MaintenanceIncident, UUID> {
    List<MaintenanceIncident> findByStatusInOrderByPriorityScoreDesc(List<MaintenanceIncident.IncidentStatus> statuses);
    Optional<MaintenanceIncident> findTopByAssetIdAndStatusNotIn(UUID assetId, List<MaintenanceIncident.IncidentStatus> closedStatuses);
    List<MaintenanceIncident> findByStatus(MaintenanceIncident.IncidentStatus status);

    @Query("SELECT COUNT(i) FROM MaintenanceIncident i WHERE i.status != 'CLOSED' AND i.status != 'RESOLVED'")
    long countActiveIncidents();
}
