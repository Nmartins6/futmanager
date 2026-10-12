package com.futmanager.dto;

import com.futmanager.domain.enums.ClassStatus;
import com.futmanager.domain.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public class SchoolClassDtos {

    public record SchoolClassRequest(
            @NotBlank(message = "O nome da turma é obrigatório")
            String name,

            @NotBlank(message = "A categoria ou faixa etária é obrigatória")
            String category,

            String location,

            Integer maxCapacity,

            @NotNull(message = "A situação da turma é obrigatória")
            ClassStatus status,

            LocalDate startDate,

            LocalDate endDate,

            List<TeacherAssignmentInput> teachers,

            List<ClassScheduleInput> schedules
    ) {}

    public record TeacherAssignmentInput(
            @NotNull(message = "O ID do professor é obrigatório")
            UUID userId,
            boolean substitute
    ) {}

    public record ClassScheduleInput(
            @NotNull(message = "O dia da semana é obrigatório")
            DayOfWeek dayOfWeek,

            @NotNull(message = "O horário inicial é obrigatório")
            LocalTime startTime,

            @NotNull(message = "O horário final é obrigatório")
            LocalTime endTime
    ) {}

    public record SchoolClassResponse(
            UUID id,
            String name,
            String category,
            String location,
            Integer maxCapacity,
            ClassStatus status,
            LocalDate startDate,
            LocalDate endDate,
            List<TeacherSummaryDto> teachers,
            List<ClassScheduleDto> schedules,
            int activeStudentsCount
    ) {}

    public record TeacherSummaryDto(
            UUID userId,
            String fullName,
            String email,
            boolean substitute
    ) {}

    public record ClassScheduleDto(
            UUID id,
            DayOfWeek dayOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {}

    public record EnrollmentCreateRequest(
            @NotNull(message = "O aluno é obrigatório")
            UUID studentId,

            @NotNull(message = "A turma é obrigatória")
            UUID classId,

            @NotNull(message = "A data de entrada é obrigatória")
            LocalDate entryDate,

            LocalDate exitDate,

            EnrollmentStatus status
    ) {}

    public record EnrollmentUpdateRequest(
            LocalDate entryDate,
            LocalDate exitDate,
            @NotNull(message = "A situação do vínculo é obrigatória")
            EnrollmentStatus status
    ) {}

    public record ClassEnrollmentDto(
            UUID id,
            UUID studentId,
            String studentName,
            LocalDate studentBirthDate,
            int studentAge,
            String studentPhotoUrl,
            UUID classId,
            String className,
            LocalDate entryDate,
            LocalDate exitDate,
            EnrollmentStatus status
    ) {}
}
