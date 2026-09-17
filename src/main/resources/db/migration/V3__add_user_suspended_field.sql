-- V3__add_user_suspended_field.sql
-- Agregar campos de suspensión a la tabla users.

ALTER TABLE users ADD COLUMN suspended BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE users ADD COLUMN suspended_at TIMESTAMP;
