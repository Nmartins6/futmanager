package com.futmanager.controller;

import com.futmanager.domain.enums.ClassStatus;
import com.futmanager.dto.SchoolClassDtos.*;
import com.futmanager.service.SchoolClassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/classes")
@RequiredArgsConstructor
@Tag(name = "Turmas", description = "Gerenciamento de turmas, horários e matrículas")
public class SchoolClassController {

    private final SchoolClassService schoolClassService;

    @GetMapping
    @Operation(summary = "Pesquisar turmas com paginação (professores vêem suas turmas atribuídas)")
    public ResponseEntity<Page<SchoolClassResponse>> listClasses(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) ClassStatus status,
            Pageable pageable) {
        return ResponseEntity.ok(schoolClassService.listClasses(query, status, pageable));
    }

    @GetMapping("/all")
    @Operation(summary = "Listar todas as turmas ativas autorizadas")
    public ResponseEntity<List<SchoolClassResponse>> listAllActiveClasses() {
        return ResponseEntity.ok(schoolClassService.listAllActiveClasses());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes da turma por ID")
    public ResponseEntity<SchoolClassResponse> getClassById(@PathVariable UUID id) {
        return ResponseEntity.ok(schoolClassService.getClassById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Cadastrar nova turma com professores e horários recorrentes")
    public ResponseEntity<SchoolClassResponse> createClass(@Valid @RequestBody SchoolClassRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schoolClassService.createClass(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Atualizar dados, horários e professores da turma")
    public ResponseEntity<SchoolClassResponse> updateClass(
            @PathVariable UUID id,
            @Valid @RequestBody SchoolClassRequest request) {
        return ResponseEntity.ok(schoolClassService.updateClass(id, request));
    }

    @GetMapping("/{id}/enrollments")
    @Operation(summary = "Listar histórico e matrículas de alunos na turma")
    public ResponseEntity<List<ClassEnrollmentDto>> listClassEnrollments(@PathVariable UUID id) {
        return ResponseEntity.ok(schoolClassService.listEnrollmentsByClass(id));
    }

    @PostMapping("/enroll")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Matricular aluno na turma com validação anti-sobreposição de períodos")
    public ResponseEntity<ClassEnrollmentDto> enrollStudent(@Valid @RequestBody EnrollmentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(schoolClassService.enrollStudent(request));
    }

    @PutMapping("/enrollments/{enrollmentId}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Atualizar período ou situação da matrícula do aluno")
    public ResponseEntity<ClassEnrollmentDto> updateEnrollment(
            @PathVariable UUID enrollmentId,
            @Valid @RequestBody EnrollmentUpdateRequest request) {
        return ResponseEntity.ok(schoolClassService.updateEnrollment(enrollmentId, request));
    }
}
