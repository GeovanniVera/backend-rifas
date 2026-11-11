package com.semillatecnologica.backend.modules.raffles.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterOxxoRequest(
        @NotBlank(message = "paymentIntentId es requerido")
        String paymentIntentId
) {
}
