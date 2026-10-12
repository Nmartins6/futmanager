package com.futmanager.service;

import com.futmanager.config.UserPrincipal;
import com.futmanager.domain.entity.*;
import com.futmanager.domain.enums.AttendanceState;
import com.futmanager.domain.enums.ClassStatus;
import com.futmanager.domain.enums.LessonStatus;
import com.futmanager.domain.enums.LessonType;
import com.futmanager.domain.enums.Role;
import com.futmanager.dto.LessonDtos.*;
import com.futmanager.exception.BusinessException;
import com.futmanager.exception.ConflictException;
import com.futmanager.exception.ResourceNotFoundException;
import com.futmanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LessonService {

    private final LessonRepository lessonRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final SchoolClassService schoolClassService;
    private final SchoolConfigService schoolConfigService;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<LessonResponse> searchLessons(
            UUID classId,
            LessonStatus status,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {

        UserPrincipal principal = userService.getCurrentUserPrincipal();
        UUID teacherFilter = (principal != null && principal.getRole() == Role.PROFESSOR) ? principal.getId() : null;
        UUID tenantId = (principal != null) ? principal.getTenantId() : com.futmanager.config.TenantContext.getTenantId();

        return lessonRepository.searchLessons(tenantId, classId, status, startDate, endDate, teacherFilter, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<LessonResponse> getLessonsOnDate(LocalDate date) {
        UserPrincipal principal = userService.getCurrentUserPrincipal();
        UUID teacherFilter = (principal != null && principal.getRole() == Role.PROFESSOR) ? principal.getId() : null;
        UUID tenantId = (principal != null) ? principal.getTenantId() : com.futmanager.config.TenantContext.getTenantId();

        return lessonRepository.findLessonsOnDate(tenantId, date, teacherFilter)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LessonResponse getLessonById(UUID id) {
        Lesson lesson = lessonRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Aula não encontrada com ID: " + id));

        schoolClassService.validateTeacherAccessToClass(lesson.getSchoolClass());

        return toResponse(lesson);
    }

    @Transactional
    public LessonResponse createLesson(LessonCreateRequest request) {
        SchoolClass schoolClass = schoolClassRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada: " + request.classId()));

        schoolClassService.validateTeacherAccessToClass(schoolClass);

        if (request.endTime().isBefore(request.startTime()) || request.endTime().equals(request.startTime())) {
            throw new BusinessException("O horário final deve ser posterior ao horário inicial.");
        }

        // Verificar unicidade
        Optional<Lesson> existing = lessonRepository.findBySchoolClassIdAndDateAndStartTime(
                schoolClass.getId(), request.date(), request.startTime()
        );
        if (existing.isPresent()) {
            throw new ConflictException("Já existe uma aula agendada para esta turma nesta data e horário.");
        }

        Lesson lesson = Lesson.builder()
                .schoolClass(schoolClass)
                .date(request.date())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .status(LessonStatus.AGENDADA)
                .type(request.type() != null ? request.type() : LessonType.EXTRA)
                .notes(request.notes())
                .build();

        lesson = lessonRepository.save(lesson);
        log.info("Aula criada manualmente: Turma {} em {} {}-{}", schoolClass.getName(), lesson.getDate(), lesson.getStartTime(), lesson.getEndTime());
        return toResponse(lesson);
    }

    @Transactional
    public LessonResponse cancelLesson(UUID lessonId, LessonCancelRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Aula não encontrada com ID: " + lessonId));

        schoolClassService.validateTeacherAccessToClass(lesson.getSchoolClass());

        if (lesson.getStatus() == LessonStatus.REALIZADA) {
            throw new BusinessException("Aulas já marcadas como realizadas não podem ser canceladas. É necessário reverter ou ajustar a chamada antes.");
        }

        lesson.setStatus(LessonStatus.CANCELADA);
        String reason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? " [Motivo cancelamento: " + request.reason() + "]"
                : " [Cancelada]";
        lesson.setNotes((lesson.getNotes() != null ? lesson.getNotes() : "") + reason);

        lesson = lessonRepository.save(lesson);
        log.info("Aula {} cancelada. Notas: {}", lesson.getId(), lesson.getNotes());
        return toResponse(lesson);
    }

    @Transactional
    public LessonResponse rescheduleLesson(UUID lessonId, LessonRescheduleRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Aula não encontrada com ID: " + lessonId));

        schoolClassService.validateTeacherAccessToClass(lesson.getSchoolClass());

        if (lesson.getAttendance() != null && lesson.getAttendance().getStatus() == AttendanceState.FINALIZADA) {
            throw new BusinessException("Aulas com chamada já finalizada não podem ser remarcadas.");
        }

        if (request.newEndTime().isBefore(request.newStartTime()) || request.newEndTime().equals(request.newStartTime())) {
            throw new BusinessException("O novo horário final deve ser posterior ao novo horário inicial.");
        }

        // Verifica colisão
        Optional<Lesson> existing = lessonRepository.findBySchoolClassIdAndDateAndStartTime(
                lesson.getSchoolClass().getId(), request.newDate(), request.newStartTime()
        );
        if (existing.isPresent() && !existing.get().getId().equals(lesson.getId())) {
            throw new ConflictException("Já existe uma aula agendada para esta turma no novo horário informado.");
        }

        LocalDate oldDate = lesson.getDate();
        LocalTime oldTime = lesson.getStartTime();

        lesson.setDate(request.newDate());
        lesson.setStartTime(request.newStartTime());
        lesson.setEndTime(request.newEndTime());
        lesson.setType(LessonType.REMARCADA);

        String reason = (request.reason() != null && !request.reason().isBlank()) ? request.reason() : "Ajuste de calendário";
        String note = String.format(" [Remarcada de %s %s. Motivo: %s]", oldDate, oldTime, reason);
        lesson.setNotes((lesson.getNotes() != null ? lesson.getNotes() : "") + note);

        lesson = lessonRepository.save(lesson);
        return toResponse(lesson);
    }

    /**
     * Geração idempotente de aulas futuras com base nos horários recorrentes das turmas ativas.
     * Não duplica aulas existentes e respeita o horizonte configurável.
     */
    @Transactional
    public GenerateLessonsResponse generateLessonsForHorizon(Integer customHorizonDays) {
        int horizon = (customHorizonDays != null && customHorizonDays > 0)
                ? customHorizonDays
                : schoolConfigService.getLessonHorizonDays();

        LocalDate today = LocalDate.now();
        LocalDate targetEndDate = today.plusDays(horizon);

        UUID currentTenantId = com.futmanager.config.TenantContext.getTenantId();
        List<SchoolClass> activeClasses = (currentTenantId != null)
                ? schoolClassRepository.findByTenantIdAndStatus(currentTenantId, ClassStatus.ATIVO)
                : schoolClassRepository.findByStatus(ClassStatus.ATIVO);
        int generatedCount = 0;

        for (SchoolClass sc : activeClasses) {
            if (sc.getSchedules() == null || sc.getSchedules().isEmpty()) {
                continue;
            }

            LocalDate cursor = today;
            while (!cursor.isAfter(targetEndDate)) {
                // Verificar se a turma estava ativa nesta data
                if (sc.getStartDate() != null && cursor.isBefore(sc.getStartDate())) {
                    cursor = cursor.plusDays(1);
                    continue;
                }
                if (sc.getEndDate() != null && cursor.isAfter(sc.getEndDate())) {
                    cursor = cursor.plusDays(1);
                    continue;
                }

                java.time.DayOfWeek dayOfWeek = cursor.getDayOfWeek();

                for (ClassSchedule schedule : sc.getSchedules()) {
                    if (schedule.getDayOfWeek() == dayOfWeek) {
                        // Idempotência: só cria se não existir aula com a mesma turma, data e horário
                        Optional<Lesson> existing = lessonRepository.findBySchoolClassIdAndDateAndStartTime(
                                sc.getId(), cursor, schedule.getStartTime()
                        );

                        if (existing.isEmpty()) {
                            Lesson newLesson = Lesson.builder()
                                    .schoolClass(sc)
                                    .date(cursor)
                                    .startTime(schedule.getStartTime())
                                    .endTime(schedule.getEndTime())
                                    .status(LessonStatus.AGENDADA)
                                    .type(LessonType.REGULAR)
                                    .notes("Aula regular gerada automaticamente")
                                    .build();

                            lessonRepository.save(newLesson);
                            generatedCount++;
                        }
                    }
                }

                cursor = cursor.plusDays(1);
            }
        }

        log.info("Geração idempotente concluída: {} aulas geradas até {}", generatedCount, targetEndDate);
        return new GenerateLessonsResponse(
                generatedCount,
                String.format("Geração idempotente concluída: %d novas aulas geradas para o horizonte de %d dias (até %s).",
                        generatedCount, horizon, targetEndDate)
        );
    }

    public LessonResponse toResponse(Lesson l) {
        UUID attendanceId = l.getAttendance() != null ? l.getAttendance().getId() : null;
        AttendanceState attendanceState = l.getAttendance() != null ? l.getAttendance().getStatus() : null;

        int eligibleCount = classEnrollmentRepository.findEligibleEnrollmentsOnDate(l.getSchoolClass().getId(), l.getDate()).size();
        boolean isPending = l.getStatus() == LessonStatus.REALIZADA && (attendanceState == null || attendanceState == AttendanceState.RASCUNHO);

        return new LessonResponse(
                l.getId(),
                l.getSchoolClass().getId(),
                l.getSchoolClass().getName(),
                l.getSchoolClass().getCategory(),
                l.getDate(),
                l.getStartTime(),
                l.getEndTime(),
                l.getStatus(),
                l.getType(),
                l.getNotes(),
                attendanceId,
                attendanceState,
                eligibleCount,
                isPending
        );
    }
}
