-- V3__add_soft_delete_to_users.sql
-- Migration: adiciona colunas de soft delete na tabela users (Phase 9, D-12)
-- Statements individuais para compatibilidade com H2 (nao aceita ADD COLUMN com virgula)

ALTER TABLE users ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE users ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_users_is_active ON users (is_active);

UPDATE users SET is_active = TRUE WHERE is_active IS NULL;
