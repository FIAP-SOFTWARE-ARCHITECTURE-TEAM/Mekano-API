-- V2__create_refresh_tokens_table.sql
-- Migration: criação da tabela refresh_tokens (Phase 9, D-01)

CREATE TABLE refresh_tokens
(
    id          UUID         NOT NULL,
    jti         VARCHAR(36)  NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL,
    user_id     UUID         NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    rotated_at  TIMESTAMP,
    created_at  TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_tokens_jti UNIQUE (jti),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX idx_refresh_tokens_jti ON refresh_tokens (jti);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
