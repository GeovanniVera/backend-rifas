-- V13__add_tickets_sold_to_raffles.sql
-- Contador de boletos vendidos por rifa.
-- Se incrementa al comprar boletos (módulo de tickets futuro).
-- Regla: una rifa con boletos vendidos NO se puede cancelar.

ALTER TABLE raffles ADD COLUMN tickets_sold INT NOT NULL DEFAULT 0;