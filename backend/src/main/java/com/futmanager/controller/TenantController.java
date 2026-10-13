package com.futmanager.controller;

import com.futmanager.dto.TenantDtos.*;
import com.futmanager.service.TenantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tenants")
@RequiredArgsConstructor
@Tag(name = "SaaS Escolinhas (Tenants)", description = "Cadastro de novas escolinhas, planos e gestão multi-tenant")
public class TenantController {

    private final TenantService tenantService;

    @PostMapping("/onboard")
    @Operation(summary = "Criar nova escolinha (Onboarding self-service para novos clientes do SaaS)")
    public ResponseEntity<OnboardResponse> onboardSchool(@Valid @RequestBody TenantOnboardRequest request) {
        return ResponseEntity.ok(tenantService.onboardSchool(request));
    }

    @GetMapping("/current")
    @Operation(summary = "Obter dados da escolinha ativa do usuário autenticado")
    public ResponseEntity<TenantResponse> getCurrentTenant() {
        return ResponseEntity.ok(tenantService.getCurrentTenant());
    }

    @PutMapping("/current")
    @PreAuthorize("hasRole('ADMINISTRADOR') or hasRole('SUPER_ADMIN')")
    @Operation(summary = "Atualizar informações da escolinha ativa")
    public ResponseEntity<TenantResponse> updateCurrentTenant(@Valid @RequestBody TenantUpdateRequest request) {
        return ResponseEntity.ok(tenantService.updateCurrentTenant(request));
    }

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Listar todas as escolinhas cadastradas no SaaS (Acesso restrito ao Super Admin)")
    public ResponseEntity<List<TenantResponse>> listAllTenants() {
        return ResponseEntity.ok(tenantService.listAllTenants());
    }
}
