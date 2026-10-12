package com.futmanager.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class ImportExportDtos {

    public record ImportRowDto(
            int lineNumber,
            String studentName,
            String birthDateStr,
            LocalDate parsedBirthDate,
            String enrollmentDateStr,
            LocalDate parsedEnrollmentDate,
            String guardianName,
            String guardianPhone,
            String guardianEmail,
            String relationship,
            String className,
            String notes,
            boolean valid,
            List<String> errors,
            boolean possibleDuplicate,
            String duplicateReason
    ) {}

    public record ImportPreviewResponse(
            int totalRows,
            int validRows,
            int invalidRows,
            int duplicateWarningsCount,
            List<ImportRowDto> rows
    ) {}

    public record ImportConfirmRequest(
            List<ImportRowDto> rows
    ) {}

    public record ImportResultResponse(
            int studentsImported,
            int guardiansImported,
            int enrollmentsCreated,
            String message
    ) {}
}
