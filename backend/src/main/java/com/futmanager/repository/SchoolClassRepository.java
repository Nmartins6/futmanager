package com.futmanager.repository;

import com.futmanager.domain.entity.SchoolClass;
import com.futmanager.domain.enums.ClassStatus;
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
public interface SchoolClassRepository extends JpaRepository<SchoolClass, UUID> {

    List<SchoolClass> findByTenantIdAndStatus(UUID tenantId, ClassStatus status);

    List<SchoolClass> findByStatus(ClassStatus status);

    List<SchoolClass> findByTenantId(UUID tenantId);

    long countByTenantId(UUID tenantId);

    @Query("SELECT DISTINCT c FROM SchoolClass c " +
           "JOIN c.teachers ct " +
           "WHERE c.tenant.id = :tenantId " +
           "AND ct.user.id = :teacherId " +
           "AND (:status IS NULL OR c.status = :status)")
    List<SchoolClass> findByTeacherId(
            @Param("tenantId") UUID tenantId,
            @Param("teacherId") UUID teacherId,
            @Param("status") ClassStatus status);

    @Query("SELECT DISTINCT c FROM SchoolClass c " +
           "LEFT JOIN c.teachers ct " +
           "WHERE c.tenant.id = :tenantId " +
           "AND (:query IS NULL OR :query = '' OR LOWER(c.name) LIKE :query OR LOWER(c.category) LIKE :query) " +
           "AND (:status IS NULL OR c.status = :status) " +
           "AND (:teacherId IS NULL OR ct.user.id = :teacherId)")
    Page<SchoolClass> searchClasses(
            @Param("tenantId") UUID tenantId,
            @Param("query") String query,
            @Param("status") ClassStatus status,
            @Param("teacherId") UUID teacherId,
            Pageable pageable);

    Optional<SchoolClass> findByIdAndTenantId(UUID id, UUID tenantId);
}
