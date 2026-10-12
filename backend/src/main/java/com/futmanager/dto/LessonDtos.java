package com.futmanager.dto;

import com.futmanager.domain.enums.AttendanceState;
import com.futmanager.domain.enums.LessonStatus;
import com.futmanager.domain.enums.LessonType;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public class LessonDtos {

    public record LessonCreateRequest(
            @NotNull(message = "A turma é obrigatória")
            UUID classId,

            @NotNull(message = "A data da aula é obrigatória")
            LocalDate date,

            @NotNull(message = "O horário inicial é obrigatório")
            LocalTime startTime,

            @NotNull(message = "O horário final é obrigatório")
            LocalTime endTime,

            LessonType type,

            String notes
    ) {}

    public record LessonRescheduleRequest(
            @NotNull(message = "A nova data é obrigatória")
            LocalDate newDate,

            @NotNull(message = "O novo horário inicial é obrigatório")
            LocalTime newStartTime,

            @NotNull(message = "O novo horário final é obrigatório")
            LocalTime newEndTime,

            String reason
    ) {}

    public record LessonCancelRequest(
            String reason
    ) {}

    public record LessonResponse(
            UUID id,
            UUID classId,
            String className,
            String category,
            LocalDate date,
            LocalTime startTime,
            LocalTime endTime,
            LessonStatus status,
            LessonType type,
            String notes,
            UUID attendanceId,
            AttendanceState attendanceStatus,
            int eligibleStudentsCount,
            boolean hasPendingAttendance
    ) {}

    public record GenerateLessonsRequest(
            Integer horizonDays
    ) {}

    public record GenerateLessonsResponse(
            int generatedCount,
            String message
    ) {}
}
