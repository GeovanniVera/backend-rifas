package com.semillatecnologica.backend.modules.raffles.dto;

import jakarta.validation.constraints.NotBlank;

public record SimulateOxxoRequest(
        @NotBlank(message = "paymentIntentId es requerido")
        String paymentIntentId
) {
}
