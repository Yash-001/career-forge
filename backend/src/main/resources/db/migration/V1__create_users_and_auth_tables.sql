-- V1__create_users_and_auth_tables.sql
-- Initial schema for authentication and user accounts.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Users ─────────────────────────────────────────────────────────────────────
CREATE TABLE users (
    id                UUID        NOT NULL DEFAULT gen_random_uuid(),
    email             VARCHAR(255) NOT NULL,
    password_hash     VARCHAR(255) NOT NULL,
    first_name        VARCHAR(100),
    last_name         VARCHAR(100),
    subscription_tier VARCHAR(20)  NOT NULL DEFAULT 'FREE',
    enabled           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email),
    CONSTRAINT chk_users_subscription_tier CHECK (subscription_tier IN ('FREE', 'PRO'))
);

CREATE INDEX idx_users_email ON users (email);

-- ── Password Reset Tokens ─────────────────────────────────────────────────────
CREATE TABLE password_reset_tokens (
    id         UUID        NOT NULL DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    token_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMPTZ  NOT NULL,
    used       BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_password_reset_tokens PRIMARY KEY (id),
    CONSTRAINT fk_prt_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_prt_token_hash ON password_reset_tokens (token_hash);
CREATE INDEX idx_prt_user_id    ON password_reset_tokens (user_id);
