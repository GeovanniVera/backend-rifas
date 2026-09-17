-- V2__seed_default_roles.sql
-- Roles base del sistema requeridos por la lógica de autenticación.

-- Roles
INSERT INTO roles (id, name, description) VALUES
    ('role-admin', 'admin', 'Administrador con acceso total al sistema'),
    ('role-editor', 'editor', 'Editor con permisos de escritura'),
    ('role-viewer', 'viewer', 'Rol predeterminado — solo lectura');

-- Permisos: granularidad por recurso
INSERT INTO permissions (id, name, description) VALUES
    ('perm-users-read',       'users.read',       'Ver usuarios'),
    ('perm-users-write',      'users.write',      'Editar/eliminar usuarios'),
    ('perm-roles-read',       'roles.read',       'Ver roles'),
    ('perm-roles-write',      'roles.write',      'Crear/editar/eliminar roles'),
    ('perm-roles-assign',     'roles.assign',     'Asignar roles a usuarios'),
    ('perm-permissions-read', 'permissions.read', 'Ver catálogo de permisos'),
    ('perm-audit-read',       'audit.read',       'Consultar auditoría');

-- Admin: todos los permisos
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-admin', 'perm-users-read'),
    ('role-admin', 'perm-users-write'),
    ('role-admin', 'perm-roles-read'),
    ('role-admin', 'perm-roles-write'),
    ('role-admin', 'perm-roles-assign'),
    ('role-admin', 'perm-permissions-read'),
    ('role-admin', 'perm-audit-read');

-- Editor: lectura y escritura en usuarios y roles
INSERT INTO role_permissions (role_id, permission_id) VALUES
    ('role-editor', 'perm-users-read'),
    ('role-editor', 'perm-users-write'),
    ('role-editor', 'perm-roles-read'),
    ('role-editor', 'perm-roles-write'),
    ('role-editor', 'perm-permissions-read');

-- Viewer: sin permisos de administración
-- Su único permiso (files.read) se agrega en V6 (módulo de almacenamiento).
-- El viewer NO accede a la gestión de usuarios/roles/permisos.
