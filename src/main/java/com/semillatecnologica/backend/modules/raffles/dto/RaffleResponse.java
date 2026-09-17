package com.semillatecnologica.backend.modules.raffles.dto;

import com.semillatecnologica.backend.modules.raffles.model.Raffle;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta de una rifa.
 */
@Schema(description = "Rifa de un producto")
public record RaffleResponse(
    String id,
    String productId,
    String productName,
    String productImageUrl,
    String name,
    String description,
    BigDecimal marginPercent,
    int ticketCount,
    int ticketsSold,
    BigDecimal ticketPrice,
    Raffle.Status status,
    LocalDateTime createdAt
) {
    public static RaffleResponse from(Raffle raffle, String productName, String productImageUrl) {
        return new RaffleResponse(
                raffle.getId(),
                raffle.getProductId(),
                productName,
                productImageUrl,
                raffle.getName(),
                raffle.getDescription(),
                raffle.getMarginPercent(),
                raffle.getTicketCount(),
                raffle.getTicketsSold(),
                raffle.getTicketPrice(),
                raffle.getStatus(),
                raffle.getCreatedAt()
        );
    }
}