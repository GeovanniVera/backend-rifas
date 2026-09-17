# Seguridad

Medidas de seguridad del sistema (OWASP Top 10).

## Autenticación (A07)

| Medida | Detalle |
|---|---|
| Hash de contraseñas | Argon2 (`Argon2PasswordEncoder`) |
| Access token | JWT (15 min) |
| Refresh token | Opaco en cookie HttpOnly (7 días), rotación + detección de reuso |
| Tokens de verificación | Opacos, hash SHA-256 en BD |
| OTP | 6 dígitos, hash, max 5 intentos, expira en 15 min |

## Rate limiting (A07)

Capa triple en login (cuenta + IP + combinado), y por cuenta/IP en OTP y register.
Implementado en memoria (`RateLimiterService`) — migrar a Redis en v2.

## Suspensión de cuenta

La suspensión se verifica en **cada request JWT**:
```
JwtAuthenticationFilter → loadUserById → ¿suspendido? → 403 ACCOUNT_SUSPENDED
```
El access token emitido antes de suspender deja de funcionar inmediatamente.

## Control de acceso (A01)

- RBAC por **permisos** (no roles)
- Endpoints protegidos con `@PreAuthorize("hasAuthority('...')")`
- Frontend: rutas con `RequirePrivilege`, componentes con `Can`

## Protección de datos (A02)

- Refresh tokens nunca en el body (solo cookie HttpOnly)
- Tokens de verificación/OTP almacenados como hash
- Errores no exponen stack traces ni detalles internos

## Inyección (A03)

- JPA/Hibernate con queries parametrizadas
- SQL nativo con `:param` (nunca concatenación)

## Upload de archivos

- Whitelist de extensiones (~25)
- Tamaño máx 2MB
- MIME sniffing (magic bytes)
- Path traversal bloqueado (incluido en descarga)

## Logging y monitoreo (A09)

- Access logs con correlación (`requestId`)
- Auditoría de eventos de seguridad
- Logs estructurados (logstash en prod)

## Pendientes de seguridad

| Pendiente | Área |
|---|---|
| Security headers (CSP, HSTS, X-Frame-Options) | A05 |
| Password policy (complejidad mínima) | A07 |
| Auditoría de dependencias | A06 |
| Rate limiting en Redis (multi-instancia) | A07 |
| Tests de seguridad | — |