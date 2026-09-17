# Backend — TiendaRifas

Backend de TiendaRifas — venta de rifas \(Java 21, Spring Boot 4.1.1, Maven, PostgreSQL 16, Flyway).

## Documentación

La documentación completa está en [`/docs`](./docs/README.md), organizada módulo por módulo:

| Módulo | Documento |
|---|---|
| Auth | [docs/01-auth.md](./docs/01-auth.md) |
| Admin — Usuarios | [docs/02-admin-users.md](./docs/02-admin-users.md) |
| Admin — RBAC | [docs/03-admin-rbac.md](./docs/03-admin-rbac.md) |
| Auditoría | [docs/04-audit.md](./docs/04-audit.md) |
| Notificaciones | [docs/05-notifications.md](./docs/05-notifications.md) |
| Almacenamiento | [docs/06-storage.md](./docs/06-storage.md) |
| Pagos | [docs/07-payments.md](./docs/07-payments.md) |
| Seguridad | [docs/08-security.md](./docs/08-security.md) |
| Infraestructura | [docs/09-infrastructure.md](./docs/09-infrastructure.md) |

Pendientes documentados: [PENDIENTES.md](./PENDIENTES.md)

## Quick start

```bash
# 1. Levantar infraestructura
docker compose up -d postgres mailhog

# 2. Configurar entorno
#    Copiar .env.example a .env y ajustar si es necesario

# 3. Ejecutar
./mvnw.cmd spring-boot:run
```

- API: http://localhost:8080/api
- Swagger: http://localhost:8080/api/docs/swagger
- MailHog UI: http://localhost:8025
- Health: http://localhost:8080/api/actuator/health

## Módulos

```
modules/
├── auth/          # Autenticación y tokens
├── admin/         # Usuarios, roles, permisos, auditoría
├── notification/  # Email + notificaciones in-app
├── storage/       # Archivos (local + Cloudinary)
└── payments/      # Pagos (InMemory + Stripe + PayPal)
```

## Licencia de uso

Plantilla base para proyectos. Copiar y adaptar.