package com.semillatecnologica.backend.modules.notification.inapp.dto;

import com.semillatecnologica.backend.modules.notification.inapp.model.InAppNotification;

import java.time.LocalDateTime;

/**
 * DTO de respuesta para notificaciones in-app.
 *
 * <p>El frontend consume este formato para renderizar la lista
 * de notificaciones y el badge de no leídas.</p>
 */
public record InAppNotificationResponse(
    String id,
    String type,
    String title,
    String body,
    String entityType,
    String entityId,
    String actionUrl,
    boolean read,
    LocalDateTime createdAt
) {
    public static InAppNotificationResponse from(InAppNotification entity) {
        return new InAppNotificationResponse(
                entity.getId(),
                entity.getType(),
                entity.getTitle(),
                entity.getBody(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getActionUrl(),
                entity.isRead(),
                entity.getCreatedAt()
        );
    }
}
