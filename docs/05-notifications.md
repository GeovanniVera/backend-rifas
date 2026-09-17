# Módulo de Notificaciones

Centraliza el envío de notificaciones: email (canales) e in-app (persistidas).

## Responsabilidades

- **Canales externos** (Strategy pattern): email por JavaMail → MailHog en dev
- **Notificaciones in-app**: persistidas en BD para mostrar en el frontend
- Renderizado de templates (verificación, bienvenida, reset, OTP)

## Estructura

```
modules/notification/
├── channel/        # INotificationChannel, EmailChannel, NotificationChannelFactory...
├── inapp/
│   ├── controller/NotificationController.java
│   ├── dto/
│   ├── model/InAppNotification.java
│   ├── repository/
│   └── service/InAppNotificationService.java
├── service/NotificationService.java
└── template/TemplateRenderer.java
```

## Canales externos (Strategy)

```
NotificationService.notify("EMAIL", to, subject, template, vars)
  → TemplateRenderer.render(...)
  → NotificationChannelFactory.resolve("EMAIL")
  → EmailChannel.send(...)
```

Agregar un canal nuevo = implementar `INotificationChannel` (ej: Sms, WhatsApp).

## Endpoints in-app

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/notifications` | Listar (filtros type, unreadOnly) |
| GET | `/notifications/unread-count` | Badge de no leídas |
| PATCH | `/notifications/{id}/read` | Marcar una como leída |
| PATCH | `/notifications/read-all` | Marcar todas |
| DELETE | `/notifications/{id}` | Eliminar |

## Templates disponibles

| Template | Uso |
|---|---|
| `verification` | Email de verificación de cuenta |
| `welcome` | Email de bienvenida |
| `password-reset` | Email de restablecimiento |
| `password-reset-otp` | Email con OTP de recuperación |

## Configuración

```yaml
notifications:
  email:
    from: ${MAIL_FROM:noreply@tiendarifas.com}
    verification:
      base-url: ${VERIFICATION_BASE_URL:http://localhost:5173/verify-email/confirm}
    password-reset:
      base-url: ${PASSWORD_RESET_BASE_URL:http://localhost:5173/reset-password}
```

## Pendiente (v2)

- Cola persistente con reintentos y backoff para envíos importantes.