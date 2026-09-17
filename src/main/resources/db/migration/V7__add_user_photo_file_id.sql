-- V7__add_user_photo_file_id.sql
-- Campo para vincular foto de perfil del usuario con stored_files.

ALTER TABLE users ADD COLUMN photo_file_id VARCHAR(36);
ALTER TABLE users ADD CONSTRAINT fk_users_photo_file
    FOREIGN KEY (photo_file_id) REFERENCES stored_files(id) ON DELETE SET NULL;
