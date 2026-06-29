-- V27__add_payment_fields_to_ordens_de_servico.sql
-- Migration: adiciona colunas de pagamento/entrega (Phase 3, D-24)
-- A V18 já foi aplicada antes destes campos existirem.

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'ordens_de_servico' AND column_name = 'status_pagamento') THEN
        ALTER TABLE ordens_de_servico ADD COLUMN status_pagamento VARCHAR(20) DEFAULT 'NAO_COBRADO' NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'ordens_de_servico' AND column_name = 'status_entrega') THEN
        ALTER TABLE ordens_de_servico ADD COLUMN status_entrega VARCHAR(20) DEFAULT 'NAO_LIBERADA' NOT NULL;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'ordens_de_servico' AND column_name = 'cobranca_gerada_em') THEN
        ALTER TABLE ordens_de_servico ADD COLUMN cobranca_gerada_em TIMESTAMP;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'ordens_de_servico' AND column_name = 'pagamento_confirmado_em') THEN
        ALTER TABLE ordens_de_servico ADD COLUMN pagamento_confirmado_em TIMESTAMP;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'ordens_de_servico' AND column_name = 'referencia_pagamento') THEN
        ALTER TABLE ordens_de_servico ADD COLUMN referencia_pagamento VARCHAR(100);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'ordens_de_servico' AND column_name = 'entregue_em') THEN
        ALTER TABLE ordens_de_servico ADD COLUMN entregue_em TIMESTAMP;
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                   WHERE table_name = 'ordens_de_servico' AND column_name = 'recebido_por') THEN
        ALTER TABLE ordens_de_servico ADD COLUMN recebido_por VARCHAR(100);
    END IF;
END $$;