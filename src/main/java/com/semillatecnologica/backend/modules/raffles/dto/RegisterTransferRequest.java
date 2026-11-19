package com.semillatecnologica.backend.modules.raffles.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterTransferRequest(
        @NotBlank(message = "El ID de intención de pago es obligatorio")
        String paymentIntentId,
        String trackingReference
) {
}
