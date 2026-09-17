package com.semillatecnologica.backend.modules.auth.repository;

import com.semillatecnologica.backend.modules.auth.model.OtpChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repositorio de desafíos OTP.
 *
 * <p>Proporciona consultas para recuperación de contraseña
 * y control de intentos.</p>
 */
@Repository
public interface OtpChallengeRepository extends JpaRepository<OtpChallenge, String> {

    /**
     * Busca un desafío activo (no expirado, no agotado) por usuario y propósito.
     *
     * @param userId ID del usuario
     * @param purpose Propósito del desafío (password_reset)
     * @return El desafío activo si existe, vacío si no
     */
    @Query("SELECT oc FROM OtpChallenge oc " +
           "WHERE oc.user.id = :userId " +
           "AND oc.purpose = :purpose " +
           "AND oc.expiresAt > CURRENT_TIMESTAMP " +
           "AND oc.attempts < oc.maxAttempts " +
           "ORDER BY oc.createdAt DESC " +
           "LIMIT 1")
    Optional<OtpChallenge> findActiveByUserIdAndPurpose(
            @Param("userId") String userId,
            @Param("purpose") String purpose);

    /**
     * Invalida todos los desafíos activos de un usuario para un propósito dado.
     *
     * @param userId ID del usuario
     * @param purpose Propósito de los desafíos a invalidar
     */
    @Modifying
    @Query("UPDATE OtpChallenge oc SET oc.attempts = oc.maxAttempts " +
           "WHERE oc.user.id = :userId " +
           "AND oc.purpose = :purpose " +
           "AND oc.attempts < oc.maxAttempts")
    void invalidateAllByUserIdAndPurpose(
            @Param("userId") String userId,
            @Param("purpose") String purpose);

    /**
     * Elimina todos los desafíos expirados (cleanup batch).
     *
     * @param now Fecha actual para comparar expiración
     * @return Cantidad de desafíos eliminados
     */
    @Modifying
    @Query("DELETE FROM OtpChallenge oc WHERE oc.expiresAt < :now")
    int deleteAllExpired(@Param("now") LocalDateTime now);
}
