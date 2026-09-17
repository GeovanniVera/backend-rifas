-- V10__add_audit_mine_permission.sql
-- Permiso para ver solo las propias acciones de auditoría.
-- audit.read = ver todo (admin), audit.read-mine = ver solo lo propio (cualquier usuario).

INSERT INTO permissions (id, name, description) VALUES
    ('perm-audit-read-mine', 'audit.read-mine', 'Ver solo sus propios eventos de auditoría');

-- Editor y Viewer: ver solo sus propias acciones
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-editor', 'perm-audit-read-mine'),
    ('role-viewer', 'perm-audit-read-mine');