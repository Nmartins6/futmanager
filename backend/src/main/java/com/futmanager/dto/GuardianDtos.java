package com.futmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public class GuardianDtos {

    public record GuardianRequest(
            @NotBlank(message = "O nome completo do responsável é obrigatório")
            @Size(max = 255, message = "O nome não pode exceder 255 caracteres")
            String fullName,

            @NotBlank(message = "O telefone/WhatsApp é obrigatório")
            @Size(max = 50, message = "O telefone não pode exceder 50 caracteres")
            String phone,

            @jakarta.validation.constraints.Pattern(
                regexp = "^$|^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,7}$",
                message = "Formato de e-mail inválido. Digite um e-mail válido como usuario@exemplo.com"
            )
            String email
    ) {}

    public record GuardianResponse(
            UUID id,
            String fullName,
            String phone,
            String email,
            int linkedStudentsCount,
            Instant createdAt
    ) {}
}
