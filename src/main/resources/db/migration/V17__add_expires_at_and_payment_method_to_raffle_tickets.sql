-- V17__add_expires_at_and_payment_method_to_raffle_tickets.sql
-- Agrega campos de expiración dinámica y método de pago (CARD, OXXO) a la reserva de boletos.

ALTER TABLE raffle_tickets
    ADD COLUMN payment_method VARCHAR(30) DEFAULT 'CARD',
    ADD COLUMN expires_at TIMESTAMP;

CREATE INDEX idx_raffle_tickets_expires_at ON raffle_tickets(expires_at);
