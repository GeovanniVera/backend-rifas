-- V14__remove_store_admin_permissions_from_viewer.sql
-- Quitar permisos de administración de catálogo y rifas (products.read, raffles.read)
-- del rol 'viewer'. El rol 'viewer' está destinado a clientes/compradores,
-- quienes acceden a la tienda mediante los endpoints públicos /products y /raffles.

DELETE FROM role_permissions
WHERE role_id = 'role-viewer'
  AND permission_id IN ('perm-products-read', 'perm-raffles-read');
