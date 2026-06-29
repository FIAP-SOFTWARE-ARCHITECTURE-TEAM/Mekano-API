-- V22__fix_pecas_requisicoes_columns.sql
-- Migration: corrige colunas das tabelas pecas e requisicoes_compra
-- As migrations V9 e V10 originais foram aplicadas sem algumas colunas
-- que foram adicionadas posteriormente nos arquivos.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'pecas' AND column_name = 'codigo') THEN
        ALTER TABLE pecas ADD COLUMN codigo VARCHAR(20);
        UPDATE pecas SET codigo = 'P' || CAST(id AS VARCHAR) WHERE codigo IS NULL;
        ALTER TABLE pecas ALTER COLUMN codigo SET NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'pecas' AND column_name = 'valor_unitario') THEN
        ALTER TABLE pecas ADD COLUMN valor_unitario DECIMAL(12,2);
        UPDATE pecas SET valor_unitario = 0 WHERE valor_unitario IS NULL;
        ALTER TABLE pecas ALTER COLUMN valor_unitario SET NOT NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_pecas_codigo ON pecas(codigo);

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'requisicoes_compra' AND column_name = 'motivo') THEN
        ALTER TABLE requisicoes_compra ADD COLUMN motivo VARCHAR(30);
        UPDATE requisicoes_compra SET motivo = 'OUTROS' WHERE motivo IS NULL;
        ALTER TABLE requisicoes_compra ALTER COLUMN motivo SET NOT NULL;
    END IF;
END $$;
