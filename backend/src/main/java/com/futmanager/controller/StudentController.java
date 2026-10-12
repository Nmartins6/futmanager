package com.futmanager.controller;

import com.futmanager.domain.enums.StudentStatus;
import com.futmanager.dto.StudentDtos.*;
import com.futmanager.service.StudentService;
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

import java.util.UUID;

@RestController
@RequestMapping("/students")
@RequiredArgsConstructor
@Tag(name = "Alunos", description = "Gerenciamento de alunos e fichas cadastrais")
public class StudentController {

    private final StudentService studentService;

    @GetMapping
    @Operation(summary = "Pesquisar alunos com filtros por nome, responsável, turma, faixa etária e situação")
    public ResponseEntity<Page<StudentSummaryResponse>> listStudents(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) Integer minAge,
            @RequestParam(required = false) Integer maxAge,
            Pageable pageable) {
        return ResponseEntity.ok(studentService.listStudents(query, status, classId, minAge, maxAge, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter ficha completa do aluno com histórico e responsáveis")
    public ResponseEntity<StudentResponse> getStudentById(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.getStudentById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Cadastrar novo aluno")
    public ResponseEntity<StudentResponse> createStudent(@Valid @RequestBody StudentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(studentService.createStudent(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Atualizar cadastro do aluno")
    public ResponseEntity<StudentResponse> updateStudent(
            @PathVariable UUID id,
            @Valid @RequestBody StudentRequest request) {
        return ResponseEntity.ok(studentService.updateStudent(id, request));
    }

    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Inativar ou reativar aluno (mantém histórico íntegro)")
    public ResponseEntity<StudentResponse> toggleStatus(@PathVariable UUID id) {
        return ResponseEntity.ok(studentService.toggleStudentStatus(id));
    }
}
