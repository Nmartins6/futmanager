package com.futmanager.service;

import com.futmanager.config.UserPrincipal;
import com.futmanager.domain.entity.*;
import com.futmanager.domain.enums.ClassStatus;
import com.futmanager.domain.enums.EnrollmentStatus;
import com.futmanager.domain.enums.Role;
import com.futmanager.dto.SchoolClassDtos.*;
import com.futmanager.exception.BusinessException;
import com.futmanager.exception.ConflictException;
import com.futmanager.exception.ForbiddenException;
import com.futmanager.exception.ResourceNotFoundException;
import com.futmanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SchoolClassService {

    private final SchoolClassRepository schoolClassRepository;
    private final UserRepository userRepository;
    private final ClassTeacherRepository classTeacherRepository;
    private final ClassScheduleRepository classScheduleRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final StudentRepository studentRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<SchoolClassResponse> listClasses(String query, ClassStatus status, Pageable pageable) {
        UserPrincipal principal = userService.getCurrentUserPrincipal();
        UUID teacherFilter = null;
        UUID tenantId = principal != null && principal.getTenantId() != null ? principal.getTenantId() : userService.getCurrentUserEntity().getTenant().getId();

        // Se o usuário autenticado for Professor (e não Admin), filtra apenas suas turmas autorizadas
        if (principal != null && principal.getRole() == Role.PROFESSOR) {
            teacherFilter = principal.getId();
        }

        String search = (query != null && !query.isBlank()) ? "%" + query.trim().toLowerCase() + "%" : null;
        return schoolClassRepository.searchClasses(tenantId, search, status, teacherFilter, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<SchoolClassResponse> listAllActiveClasses() {
        User currentUser = userService.getCurrentUserEntity();
        UUID tenantId = currentUser.getTenant().getId();
        UserPrincipal principal = userService.getCurrentUserPrincipal();
        if (principal != null && principal.getRole() == Role.PROFESSOR) {
            return schoolClassRepository.findByTeacherId(tenantId, principal.getId(), ClassStatus.ATIVO)
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }
        return schoolClassRepository.findByTenantIdAndStatus(tenantId, ClassStatus.ATIVO)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public SchoolClassResponse getClassById(UUID id) {
        User currentUser = userService.getCurrentUserEntity();
        SchoolClass schoolClass = schoolClassRepository.findByIdAndTenantId(id, currentUser.getTenant().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada com ID: " + id));

        validateTeacherAccessToClass(schoolClass);

        return toResponse(schoolClass);
    }

    @Transactional
    public SchoolClassResponse createClass(SchoolClassRequest request) {
        User currentUser = userService.getCurrentUserEntity();
        SchoolClass schoolClass = SchoolClass.builder()
                .tenant(currentUser.getTenant())
                .name(request.name().trim())
                .category(request.category().trim())
                .location(request.location())
                .maxCapacity(request.maxCapacity())
                .status(request.status() != null ? request.status() : ClassStatus.ATIVO)
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();

        schoolClass = schoolClassRepository.save(schoolClass);

        syncTeachers(schoolClass, request.teachers());
        syncSchedules(schoolClass, request.schedules());

        return toResponse(schoolClass);
    }

    @Transactional
    public SchoolClassResponse updateClass(UUID id, SchoolClassRequest request) {
        SchoolClass schoolClass = schoolClassRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada com ID: " + id));

        schoolClass.setName(request.name().trim());
        schoolClass.setCategory(request.category().trim());
        schoolClass.setLocation(request.location());
        schoolClass.setMaxCapacity(request.maxCapacity());
        schoolClass.setStatus(request.status());
        schoolClass.setStartDate(request.startDate());
        schoolClass.setEndDate(request.endDate());

        schoolClass = schoolClassRepository.save(schoolClass);

        syncTeachers(schoolClass, request.teachers());
        syncSchedules(schoolClass, request.schedules());

        return toResponse(schoolClass);
    }

    @Transactional
    public ClassEnrollmentDto enrollStudent(EnrollmentCreateRequest request) {
        Student student = studentRepository.findById(request.studentId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado com ID: " + request.studentId()));

        SchoolClass schoolClass = schoolClassRepository.findById(request.classId())
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada com ID: " + request.classId()));

        validateStudentAgeForClassCategory(student, schoolClass);

        if (request.exitDate() != null && request.exitDate().isBefore(request.entryDate())) {
            throw new BusinessException("A data de saída do vínculo não pode ser anterior à data de entrada.");
        }

        // Regra crítica: Prevenir períodos sobrepostos para o mesmo aluno na mesma turma
        List<ClassEnrollment> overlapping = classEnrollmentRepository.findOverlappingEnrollments(
                student.getId(), schoolClass.getId(), request.entryDate(), request.exitDate(), null
        );

        if (!overlapping.isEmpty()) {
            throw new ConflictException("Já existe um vínculo ativo ou sobreposto para este aluno nesta turma no período informado.");
        }

        ClassEnrollment enrollment = ClassEnrollment.builder()
                .student(student)
                .schoolClass(schoolClass)
                .entryDate(request.entryDate())
                .exitDate(request.exitDate())
                .status(request.status() != null ? request.status() : EnrollmentStatus.ATIVO)
                .build();

        enrollment = classEnrollmentRepository.save(enrollment);
        return toEnrollmentDto(enrollment);
    }

    @Transactional
    public ClassEnrollmentDto updateEnrollment(UUID enrollmentId, EnrollmentUpdateRequest request) {
        ClassEnrollment enrollment = classEnrollmentRepository.findById(enrollmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Vínculo não encontrado com ID: " + enrollmentId));

        LocalDate entryDate = request.entryDate() != null ? request.entryDate() : enrollment.getEntryDate();
        LocalDate exitDate = request.exitDate();

        if (exitDate != null && exitDate.isBefore(entryDate)) {
            throw new BusinessException("A data de saída do vínculo não pode ser anterior à data de entrada.");
        }

        // Validar sobreposição excluindo o próprio registro
        List<ClassEnrollment> overlapping = classEnrollmentRepository.findOverlappingEnrollments(
                enrollment.getStudent().getId(), enrollment.getSchoolClass().getId(), entryDate, exitDate, enrollmentId
        );

        if (!overlapping.isEmpty()) {
            throw new ConflictException("A alteração de datas gera sobreposição com outro vínculo existente deste aluno nesta turma.");
        }

        enrollment.setEntryDate(entryDate);
        enrollment.setExitDate(exitDate);
        enrollment.setStatus(request.status());

        enrollment = classEnrollmentRepository.save(enrollment);
        return toEnrollmentDto(enrollment);
    }

    @Transactional(readOnly = true)
    public List<ClassEnrollmentDto> listEnrollmentsByClass(UUID classId) {
        return classEnrollmentRepository.findBySchoolClassId(classId)
                .stream()
                .map(this::toEnrollmentDto)
                .toList();
    }

    private void syncTeachers(SchoolClass schoolClass, List<TeacherAssignmentInput> teacherInputs) {
        if (teacherInputs == null) {
            return;
        }

        schoolClass.getTeachers().clear();

        for (TeacherAssignmentInput input : teacherInputs) {
            User user = userRepository.findById(input.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("Professor não encontrado com ID: " + input.userId()));

            ClassTeacher ct = ClassTeacher.builder()
                    .schoolClass(schoolClass)
                    .user(user)
                    .substitute(input.substitute())
                    .build();

            schoolClass.getTeachers().add(ct);
        }

        schoolClassRepository.save(schoolClass);
    }

    private void syncSchedules(SchoolClass schoolClass, List<ClassScheduleInput> scheduleInputs) {
        if (scheduleInputs == null) {
            return;
        }

        schoolClass.getSchedules().clear();

        for (ClassScheduleInput input : scheduleInputs) {
            if (input.endTime().isBefore(input.startTime()) || input.endTime().equals(input.startTime())) {
                throw new BusinessException("O horário final deve ser posterior ao horário inicial (" + input.dayOfWeek() + ").");
            }

            ClassSchedule cs = ClassSchedule.builder()
                    .schoolClass(schoolClass)
                    .dayOfWeek(input.dayOfWeek())
                    .startTime(input.startTime())
                    .endTime(input.endTime())
                    .build();

            schoolClass.getSchedules().add(cs);
        }

        schoolClassRepository.save(schoolClass);
    }

    public void validateTeacherAccessToClass(SchoolClass schoolClass) {
        UserPrincipal principal = userService.getCurrentUserPrincipal();
        if (principal == null) {
            return;
        }

        if (principal.getRole() != Role.SUPER_ADMIN && schoolClass.getTenant() != null) {
            if (!schoolClass.getTenant().getId().equals(principal.getTenantId())) {
                throw new ForbiddenException("Você não tem acesso aos dados de outra escolinha.");
            }
        }

        if (principal.getRole() == Role.ADMINISTRADOR || principal.getRole() == Role.SUPER_ADMIN) {
            return; // Administradores da escolinha têm acesso total às suas turmas
        }

        boolean authorized = schoolClass.getTeachers().stream()
                .anyMatch(t -> t.getUser().getId().equals(principal.getId()));

        if (!authorized) {
            throw new ForbiddenException("Você não possui autorização como professor titular ou substituto para esta turma.");
        }
    }

    public SchoolClassResponse toResponse(SchoolClass c) {
        List<TeacherSummaryDto> teachers = c.getTeachers().stream()
                .map(t -> new TeacherSummaryDto(
                        t.getUser().getId(),
                        t.getUser().getFullName(),
                        t.getUser().getEmail(),
                        t.isSubstitute()
                ))
                .toList();

        List<ClassScheduleDto> schedules = c.getSchedules().stream()
                .map(s -> new ClassScheduleDto(
                        s.getId(),
                        s.getDayOfWeek(),
                        s.getStartTime(),
                        s.getEndTime()
                ))
                .toList();

        int activeStudents = (int) c.getEnrollments().stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ATIVO && e.isEligibleOn(LocalDate.now()))
                .count();

        return new SchoolClassResponse(
                c.getId(),
                c.getName(),
                c.getCategory(),
                c.getLocation(),
                c.getMaxCapacity(),
                c.getStatus(),
                c.getStartDate(),
                c.getEndDate(),
                teachers,
                schedules,
                activeStudents
        );
    }

    private ClassEnrollmentDto toEnrollmentDto(ClassEnrollment e) {
        return new ClassEnrollmentDto(
                e.getId(),
                e.getStudent().getId(),
                e.getStudent().getFullName(),
                e.getStudent().getBirthDate(),
                e.getStudent().getAge(),
                e.getStudent().getPhotoUrl(),
                e.getSchoolClass().getId(),
                e.getSchoolClass().getName(),
                e.getEntryDate(),
                e.getExitDate(),
                e.getStatus()
        );
    }

    public static void validateStudentAgeForClassCategory(Student student, SchoolClass schoolClass) {
        if (student == null || schoolClass == null || schoolClass.getCategory() == null) {
            return;
        }

        String category = schoolClass.getCategory().trim();
        int studentAge = student.getAge();

        // 1. Padrão "Sub-X", "Sub X", "SubX", "U-X", "UX" (ex: Sub-9, Sub 11, Sub-7)
        java.util.regex.Pattern subPattern = java.util.regex.Pattern.compile("(?i)(?:sub[- ]?|u[- ]?)(\\d{1,2})");
        java.util.regex.Matcher subMatcher = subPattern.matcher(category);
        if (subMatcher.find()) {
            int maxAge = Integer.parseInt(subMatcher.group(1));
            if (studentAge > maxAge) {
                throw new BusinessException(String.format(
                        "O aluno '%s' possui %d anos e não pode estar na turma '%s' (categoria %s). A idade máxima permitida para esta categoria é %d anos.",
                        student.getFullName(), studentAge, schoolClass.getName(), category, maxAge
                ));
            }
        }

        // 2. Padrão de faixa "X a Y anos" ou "X-Y anos" (ex: "7 a 9 anos", "10-12 anos")
        java.util.regex.Pattern rangePattern = java.util.regex.Pattern.compile("(\\d{1,2})\\s*(?:a|-)\\s*(\\d{1,2})", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher rangeMatcher = rangePattern.matcher(category);
        if (rangeMatcher.find()) {
            int minAge = Integer.parseInt(rangeMatcher.group(1));
            int maxAge = Integer.parseInt(rangeMatcher.group(2));
            if (studentAge > maxAge) {
                throw new BusinessException(String.format(
                        "O aluno '%s' possui %d anos e excede a faixa etária da turma '%s' (categoria %s, máxima %d anos).",
                        student.getFullName(), studentAge, schoolClass.getName(), category, maxAge
                ));
            }
            if (studentAge < minAge) {
                throw new BusinessException(String.format(
                        "O aluno '%s' possui %d anos e está abaixo da faixa etária da turma '%s' (categoria %s, mínima %d anos).",
                        student.getFullName(), studentAge, schoolClass.getName(), category, minAge
                ));
            }
        }
    }
}
