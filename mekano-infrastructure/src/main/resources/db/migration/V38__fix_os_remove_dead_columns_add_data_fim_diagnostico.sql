-- V38: Remove colunas mortas e adiciona data_fim_diagnostico
-- data_pagamento: removida (nunca foi usada no domain)
-- forma_pagamento: removida (nunca foi mapeada no domain)
-- endereco_entrega: removida (nunca foi mapeada no domain)
-- data_fim_diagnostico: adicionada para registrar quando o mecanico finaliza diagnostico

ALTER TABLE ordens_de_servico DROP COLUMN IF EXISTS data_pagamento;
ALTER TABLE ordens_de_servico DROP COLUMN IF EXISTS forma_pagamento;
ALTER TABLE ordens_de_servico DROP COLUMN IF EXISTS endereco_entrega;

ALTER TABLE ordens_de_servico ADD COLUMN data_fim_diagnostico TIMESTAMP;
