# Módulo Admin — Roles y Permisos (RBAC)

Control de acceso basado en permisos (no en roles).

## Principio

La autorización se hace por **permisos**, no por roles.
Los roles pueden cambiar con el tiempo; los permisos son estables.

```
Usuario → roles → permisos efectivos (unión) → se autoriza por permiso
```

## Estructura

```
modules/admin/
├── controller/AdminRoleController.java
└── service/
    ├── AdminRoleService.java    # CRUD roles + catálogo permisos
    └── AssignmentService.java   # Asignación roles ↔ usuarios
```

## Endpoints

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/admin/roles` | `roles.read` | Listar roles |
| GET | `/admin/roles/{id}` | `roles.read` | Detalle de rol |
| POST | `/admin/roles` | `roles.write` | Crear rol (con permissionIds) |
| PUT | `/admin/roles/{id}` | `roles.write` | Actualizar rol (con permissionIds) |
| DELETE | `/admin/roles/{id}` | `roles.write` | Eliminar (no si está asignado) |
| GET | `/admin/permissions` | `permissions.read` | Catálogo de permisos (solo lectura) |
| POST | `/admin/users/{id}/roles` | `roles.assign` | Reemplaza roles del usuario |
| DELETE | `/admin/users/{id}/roles/{roleId}` | `roles.assign` | Remueve un rol |

## Permisos del sistema

| Permiso | Descripción |
|---|---|
| `users.read` / `users.write` | Ver / gestionar usuarios |
| `roles.read` / `roles.write` / `roles.assign` | Roles |
| `permissions.read` | Catálogo |
| `audit.read` | Auditoría (todo) |
| `audit.read-mine` | Auditoría (solo propio) |
| `files.read` / `files.upload` / `files.delete` | Almacenamiento |
| `settings.brand` | Colores de marca |

## Roles base (seed)

| Rol | Permisos |
|---|---|
| `admin` | Todos |
| `editor` | `users.read`, `roles.read`, `roles.write`, `permissions.read`, `audit.read-mine`, `files.read` |
| `viewer` | `audit.read-mine`, `files.read` |

## Nota

Las migraciones del seed están en `db/migration/`:
- V2: roles y permisos base
- V6: permisos de archivos
- V9: permisos de settings
- V10: permiso `audit.read-mine`