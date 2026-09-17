package com.semillatecnologica.backend.modules.auth.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad que representa un desafío OTP para recuperación de contraseña.
 *
 * <p>Se almacena solo el hash del OTP. Se registra el contador de intentos
 * para detectar abuso y bloquear el desafío al alcanzar el límite máximo.</p>
 */
@Entity
@Table(name = "otp_challenges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpChallenge {

    @Id
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "otp_hash", nullable = false)
    private String otpHash;

    /**
     * Propósito del OTP: password_reset.
     */
    @Column(nullable = false, length = 50)
    private String purpose;

    @Column(nullable = false)
    @Builder.Default
    private Integer attempts = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 5;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Indica si el desafío ha expirado.
     *
     * @return true si la fecha de expiración es anterior al momento actual
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Indica si se alcanzó el límite máximo de intentos.
     *
     * @return true si attempts >= maxAttempts
     */
    public boolean isMaxAttemptsReached() {
        return attempts >= maxAttempts;
    }

    /**
     * Incrementa el contador de intentos de forma atómica.
     */
    public void incrementAttempts() {
        this.attempts++;
    }
}
