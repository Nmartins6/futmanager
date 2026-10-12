package com.futmanager.controller;

import com.futmanager.dto.GuardianDtos.GuardianRequest;
import com.futmanager.dto.GuardianDtos.GuardianResponse;
import com.futmanager.service.GuardianService;
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
@RequestMapping("/guardians")
@RequiredArgsConstructor
@Tag(name = "Responsáveis", description = "Gerenciamento de responsáveis e contatos de emergência")
public class GuardianController {

    private final GuardianService guardianService;

    @GetMapping
    @Operation(summary = "Pesquisar responsáveis por nome, telefone ou e-mail")
    public ResponseEntity<Page<GuardianResponse>> listGuardians(
            @RequestParam(required = false) String query,
            Pageable pageable) {
        return ResponseEntity.ok(guardianService.listGuardians(query, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter dados do responsável por ID")
    public ResponseEntity<GuardianResponse> getGuardianById(@PathVariable UUID id) {
        return ResponseEntity.ok(guardianService.getGuardianById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Cadastrar novo responsável")
    public ResponseEntity<GuardianResponse> createGuardian(@Valid @RequestBody GuardianRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(guardianService.createGuardian(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Atualizar responsável")
    public ResponseEntity<GuardianResponse> updateGuardian(
            @PathVariable UUID id,
            @Valid @RequestBody GuardianRequest request) {
        return ResponseEntity.ok(guardianService.updateGuardian(id, request));
    }
}
