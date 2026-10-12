package com.futmanager.repository;

import com.futmanager.domain.entity.AttendanceItem;
import com.futmanager.domain.enums.AttendanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AttendanceItemRepository extends JpaRepository<AttendanceItem, UUID> {

    List<AttendanceItem> findByAttendanceId(UUID attendanceId);

    Optional<AttendanceItem> findByAttendanceIdAndStudentId(UUID attendanceId, UUID studentId);

    @Query("SELECT ai FROM AttendanceItem ai " +
           "JOIN ai.attendance a " +
           "JOIN a.lesson l " +
           "WHERE ai.student.id = :studentId " +
           "AND a.status = 'FINALIZADA' " +
           "AND l.status = 'REALIZADA' " +
           "AND (:classId IS NULL OR l.schoolClass.id = :classId) " +
           "AND (:startDate IS NULL OR l.date >= :startDate) " +
           "AND (:endDate IS NULL OR l.date <= :endDate) " +
           "ORDER BY l.date ASC")
    List<AttendanceItem> findFinalizedItemsForStudent(
            @Param("studentId") UUID studentId,
            @Param("classId") UUID classId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT ai FROM AttendanceItem ai " +
           "JOIN ai.attendance a " +
           "JOIN a.lesson l " +
           "JOIN l.schoolClass c " +
           "WHERE a.status = 'FINALIZADA' " +
           "AND l.status = 'REALIZADA' " +
           "AND (:tenantId IS NULL OR c.tenant.id = :tenantId) " +
           "AND (:classId IS NULL OR c.id = :classId) " +
           "AND (:startDate IS NULL OR l.date >= :startDate) " +
           "AND (:endDate IS NULL OR l.date <= :endDate)")
    List<AttendanceItem> findFinalizedItems(
            @Param("tenantId") UUID tenantId,
            @Param("classId") UUID classId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
