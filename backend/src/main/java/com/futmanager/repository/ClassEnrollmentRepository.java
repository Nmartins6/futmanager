package com.futmanager.repository;

import com.futmanager.domain.entity.ClassEnrollment;
import com.futmanager.domain.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ClassEnrollmentRepository extends JpaRepository<ClassEnrollment, UUID> {

    List<ClassEnrollment> findByStudentId(UUID studentId);

    List<ClassEnrollment> findBySchoolClassId(UUID classId);

    List<ClassEnrollment> findBySchoolClassIdAndStatus(UUID classId, EnrollmentStatus status);

    @Query("SELECT e FROM ClassEnrollment e " +
           "JOIN FETCH e.student s " +
           "WHERE e.schoolClass.id = :classId " +
           "AND e.entryDate <= :date " +
           "AND (e.exitDate IS NULL OR e.exitDate >= :date)")
    List<ClassEnrollment> findEligibleEnrollmentsOnDate(@Param("classId") UUID classId, @Param("date") LocalDate date);

    @Query("SELECT e FROM ClassEnrollment e " +
           "WHERE e.student.id = :studentId " +
           "AND e.schoolClass.id = :classId " +
           "AND (:excludeId IS NULL OR e.id <> :excludeId) " +
           "AND (e.entryDate <= :newExitDate OR :newExitDate IS NULL) " +
           "AND (e.exitDate >= :newEntryDate OR e.exitDate IS NULL)")
    List<ClassEnrollment> findOverlappingEnrollments(
            @Param("studentId") UUID studentId,
            @Param("classId") UUID classId,
            @Param("newEntryDate") LocalDate newEntryDate,
            @Param("newExitDate") LocalDate newExitDate,
            @Param("excludeId") UUID excludeId);

    Optional<ClassEnrollment> findFirstByStudentIdAndSchoolClassIdAndStatus(UUID studentId, UUID classId, EnrollmentStatus status);
}
