# Módulo Admin — Usuarios

Gestión administrativa de usuarios del sistema.

## Responsabilidades

- Listar usuarios (paginado)
- Ver detalle de usuario
- Suspender y reactivar cuentas (revoca sesiones)
- La edición de perfil es del propio usuario (`PUT /auth/me`), no del admin

## Estructura

```
modules/admin/
├── controller/AdminUserController.java
├── dto/            # UserResponse, UpdateUserRequest...
├── service/        # AdminUserService, AssignmentService, AuditService
├── model/          # AuditLog
└── repository/     # AuditLogRepository
```

## Endpoints

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/admin/users` | `users.read` | Listar usuarios (paginado) |
| GET | `/admin/users/{id}` | `users.read` | Detalle de usuario |
| POST | `/admin/users/{id}/suspend` | `users.write` | Suspender (revoca sesiones) |
| POST | `/admin/users/{id}/reactivate` | `users.write` | Reactivar |

## Decisión de diseño

El admin **NO edita** el perfil del usuario (nombre/email/foto).
Esa es responsabilidad del propio usuario desde `PUT /auth/me`.

El admin solo hace **gestión**: ver, suspender, reactivar.
La eliminación de cuentas está deshabilitada — el patrón futuro es
suspender → gracia de 5 meses → auto-eliminación con aviso.

## Suspensión

```
POST /admin/users/{id}/suspend
  → user.suspended = true
  → refresh tokens revocados
  → el próximo request con JWT → 403 ACCOUNT_SUSPENDED
```

## Auditoría

Cada operación registra eventos: `ACCOUNT_SUSPENDED`, `ACCOUNT_REACTIVATED`.