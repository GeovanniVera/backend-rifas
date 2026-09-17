-- V8__create_payments_table.sql
-- Tabla de pagos para persistir estado local de transacciones.

-- =============================================
-- TABLA: payments
-- Estado local de pagos procesados por gateways externos.
-- =============================================
CREATE TABLE payments (
    id              VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    gateway         VARCHAR(50) NOT NULL,   -- STRIPE, PAYPAL, IN_MEMORY
    gateway_id      VARCHAR(255),           -- ID del pago en el gateway externo
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    amount          BIGINT NOT NULL,        -- en centavos/cents
    currency        VARCHAR(3) NOT NULL,    -- USD, EUR, ARS, etc.
    status          VARCHAR(50) NOT NULL,   -- PENDING, COMPLETED, FAILED, REFUNDED
    description     TEXT,
    metadata_json   JSONB,
    failure_reason  TEXT,
    paid_at         TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payments_user ON payments(user_id);
CREATE INDEX idx_payments_gateway_id ON payments(gateway_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_idempotency ON payments(idempotency_key);
CREATE INDEX idx_payments_created ON payments(created_at);
