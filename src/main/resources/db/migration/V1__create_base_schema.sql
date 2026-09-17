-- V1__create_base_schema.sql
-- Esquema base del sistema: identidad, roles, permisos, sesiones y auditoría.

-- =============================================
-- TABLA: users
-- Almacena las cuentas de usuario del sistema.
-- =============================================
CREATE TABLE users (
    id              VARCHAR(36) PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(255) NOT NULL,
    password        VARCHAR(255) NOT NULL,
    is_verified     BOOLEAN NOT NULL DEFAULT FALSE,
    terms_accepted_at TIMESTAMP,
    terms_version   VARCHAR(20),
    password_updated_at TIMESTAMP,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =============================================
-- TABLA: roles
-- Catálogo de roles del sistema (admin, editor, viewer, etc.).
-- =============================================
CREATE TABLE roles (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =============================================
-- TABLA: permissions
-- Catálogo de permisos en formato recurso.acción.
-- Ejemplos: users.read, roles.assign, audit.read.
-- =============================================
CREATE TABLE permissions (
    id          VARCHAR(36) PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =============================================
-- TABLA: user_roles
-- Relación N:N entre usuarios y roles.
-- =============================================
CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id VARCHAR(36) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- =============================================
-- TABLA: role_permissions
-- Relación N:N entre roles y permisos.
-- =============================================
CREATE TABLE role_permissions (
    role_id       VARCHAR(36) NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id VARCHAR(36) NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

-- =============================================
-- TABLA: refresh_tokens
-- Tokens de sesión de larga duración.
-- Se almacena solo el hash; el valor original nunca se persiste.
-- Soporta rotación por familias y detección de reutilización.
-- =============================================
CREATE TABLE refresh_tokens (
    id           VARCHAR(36) PRIMARY KEY,
    user_id      VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    family_id    VARCHAR(36) NOT NULL,
    token_hash   VARCHAR(255) NOT NULL,
    expires_at   TIMESTAMP NOT NULL,
    used_at      TIMESTAMP,
    revoked_at   TIMESTAMP,
    replaced_by  VARCHAR(36),
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    last_used_at TIMESTAMP,
    ip_address   VARCHAR(45),
    user_agent   TEXT
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_hash ON refresh_tokens(token_hash);

-- =============================================
-- TABLA: verification_tokens
-- Tokens opacos de un solo uso para verificación de correo
-- y recuperación de contraseña.
-- =============================================
CREATE TABLE verification_tokens (
    id          VARCHAR(36) PRIMARY KEY,
    user_id     VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(255) NOT NULL,
    purpose     VARCHAR(50) NOT NULL,  -- email_verification, password_reset
    expires_at  TIMESTAMP NOT NULL,
    used_at     TIMESTAMP,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_verification_tokens_user ON verification_tokens(user_id);
CREATE INDEX idx_verification_tokens_hash ON verification_tokens(token_hash);

-- =============================================
-- TABLA: otp_challenges
-- Desafíos OTP para recuperación de contraseña.
-- Se almacena solo el hash del OTP.
-- =============================================
CREATE TABLE otp_challenges (
    id              VARCHAR(36) PRIMARY KEY,
    user_id         VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    otp_hash        VARCHAR(255) NOT NULL,
    purpose         VARCHAR(50) NOT NULL,  -- password_reset
    attempts        INT NOT NULL DEFAULT 0,
    max_attempts    INT NOT NULL DEFAULT 5,
    expires_at      TIMESTAMP NOT NULL,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_otp_challenges_user ON otp_challenges(user_id);

-- =============================================
-- TABLA: audit_logs
-- Registro inmutable de eventos de negocio y seguridad.
-- Los registros no se modifican ni eliminan desde la API ordinaria.
-- =============================================
CREATE TABLE audit_logs (
    id          VARCHAR(36) PRIMARY KEY,
    request_id  VARCHAR(50),
    actor_id    VARCHAR(36),
    action      VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id   VARCHAR(36),
    before_json JSONB,
    after_json  JSONB,
    ip_address  VARCHAR(45),
    user_agent  TEXT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_actor ON audit_logs(actor_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);
