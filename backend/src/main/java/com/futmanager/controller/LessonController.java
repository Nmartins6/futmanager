package com.futmanager.controller;

import com.futmanager.domain.enums.LessonStatus;
import com.futmanager.dto.LessonDtos.*;
import com.futmanager.service.LessonService;
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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/lessons")
@RequiredArgsConstructor
@Tag(name = "Aulas", description = "Gerenciamento de aulas, calendário, cancelamentos e remarcações")
public class LessonController {

    private final LessonService lessonService;

    @GetMapping
    @Operation(summary = "Pesquisar aulas no calendário com filtros por turma, data e situação")
    public ResponseEntity<Page<LessonResponse>> searchLessons(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) LessonStatus status,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            Pageable pageable) {
        return ResponseEntity.ok(lessonService.searchLessons(classId, status, startDate, endDate, pageable));
    }

    @GetMapping("/today")
    @Operation(summary = "Listar aulas de hoje para o professor/administrador com atalhos de chamada")
    public ResponseEntity<List<LessonResponse>> getTodayLessons() {
        return ResponseEntity.ok(lessonService.getLessonsOnDate(LocalDate.now()));
    }

    @GetMapping("/by-date")
    @Operation(summary = "Listar aulas em uma data específica")
    public ResponseEntity<List<LessonResponse>> getLessonsByDate(@RequestParam LocalDate date) {
        return ResponseEntity.ok(lessonService.getLessonsOnDate(date));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter aula por ID")
    public ResponseEntity<LessonResponse> getLessonById(@PathVariable UUID id) {
        return ResponseEntity.ok(lessonService.getLessonById(id));
    }

    @PostMapping
    @Operation(summary = "Criar aula avulsa ou extra")
    public ResponseEntity<LessonResponse> createLesson(@Valid @RequestBody LessonCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(lessonService.createLesson(request));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancelar treino (não gera faltas para os alunos)")
    public ResponseEntity<LessonResponse> cancelLesson(
            @PathVariable UUID id,
            @RequestBody(required = false) LessonCancelRequest request) {
        return ResponseEntity.ok(lessonService.cancelLesson(id, request));
    }

    @PostMapping("/{id}/reschedule")
    @Operation(summary = "Remarcar treino para nova data e horário")
    public ResponseEntity<LessonResponse> rescheduleLesson(
            @PathVariable UUID id,
            @Valid @RequestBody LessonRescheduleRequest request) {
        return ResponseEntity.ok(lessonService.rescheduleLesson(id, request));
    }

    @PostMapping("/generate-horizon")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Gerar aulas futuras de forma idempotente para o horizonte configurado")
    public ResponseEntity<GenerateLessonsResponse> generateLessons(
            @RequestBody(required = false) GenerateLessonsRequest request) {
        Integer horizon = request != null ? request.horizonDays() : null;
        return ResponseEntity.ok(lessonService.generateLessonsForHorizon(horizon));
    }
}
