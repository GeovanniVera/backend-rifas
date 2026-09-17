package com.semillatecnologica.backend.modules.auth.repository;

import com.semillatecnologica.backend.modules.auth.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repositorio de refresh tokens del sistema.
 *
 * <p>Proporciona consultas para rotación, detección de reutilización
 * y limpieza de tokens expirados.</p>
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, String> {

    /**
     * Busca un refresh token por su hash.
     *
     * @param tokenHash Hash del token a buscar
     * @return El token si existe, vacío si no
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Busca todos los tokens activos de una familia.
     *
     * @param familyId ID de la familia de tokens
     * @return Lista de tokens de la familia
     */
    List<RefreshToken> findByFamilyId(String familyId);

    /**
     * Cuenta cuántos tokens activos tiene un usuario.
     *
     * @param userId ID del usuario
     * @return Cantidad de tokens no revocados y no expirados
     */
    @Query("SELECT COUNT(rt) FROM RefreshToken rt " +
           "WHERE rt.user.id = :userId " +
           "AND rt.revokedAt IS NULL " +
           "AND rt.expiresAt > CURRENT_TIMESTAMP")
    long countActiveTokensByUserId(@Param("userId") String userId);

    /**
     * Elimina todos los tokens expirados de un usuario.
     *
     * @param userId ID del usuario
     * @param now Fecha actual para comparar expiración
     */
    void deleteByUserIdAndExpiresAtBefore(String userId, LocalDateTime now);

    /**
     * Revoca todos los tokens activos de un usuario.
     *
     * @param userId ID del usuario
     * @param revokedAt Timestamp de revocación
     */
    @Modifying
    @Query(value = "UPDATE refresh_tokens SET revoked_at = :revokedAt " +
           "WHERE user_id = :userId AND revoked_at IS NULL", nativeQuery = true)
    void revokeAllByUserId(@Param("userId") String userId, @Param("revokedAt") LocalDateTime revokedAt);

    /**
     * Elimina todos los tokens expirados (cleanup batch).
     *
     * @param now Fecha actual para comparar expiración
     * @return Cantidad de tokens eliminados
     */
    @Modifying
    @Query(value = "DELETE FROM refresh_tokens WHERE expires_at < :now", nativeQuery = true)
    int deleteAllExpired(@Param("now") LocalDateTime now);
}
