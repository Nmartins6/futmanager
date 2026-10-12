package com.futmanager.dto;

import com.futmanager.domain.enums.AttendanceState;
import com.futmanager.domain.enums.AttendanceStatus;
import com.futmanager.domain.enums.AuditAction;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class AttendanceDtos {

    public record AttendanceSaveRequest(
            Long version, // Controle de concorrência otimista
            boolean finalizeAttendance, // true se estiver finalizando a chamada
            String changeReason, // Obrigatório quando alterar uma chamada já finalizada
            List<AttendanceItemInput> items
    ) {}

    public record AttendanceItemInput(
            @NotNull(message = "O ID do aluno é obrigatório")
            UUID studentId,
            AttendanceStatus status, // null se ainda não classificado no rascunho
            String notes // Justificativa ou observação
    ) {}

    public record AttendanceResponse(
            UUID id,
            UUID lessonId,
            UUID classId,
            String className,
            String category,
            LocalDate lessonDate,
            LocalTime startTime,
            LocalTime endTime,
            AttendanceState status,
            UUID startedByUserId,
            String startedByUserName,
            Instant startedAt,
            UUID finalizedByUserId,
            String finalizedByUserName,
            Instant finalizedAt,
            Long version,
            int totalEligibleStudents,
            int presentCount,
            int absentCount,
            int excusedCount,
            int unclassifiedCount,
            List<AttendanceStudentItemDto> students,
            List<AttendanceAuditLogDto> auditLogs
    ) {}

    public record AttendanceStudentItemDto(
            UUID studentId,
            String studentName,
            LocalDate birthDate,
            int ageOnLessonDate,
            String photoUrl,
            AttendanceStatus status,
            String notes
    ) {}

    public record AttendanceAuditLogDto(
            UUID id,
            AuditAction action,
            UUID changedByUserId,
            String changedByUserName,
            String reason,
            String details,
            Instant createdAt
    ) {}
}
