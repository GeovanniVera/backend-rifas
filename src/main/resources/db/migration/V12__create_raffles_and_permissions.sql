-- V12__create_raffles_and_permissions.sql
-- Módulo de rifas: toma un producto, lo aparta del stock y lo rifa con margen.

-- =============================================
-- TABLA: raffles
-- Una rifa referencia un producto (que se aparta del stock de venta).
-- ticket_price se calcula: (price × (1 + margin/100)) / ticket_count
-- =============================================
CREATE TABLE raffles (
    id              VARCHAR(36) PRIMARY KEY,
    product_id      VARCHAR(36) NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    name            VARCHAR(255) NOT NULL,
    description     TEXT,
    margin_percent  NUMERIC(5, 2) NOT NULL DEFAULT 7.00,
    ticket_count    INT NOT NULL,
    ticket_price    NUMERIC(12, 2) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE, DRAW, FINISHED, CANCELLED
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_raffles_status ON raffles(status);
CREATE INDEX idx_raffles_product ON raffles(product_id);
CREATE INDEX idx_raffles_created ON raffles(created_at);

-- Permisos del módulo de rifas
INSERT INTO permissions (id, name, description) VALUES
    ('perm-raffles-read',  'raffles.read',  'Ver rifas'),
    ('perm-raffles-write', 'raffles.write', 'Crear/editar/eliminar rifas');

-- Admin: todos los permisos de rifas
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-admin', 'perm-raffles-read'),
    ('role-admin', 'perm-raffles-write');

-- Editor: ver y gestionar rifas
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-editor', 'perm-raffles-read'),
    ('role-editor', 'perm-raffles-write');

-- Viewer: ver rifas (tienda)
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-viewer', 'perm-raffles-read');