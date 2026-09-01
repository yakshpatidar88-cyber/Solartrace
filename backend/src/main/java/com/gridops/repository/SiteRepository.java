package com.gridops.repository;

import com.gridops.domain.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SiteRepository extends JpaRepository<Site, UUID> {
}
