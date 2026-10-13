package com.futmanager.service;

import com.futmanager.domain.entity.*;
import com.futmanager.domain.enums.*;
import com.futmanager.dto.AttendanceDtos.*;
import com.futmanager.exception.BusinessException;
import com.futmanager.exception.ConflictException;
import com.futmanager.exception.ResourceNotFoundException;
import com.futmanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final AttendanceItemRepository attendanceItemRepository;
    private final AttendanceAuditLogRepository auditLogRepository;
    private final LessonRepository lessonRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final StudentRepository studentRepository;
    private final SchoolClassService schoolClassService;
    private final UserService userService;

    /**
     * Obtém ou inicializa a chamada de uma aula.
     * Mostra os alunos elegíveis com base na data da aula.
     */
    @Transactional
    public AttendanceResponse getOrInitAttendance(UUID lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Aula não encontrada com ID: " + lessonId));

        schoolClassService.validateTeacherAccessToClass(lesson.getSchoolClass());

        User currentUser = userService.getCurrentUserEntity();

        Optional<Attendance> attendanceOpt = attendanceRepository.findByLessonId(lessonId);

        Attendance attendance;
        if (attendanceOpt.isPresent()) {
            attendance = attendanceOpt.get();
        } else {
            // Inicializar primeira chamada em estado RASCUNHO
            attendance = Attendance.builder()
                    .lesson(lesson)
                    .status(AttendanceState.RASCUNHO)
                    .startedByUser(currentUser)
                    .startedAt(Instant.now())
                    .version(0L)
                    .build();

            attendance = attendanceRepository.save(attendance);

            // Log de auditoria de inicialização
            AttendanceAuditLog logEntry = AttendanceAuditLog.builder()
                    .attendance(attendance)
                    .action(AuditAction.INICIADA)
                    .changedByUser(currentUser)
                    .reason("Chamada iniciada")
                    .details("Chamada aberta para preenchimento")
                    .build();
            auditLogRepository.save(logEntry);
        }

        return buildAttendanceResponse(attendance);
    }

    /**
     * Salva ou finaliza a chamada com controle estrito de autoria e concorrência otimista.
     */
    @Transactional
    public AttendanceResponse saveAttendance(UUID lessonId, AttendanceSaveRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Aula não encontrada com ID: " + lessonId));

        schoolClassService.validateTeacherAccessToClass(lesson.getSchoolClass());

        User currentUser = userService.getCurrentUserEntity();

        Attendance attendance = attendanceRepository.findByLessonId(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Chamada não inicializada para esta aula."));

        // Proteção contra sobrescrita concorrente (Optimistic Locking)
        if (request.version() != null && !request.version().equals(attendance.getVersion())) {
            throw new ConflictException(
                    "Conflito de edição simultânea: A chamada foi modificada por outro usuário após o carregamento desta página. Recarregue para obter os dados atuais."
            );
        }

        // Buscar alunos elegíveis na data da aula
        List<ClassEnrollment> eligibleEnrollments = classEnrollmentRepository.findEligibleEnrollmentsOnDate(
                lesson.getSchoolClass().getId(), lesson.getDate()
        );
        Set<UUID> eligibleStudentIds = eligibleEnrollments.stream()
                .map(e -> e.getStudent().getId())
                .collect(Collectors.toSet());

        // Mapear itens atuais para comparação e auditoria
        Map<UUID, AttendanceItem> currentItemsMap = attendance.getItems().stream()
                .collect(Collectors.toMap(item -> item.getStudent().getId(), item -> item));

        List<String> auditChanges = new ArrayList<>();
        int classifiedCount = 0;

        if (request.items() != null) {
            for (AttendanceItemInput input : request.items()) {
                if (input.studentId() == null) continue;

                AttendanceItem item = currentItemsMap.get(input.studentId());

                if (item == null) {
                    // Novo item
                    UUID tenantId = currentUser.getTenant().getId();
                    Student student = studentRepository.findByIdAndTenantId(input.studentId(), tenantId)
                            .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado: " + input.studentId()));

                    item = AttendanceItem.builder()
                            .attendance(attendance)
                            .student(student)
                            .status(input.status() != null ? input.status() : AttendanceStatus.PRESENTE)
                            .notes(input.notes())
                            .build();

                    attendance.getItems().add(item);
                    currentItemsMap.put(student.getId(), item);
                    auditChanges.add(String.format("Aluno %s registrado como %s", student.getFullName(), item.getStatus()));
                } else {
                    // Alteração
                    if (input.status() != null && item.getStatus() != input.status()) {
                        auditChanges.add(String.format("Aluno %s alterado de %s para %s",
                                item.getStudent().getFullName(), item.getStatus(), input.status()));
                        item.setStatus(input.status());
                    }
                    if (input.notes() != null && !input.notes().equals(item.getNotes())) {
                        item.setNotes(input.notes());
                    }
                }

                if (item.getStatus() != null) {
                    classifiedCount++;
                }
            }
        }

        boolean wasAlreadyFinalized = attendance.getStatus() == AttendanceState.FINALIZADA;

        if (request.finalizeAttendance()) {
            // Regra crítica: Finalizar somente quando TODOS os alunos elegíveis estiverem classificados
            for (UUID studentId : eligibleStudentIds) {
                AttendanceItem item = currentItemsMap.get(studentId);
                if (item == null || item.getStatus() == null) {
                    throw new BusinessException(
                            "Para finalizar a chamada, todos os alunos da turma devem ser classificados (Presente, Ausente ou Falta Justificada)."
                    );
                }
            }

            attendance.setStatus(AttendanceState.FINALIZADA);
            attendance.setFinalizedByUser(currentUser);
            attendance.setFinalizedAt(Instant.now());

            // Atualiza status da aula para REALIZADA
            if (lesson.getStatus() != LessonStatus.REALIZADA) {
                lesson.setStatus(LessonStatus.REALIZADA);
                lessonRepository.save(lesson);
            }

            String auditDetail = String.format("Chamada finalizada por %s. Total de presentes: %d",
                    currentUser.getFullName(),
                    attendance.getItems().stream().filter(i -> i.getStatus() == AttendanceStatus.PRESENTE).count());

            AttendanceAuditLog logEntry = AttendanceAuditLog.builder()
                    .attendance(attendance)
                    .action(AuditAction.FINALIZADA)
                    .changedByUser(currentUser)
                    .reason(request.changeReason() != null ? request.changeReason() : "Chamada finalizada normalmente")
                    .details(auditDetail)
                    .build();
            auditLogRepository.save(logEntry);

        } else if (wasAlreadyFinalized) {
            // Edição / Correção de chamada já finalizada: exige motivo e grava auditoria detalhada
            if (request.changeReason() == null || request.changeReason().isBlank()) {
                throw new BusinessException("É obrigatório informar o motivo da alteração de uma chamada já finalizada.");
            }

            String changeDetails = auditChanges.isEmpty() ? "Observações ou justificativas ajustadas" : String.join("; ", auditChanges);

            AttendanceAuditLog logEntry = AttendanceAuditLog.builder()
                    .attendance(attendance)
                    .action(AuditAction.ALTERADA)
                    .changedByUser(currentUser)
                    .reason(request.changeReason().trim())
                    .details(changeDetails)
                    .build();
            auditLogRepository.save(logEntry);

        } else {
            // Salvar rascunho
            AttendanceAuditLog logEntry = AttendanceAuditLog.builder()
                    .attendance(attendance)
                    .action(AuditAction.SALVO_RASCUNHO)
                    .changedByUser(currentUser)
                    .reason("Rascunho salvo")
                    .details(String.format("%d alunos marcados até o momento", classifiedCount))
                    .build();
            auditLogRepository.save(logEntry);
        }

        try {
            attendance = attendanceRepository.save(attendance);
        } catch (OptimisticLockingFailureException e) {
            throw new ConflictException("Outro usuário acabou de atualizar esta chamada. Recarregue a página antes de salvar.");
        }

        log.info("Chamada da aula {} salva com sucesso. Situação: {}, Autor: {}",
                lesson.getId(), attendance.getStatus(), currentUser.getEmail());

        return buildAttendanceResponse(attendance);
    }

    private AttendanceResponse buildAttendanceResponse(Attendance attendance) {
        Lesson lesson = attendance.getLesson();
        LocalDate lessonDate = lesson.getDate();

        // 1. Alunos elegíveis pela matrícula na data da aula
        List<ClassEnrollment> eligibleEnrollments = classEnrollmentRepository.findEligibleEnrollmentsOnDate(
                lesson.getSchoolClass().getId(), lessonDate
        );

        // Mapa de itens gravados
        Map<UUID, AttendanceItem> itemsMap = attendance.getItems().stream()
                .collect(Collectors.toMap(i -> i.getStudent().getId(), i -> i));

        // Lista unificada: alunos elegíveis + alunos históricos já registrados na chamada
        Map<UUID, Student> allStudentsMap = new LinkedHashMap<>();
        for (ClassEnrollment e : eligibleEnrollments) {
            allStudentsMap.put(e.getStudent().getId(), e.getStudent());
        }
        for (AttendanceItem item : attendance.getItems()) {
            allStudentsMap.putIfAbsent(item.getStudent().getId(), item.getStudent());
        }

        List<AttendanceStudentItemDto> studentItems = new ArrayList<>();
        int presentCount = 0;
        int absentCount = 0;
        int excusedCount = 0;
        int unclassifiedCount = 0;

        for (Student student : allStudentsMap.values()) {
            AttendanceItem item = itemsMap.get(student.getId());

            AttendanceStatus status = item != null ? item.getStatus() : null;
            String notes = item != null ? item.getNotes() : null;

            if (status == AttendanceStatus.PRESENTE) {
                presentCount++;
            } else if (status == AttendanceStatus.AUSENTE) {
                absentCount++;
            } else if (status == AttendanceStatus.FALTA_JUSTIFICADA) {
                excusedCount++;
            } else {
                unclassifiedCount++;
            }

            studentItems.add(new AttendanceStudentItemDto(
                    student.getId(),
                    student.getFullName(),
                    student.getBirthDate(),
                    student.getAgeAt(lessonDate),
                    student.getPhotoUrl(),
                    status,
                    notes
            ));
        }

        // Ordenar alfabeticamente pelo nome do aluno
        studentItems.sort(Comparator.comparing(AttendanceStudentItemDto::studentName, String.CASE_INSENSITIVE_ORDER));

        // Histórico de auditoria
        List<AttendanceAuditLogDto> auditLogs = auditLogRepository.findByAttendanceIdOrderByCreatedAtDesc(attendance.getId())
                .stream()
                .map(log -> new AttendanceAuditLogDto(
                        log.getId(),
                        log.getAction(),
                        log.getChangedByUser().getId(),
                        log.getChangedByUser().getFullName(),
                        log.getReason(),
                        log.getDetails(),
                        log.getCreatedAt()
                ))
                .toList();

        return new AttendanceResponse(
                attendance.getId(),
                lesson.getId(),
                lesson.getSchoolClass().getId(),
                lesson.getSchoolClass().getName(),
                lesson.getSchoolClass().getCategory(),
                lessonDate,
                lesson.getStartTime(),
                lesson.getEndTime(),
                attendance.getStatus(),
                attendance.getStartedByUser().getId(),
                attendance.getStartedByUser().getFullName(),
                attendance.getStartedAt(),
                attendance.getFinalizedByUser() != null ? attendance.getFinalizedByUser().getId() : null,
                attendance.getFinalizedByUser() != null ? attendance.getFinalizedByUser().getFullName() : null,
                attendance.getFinalizedAt(),
                attendance.getVersion(),
                studentItems.size(),
                presentCount,
                absentCount,
                excusedCount,
                unclassifiedCount,
                studentItems,
                auditLogs
        );
    }
}
