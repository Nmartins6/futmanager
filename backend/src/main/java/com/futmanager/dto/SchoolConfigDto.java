package com.futmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class SchoolConfigDto {

    public record SchoolConfigResponse(
            String schoolName,
            String cnpj,
            String phone,
            String address,
            String timezone,
            int lessonHorizonDays,
            double lowFrequencyThreshold
    ) {}

    public record SchoolConfigRequest(
            @NotBlank(message = "O nome da escolinha é obrigatório")
            String schoolName,
            String cnpj,
            String phone,
            String address,
            @NotBlank(message = "O fuso horário é obrigatório")
            String timezone,
            @NotNull(message = "O horizonte de dias é obrigatório")
            Integer lessonHorizonDays,
            @NotNull(message = "O limite de baixa frequência é obrigatório")
            Double lowFrequencyThreshold
    ) {}
}
