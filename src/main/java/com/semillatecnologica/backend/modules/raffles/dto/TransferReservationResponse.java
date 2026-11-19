package com.semillatecnologica.backend.modules.raffles.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record TransferReservationResponse(
        String paymentIntentId,
        String paymentMethod,
        String referenceCode,
        String trackingReference,
        String bankName,
        String beneficiary,
        String clabe,
        String accountNumber,
        BigDecimal totalAmount,
        LocalDateTime expiresAt,
        long expiresInMinutes,
        List<Integer> ticketNumbers
) {
}
