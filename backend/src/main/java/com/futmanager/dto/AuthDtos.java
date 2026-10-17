package com.futmanager.dto;

import com.futmanager.domain.enums.Role;
import com.futmanager.domain.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
            String tenantPlan,
            boolean emailVerified
    ) {
        // Construtor de retrocompatibilidade com 10 parâmetros
        public LoginResponse(
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
        ) {
            this(token, id, email, fullName, role, status, tenantId, tenantName, tenantSlug, tenantPlan, false);
        }
    }

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

    public record ForgotPasswordRequest(
            @NotBlank(message = "O e-mail é obrigatório")
            @Pattern(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,7}$",
                message = "Formato de e-mail inválido. Digite um e-mail válido como usuario@exemplo.com"
            )
            String email
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
            String email,
            String code,
            String token,

            @NotBlank(message = "A nova senha é obrigatória")
            @Size(min = 6, message = "A nova senha deve ter no mínimo 6 caracteres")
            String newPassword
    ) {
        public ResetPasswordRequest(String token, String newPassword) {
            this(null, null, token, newPassword);
        }
    }

    public record ResetPasswordWithTokenRequest(
            @NotBlank(message = "O token de recuperação é obrigatório")
            String token,

            @NotBlank(message = "A nova senha é obrigatória")
            @Size(min = 6, message = "A nova senha deve ter no mínimo 6 caracteres")
            String newPassword
    ) {}

    public record VerifyEmailRequest(
            @NotBlank(message = "O token de verificação é obrigatório")
            String token
    ) {}

    public record ResendVerificationRequest(
            @NotBlank(message = "O e-mail é obrigatório")
            @Pattern(
                regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,7}$",
                message = "Formato de e-mail inválido. Digite um e-mail válido como usuario@exemplo.com"
            )
            String email
    ) {}

    public record PasswordRecoveryResponse(
            String message,
            String code
    ) {}

    public record GenericMessageResponse(
            String message
    ) {}

    public record TokenValidationResponse(
            boolean valid,
            String email,
            String message
    ) {}
}
