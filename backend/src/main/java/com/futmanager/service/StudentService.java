package com.futmanager.service;

import com.futmanager.domain.entity.*;
import com.futmanager.domain.enums.EnrollmentStatus;
import com.futmanager.domain.enums.StudentStatus;
import com.futmanager.dto.StudentDtos.*;
import com.futmanager.exception.BusinessException;
import com.futmanager.exception.ResourceNotFoundException;
import com.futmanager.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class StudentService {

    private final StudentRepository studentRepository;
    private final GuardianRepository guardianRepository;
    private final StudentGuardianRepository studentGuardianRepository;
    private final SchoolClassRepository schoolClassRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final UserService userService;

    @Transactional(readOnly = true)
    public Page<StudentSummaryResponse> listStudents(
            String query,
            StudentStatus status,
            UUID classId,
            Integer minAge,
            Integer maxAge,
            Pageable pageable) {

        User currentUser = userService.getCurrentUserEntity();
        UUID tenantId = currentUser.getTenant().getId();

        LocalDate now = LocalDate.now();
        LocalDate minBirthDate = (maxAge != null) ? now.minusYears(maxAge + 1L).plusDays(1) : LocalDate.of(1900, 1, 1);
        LocalDate maxBirthDate = (minAge != null) ? now.minusYears(minAge) : LocalDate.of(2099, 12, 31);

        String search = (query != null && !query.isBlank()) ? "%" + query.trim().toLowerCase() + "%" : null;
        return studentRepository.searchStudents(tenantId, search, status, classId, minBirthDate, maxBirthDate, pageable)
                .map(s -> new StudentSummaryResponse(
                        s.getId(),
                        s.getFullName(),
                        s.getBirthDate(),
                        s.getAge(),
                        s.getPhotoUrl(),
                        s.getStatus(),
                        s.getEnrollmentDate(),
                        s.getEnrollments().stream()
                                .filter(e -> e.getStatus() == EnrollmentStatus.ATIVO)
                                .map(e -> e.getSchoolClass().getName())
                                .toList()
                ));
    }

    @Transactional(readOnly = true)
    public StudentResponse getStudentById(UUID id) {
        User currentUser = userService.getCurrentUserEntity();
        Student student = studentRepository.findByIdAndTenantId(id, currentUser.getTenant().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado com ID: " + id));

        return toFullResponse(student);
    }

    @Transactional
    public StudentResponse createStudent(StudentRequest request) {
        validateBirthDate(request.birthDate());

        User currentUser = userService.getCurrentUserEntity();
        Tenant tenant = currentUser.getTenant();

        // Verificar homônimos dentro da mesma escolinha
        List<Student> homonyms = studentRepository.findByTenantIdAndFullNameIgnoreCase(tenant.getId(), request.fullName().trim());
        if (!homonyms.isEmpty()) {
            log.info("Aviso: Cadastro de aluno homônimo '{}' na escolinha {}. Alunos existentes com o mesmo nome: {}",
                    request.fullName(), tenant.getName(), homonyms.size());
        }

        Student student = Student.builder()
                .tenant(tenant)
                .fullName(request.fullName().trim())
                .birthDate(request.birthDate())
                .photoUrl(request.photoUrl())
                .status(request.status() != null ? request.status() : StudentStatus.ATIVO)
                .enrollmentDate(request.enrollmentDate() != null ? request.enrollmentDate() : LocalDate.now())
                .notes(request.notes())
                .build();

        student = studentRepository.save(student);

        // Vincular responsáveis
        syncGuardians(student, request.guardians(), tenant);

        // Vincular turmas iniciais, se informadas
        if (request.classIds() != null) {
            for (UUID classId : request.classIds()) {
                enrollStudentInClass(student, classId, student.getEnrollmentDate());
            }
        }

        return toFullResponse(student);
    }

    @Transactional
    public StudentResponse updateStudent(UUID id, StudentRequest request) {
        User currentUser = userService.getCurrentUserEntity();
        Student student = studentRepository.findByIdAndTenantId(id, currentUser.getTenant().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado com ID: " + id));

        validateBirthDate(request.birthDate());

        student.setFullName(request.fullName().trim());
        student.setBirthDate(request.birthDate());
        if (request.photoUrl() != null) {
            student.setPhotoUrl(request.photoUrl());
        }
        student.setStatus(request.status());
        student.setEnrollmentDate(request.enrollmentDate());
        student.setNotes(request.notes());

        student = studentRepository.save(student);

        syncGuardians(student, request.guardians(), currentUser.getTenant());

        if (request.classIds() != null) {
            for (UUID classId : request.classIds()) {
                enrollStudentInClass(student, classId, student.getEnrollmentDate());
            }
        }

        return toFullResponse(student);
    }

    @Transactional
    public StudentResponse toggleStudentStatus(UUID id) {
        User currentUser = userService.getCurrentUserEntity();
        Student student = studentRepository.findByIdAndTenantId(id, currentUser.getTenant().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Aluno não encontrado com ID: " + id));

        StudentStatus newStatus = student.getStatus() == StudentStatus.ATIVO ? StudentStatus.INATIVO : StudentStatus.ATIVO;
        student.setStatus(newStatus);
        student = studentRepository.save(student);

        log.info("Situação do aluno {} ({}) alterada para: {}", student.getFullName(), student.getId(), newStatus);
        return toFullResponse(student);
    }

    private void validateBirthDate(LocalDate birthDate) {
        if (birthDate == null) {
            throw new BusinessException("A data de nascimento é obrigatória.");
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new BusinessException("A data de nascimento não pode ser futura.");
        }
    }

    private void syncGuardians(Student student, List<StudentGuardianLinkInput> guardianInputs, Tenant tenant) {
        if (guardianInputs == null) {
            return;
        }

        student.getGuardians().clear();

        for (StudentGuardianLinkInput input : guardianInputs) {
            Guardian guardian;
            if (input.guardianId() != null) {
                guardian = guardianRepository.findByIdAndTenantId(input.guardianId(), tenant.getId())
                        .orElseThrow(() -> new ResourceNotFoundException("Responsável não encontrado: " + input.guardianId()));
            } else if (input.guardianFullName() != null && !input.guardianFullName().isBlank()) {
                if (input.guardianPhone() == null || input.guardianPhone().isBlank()) {
                    throw new BusinessException("O telefone/WhatsApp do responsável '" + input.guardianFullName().trim() + "' é obrigatório.");
                }
                String phone = input.guardianPhone().trim();

                com.futmanager.util.ValidationUtils.validateOptionalEmail(input.guardianEmail(), "O e-mail do responsável");

                // Unicidade de responsável por telefone: se já existe, reutiliza o responsável para o novo aluno
                Optional<Guardian> existingGuardian = guardianRepository.findByTenantIdAndPhone(tenant.getId(), phone);
                if (existingGuardian.isPresent()) {
                    guardian = existingGuardian.get();
                } else {
                    guardian = Guardian.builder()
                            .tenant(tenant)
                            .fullName(input.guardianFullName().trim())
                            .phone(phone)
                            .email(input.guardianEmail() != null && !input.guardianEmail().isBlank() ? input.guardianEmail().trim().toLowerCase() : null)
                            .build();
                    guardian = guardianRepository.save(guardian);
                }
            } else {
                continue;
            }

            StudentGuardian link = StudentGuardian.builder()
                    .student(student)
                    .guardian(guardian)
                    .relationship(input.relationship())
                    .mainContact(input.mainContact())
                    .emergencyContact(input.emergencyContact())
                    .build();

            student.getGuardians().add(link);
        }

        studentRepository.save(student);
    }

    private void enrollStudentInClass(Student student, UUID classId, LocalDate entryDate) {
        SchoolClass schoolClass = schoolClassRepository.findById(classId)
                .orElseThrow(() -> new ResourceNotFoundException("Turma não encontrada: " + classId));

        // Validação da categoria da turma vs idade do aluno (ex: aluno de 11 anos não pode entrar em Sub-9)
        SchoolClassService.validateStudentAgeForClassCategory(student, schoolClass);

        List<ClassEnrollment> overlapping = classEnrollmentRepository.findOverlappingEnrollments(
                student.getId(), classId, entryDate, null, null
        );

        if (overlapping.isEmpty()) {
            ClassEnrollment enrollment = ClassEnrollment.builder()
                    .student(student)
                    .schoolClass(schoolClass)
                    .entryDate(entryDate)
                    .status(EnrollmentStatus.ATIVO)
                    .build();
            classEnrollmentRepository.save(enrollment);
        }
    }

    public StudentResponse toFullResponse(Student s) {
        List<StudentGuardianResponse> guardians = s.getGuardians().stream()
                .map(sg -> new StudentGuardianResponse(
                        sg.getGuardian().getId(),
                        sg.getGuardian().getFullName(),
                        sg.getGuardian().getPhone(),
                        sg.getGuardian().getEmail(),
                        sg.getRelationship(),
                        sg.isMainContact(),
                        sg.isEmergencyContact()
                ))
                .toList();

        List<EnrollmentSummaryDto> enrollments = s.getEnrollments().stream()
                .map(e -> new EnrollmentSummaryDto(
                        e.getId(),
                        e.getSchoolClass().getId(),
                        e.getSchoolClass().getName(),
                        e.getSchoolClass().getCategory(),
                        e.getEntryDate(),
                        e.getExitDate(),
                        e.getStatus()
                ))
                .toList();

        return new StudentResponse(
                s.getId(),
                s.getFullName(),
                s.getBirthDate(),
                s.getAge(),
                s.getPhotoUrl(),
                s.getStatus(),
                s.getEnrollmentDate(),
                s.getNotes(),
                guardians,
                enrollments,
                s.getCreatedAt()
        );
    }
}
