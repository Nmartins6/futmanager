package com.futmanager.dto;

import com.futmanager.domain.enums.TenantPlan;
import com.futmanager.domain.enums.TenantStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class TenantDtos {

    public record TenantOnboardRequest(
            @NotBlank(message = "O nome da escolinha é obrigatório")
            @Size(min = 3, max = 150, message = "O nome da escolinha deve ter entre 3 e 150 caracteres")
            String schoolName,

            String slug,

            String cnpj,
            String phone,
            String address,
            TenantPlan plan,

            @NotBlank(message = "O nome do responsável / administrador é obrigatório")
            String adminFullName,

            @NotBlank(message = "O e-mail do administrador é obrigatório")
            @jakarta.validation.constraints.Pattern(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,7}$",
                message = "Formato de e-mail inválido. Digite um e-mail válido como usuario@exemplo.com"
            )
            String adminEmail,

            String adminPhone,
            String adminPassword
    ) {}

    public record TenantResponse(
            UUID id,
            String name,
            String slug,
            String cnpj,
            TenantPlan plan,
            TenantStatus status,
            Instant trialEndsAt,
            Integer maxStudents,
            String phone,
            String address,
            Instant createdAt
    ) {}

    public record TenantUpdateRequest(
            @NotBlank(message = "O nome da escolinha é obrigatório")
            String name,
            String cnpj,
            String phone,
            String address
    ) {}

    public record OnboardResponse(
            TenantResponse tenant,
            AuthDtos.LoginResponse login
    ) {}
}
