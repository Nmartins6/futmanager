package com.futmanager.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public class StatisticsDtos {

    public record DashboardOverviewResponse(
            long totalActiveStudents,
            long totalClasses,
            long pendingLessonsCount,
            long lowFrequencyStudentsCount,
            Double averageAttendancePercentage, // null se sem dados
            List<LessonDtos.LessonResponse> todayLessons,
            List<StudentFrequencySummaryDto> lowFrequencyAlerts
    ) {}

    public record StudentFrequencySummaryDto(
            UUID studentId,
            String studentName,
            int currentAge,
            String photoUrl,
            int totalLessons,
            int presences,
            int unexcusedAbsences,
            int excusedAbsences,
            Double attendancePercentage, // ex: 85.5% ou null se 0 aulas
            boolean belowThreshold,
            List<ClassBreakdownDto> classBreakdowns
    ) {}

    public record ClassBreakdownDto(
            UUID classId,
            String className,
            int totalLessons,
            int presences,
            int unexcusedAbsences,
            int excusedAbsences,
            Double attendancePercentage
    ) {}

    public record ClassComparisonDto(
            UUID classId,
            String className,
            String category,
            int totalLessonsCounted,
            int activeStudentsCount,
            int totalPresences,
            int totalAbsences,
            int totalExcused,
            Double averageAttendancePercentage
    ) {}

    public record MonthlyEvolutionDto(
            String monthYear, // ex: "2026-09", "2026-10"
            int totalLessons,
            int presences,
            int absences,
            int excusedAbsences,
            Double attendancePercentage
    ) {}

    public record StudentAttendanceHistoryItemDto(
            UUID lessonId,
            UUID classId,
            String className,
            LocalDate lessonDate,
            int ageOnLessonDate,
            String status, // PRESENTE, AUSENTE, FALTA_JUSTIFICADA
            String notes
    ) {}

    public record TeacherPerformanceDto(
            UUID teacherId,
            String teacherName,
            int totalClassesCount,
            int lessonsConducted,
            int attendancesFinalized,
            int attendancesPending
    ) {}
}
