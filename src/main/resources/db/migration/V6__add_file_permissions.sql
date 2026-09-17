-- V6__add_file_permissions.sql
-- Permisos para el módulo de almacenamiento de archivos.

-- Permisos
INSERT INTO permissions (id, name, description) VALUES
    ('perm-files-read',   'files.read',   'Descargar/ver archivos'),
    ('perm-files-upload', 'files.upload', 'Subir archivos'),
    ('perm-files-delete', 'files.delete', 'Eliminar archivos');

-- Admin: todos los permisos de archivos
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-admin', 'perm-files-read'),
    ('role-admin', 'perm-files-upload'),
    ('role-admin', 'perm-files-delete');

-- Editor: subir y ver
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-editor', 'perm-files-read'),
    ('role-editor', 'perm-files-upload');

-- Viewer: solo ver
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-viewer', 'perm-files-read');
