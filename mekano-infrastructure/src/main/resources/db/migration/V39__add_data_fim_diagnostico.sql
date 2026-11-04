-- V39: Adiciona timestamp de fim do diagnostico
-- Mantem as colunas legadas (data_pagamento, forma_pagamento, endereco_entrega)
-- para evitar perda de dados em ambientes existentes.
ALTER TABLE ordens_de_servico ADD COLUMN IF NOT EXISTS data_fim_diagnostico TIMESTAMP;
