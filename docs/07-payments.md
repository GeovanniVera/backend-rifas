# Módulo de Pagos

Abstrae cobros y reembolsos sin acoplar el dominio al proveedor.

## Responsabilidades

- **Estrategias**: InMemory (default), Stripe, PayPal
- **Orquestador**: PaymentService genera idempotencia y persiste estado local
- **Webhooks**: recepción de eventos de Stripe/PayPal
- Estado local en `payments` (no depende del proveedor)

## Estructura

```
modules/payments/
├── strategy/
│   ├── IPaymentGateway.java
│   ├── InMemoryPaymentGateway.java   # default, aprueba todo
│   ├── StripePaymentGateway.java     # requiere credenciales
│   ├── PayPalPaymentGateway.java     # requiere credenciales
│   └── ... (modelos: PaymentIntent, RefundCommand, etc.)
├── factory/PaymentGatewayFactory.java
├── service/PaymentService.java
├── model/Payment.java
├── repository/PaymentRepository.java
└── webhook/PaymentWebhookController.java
```

## Estrategias

| Gateway | Profile | Requiere | Estado |
|---|---|---|---|
| InMemory | `test`, `inmemory`, `dev` | — | ✅ Funcional |
| Stripe | `stripe`, `prod` | `STRIPE_SECRET_KEY` | ⚠️ Create/refund, webhook pendiente |
| PayPal | `paypal`, `prod` | `PAYPAL_CLIENT_ID/SECRET` | ⚠️ Create, webhook pendiente |

## Configuración

```yaml
payments:
  gateway: ${PAYMENTS_GATEWAY:IN_MEMORY}
  stripe:
    secret-key: ${STRIPE_SECRET_KEY:}
  paypal:
    client-id: ${PAYPAL_CLIENT_ID:}
    client-secret: ${PAYPAL_CLIENT_SECRET:}
    mode: ${PAYPAL_MODE:sandbox}
```

## Webhooks

```
POST /webhooks/payments/stripe
POST /webhooks/payments/paypal
```

Los webhooks son públicos (sin sesión) pero verifican firma del proveedor.

## Pendiente

- **Controller REST de pagos**: falta exponer `PaymentService.createPayment` como endpoint
- **Webhooks de Stripe/PayPal**: verificación de firma pendiente (requiere credenciales sandbox)
- El flujo de pagos end-to-end se probará con InMemory en un caso práctico