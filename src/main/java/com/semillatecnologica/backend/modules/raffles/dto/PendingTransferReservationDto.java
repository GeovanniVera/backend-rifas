package com.semillatecnologica.backend.modules.raffles.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PendingTransferReservationDto(
        String paymentIntentId,
        String raffleId,
        String raffleName,
        String userId,
        String userName,
        String userEmail,
        List<Integer> ticketNumbers,
        BigDecimal totalAmount,
        String referenceCode,
        String trackingReference,
        LocalDateTime reservedAt,
        LocalDateTime expiresAt,
        long minutesRemaining,
        boolean expired
) {
}
