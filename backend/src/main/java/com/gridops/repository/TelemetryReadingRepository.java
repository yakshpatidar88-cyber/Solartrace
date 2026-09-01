package com.gridops.repository;

import com.gridops.domain.TelemetryReading;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TelemetryReadingRepository extends JpaRepository<TelemetryReading, UUID> {

    List<TelemetryReading> findByAssetIdOrderByTimestampDesc(UUID assetId, Pageable pageable);

    @Query("SELECT t FROM TelemetryReading t WHERE t.assetId = :assetId AND t.timestamp BETWEEN :start AND :end ORDER BY t.timestamp ASC")
    List<TelemetryReading> findByAssetIdAndTimestampBetween(
            @Param("assetId") UUID assetId,
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end
    );

    Optional<TelemetryReading> findByAssetIdAndTimestamp(UUID assetId, OffsetDateTime timestamp);
}
