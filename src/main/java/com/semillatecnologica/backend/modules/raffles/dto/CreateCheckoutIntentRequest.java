package com.semillatecnologica.backend.modules.raffles.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreateCheckoutIntentRequest(
        @NotNull(message = "raffleId es requerido")
        String raffleId,

        @NotEmpty(message = "Debes seleccionar al menos un boleto")
        List<Integer> ticketNumbers
) {
}
