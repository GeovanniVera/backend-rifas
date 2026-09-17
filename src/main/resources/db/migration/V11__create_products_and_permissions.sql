-- V11__create_products_and_permissions.sql
-- Módulo de productos de la tienda + permisos.

-- =============================================
-- TABLA: products
-- Productos de tecnología. El precio se maneja con NUMERIC
-- para precisión monetaria exacta (BigDecimal en Java).
-- =============================================
CREATE TABLE products (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    price       NUMERIC(12, 2) NOT NULL,
    category    VARCHAR(50) NOT NULL,
    stock       INT NOT NULL DEFAULT 0,
    image_file_id VARCHAR(36) REFERENCES stored_files(id) ON DELETE SET NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_created ON products(created_at);

-- Permisos del módulo de productos
INSERT INTO permissions (id, name, description) VALUES
    ('perm-products-read',  'products.read',  'Ver productos'),
    ('perm-products-write', 'products.write', 'Crear/editar/eliminar productos');

-- Admin: todos los permisos de productos
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-admin', 'perm-products-read'),
    ('role-admin', 'perm-products-write');

-- Editor: ver y gestionar productos
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-editor', 'perm-products-read'),
    ('role-editor', 'perm-products-write');

-- Viewer: ver productos (tienda)
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-viewer', 'perm-products-read');