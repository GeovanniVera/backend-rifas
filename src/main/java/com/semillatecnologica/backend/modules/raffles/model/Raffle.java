package com.semillatecnologica.backend.modules.raffles.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Rifa de un producto de la tienda.
 *
 * <p>Al crear una rifa, el producto se aparta del stock de venta.
 * El precio por boleto se calcula con {@link BigDecimal}:
 * {@code (price × (1 + margin/100)) / ticketCount}</p>
 */
@Entity
@Table(name = "raffles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Raffle {

    public enum Status {
        ACTIVE, DRAW, FINISHED, CANCELLED
    }

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "product_id", nullable = false, length = 36)
    private String productId;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "margin_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal marginPercent;

    @Column(name = "ticket_count", nullable = false)
    private int ticketCount;

    @Column(name = "tickets_sold", nullable = false)
    @Builder.Default
    private int ticketsSold = 0;

    @Column(name = "ticket_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal ticketPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}