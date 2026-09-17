# Backend — Documentación por módulo

Base técnica: **Java 21, Spring Boot 4.1.1, Maven, PostgreSQL 16, Flyway**

## Índice de módulos

| # | Módulo | Descripción | Documento |
|---|---|---|---|
| 01 | **Auth** | Registro, login, JWT, refresh, OTP, verificación | [auth.md](./01-auth.md) |
| 02 | **Admin — Usuarios** | Gestión de usuarios, suspensión, reactivación | [admin-users.md](./02-admin-users.md) |
| 03 | **Admin — Roles y Permisos** | RBAC, catálogo de permisos, asignación | [admin-rbac.md](./03-admin-rbac.md) |
| 04 | **Auditoría** | Eventos de seguridad, alcance por permiso | [audit.md](./04-audit.md) |
| 05 | **Notificaciones** | Email, in-app, templates | [notifications.md](./05-notifications.md) |
| 06 | **Almacenamiento** | Local + Cloudinary, validación de archivos | [storage.md](./06-storage.md) |
| 07 | **Pagos** | InMemory + Stripe + PayPal, webhooks | [payments.md](./07-payments.md) |
| 08 | **Seguridad** | JWT, rate limiting, suspensión, OWASP | [security.md](./08-security.md) |
| 09 | **Infraestructura** | Shared, logging, base de datos, migraciones | [infrastructure.md](./09-infrastructure.md) |
| 10 | **Rifas** | Estados, reglas de negocio, ciclo de vida | [raffles.md](./10-raffles.md) |

## Cómo usar esta documentación

- **¿Nuevo en el proyecto?** Empezá por [infrastructure.md](./09-infrastructure.md) y [auth.md](./01-auth.md)
- **¿Buscás un endpoint?** Consultá el documento del módulo correspondiente
- **¿Buscás config?** Cada módulo documenta sus properties
- **¿Cambiás un módulo?** Actualizá su documento al mismo tiempo

## Pendientes globales

Ver [PENDIENTES.md](../PENDIENTES.md) en la raíz para los pendientes documentados de la v1.