-- V21__fix_nf_entradas_columns.sql
-- Migration: corrige colunas da tabela nf_entradas
-- A V11 original foi aplicada com colunas diferentes do arquivo atual.
-- V20 adicionou chave_acesso; esta migration completa a correção.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'nf_entradas' AND column_name = 'valor_total') THEN
        ALTER TABLE nf_entradas ADD COLUMN valor_total DECIMAL(12,2);
        UPDATE nf_entradas SET valor_total = 0 WHERE valor_total IS NULL;
        ALTER TABLE nf_entradas ALTER COLUMN valor_total SET NOT NULL;
    END IF;
END $$;

ALTER TABLE nf_entradas DROP COLUMN IF EXISTS quantidade;
