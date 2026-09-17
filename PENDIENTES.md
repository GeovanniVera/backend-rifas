# Backend — Pendientes documentados (v1)

Estado: **v1 funcional** — cerrada con pendientes conocidos documentados.

## 🔴 Pendientes que bloquean funcionalidad

| # | Pendiente | Dónde | Requiere |
|---|---|---|---|
| 1 | Webhooks de Stripe | `PayPalPaymentGateway` / `StripePaymentGateway` (`verifyWebhook`) | Credenciales sandbox de Stripe |
| 2 | Webhooks de PayPal | `PayPalPaymentGateway.verifyWebhook` | Credenciales sandbox de PayPal |
| 3 | Controller REST de pagos | Falta un endpoint para crear un pago desde el frontend | Decisión de diseño + pruebas |
| 4 | Cloudinary activo | `CloudinaryStorage` (configurado pero sin credenciales) | `CLOUDINARY_CLOUD_NAME/API_KEY/API_SECRET` |

## 🟡 Pendientes de arquitectura (v2)

| # | Pendiente | Detalle |
|---|---|---|
| 5 | Rate limiting en memoria → Redis | `RateLimiterService` usa ConcurrentHashMap; migrar a Redis para multi-instancia |
| 6 | Email pendiente para cambio de email | Implementar con período de gracia (el email activo sigue funcionando hasta verificar el nuevo) |
| 7 | Cola para notificaciones | Spec pide cola persistente + reintentos; hoy el envío es directo |

## 🟢 Mejoras recomendadas

| # | Mejora | Detalle |
|---|---|---|
| 8 | Tests | Cubrir auth, RBAC, rate limiting, auditoría |
| 9 | Auditoría de dependencias | Revisar vulnerabilidades (OWASP A06) |
| 10 | Security headers | CSP, X-Frame-Options, HSTS, X-Content-Type-Options |
| 11 | Password policy | Validar complejidad mínima al registrar/resetear |

## Configuración pendiente de entorno

```bash
# Pagos
STRIPE_SECRET_KEY=...
PAYPAL_CLIENT_ID=...
PAYPAL_CLIENT_SECRET=...
PAYPAL_MODE=sandbox

# Almacenamiento
STORAGE_TARGET=CLOUDINARY
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
```