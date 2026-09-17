# Infraestructura

Componentes transversales, logging, base de datos y migraciones.

## Estructura

```
backend/
├── src/main/java/com/semillatecnologica/backend/
│   ├── BackendApplication.java     # @EnableAsync + @EnableScheduling
│   ├── modules/                    # auth, admin, notification, storage, payments
│   ├── security/                   # JWT, CORS, SecurityConfig, cookies
│   └── shared/
│       ├── exception/              # GlobalExceptionHandler + excepciones de dominio
│       ├── logging/                # RequestIdFilter, AccessLogFilter, sanitización
│       ├── ratelimit/              # RateLimiterService
│       └── response/ApiResponse.java
├── src/main/resources/
│   ├── application.yaml            # config principal
│   ├── application-dev.yaml        # dev (Postgres local, SQL off)
│   ├── application-test.yaml
│   ├── application-prod.yaml
│   ├── logback-spring.xml          # dev: texto, prod: JSON (logstash)
│   └── db/migration/               # Flyway V1..V10
└── docker-compose.yml              # postgres + mailhog + backend
```

## Response API

Todas las respuestas usan `ApiResponse<T>`:

```json
{ "success": true, "message": "...", "data": {...} }
{ "success": false, "message": "...", "code": "ERROR_CODE", "requestId": "..." }
```

Códigos: `VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`,
`CONFLICT`, `RATE_LIMITED`, `INTERNAL_ERROR`.

## Logging

- **Dev**: texto legible, `com.semillatecnologica` DEBUG
- **Prod**: JSON estructurado (logstash-logback-encoder)
- Correlación por `X-Request-Id` / requestId en MDC
- Hibernate SQL en WARN (no ensuciar)

## Base de datos

| Migración | Contenido |
|---|---|
| V1 | Schema base (users, roles, permissions, tokens, audit) |
| V2 | Seed roles/permisos base |
| V3 | `users.suspended` |
| V4 | `in_app_notifications` |
| V5 | `stored_files` |
| V6 | Permisos de archivos |
| V7 | `users.photo_file_id` |
| V8 | `payments` |
| V9 | Permisos de settings |
| V10 | Permiso `audit.read-mine` |

## Schedulers

| Scheduler | Qué hace |
|---|---|
| `TokenCleanupScheduler` | Limpia tokens expirados diario (3 AM) |
| Lazy cleanup | Borra token expirado al validarlo |

## Docker

```bash
docker compose up -d postgres mailhog   # infra para dev
```

- Postgres 16 (`backend_db`)
- MailHog: SMTP 1025, UI 8025

## Comandos útiles

```bash
./mvnw.cmd spring-boot:run     # dev
./mvnw.cmd compile             # compilar
./mvnw.cmd test                # tests
```