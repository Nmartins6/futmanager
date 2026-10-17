package com.futmanager.dto;

import com.futmanager.domain.enums.Role;
import com.futmanager.domain.enums.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public class UserDtos {

    public record UserResponse(
            UUID id,
            String email,
            String fullName,
            Role role,
            UserStatus status,
            String phone,
            Instant createdAt,
            boolean emailVerified
    ) {
        public UserResponse(
                UUID id,
                String email,
                String fullName,
                Role role,
                UserStatus status,
                String phone,
                Instant createdAt
        ) {
            this(id, email, fullName, role, status, phone, createdAt, false);
        }
    }

    public record UserUpdateRequest(
            @NotBlank(message = "O nome completo é obrigatório")
            String fullName,

            @NotNull(message = "O perfil é obrigatório")
            Role role,

            @NotNull(message = "A situação é obrigatória")
            UserStatus status,

            String phone
    ) {}
}
