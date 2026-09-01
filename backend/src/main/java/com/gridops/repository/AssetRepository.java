package com.gridops.repository;

import com.gridops.domain.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssetRepository extends JpaRepository<Asset, UUID> {
    List<Asset> findBySiteId(UUID siteId);
    Optional<Asset> findBySerialNumber(String serialNumber);
}
