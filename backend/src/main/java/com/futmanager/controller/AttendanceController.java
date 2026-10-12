package com.futmanager.controller;

import com.futmanager.dto.AttendanceDtos.AttendanceResponse;
import com.futmanager.dto.AttendanceDtos.AttendanceSaveRequest;
import com.futmanager.service.AttendanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/lessons/{lessonId}/attendance")
@RequiredArgsConstructor
@Tag(name = "Chamadas", description = "Execução, rascunhos, finalização e auditoria de chamadas de presença")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @GetMapping
    @Operation(summary = "Obter ou inicializar chamada da aula (apenas alunos elegíveis pela data de matrícula)")
    public ResponseEntity<AttendanceResponse> getOrInitAttendance(@PathVariable UUID lessonId) {
        return ResponseEntity.ok(attendanceService.getOrInitAttendance(lessonId));
    }

    @PostMapping
    @Operation(summary = "Salvar rascunho ou finalizar chamada (com controle de versão otimista e auditoria de autoria)")
    public ResponseEntity<AttendanceResponse> saveAttendance(
            @PathVariable UUID lessonId,
            @Valid @RequestBody AttendanceSaveRequest request) {
        return ResponseEntity.ok(attendanceService.saveAttendance(lessonId, request));
    }
}
