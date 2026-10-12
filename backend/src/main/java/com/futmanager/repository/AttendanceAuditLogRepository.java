package com.futmanager.repository;

import com.futmanager.domain.entity.AttendanceAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AttendanceAuditLogRepository extends JpaRepository<AttendanceAuditLog, UUID> {

    List<AttendanceAuditLog> findByAttendanceIdOrderByCreatedAtDesc(UUID attendanceId);
}
