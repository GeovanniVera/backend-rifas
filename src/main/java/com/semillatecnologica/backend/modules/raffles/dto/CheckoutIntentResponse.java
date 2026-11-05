package com.semillatecnologica.backend.modules.raffles.dto;

import java.math.BigDecimal;
import java.util.List;

public record CheckoutIntentResponse(
        String clientSecret,
        String paymentIntentId,
        long amountInCents,
        BigDecimal totalAmount,
        String currency,
        List<Integer> ticketNumbers
) {
}
