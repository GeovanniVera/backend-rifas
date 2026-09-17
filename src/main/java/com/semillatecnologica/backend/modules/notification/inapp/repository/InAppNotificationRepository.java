package com.semillatecnologica.backend.modules.notification.inapp.repository;

import com.semillatecnologica.backend.modules.notification.inapp.model.InAppNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Repositorio de notificaciones in-app.
 *
 * <p>Proporciona consultas para el frontend: listing paginado,
 * conteo de no leídas, y marcado como leídas.</p>
 */
@Repository
public interface InAppNotificationRepository extends JpaRepository<InAppNotification, String> {

    /**
     * Lista notificaciones de un usuario con filtros opcionales.
     *
     * <p>Hibernate 6 / PostgreSQL: se usa SQL nativo para evitar
     * problemas con parámetros null en JPQL.</p>
     */
    @Query(value = "SELECT n.* FROM in_app_notifications n WHERE " +
           "n.user_id = :userId AND " +
           "COALESCE(:type, n.type) = n.type AND " +
           "(:unreadOnly = false OR n.read_at IS NULL) " +
           "ORDER BY n.created_at DESC",
           countQuery = "SELECT COUNT(*) FROM in_app_notifications n WHERE " +
           "n.user_id = :userId AND " +
           "COALESCE(:type, n.type) = n.type AND " +
           "(:unreadOnly = false OR n.read_at IS NULL)",
           nativeQuery = true)
    Page<InAppNotification> findByUserId(
            @Param("userId") String userId,
            @Param("type") String type,
            @Param("unreadOnly") boolean unreadOnly,
            Pageable pageable);

    /**
     * Cuenta las notificaciones no leídas de un usuario.
     */
    @Query(value = "SELECT COUNT(*) FROM in_app_notifications n " +
           "WHERE n.user_id = :userId AND n.read_at IS NULL",
           nativeQuery = true)
    long countUnreadByUserId(@Param("userId") String userId);

    /**
     * Marca todas las notificaciones no leídas de un usuario como leídas.
     */
    @Modifying
    @Query(value = "UPDATE in_app_notifications n SET read_at = NOW() " +
           "WHERE n.user_id = :userId AND n.read_at IS NULL",
           nativeQuery = true)
    int markAllAsReadByUserId(@Param("userId") String userId);

    /**
     * Elimina notificaciones más antiguas que la fecha dada.
     */
    @Modifying
    @Query(value = "DELETE FROM in_app_notifications n " +
           "WHERE n.user_id = :userId AND n.created_at < :before",
           nativeQuery = true)
    int deleteOlderThan(@Param("userId") String userId, @Param("before") LocalDateTime before);
}
