package com.semillatecnologica.backend.modules.raffles.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OxxoReservationResponse(
        String paymentIntentId,
        String paymentMethod,
        LocalDateTime expiresAt,
        long expiresInMinutes,
        List<Integer> ticketNumbers
) {
}
