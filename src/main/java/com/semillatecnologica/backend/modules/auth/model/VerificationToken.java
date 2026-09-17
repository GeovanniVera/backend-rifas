package com.semillatecnologica.backend.modules.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un token de verificación de un solo uso.
 *
 * <p>Se utiliza para verificación de correo y recuperación de contraseña.
 * Se almacena solo el hash del token; el valor original nunca se persiste.</p>
 */
@Entity
@Table(name = "verification_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerificationToken {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    /**
     * Propósito del token: email_verification o password_reset.
     */
    @Column(nullable = false, length = 50)
    private String purpose;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Indica si el token ha sido consumido.
     *
     * @return true si usedAt no es nulo
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Indica si el token ha expirado.
     *
     * @return true si la fecha de expiración es anterior al momento actual
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Marca el token como consumido.
     */
    public void consume() {
        this.usedAt = LocalDateTime.now();
    }
}
