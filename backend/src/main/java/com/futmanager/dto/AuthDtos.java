package com.futmanager.dto;

import com.futmanager.domain.enums.Role;
import com.futmanager.domain.enums.UserStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class AuthDtos {

    public record LoginRequest(
            @NotBlank(message = "O e-mail é obrigatório")
            @Email(message = "Formato de e-mail inválido")
            String email,

            @NotBlank(message = "A senha é obrigatória")
            String password
    ) {}

    public record LoginResponse(
            String token,
            UUID id,
            String email,
            String fullName,
            Role role,
            UserStatus status,
            UUID tenantId,
            String tenantName,
            String tenantSlug,
            String tenantPlan
    ) {}

    public record InviteUserRequest(
            @NotBlank(message = "O e-mail é obrigatório")
            @Email(message = "Formato de e-mail inválido")
            String email,

            @NotBlank(message = "O nome completo é obrigatório")
            String fullName,

            @NotNull(message = "O perfil (ADMINISTRADOR ou PROFESSOR) é obrigatório")
            Role role,

            String phone,
            String password
    ) {}

    public record PasswordRecoveryRequest(
            @NotBlank(message = "O e-mail é obrigatório")
            @Email(message = "Formato de e-mail inválido")
            String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "O e-mail é obrigatório")
            @Email(message = "Formato de e-mail inválido")
            String email,

            @NotBlank(message = "A nova senha é obrigatória")
            String newPassword
    ) {}

    public record PasswordRecoveryResponse(
            String message
    ) {}
}
