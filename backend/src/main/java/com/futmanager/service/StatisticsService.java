package com.futmanager.service;

import com.futmanager.config.UserPrincipal;
import com.futmanager.domain.entity.*;
import com.futmanager.domain.enums.*;
import com.futmanager.dto.LessonDtos.LessonResponse;
import com.futmanager.dto.StatisticsDtos.*;
import com.futmanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatisticsService {

    private final StudentRepository studentRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final LessonRepository lessonRepository;
    private final AttendanceItemRepository attendanceItemRepository;
    private final SchoolConfigService schoolConfigService;
    private final LessonService lessonService;
    private final UserService userService;

    /**
     * Visão Geral para o Dashboard com métricas consolidadas, aulas do dia e alertas.
     */
    @Transactional(readOnly = true)
    public DashboardOverviewResponse getDashboardOverview() {
        UserPrincipal principal = userService.getCurrentUserPrincipal();
        UUID teacherFilter = (principal != null && principal.getRole() == Role.PROFESSOR) ? principal.getId() : null;
        UUID tenantId = (principal != null) ? principal.getTenantId() : com.futmanager.config.TenantContext.getTenantId();

        long totalStudents = (tenantId != null) ? studentRepository.countByTenantId(tenantId) : studentRepository.count();
        long totalClasses = (tenantId != null) ? schoolClassRepository.countByTenantId(tenantId) : schoolClassRepository.count();

        // Aulas de hoje
        List<LessonResponse> todayLessons = lessonService.getLessonsOnDate(LocalDate.now());

        // Pendências: Aulas marcadas como REALIZADA cuja chamada ainda não foi FINALIZADA
        List<Lesson> pendingLessons = lessonRepository.findPendingAttendanceLessons(tenantId, null, null, null);
        if (teacherFilter != null) {
            pendingLessons = pendingLessons.stream()
                    .filter(l -> l.getSchoolClass().getTeachers().stream()
                            .anyMatch(t -> t.getUser().getId().equals(teacherFilter)))
                    .toList();
        }

        // Estatísticas de alunos e verificação do limite configurável
        double threshold = schoolConfigService.getLowFrequencyThreshold();
        List<StudentFrequencySummaryDto> allStudentStats = calculateAllStudentsFrequency(null, null, null, teacherFilter);

        List<StudentFrequencySummaryDto> lowFrequencyAlerts = allStudentStats.stream()
                .filter(s -> s.belowThreshold() && s.totalLessons() > 0)
                .toList();

        // Frequência média geral
        int sumPresences = allStudentStats.stream().mapToInt(StudentFrequencySummaryDto::presences).sum();
        int sumLessons = allStudentStats.stream().mapToInt(StudentFrequencySummaryDto::totalLessons).sum();
        Double generalPercentage = calculatePercentage(sumPresences, sumLessons);

        return new DashboardOverviewResponse(
                totalStudents,
                totalClasses,
                pendingLessons.size(),
                lowFrequencyAlerts.size(),
                generalPercentage,
                todayLessons,
                lowFrequencyAlerts
        );
    }

    /**
     * Calcula a frequência individual de um aluno, com histórico completo e quebra por turma.
     */
    @Transactional(readOnly = true)
    public StudentFrequencySummaryDto getStudentFrequency(UUID studentId, UUID classId, LocalDate startDate, LocalDate endDate) {
        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Aluno não encontrado: " + studentId));

        List<AttendanceItem> items = attendanceItemRepository.findFinalizedItemsForStudent(studentId, classId, startDate, endDate);

        double threshold = schoolConfigService.getLowFrequencyThreshold();
        return buildStudentFrequencySummary(student, items, threshold);
    }

    /**
     * Histórico detalhado de presenças de um aluno aula por aula.
     */
    @Transactional(readOnly = true)
    public List<StudentAttendanceHistoryItemDto> getStudentAttendanceHistory(
            UUID studentId, UUID classId, LocalDate startDate, LocalDate endDate) {

        List<AttendanceItem> items = attendanceItemRepository.findFinalizedItemsForStudent(studentId, classId, startDate, endDate);

        return items.stream()
                .map(item -> {
                    Lesson lesson = item.getAttendance().getLesson();
                    return new StudentAttendanceHistoryItemDto(
                            lesson.getId(),
                            lesson.getSchoolClass().getId(),
                            lesson.getSchoolClass().getName(),
                            lesson.getDate(),
                            item.getStudent().getAgeAt(lesson.getDate()),
                            item.getStatus().name(),
                            item.getNotes()
                    );
                })
                .sorted(Comparator.comparing(StudentAttendanceHistoryItemDto::lessonDate).reversed())
                .toList();
    }

    /**
     * Comparação de frequência entre turmas.
     */
    @Transactional(readOnly = true)
    public List<ClassComparisonDto> compareClasses(LocalDate startDate, LocalDate endDate) {
        UUID tenantId = com.futmanager.config.TenantContext.getTenantId();
        List<SchoolClass> classes = (tenantId != null)
                ? schoolClassRepository.findByTenantIdAndStatus(tenantId, ClassStatus.ATIVO)
                : schoolClassRepository.findByStatus(ClassStatus.ATIVO);
        List<ClassComparisonDto> result = new ArrayList<>();

        for (SchoolClass sc : classes) {
            List<AttendanceItem> items = attendanceItemRepository.findFinalizedItems(tenantId, sc.getId(), startDate, endDate);

            int presences = 0;
            int absences = 0;
            int excused = 0;

            Set<UUID> lessonIds = new HashSet<>();

            for (AttendanceItem item : items) {
                lessonIds.add(item.getAttendance().getLesson().getId());
                if (item.getStatus() == AttendanceStatus.PRESENTE) {
                    presences++;
                } else if (item.getStatus() == AttendanceStatus.AUSENTE) {
                    absences++;
                } else if (item.getStatus() == AttendanceStatus.FALTA_JUSTIFICADA) {
                    excused++;
                }
            }

            int totalCounted = presences + absences + excused;
            Double avgPercentage = calculatePercentage(presences, totalCounted);

            int activeStudents = (int) sc.getEnrollments().stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.ATIVO)
                    .count();

            result.add(new ClassComparisonDto(
                    sc.getId(),
                    sc.getName(),
                    sc.getCategory(),
                    lessonIds.size(),
                    activeStudents,
                    presences,
                    absences,
                    excused,
                    avgPercentage
            ));
        }

        return result;
    }

    /**
     * Evolução mensal da escolinha ou de uma turma específica.
     */
    @Transactional(readOnly = true)
    public List<MonthlyEvolutionDto> getMonthlyEvolution(UUID classId, Integer year) {
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        LocalDate startDate = LocalDate.of(targetYear, 1, 1);
        LocalDate endDate = LocalDate.of(targetYear, 12, 31);

        UUID tenantId = com.futmanager.config.TenantContext.getTenantId();
        List<AttendanceItem> items = attendanceItemRepository.findFinalizedItems(tenantId, classId, startDate, endDate);

        // Agrupar por "YYYY-MM"
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, List<AttendanceItem>> groupedByMonth = items.stream()
                .collect(Collectors.groupingBy(item -> item.getAttendance().getLesson().getDate().format(monthFormatter)));

        List<MonthlyEvolutionDto> evolution = new ArrayList<>();

        // Garantir ordenação cronológica
        List<String> months = new ArrayList<>(groupedByMonth.keySet());
        Collections.sort(months);

        for (String month : months) {
            List<AttendanceItem> monthItems = groupedByMonth.get(month);

            int presences = (int) monthItems.stream().filter(i -> i.getStatus() == AttendanceStatus.PRESENTE).count();
            int absences = (int) monthItems.stream().filter(i -> i.getStatus() == AttendanceStatus.AUSENTE).count();
            int excused = (int) monthItems.stream().filter(i -> i.getStatus() == AttendanceStatus.FALTA_JUSTIFICADA).count();

            Set<UUID> lessonsInMonth = monthItems.stream()
                    .map(i -> i.getAttendance().getLesson().getId())
                    .collect(Collectors.toSet());

            int totalCounted = presences + absences + excused;
            Double pct = calculatePercentage(presences, totalCounted);

            evolution.add(new MonthlyEvolutionDto(
                    month,
                    lessonsInMonth.size(),
                    presences,
                    absences,
                    excused,
                    pct
            ));
        }

        return evolution;
    }

    /**
     * Lista aulas com chamada pendente.
     */
    @Transactional(readOnly = true)
    public List<LessonResponse> getPendingLessons(UUID classId, LocalDate startDate, LocalDate endDate) {
        UserPrincipal principal = userService.getCurrentUserPrincipal();
        UUID teacherFilter = (principal != null && principal.getRole() == Role.PROFESSOR) ? principal.getId() : null;
        UUID tenantId = (principal != null) ? principal.getTenantId() : com.futmanager.config.TenantContext.getTenantId();

        List<Lesson> pending = lessonRepository.findPendingAttendanceLessons(tenantId, classId, startDate, endDate);
        if (teacherFilter != null) {
            pending = pending.stream()
                    .filter(l -> l.getSchoolClass().getTeachers().stream()
                            .anyMatch(t -> t.getUser().getId().equals(teacherFilter)))
                    .toList();
        }

        return pending.stream().map(lessonService::toResponse).toList();
    }

    /**
     * Calcula a frequência de todos os alunos respeitando filtros de turma e período.
     */
    public List<StudentFrequencySummaryDto> calculateAllStudentsFrequency(
            UUID classId, LocalDate startDate, LocalDate endDate, UUID teacherFilter) {

        UUID tenantId = com.futmanager.config.TenantContext.getTenantId();
        List<Student> students = (tenantId != null) ? studentRepository.findByTenantId(tenantId) : studentRepository.findAll();
        List<AttendanceItem> allItems = attendanceItemRepository.findFinalizedItems(tenantId, classId, startDate, endDate);

        double threshold = schoolConfigService.getLowFrequencyThreshold();

        Map<UUID, List<AttendanceItem>> itemsByStudent = allItems.stream()
                .collect(Collectors.groupingBy(i -> i.getStudent().getId()));

        List<StudentFrequencySummaryDto> result = new ArrayList<>();

        for (Student s : students) {
            // Se houver filtro de turma, ignora alunos que nunca tiveram matrícula nessa turma
            if (classId != null) {
                boolean enrolled = s.getEnrollments().stream().anyMatch(e -> e.getSchoolClass().getId().equals(classId));
                if (!enrolled) continue;
            }

            // Se for professor, ignora alunos de turmas não atribuídas a ele
            if (teacherFilter != null) {
                boolean assigned = s.getEnrollments().stream()
                        .anyMatch(e -> e.getSchoolClass().getTeachers().stream()
                                .anyMatch(t -> t.getUser().getId().equals(teacherFilter)));
                if (!assigned) continue;
            }

            List<AttendanceItem> studentItems = itemsByStudent.getOrDefault(s.getId(), Collections.emptyList());
            result.add(buildStudentFrequencySummary(s, studentItems, threshold));
        }

        result.sort(Comparator.comparing(StudentFrequencySummaryDto::studentName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private StudentFrequencySummaryDto buildStudentFrequencySummary(
            Student student, List<AttendanceItem> items, double threshold) {

        int presences = 0;
        int unexcused = 0;
        int excused = 0;

        // Quebra por turma
        Map<UUID, List<AttendanceItem>> itemsByClass = new HashMap<>();

        for (AttendanceItem item : items) {
            UUID cId = item.getAttendance().getLesson().getSchoolClass().getId();
            itemsByClass.computeIfAbsent(cId, k -> new ArrayList<>()).add(item);

            if (item.getStatus() == AttendanceStatus.PRESENTE) {
                presences++;
            } else if (item.getStatus() == AttendanceStatus.AUSENTE) {
                unexcused++;
            } else if (item.getStatus() == AttendanceStatus.FALTA_JUSTIFICADA) {
                excused++;
            }
        }

        int totalCounted = presences + unexcused + excused;
        // Regra consolidada: Soma de presenças / Soma de aulas contabilizadas (NUNCA a média simples das % das turmas)
        Double consolidatedPercentage = calculatePercentage(presences, totalCounted);

        boolean isBelowThreshold = consolidatedPercentage != null && consolidatedPercentage < threshold;

        // Construir quebra por turma
        List<ClassBreakdownDto> breakdowns = new ArrayList<>();
        for (Map.Entry<UUID, List<AttendanceItem>> entry : itemsByClass.entrySet()) {
            List<AttendanceItem> classItems = entry.getValue();
            String className = classItems.get(0).getAttendance().getLesson().getSchoolClass().getName();

            int cp = (int) classItems.stream().filter(i -> i.getStatus() == AttendanceStatus.PRESENTE).count();
            int cu = (int) classItems.stream().filter(i -> i.getStatus() == AttendanceStatus.AUSENTE).count();
            int ce = (int) classItems.stream().filter(i -> i.getStatus() == AttendanceStatus.FALTA_JUSTIFICADA).count();
            int ct = cp + cu + ce;

            breakdowns.add(new ClassBreakdownDto(
                    entry.getKey(),
                    className,
                    ct,
                    cp,
                    cu,
                    ce,
                    calculatePercentage(cp, ct)
            ));
        }

        return new StudentFrequencySummaryDto(
                student.getId(),
                student.getFullName(),
                student.getAge(),
                student.getPhotoUrl(),
                totalCounted,
                presences,
                unexcused,
                excused,
                consolidatedPercentage,
                isBelowThreshold,
                breakdowns
        );
    }

    /**
     * Calcula o percentual com arredondamento seguro em 1 casa decimal.
     * Retorna null quando o total for 0 (evitando divisão por zero).
     */
    public Double calculatePercentage(int presences, int totalCounted) {
        if (totalCounted <= 0) {
            return null; // Sem dados
        }
        double val = ((double) presences / totalCounted) * 100.0;
        return BigDecimal.valueOf(val).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
