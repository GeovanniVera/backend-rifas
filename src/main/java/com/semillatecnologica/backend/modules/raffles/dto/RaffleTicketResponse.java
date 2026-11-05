package com.semillatecnologica.backend.modules.raffles.dto;

import com.semillatecnologica.backend.modules.raffles.model.RaffleTicket;

public record RaffleTicketResponse(
        String id,
        String raffleId,
        int ticketNumber,
        String status,
        boolean isMine
) {
    public static RaffleTicketResponse from(RaffleTicket ticket, String currentUserId) {
        boolean isMine = currentUserId != null && currentUserId.equals(ticket.getUserId());
        return new RaffleTicketResponse(
                ticket.getId(),
                ticket.getRaffleId(),
                ticket.getTicketNumber(),
                ticket.getStatus().name(),
                isMine
        );
    }
}
