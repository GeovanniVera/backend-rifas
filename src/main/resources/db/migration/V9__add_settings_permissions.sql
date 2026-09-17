-- V9__add_settings_permissions.sql
-- Permisos para el módulo de ajustes.

-- Permisos de ajustes
INSERT INTO permissions (id, name, description) VALUES
    ('perm-settings-brand', 'settings.brand', 'Administrar colores de marca');

-- Admin: todos los permisos de ajustes
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-admin', 'perm-settings-brand');
