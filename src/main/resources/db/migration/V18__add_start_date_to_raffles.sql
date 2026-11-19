-- V18__add_start_date_to_raffles.sql
-- Fecha de inicio del rango de la rifa (complementa a draw_date como fecha de fin/sorteo).

ALTER TABLE raffles ADD COLUMN start_date TIMESTAMP;
CREATE INDEX idx_raffles_start_date ON raffles(start_date);
