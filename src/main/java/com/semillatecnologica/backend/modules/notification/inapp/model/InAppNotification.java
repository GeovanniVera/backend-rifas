package com.semillatecnologica.backend.modules.notification.inapp.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Notificación in-app persistida para mostrar en la UI del frontend.
 *
 * <p>Cada registro representa un evento relevante para el usuario:
 * inicio de sesión, cambio de estado, comentario, etc.</p>
 *
 * <p>Los campos entity_type y entity_id vinculan la notificación
 * a una entidad concreta para deep-links y contexto.</p>
 */
@Entity
@Table(name = "in_app_notifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InAppNotification {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(nullable = false, length = 50)
    private String type;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "entity_id", length = 36)
    private String entityId;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Marca la notificación como leída.
     */
    public void markAsRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    /**
     * Indica si la notificación fue leída.
     */
    public boolean isRead() {
        return this.readAt != null;
    }
}
