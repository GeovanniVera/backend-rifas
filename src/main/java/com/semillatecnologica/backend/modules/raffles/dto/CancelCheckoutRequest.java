package com.semillatecnologica.backend.modules.raffles.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelCheckoutRequest(
        @NotBlank(message = "El ID de intención de pago es obligatorio")
        String paymentIntentId
) {
}
