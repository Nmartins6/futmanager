package com.futmanager.dto;

import com.futmanager.domain.enums.Role;
import com.futmanager.domain.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

public class AuthDtos {

    public record LoginRequest(
            @NotBlank(message = "O e-mail é obrigatório")
            @Pattern(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,7}$",
                message = "Formato de e-mail inválido. Digite um e-mail válido como usuario@exemplo.com"
            )
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
            @Pattern(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,7}$",
                message = "Formato de e-mail inválido. Digite um e-mail válido como usuario@exemplo.com"
            )
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
            @Pattern(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,7}$",
                message = "Formato de e-mail inválido. Digite um e-mail válido como usuario@exemplo.com"
            )
            String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "O e-mail é obrigatório")
            @Pattern(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,7}$",
                message = "Formato de e-mail inválido. Digite um e-mail válido como usuario@exemplo.com"
            )
            String email,

            @NotBlank(message = "O código de recuperação de 6 dígitos é obrigatório")
            String code,

            @NotBlank(message = "A nova senha é obrigatória")
            String newPassword
    ) {}

    public record PasswordRecoveryResponse(
            String message,
            String code
    ) {}
}
