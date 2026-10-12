package com.futmanager.controller;

import com.futmanager.dto.SchoolConfigDto.SchoolConfigRequest;
import com.futmanager.dto.SchoolConfigDto.SchoolConfigResponse;
import com.futmanager.service.SchoolConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/configs")
@RequiredArgsConstructor
@Tag(name = "Configurações da Escolinha", description = "Parâmetros globais, limites de frequência e dados institucionais")
public class SchoolConfigController {

    private final SchoolConfigService schoolConfigService;

    @GetMapping
    @Operation(summary = "Obter configurações da escolinha (fuso, horizonte, limite de frequência)")
    public ResponseEntity<SchoolConfigResponse> getConfigs() {
        return ResponseEntity.ok(schoolConfigService.getConfigs());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Operation(summary = "Atualizar configurações da escolinha")
    public ResponseEntity<SchoolConfigResponse> updateConfigs(@Valid @RequestBody SchoolConfigRequest request) {
        return ResponseEntity.ok(schoolConfigService.updateConfigs(request));
    }
}
