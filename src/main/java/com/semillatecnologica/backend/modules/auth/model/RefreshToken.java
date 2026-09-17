package com.semillatecnologica.backend.modules.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un refresh token del sistema.
 *
 * <p>Almacena solo el hash del token original. El valor cleartext nunca se persiste.
 * Soporta rotación por familias y detección de reutilización.</p>
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "token_hash", nullable = false)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "replaced_by", length = 36)
    private String replacedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    /**
     * Indica si el token ha sido utilizado.
     *
     * @return true si usedAt no es nulo
     */
    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Indica si el token ha sido revocado.
     *
     * @return true si revokedAt no es nulo
     */
    public boolean isRevoked() {
        return revokedAt != null;
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
     * Marca el token como utilizado.
     */
    public void markAsUsed() {
        this.usedAt = LocalDateTime.now();
        this.lastUsedAt = LocalDateTime.now();
    }

    /**
     * Marca el token como revocado.
     */
    public void revoke() {
        this.revokedAt = LocalDateTime.now();
    }
}
