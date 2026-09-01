package com.gridops.repository;

import com.gridops.domain.Anomaly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnomalyRepository extends JpaRepository<Anomaly, UUID> {
    List<Anomaly> findByAssetIdOrderByDetectedAtDesc(UUID assetId);
    Optional<Anomaly> findTopByAssetIdAndStatusInOrderByDetectedAtDesc(UUID assetId, List<Anomaly.AnomalyStatus> statuses);
    List<Anomaly> findByStatus(Anomaly.AnomalyStatus status);
}
