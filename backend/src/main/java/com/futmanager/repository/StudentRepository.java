package com.futmanager.repository;

import com.futmanager.domain.entity.Student;
import com.futmanager.domain.enums.StudentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    @Query("SELECT DISTINCT s FROM Student s " +
           "LEFT JOIN s.guardians sg " +
           "LEFT JOIN sg.guardian g " +
           "LEFT JOIN s.enrollments e " +
           "WHERE s.tenant.id = :tenantId " +
           "AND (:query IS NULL OR :query = '' OR LOWER(s.fullName) LIKE :query " +
           "      OR (g IS NOT NULL AND LOWER(g.fullName) LIKE :query)) " +
           "AND (:status IS NULL OR s.status = :status) " +
           "AND (:classId IS NULL OR (e.schoolClass.id = :classId AND e.status = 'ATIVO')) " +
           "AND (:minBirthDate IS NULL OR s.birthDate >= :minBirthDate) " +
           "AND (:maxBirthDate IS NULL OR s.birthDate <= :maxBirthDate)")
    Page<Student> searchStudents(
            @Param("tenantId") UUID tenantId,
            @Param("query") String query,
            @Param("status") StudentStatus status,
            @Param("classId") UUID classId,
            @Param("minBirthDate") LocalDate minBirthDate,
            @Param("maxBirthDate") LocalDate maxBirthDate,
            Pageable pageable);

    List<Student> findByTenantIdAndFullNameIgnoreCase(UUID tenantId, String fullName);

    List<Student> findByFullNameIgnoreCase(String fullName);

    List<Student> findByTenantId(UUID tenantId);

    long countByTenantId(UUID tenantId);

    Optional<Student> findByIdAndTenantId(UUID id, UUID tenantId);
}
