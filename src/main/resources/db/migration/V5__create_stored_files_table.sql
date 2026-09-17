-- V5__create_stored_files_table.sql
-- Tabla de metadatos de archivos almacenados.
-- Solo guarda referencia; el archivo vive en disco local o Cloudinary.

-- =============================================
-- TABLA: stored_files
-- Metadatos de archivos subidos al sistema.
-- =============================================
CREATE TABLE stored_files (
    id              VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    original_name   VARCHAR(255) NOT NULL,
    storage_key     VARCHAR(500) NOT NULL UNIQUE,
    mime_type       VARCHAR(100) NOT NULL,
    size_bytes      BIGINT NOT NULL,
    storage_target  VARCHAR(50) NOT NULL,   -- LOCAL, CLOUDINARY
    public_url      VARCHAR(500),           -- null si es privado
    entity_type     VARCHAR(50),            -- vinculación opcional
    entity_id       VARCHAR(36),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stored_files_user ON stored_files(user_id);
CREATE INDEX idx_stored_files_entity ON stored_files(entity_type, entity_id);
CREATE INDEX idx_stored_files_created ON stored_files(created_at);
