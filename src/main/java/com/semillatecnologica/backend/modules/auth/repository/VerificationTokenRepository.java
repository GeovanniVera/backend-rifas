package com.semillatecnologica.backend.modules.auth.repository;

import com.semillatecnologica.backend.modules.auth.model.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositorio de tokens de verificación.
 *
 * <p>Proporciona consultas para verificación de correo y recuperación
 * de contraseña.</p>
 */
@Repository
public interface VerificationTokenRepository extends JpaRepository<VerificationToken, String> {

    /**
     * Busca un token por su hash y propósito.
     *
     * @param tokenHash Hash del token a buscar
     * @param purpose Propósito del token (email_verification, password_reset)
     * @return El token si existe, vacío si no
     */
    Optional<VerificationToken> findByTokenHashAndPurpose(String tokenHash, String purpose);

    /**
     * Busca un token válido (no usado y no expirado) por hash y propósito.
     *
     * @param tokenHash Hash del token
     * @param purpose Propósito del token
     * @return El token válido si existe, vacío si no
     */
    @Query("SELECT vt FROM VerificationToken vt " +
           "WHERE vt.tokenHash = :tokenHash " +
           "AND vt.purpose = :purpose " +
           "AND vt.usedAt IS NULL " +
           "AND vt.expiresAt > CURRENT_TIMESTAMP")
    Optional<VerificationToken> findValidByTokenHashAndPurpose(
            @Param("tokenHash") String tokenHash,
            @Param("purpose") String purpose);

    /**
     * Invalida todos los tokens de un usuario para un propósito dado.
     *
     * @param userId ID del usuario
     * @param purpose Propósito de los tokens a invalidar
     */
    @Modifying
    @Query("UPDATE VerificationToken vt SET vt.usedAt = CURRENT_TIMESTAMP " +
           "WHERE vt.user.id = :userId " +
           "AND vt.purpose = :purpose " +
           "AND vt.usedAt IS NULL")
    void invalidateAllByUserIdAndPurpose(
            @Param("userId") String userId,
            @Param("purpose") String purpose);

    /**
     * Elimina todos los tokens expirados (cleanup batch).
     *
     * @param now Fecha actual para comparar expiración
     * @return Cantidad de tokens eliminados
     */
    @Modifying
    @Query("DELETE FROM VerificationToken vt WHERE vt.expiresAt < :now")
    int deleteAllExpired(@Param("now") LocalDateTime now);
}
