-- V34__add_saldo_reservado_to_pecas.sql
-- Migration: adiciona coluna saldo_reservado na tabela pecas para reserva atômica de estoque
-- Coluna única por ALTER (compatibilidade H2), BIGINT puro (sem BIGSERIAL)

ALTER TABLE pecas ADD COLUMN saldo_reservado BIGINT NOT NULL DEFAULT 0;