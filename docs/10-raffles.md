# Módulo Rifas — Estados

Cada rifa tiene un ciclo de vida por estados. **No se eliminan rifas** —
solo cambian de estado para preservar el historial y la auditoría.

## Ciclo de vida

```
                ┌─────────────────────────────────────┐
                │                                     │
                ▼                                     │
ACTIVE ──────► DRAW ──────► FINISHED                 │
  │            │                                     │
  │ (sin       │ (no se puede cancelar)              │
  │  boletos)  │                                     │
  └──► CANCELLED ────────────────────────────────────┘
```

## Estados

| Estado | Descripción | Boletos | Transiciones permitidas |
|---|---|---|---|
| **ACTIVE** | Rifa activa, a la venta. El producto está apartado del stock. | Se venden | → `DRAW` (avanzar a sorteo), → `CANCELLED` (solo si `ticketsSold == 0`) |
| **DRAW** | Sorteo en curso. Ya no se venden boletos. | Congelados | → `FINISHED` (sorteo realizado) |
| **FINISHED** | Sorteo realizado, ganador definido. | Final | Terminal — no se cambia |
| **CANCELLED** | Rifa cancelada. Solo posible si NO hubo boletos vendidos. | No hubo | Terminal — no se cambia |

## Reglas de negocio

1. **No se puede cancelar** una rifa con `ticketsSold > 0`
   (los compradores ya pagaron — la rifa debe llegar a sorteo).
2. **No se puede eliminar** una rifa en `DRAW` o `FINISHED`
   (el backend lo bloquea; el frontend ni lo ofrece).
3. `FINISHED` y `CANCELLED` son estados **terminales**.
4. Al crear una rifa, el producto se **aparta del stock** (`stock = 0`).
5. `ticketsSold` se incrementa al comprar boletos (módulo de tickets futuro).

## Validación en backend

```java
ACTIVE → DRAW:              permitido
ACTIVE → CANCELLED:         permitido SOLO si ticketsSold == 0
DRAW → FINISHED:            permitido
DRAW → CANCELLED:           NO permitido
FINISHED/CANCELLED → *:     NO permitido (terminal)
```

## Auditoría

Cada cambio de estado registra `RAFFLE_STATUS_CHANGED` con
`before.status` y `after.status`.