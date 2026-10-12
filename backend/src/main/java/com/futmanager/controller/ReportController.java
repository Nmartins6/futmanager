package com.futmanager.controller;

import com.futmanager.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
@Tag(name = "Relatórios", description = "Emissão de relatórios gerenciais em PDF e XLSX")
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/class-frequency/pdf")
    @Operation(summary = "Exportar relatório de frequência por turma em PDF")
    public ResponseEntity<byte[]> getFrequencyByClassPdf(
            @RequestParam UUID classId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        byte[] pdfBytes = reportService.generateClassFrequencyPdf(classId, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio_frequencia_turma.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/student-individual/pdf")
    @Operation(summary = "Exportar ficha individual de frequência do aluno em PDF")
    public ResponseEntity<byte[]> getStudentIndividualPdf(
            @RequestParam UUID studentId,
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        byte[] pdfBytes = reportService.generateStudentIndividualPdf(studentId, classId, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ficha_individual_aluno.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/low-frequency/pdf")
    @Operation(summary = "Exportar relatório de alunos com baixa frequência (< limite) em PDF")
    public ResponseEntity<byte[]> getLowFrequencyPdf(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate) {

        byte[] pdfBytes = reportService.generateLowFrequencyPdf(classId, startDate, endDate);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"relatorio_baixa_frequencia.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @GetMapping("/monthly-summary/xlsx")
    @Operation(summary = "Exportar resumo mensal da escolinha em planilha XLSX")
    public ResponseEntity<byte[]> getMonthlySummaryXlsx(
            @RequestParam(required = false) UUID classId,
            @RequestParam(required = false) Integer year) {

        byte[] xlsxBytes = reportService.generateMonthlySummaryXlsx(classId, year);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resumo_mensal.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsxBytes);
    }
}
