-- V4__add_audit_columns_to_users.sql
-- Migration: adiciona colunas de auditoria à tabela users
-- created_by: UUID do usuário que criou o registro
-- updated_by: UUID do usuário que fez a última atualização
-- updated_at: timestamp da última atualização

ALTER TABLE users
    ADD COLUMN created_by UUID,
    ADD COLUMN updated_by UUID,
    ADD COLUMN updated_at TIMESTAMP;

-- Índice para queries de auditoria
CREATE INDEX idx_users_created_by ON users (created_by);
CREATE INDEX idx_users_updated_by ON users (updated_by);
