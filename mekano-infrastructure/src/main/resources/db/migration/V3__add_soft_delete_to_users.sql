-- V3__add_soft_delete_to_users.sql
-- Migration: adiciona colunas de soft delete à tabela users (Phase 9, D-12)

ALTER TABLE users
    ADD COLUMN deleted_at TIMESTAMP,
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Índice para queries de listagem filtradas (apenas ativos)
CREATE INDEX idx_users_is_active ON users (is_active);

-- Atualizar registros existentes — marcar como ativos (default já é TRUE, mas explicitar)
UPDATE users SET is_active = TRUE WHERE is_active IS NULL;
