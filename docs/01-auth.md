# Módulo Auth

Autenticación y autorización de identidad.

## Responsabilidades

- Registro de usuarios (rol default `viewer`)
- Login con email + contraseña (Argon2)
- Access token JWT (15 min) + refresh token en cookie HttpOnly (7 días)
- Verificación de email (token opaco, 24 h)
- Recuperación de contraseña con OTP (6 dígitos, 15 min)
- Logout y logout-all (revocación de sesiones)

## Estructura

```
modules/auth/
├── controller/AuthController.java
├── dto/            # LoginRequest, RegisterRequest, TokenResponse, UserResponse...
├── model/          # User, Role, Permission, RefreshToken, VerificationToken, OtpChallenge
├── repository/
├── service/        # AuthService, AuthServiceImpl
└── scheduler/      # TokenCleanupScheduler
```

## Endpoints

| Método | Ruta | Público | Descripción |
|---|---|---|---|
| POST | `/auth/register` | ✅ | Registra usuario (respuesta opaca) |
| POST | `/auth/login` | ✅ | Login → access token + cookie refresh |
| POST | `/auth/refresh` | ✅ | Rota refresh token (desde cookie) |
| POST | `/auth/logout` | ✅ | Revoca sesión actual |
| POST | `/auth/logout-all` | 🔒 | Revoca todas las sesiones |
| GET | `/auth/me` | 🔒 | Datos del usuario autenticado |
| PUT | `/auth/me` | 🔒 | Actualiza nombre y foto (multipart) |
| POST | `/auth/forgot-password` | ✅ | Solicita OTP de recuperación |
| POST | `/auth/verify-otp` | ✅ | Verifica OTP → reset token |
| POST | `/auth/reset-password` | ✅ | Cambia contraseña con token |
| POST | `/auth/verify-email` | ✅ | Verifica email con token |
| POST | `/auth/resend-verification` | ✅ | Reenvía email de verificación |

## Rate limiting

| Endpoint | Por cuenta | Por IP | Ventana |
|---|---|---|---|
| `/auth/login` | 5 | 20 (+5 combinado) | 15 min |
| `/auth/forgot-password` | 3 | 10 | 1 hora |
| `/auth/verify-otp` | 5 | 20 | 15 min |
| `/auth/register` | — | 3 | 1 hora |

## Configuración

```yaml
auth:
  jwt:
    secret: ${JWT_SECRET:...}
    access-token-expiration: 900
  cors:
    allowed-origins: http://localhost:3000,http://localhost:5173
```

## Notas

- El cambio de email está **deshabilitado** (v1). Requiere patrón de email pendiente (v2).
- Los tokens expirados se limpian con estrategia híbrida (lazy + scheduler diario).
- `UserResponse` incluye `photoUrl` resuelta desde el módulo de almacenamiento.