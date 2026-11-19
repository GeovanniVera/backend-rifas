package com.semillatecnologica.backend.modules.raffles.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un boleto individual de una rifa.
 *
 * <p>Cada rifa genera boletos numerados del 1 a N (ticketCount).
 * Se cuenta con una restricción UNIQUE(raffle_id, ticket_number) en base de datos
 * para garantizar exclusión mutua.</p>
 */
@Entity
@Table(name = "raffle_tickets", uniqueConstraints = {
        @UniqueConstraint(name = "uk_raffle_ticket", columnNames = {"raffle_id", "ticket_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RaffleTicket {

    public enum Status {
        AVAILABLE,
        RESERVED,
        SOLD
    }

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "raffle_id", nullable = false, length = 36)
    private String raffleId;

    @Column(name = "ticket_number", nullable = false)
    private int ticketNumber;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.AVAILABLE;

    @Column(name = "payment_intent_id")
    private String paymentIntentId;

    @Column(name = "payment_method", length = 30)
    @Builder.Default
    private String paymentMethod = "CARD";

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "purchased_at")
    private LocalDateTime purchasedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
