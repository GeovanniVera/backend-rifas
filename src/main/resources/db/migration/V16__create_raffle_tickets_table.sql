-- V16__create_raffle_tickets_table.sql
-- Tabla de boletos individuales para rifas con control de concurrencia y Stripe.

CREATE TABLE raffle_tickets (
    id                  VARCHAR(36) PRIMARY KEY,
    raffle_id           VARCHAR(36) NOT NULL REFERENCES raffles(id) ON DELETE CASCADE,
    ticket_number       INT NOT NULL,
    user_id             VARCHAR(36) REFERENCES users(id) ON DELETE SET NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE', -- AVAILABLE, RESERVED, SOLD
    payment_intent_id   VARCHAR(255),
    reserved_at         TIMESTAMP,
    purchased_at        TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_raffle_ticket UNIQUE (raffle_id, ticket_number)
);

CREATE INDEX idx_raffle_tickets_raffle ON raffle_tickets(raffle_id);
CREATE INDEX idx_raffle_tickets_user ON raffle_tickets(user_id);
CREATE INDEX idx_raffle_tickets_status ON raffle_tickets(status);
CREATE INDEX idx_raffle_tickets_payment_intent ON raffle_tickets(payment_intent_id);
