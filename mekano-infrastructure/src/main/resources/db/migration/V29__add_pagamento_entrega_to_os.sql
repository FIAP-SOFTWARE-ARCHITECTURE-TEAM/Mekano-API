-- V29: Add payment and delivery columns to ordens_de_servico table
ALTER TABLE ordens_de_servico ADD COLUMN IF NOT EXISTS status_pagamento VARCHAR(50) DEFAULT 'NAO_COBRADO';
ALTER TABLE ordens_de_servico ADD COLUMN IF NOT EXISTS data_pagamento TIMESTAMP;
ALTER TABLE ordens_de_servico ADD COLUMN IF NOT EXISTS forma_pagamento VARCHAR(50);
ALTER TABLE ordens_de_servico ADD COLUMN IF NOT EXISTS valor_pago DECIMAL(19,2);
ALTER TABLE ordens_de_servico ADD COLUMN IF NOT EXISTS status_entrega VARCHAR(50) DEFAULT 'NAO_LIBERADA';
ALTER TABLE ordens_de_servico ADD COLUMN IF NOT EXISTS data_entrega TIMESTAMP;
ALTER TABLE ordens_de_servico ADD COLUMN IF NOT EXISTS endereco_entrega VARCHAR(500);
ALTER TABLE ordens_de_servico ADD COLUMN IF NOT EXISTS cobranca_gerada_em TIMESTAMP;
ALTER TABLE ordens_de_servico ADD COLUMN IF NOT EXISTS pagamento_confirmado_em TIMESTAMP;
ALTER TABLE ordens_de_servico ADD COLUMN IF NOT EXISTS referencia_pagamento VARCHAR(255);
ALTER TABLE ordens_de_servico ADD COLUMN IF NOT EXISTS entregue_em TIMESTAMP;
ALTER TABLE ordens_de_servico ADD COLUMN IF NOT EXISTS recebido_por VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_os_status_pagamento ON ordens_de_servico(status_pagamento);
CREATE INDEX IF NOT EXISTS idx_os_status_entrega ON ordens_de_servico(status_entrega);
