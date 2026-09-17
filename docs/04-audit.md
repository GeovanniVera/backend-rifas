# Módulo de Auditoría

Registro inmutable de eventos de seguridad y negocio.

## Responsabilidades

- Persistir eventos de seguridad (login, logout, suspensión, etc.)
- Consulta con filtros (acción, actor, entidad, rango de fechas)
- **Alcance por permisos**: todo o solo lo propio

## Estructura

```
modules/admin/
├── controller/AdminAuditController.java
├── model/AuditLog.java
└── repository/AuditLogRepository.java
```

## Endpoint

| Método | Ruta | Permiso | Descripción |
|---|---|---|---|
| GET | `/admin/audit-logs` | `audit.read` o `audit.read-mine` | Consulta con filtros |

### Filtros

| Parámetro | Tipo | Descripción |
|---|---|---|
| `action` | string | Tipo de acción |
| `actorId` | string | ID del actor |
| `entityType` | string | Tipo de entidad |
| `entityId` | string | ID de entidad |
| `from` / `to` | ISO 8601 | Rango de fechas |
| `page` / `size` | int | Paginación |

## Alcance por permisos

| Permiso | Comportamiento |
|---|---|
| `audit.read` | Ve **todos** los eventos |
| `audit.read-mine` | Fuerza `actorId = usuario actual` → solo lo propio |

```
¿Tiene audit.read? → devuelve todo
¿Solo audit.read-mine? → filtra por actorId = usuario autenticado
```

## Eventos registrados

| Evento | Cuándo |
|---|---|
| `LOGIN_SUCCEEDED` | Login exitoso |
| `LOGIN_FAILED` | Login fallido |
| `LOGOUT` | Cierre de sesión |
| `ACCOUNT_SUSPENDED` | Cuenta suspendida |
| `ACCOUNT_REACTIVATED` | Cuenta reactivada |

## Modelo

| Campo | Tipo |
|---|---|
| `id`, `requestId` | string |
| `actorId` | string (quién) |
| `action` | string |
| `entityType`, `entityId` | string (qué) |
| `beforeJson`, `afterJson` | jsonb (estado antes/después) |
| `ipAddress`, `userAgent` | contexto |
| `createdAt` | timestamp |

## Configuración

```yaml
audit:
  enabled: true   # desactiva la persistencia de auditoría
```