-- V4__create_in_app_notifications_table.sql
-- Tabla de notificaciones in-app: persiste para mostrar en la UI del frontend.

-- =============================================
-- TABLA: in_app_notifications
-- Notificaciones que se almacenan y se muestran en la aplicación.
-- Cada registro es inmutable excepto read_at (se marca al leer).
-- =============================================
CREATE TABLE in_app_notifications (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type        VARCHAR(50) NOT NULL,   -- SECURITY, WORKFLOW, SYSTEM, SOCIAL
    title       VARCHAR(255) NOT NULL,
    body        TEXT NOT NULL,
    entity_type VARCHAR(50),            -- "REQUEST", "USER", null
    entity_id   VARCHAR(36),            -- ID de la entidad relacionada
    action_url  VARCHAR(500),           -- deep-link para el frontend
    read_at     TIMESTAMP,              -- null = no leído
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_inapp_notif_user ON in_app_notifications(user_id);
CREATE INDEX idx_inapp_notif_unread ON in_app_notifications(user_id, created_at DESC) WHERE read_at IS NULL;
CREATE INDEX idx_inapp_notif_created ON in_app_notifications(created_at);
