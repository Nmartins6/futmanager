package com.futmanager.repository;

import com.futmanager.domain.entity.Guardian;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GuardianRepository extends JpaRepository<Guardian, UUID> {

    @Query("SELECT g FROM Guardian g WHERE " +
           "g.tenant.id = :tenantId AND " +
           "(:query IS NULL OR :query = '' OR LOWER(g.fullName) LIKE :query " +
           " OR LOWER(g.phone) LIKE :query " +
           " OR LOWER(g.email) LIKE :query)")
    Page<Guardian> searchGuardians(
            @Param("tenantId") UUID tenantId,
            @Param("query") String query,
            Pageable pageable);

    List<Guardian> findByTenantIdAndFullNameIgnoreCaseAndPhone(UUID tenantId, String fullName, String phone);

    Optional<Guardian> findByIdAndTenantId(UUID id, UUID tenantId);

    Optional<Guardian> findByTenantIdAndPhone(UUID tenantId, String phone);

    boolean existsByTenantIdAndPhone(UUID tenantId, String phone);

    boolean existsByTenantIdAndPhoneAndIdNot(UUID tenantId, String phone, UUID excludeId);
}
