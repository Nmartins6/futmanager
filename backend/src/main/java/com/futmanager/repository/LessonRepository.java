package com.futmanager.repository;

import com.futmanager.domain.entity.Lesson;
import com.futmanager.domain.enums.LessonStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, UUID> {

    Optional<Lesson> findBySchoolClassIdAndDateAndStartTime(UUID classId, LocalDate date, LocalTime startTime);

    List<Lesson> findBySchoolClassIdAndDateBetweenOrderByDateAscStartTimeAsc(UUID classId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT DISTINCT l FROM Lesson l " +
           "JOIN l.schoolClass c " +
           "LEFT JOIN c.teachers ct " +
           "WHERE (:tenantId IS NULL OR c.tenant.id = :tenantId) " +
           "AND l.date = :date " +
           "AND (:teacherId IS NULL OR ct.user.id = :teacherId) " +
           "ORDER BY l.startTime ASC")
    List<Lesson> findLessonsOnDate(
            @Param("tenantId") UUID tenantId,
            @Param("date") LocalDate date,
            @Param("teacherId") UUID teacherId);

    @Query("SELECT l FROM Lesson l " +
           "JOIN l.schoolClass c " +
           "LEFT JOIN l.attendance a " +
           "WHERE (:tenantId IS NULL OR c.tenant.id = :tenantId) " +
           "AND l.status = com.futmanager.domain.enums.LessonStatus.REALIZADA " +
           "AND (a IS NULL OR a.status = com.futmanager.domain.enums.AttendanceState.RASCUNHO) " +
           "AND (:classId IS NULL OR c.id = :classId) " +
           "AND l.date BETWEEN :startDate AND :endDate " +
           "ORDER BY l.date DESC")
    List<Lesson> findPendingAttendanceLessons(
            @Param("tenantId") UUID tenantId,
            @Param("classId") UUID classId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("SELECT DISTINCT l FROM Lesson l " +
           "JOIN l.schoolClass c " +
           "LEFT JOIN c.teachers ct " +
           "WHERE (:tenantId IS NULL OR c.tenant.id = :tenantId) " +
           "AND (:classId IS NULL OR c.id = :classId) " +
           "AND (:status IS NULL OR l.status = :status) " +
           "AND l.date BETWEEN :startDate AND :endDate " +
           "AND (:teacherId IS NULL OR ct.user.id = :teacherId) " +
           "ORDER BY l.date DESC, l.startTime DESC")
    Page<Lesson> searchLessons(
            @Param("tenantId") UUID tenantId,
            @Param("classId") UUID classId,
            @Param("status") LessonStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("teacherId") UUID teacherId,
            Pageable pageable);
}
