package com.futmanager.repository;

import com.futmanager.domain.entity.SchoolConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolConfigRepository extends JpaRepository<SchoolConfig, String> {

    Optional<SchoolConfig> findByTenantIdAndConfigKey(UUID tenantId, String configKey);

    List<SchoolConfig> findByTenantId(UUID tenantId);
}
