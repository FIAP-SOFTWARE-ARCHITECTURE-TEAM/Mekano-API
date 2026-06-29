-- V20__add_chave_acesso_to_nf_entradas.sql
-- Migration: adiciona coluna chave_acesso à tabela nf_entradas
-- A migration V11 original foi aplicada sem esta coluna e depois editada.
-- Esta migration corrige o schema para alinhar com a entidade JPA.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'nf_entradas' AND column_name = 'chave_acesso') THEN
        ALTER TABLE nf_entradas ADD COLUMN chave_acesso VARCHAR(44);
        UPDATE nf_entradas SET chave_acesso = 'PENDENTE' WHERE chave_acesso IS NULL;
        ALTER TABLE nf_entradas ALTER COLUMN chave_acesso SET NOT NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_nf_entradas_chave_acesso ON nf_entradas(chave_acesso);
