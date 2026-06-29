-- V20__add_chave_acesso_to_nf_entradas.sql
-- Migration: adiciona coluna chave_acesso à tabela nf_entradas
-- A migration V11 original foi aplicada sem esta coluna e depois editada.
-- Esta migration corrige o schema para alinhar com a entidade JPA.

ALTER TABLE nf_entradas ADD COLUMN chave_acesso VARCHAR(44);

-- Se não houver registros, podemos aplicar NOT NULL imediatamente.
-- Caso existam registros, preencha com um valor default antes de aplicar NOT NULL.
UPDATE nf_entradas SET chave_acesso = 'PENDENTE' WHERE chave_acesso IS NULL;

ALTER TABLE nf_entradas ALTER COLUMN chave_acesso SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_nf_entradas_chave_acesso ON nf_entradas(chave_acesso);
