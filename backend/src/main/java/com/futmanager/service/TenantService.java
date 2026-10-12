package com.futmanager.service;

import com.futmanager.config.JwtService;
import com.futmanager.config.TenantContext;
import com.futmanager.config.UserPrincipal;
import com.futmanager.domain.entity.SchoolConfig;
import com.futmanager.domain.entity.Tenant;
import com.futmanager.domain.entity.User;
import com.futmanager.domain.enums.Role;
import com.futmanager.domain.enums.TenantPlan;
import com.futmanager.domain.enums.TenantStatus;
import com.futmanager.domain.enums.UserStatus;
import com.futmanager.dto.AuthDtos.LoginResponse;
import com.futmanager.dto.TenantDtos.*;
import com.futmanager.exception.BusinessException;
import com.futmanager.exception.ConflictException;
import com.futmanager.exception.ResourceNotFoundException;
import com.futmanager.repository.SchoolConfigRepository;
import com.futmanager.repository.TenantRepository;
import com.futmanager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SchoolConfigRepository schoolConfigRepository;
    private final JwtService jwtService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Transactional
    public OnboardResponse onboardSchool(TenantOnboardRequest request) {
        String baseSlug;
        if (request.slug() != null && !request.slug().isBlank()) {
            baseSlug = request.slug().toLowerCase().trim().replaceAll("[^a-z0-9-]", "-");
        } else {
            String normalized = java.text.Normalizer.normalize(request.schoolName(), java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                    .toLowerCase()
                    .trim()
                    .replaceAll("[^a-z0-9]+", "-")
                    .replaceAll("^-|-$", "");
            baseSlug = normalized.isBlank() ? "escolinha" : normalized;
        }

        String slug = baseSlug;
        int suffix = 1;
        while (tenantRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + suffix++;
        }

        String adminEmail = request.adminEmail().toLowerCase().trim();
        if (userRepository.existsByEmail(adminEmail)) {
            throw new ConflictException("Já existe um usuário cadastrado com este e-mail: " + adminEmail);
        }

        TenantPlan plan = request.plan() != null ? request.plan() : TenantPlan.PRO;
        int maxStudents = (plan == TenantPlan.PRO) ? 150 : (plan == TenantPlan.ENTERPRISE ? 9999 : 50);

        // 1. Criar a Escolinha (Tenant) com 14 dias de Trial
        Tenant tenant = Tenant.builder()
                .name(request.schoolName().trim())
                .slug(slug)
                .cnpj(request.cnpj() != null ? request.cnpj().trim() : null)
                .phone(request.phone() != null ? request.phone().trim() : null)
                .address(request.address() != null ? request.address().trim() : null)
                .plan(plan)
                .status(TenantStatus.ATIVO)
                .trialEndsAt(Instant.now().plus(14, ChronoUnit.DAYS))
                .maxStudents(maxStudents)
                .build();

        tenant = tenantRepository.save(tenant);

        // 2. Criar o Usuário Administrador da Escolinha
        String rawPassword = (request.adminPassword() != null && !request.adminPassword().isBlank()) 
                ? request.adminPassword().trim() 
                : "admin123";

        User adminUser = User.builder()
                .tenant(tenant)
                .email(adminEmail)
                .fullName(request.adminFullName().trim())
                .role(Role.ADMINISTRADOR)
                .status(UserStatus.ATIVO)
                .phone(request.adminPhone() != null ? request.adminPhone().trim() : null)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .build();

        adminUser = userRepository.save(adminUser);

        // 3. Criar configurações iniciais da escolinha
        createInitialConfig(tenant, "school_name", tenant.getName(), "Nome da escolinha");
        createInitialConfig(tenant, "cnpj", tenant.getCnpj() != null ? tenant.getCnpj() : "", "CNPJ da instituição");
        createInitialConfig(tenant, "phone", tenant.getPhone() != null ? tenant.getPhone() : "", "Telefone de contato");
        createInitialConfig(tenant, "address", tenant.getAddress() != null ? tenant.getAddress() : "", "Endereço");
        createInitialConfig(tenant, "timezone", "America/Sao_Paulo", "Fuso horário padrão");
        createInitialConfig(tenant, "lesson_horizon_days", "30", "Horizonte de geração de aulas em dias");
        createInitialConfig(tenant, "low_frequency_threshold", "75.0", "Percentual mínimo de frequência");

        log.info("Nova escolinha cadastrada com sucesso no SaaS: '{}' ({}) com admin {}",
                tenant.getName(), tenant.getSlug(), adminUser.getEmail());

        // 4. Gerar Token de Acesso Imediato
        String token = jwtService.generateDevToken(
                adminUser.getId(),
                adminUser.getEmail(),
                adminUser.getRole().name(),
                tenant.getId(),
                tenant.getSlug()
        );

        LoginResponse login = new LoginResponse(
                token,
                adminUser.getId(),
                adminUser.getEmail(),
                adminUser.getFullName(),
                adminUser.getRole(),
                adminUser.getStatus(),
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getPlan().name()
        );

        return new OnboardResponse(toResponse(tenant), login);
    }

    @Transactional(readOnly = true)
    public TenantResponse getCurrentTenant() {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResourceNotFoundException("Nenhuma escolinha ativa vinculada à sessão.");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Escolinha não encontrada com ID: " + tenantId));

        return toResponse(tenant);
    }

    @Transactional
    public TenantResponse updateCurrentTenant(TenantUpdateRequest request) {
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ResourceNotFoundException("Nenhuma escolinha ativa vinculada à sessão.");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Escolinha não encontrada com ID: " + tenantId));

        tenant.setName(request.name().trim());
        tenant.setCnpj(request.cnpj() != null ? request.cnpj().trim() : null);
        tenant.setPhone(request.phone() != null ? request.phone().trim() : null);
        tenant.setAddress(request.address() != null ? request.address().trim() : null);

        tenant = tenantRepository.save(tenant);
        return toResponse(tenant);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> listAllTenants() {
        return tenantRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    private void createInitialConfig(Tenant tenant, String key, String value, String desc) {
        SchoolConfig config = SchoolConfig.builder()
                .tenant(tenant)
                .configKey(key)
                .configValue(value)
                .description(desc)
                .build();
        schoolConfigRepository.save(config);
    }

    public TenantResponse toResponse(Tenant t) {
        return new TenantResponse(
                t.getId(),
                t.getName(),
                t.getSlug(),
                t.getCnpj(),
                t.getPlan(),
                t.getStatus(),
                t.getTrialEndsAt(),
                t.getMaxStudents(),
                t.getPhone(),
                t.getAddress(),
                t.getCreatedAt()
        );
    }
}
