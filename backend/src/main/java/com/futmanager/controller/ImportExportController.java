package com.futmanager.controller;

import com.futmanager.domain.enums.StudentStatus;
import com.futmanager.dto.ImportExportDtos.*;
import com.futmanager.service.ImportExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/import-export")
@RequiredArgsConstructor
@Tag(name = "Importação e Exportação", description = "Importação em lote via CSV/XLSX e exportação segura com proteção anti-fórmula")
public class ImportExportController {

    private final ImportExportService importExportService;

    @GetMapping("/template/csv")
    @Operation(summary = "Baixar planilha modelo em CSV com instruções de preenchimento")
    public ResponseEntity<byte[]> downloadCsvTemplate() {
        byte[] csv = importExportService.generateCsvTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"modelo_importacao_futmanager.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/template/xlsx")
    @Operation(summary = "Baixar planilha modelo em XLSX com formatação e cabeçalhos estilizados")
    public ResponseEntity<byte[]> downloadXlsxTemplate() {
        byte[] xlsx = importExportService.generateXlsxTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"modelo_importacao_futmanager.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Carregar arquivo CSV ou XLSX para pré-visualização, validação de linhas e detecção de duplicidades")
    public ResponseEntity<ImportPreviewResponse> previewImport(@RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(importExportService.previewImport(file));
    }

    @PostMapping("/confirm")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Efetivar importação transacional após confirmação explícita do usuário")
    public ResponseEntity<ImportResultResponse> confirmImport(@Valid @RequestBody ImportConfirmRequest request) {
        return ResponseEntity.ok(importExportService.confirmImport(request));
    }

    @GetMapping("/export/students/csv")
    @Operation(summary = "Exportar lista de alunos em CSV (com proteção contra injeção de fórmulas)")
    public ResponseEntity<byte[]> exportStudentsCsv(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) UUID classId) {

        byte[] csv = importExportService.exportStudentsCsv(query, status, classId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"alunos_exportacao.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/export/students/xlsx")
    @Operation(summary = "Exportar lista de alunos em XLSX")
    public ResponseEntity<byte[]> exportStudentsXlsx(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) StudentStatus status,
            @RequestParam(required = false) UUID classId) {

        byte[] xlsx = importExportService.exportStudentsXlsx(query, status, classId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"alunos_exportacao.xlsx\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(xlsx);
    }
}
