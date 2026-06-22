-- V4__add_audit_columns_to_users.sql
-- Migration: adiciona colunas de auditoria na tabela users
-- Statements individuais para compatibilidade com H2 (nao aceita ADD COLUMN com virgula)

ALTER TABLE users ADD COLUMN created_by UUID;
ALTER TABLE users ADD COLUMN updated_by UUID;
ALTER TABLE users ADD COLUMN updated_at TIMESTAMP;

CREATE INDEX idx_users_created_by ON users (created_by);
CREATE INDEX idx_users_updated_by ON users (updated_by);
