-- V15__add_draw_date_to_raffles.sql
-- Fecha del sorteo programado para la rifa.

ALTER TABLE raffles ADD COLUMN draw_date TIMESTAMP;
CREATE INDEX idx_raffles_draw_date ON raffles(draw_date);
