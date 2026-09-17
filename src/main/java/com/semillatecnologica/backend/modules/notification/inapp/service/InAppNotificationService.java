package com.semillatecnologica.backend.modules.notification.inapp.service;

import com.semillatecnologica.backend.modules.notification.inapp.dto.InAppNotificationResponse;
import com.semillatecnologica.backend.modules.notification.inapp.model.InAppNotification;
import com.semillatecnologica.backend.modules.notification.inapp.repository.InAppNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servicio de notificaciones in-app.
 *
 * <p>Responsabilidades:
 * <ul>
 *   <li>Crear notificaciones persistidas para el frontend</li>
 *   <li>Listar, filtrar y paginar notificaciones de un usuario</li>
 *   <li>Marcar como leídas (individual o todas)</li>
 *   <li>Contar no leídas para el badge</li>
 *   <li>Limpiar notificaciones antiguas</li>
 * </ul>
 *
 * <p>Este servicio NO expone endpoints REST. Los controllers
 * lo consumen internamente.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InAppNotificationService {

    private final InAppNotificationRepository repository;

    /**
     * Crea una notificación in-app para un usuario.
     *
     * @param userId ID del usuario destinatario
     * @param type Tipo: SECURITY, WORKFLOW, SYSTEM, SOCIAL
     * @param title Título corto de la notificación
     * @param body Cuerpo detallado
     * @param entityType Tipo de entidad relacionada (nullable)
     * @param entityId ID de la entidad relacionada (nullable)
     * @param actionUrl URL de deep-link (nullable)
     * @return la notificación creada
     */
    @Transactional
    public InAppNotification create(String userId, String type, String title, String body,
                                     String entityType, String entityId, String actionUrl) {
        InAppNotification notification = InAppNotification.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .type(type)
                .title(title)
                .body(body)
                .entityType(entityType)
                .entityId(entityId)
                .actionUrl(actionUrl)
                .build();

        InAppNotification saved = repository.save(notification);
        log.debug("Notificación in-app creada: userId={} type={} title={}", userId, type, title);
        return saved;
    }

    /**
     * Lista notificaciones de un usuario con filtros.
     *
     * @param userId ID del usuario
     * @param type Filtro por tipo (nullable = todos)
     * @param unreadOnly Solo no leídas
     * @param pageable Paginación
     * @return página de notificaciones
     */
    @Transactional(readOnly = true)
    public Page<InAppNotificationResponse> listByUser(String userId, String type,
                                                       boolean unreadOnly, Pageable pageable) {
        return repository.findByUserId(userId, type, unreadOnly, pageable)
                .map(InAppNotificationResponse::from);
    }

    /**
     * Cuenta las notificaciones no leídas de un usuario.
     *
     * @param userId ID del usuario
     * @return cantidad de no leídas
     */
    @Transactional(readOnly = true)
    public long countUnread(String userId) {
        return repository.countUnreadByUserId(userId);
    }

    /**
     * Marca una notificación como leída.
     *
     * @param notificationId ID de la notificación
     * @param userId ID del usuario (verificación de propiedad)
     * @return true si se marcó, false si no existe o no pertenece al usuario
     */
    @Transactional
    public boolean markAsRead(String notificationId, String userId) {
        return repository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .map(n -> {
                    n.markAsRead();
                    repository.save(n);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Marca todas las notificaciones no leídas de un usuario como leídas.
     *
     * @param userId ID del usuario
     * @return cantidad de notificaciones marcadas
     */
    @Transactional
    public int markAllAsRead(String userId) {
        int count = repository.markAllAsReadByUserId(userId);
        log.debug("Notificaciones marcadas como leídas: userId={} count={}", userId, count);
        return count;
    }

    /**
     * Elimina una notificación (archivado lógico = delete físico).
     *
     * @param notificationId ID de la notificación
     * @param userId ID del usuario (verificación de propiedad)
     * @return true si se eliminó
     */
    @Transactional
    public boolean delete(String notificationId, String userId) {
        return repository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .map(n -> {
                    repository.delete(n);
                    return true;
                })
                .orElse(false);
    }

    /**
     * Limpia notificaciones antiguas de un usuario.
     *
     * @param userId ID del usuario
     * @param daysOld Días de antigüedad mínima
     * @return cantidad eliminadas
     */
    @Transactional
    public int cleanOld(String userId, int daysOld) {
        var cutoff = java.time.LocalDateTime.now().minusDays(daysOld);
        int deleted = repository.deleteOlderThan(userId, cutoff);
        log.debug("Notificaciones antiguas eliminadas: userId={} days={} deleted={}", userId, daysOld, deleted);
        return deleted;
    }
}
