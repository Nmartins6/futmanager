package com.futmanager.controller;

import com.futmanager.dto.LessonDtos.LessonResponse;
import com.futmanager.dto.StatisticsDtos.*;
import com.futmanager.service.StatisticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/statistics")
@RequiredArgsConstructor
@Tag(name = "Estatísticas", description = "Métricas de frequência, comparativos, evolução mensal e pendências")
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/dashboard")
    @Operation(summary = "Métricas consolidadas do painel inicial (alunos ativos, pendências, alertas de baixa frequência)")
    public ResponseEntity<DashboardOverviewResponse> getDashboardOverview() {
        return ResponseEntity.ok(statisticsService.getDashboardOverview());
    }

    @GetMapping("/students/{studentId}")
    @Operation(summary = "Estatísticas consolidadas e por turma de frequência de um aluno")
    public ResponseEntity<StudentFrequencySummaryDto> getStudentFrequency(
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(statisticsService.getStudentFrequency(studentId, classId, startDate, endDate));
    }

    @GetMapping("/students/{studentId}/history")
    @Operation(summary = "Histórico detalhado aula por aula de um aluno com idade na data da aula")
    public ResponseEntity<List<StudentAttendanceHistoryItemDto>> getStudentAttendanceHistory(
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(statisticsService.getStudentAttendanceHistory(studentId, classId, startDate, endDate));
    }

    @GetMapping("/classes/comparison")
    @Operation(summary = "Comparação de frequência média e presenças entre turmas")
    public ResponseEntity<List<ClassComparisonDto>> compareClasses(
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(statisticsService.compareClasses(startDate, endDate));
    }

    @GetMapping("/evolution/monthly")
    @Operation(summary = "Evolução mensal da frequência da escolinha ou por turma")
    public ResponseEntity<List<MonthlyEvolutionDto>> getMonthlyEvolution(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) Integer year) {
        return ResponseEntity.ok(statisticsService.getMonthlyEvolution(classId, year));
    }

    @GetMapping("/pending-lessons")
    @Operation(summary = "Listar aulas com chamada pendente de finalização")
    public ResponseEntity<List<LessonResponse>> getPendingLessons(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(statisticsService.getPendingLessons(classId, startDate, endDate));
    }
}
