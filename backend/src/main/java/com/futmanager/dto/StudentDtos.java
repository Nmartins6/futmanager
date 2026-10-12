package com.futmanager.dto;

import com.futmanager.domain.enums.EnrollmentStatus;
import com.futmanager.domain.enums.StudentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class StudentDtos {

    public record StudentRequest(
            @NotBlank(message = "O nome completo do aluno é obrigatório")
            @Size(max = 255, message = "O nome não pode exceder 255 caracteres")
            String fullName,

            @NotNull(message = "A data de nascimento é obrigatória")
            @PastOrPresent(message = "A data de nascimento não pode estar no futuro")
            LocalDate birthDate,

            String photoUrl,

            @NotNull(message = "A situação do aluno é obrigatória")
            StudentStatus status,

            @NotNull(message = "A data de ingresso na escolinha é obrigatória")
            LocalDate enrollmentDate,

            String notes,

            List<StudentGuardianLinkInput> guardians,

            List<UUID> classIds
    ) {}

    public record StudentGuardianLinkInput(
            UUID guardianId,
            String guardianFullName,
            String guardianPhone,
            @jakarta.validation.constraints.Pattern(
                regexp = "^$|^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,7}$",
                message = "Formato de e-mail inválido. Digite um e-mail válido como usuario@exemplo.com"
            )
            String guardianEmail,
            @NotBlank(message = "O parentesco/vínculo é obrigatório")
            String relationship,
            boolean mainContact,
            boolean emergencyContact
    ) {}

    public record StudentResponse(
            UUID id,
            String fullName,
            LocalDate birthDate,
            int age,
            String photoUrl,
            StudentStatus status,
            LocalDate enrollmentDate,
            String notes,
            List<StudentGuardianResponse> guardians,
            List<EnrollmentSummaryDto> enrollments,
            Instant createdAt
    ) {}

    public record StudentSummaryResponse(
            UUID id,
            String fullName,
            LocalDate birthDate,
            int age,
            String photoUrl,
            StudentStatus status,
            LocalDate enrollmentDate,
            List<String> currentClasses
    ) {}

    public record StudentGuardianResponse(
            UUID guardianId,
            String fullName,
            String phone,
            String email,
            String relationship,
            boolean mainContact,
            boolean emergencyContact
    ) {}

    public record EnrollmentSummaryDto(
            UUID enrollmentId,
            UUID classId,
            String className,
            String category,
            LocalDate entryDate,
            LocalDate exitDate,
            EnrollmentStatus status
    ) {}
}
