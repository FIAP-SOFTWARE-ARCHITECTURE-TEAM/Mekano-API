-- V27: Add payment and delivery columns to ordens_servico table
ALTER TABLE ordens_servico ADD COLUMN status_pagamento VARCHAR(50) DEFAULT 'PENDENTE';
ALTER TABLE ordens_servico ADD COLUMN data_pagamento TIMESTAMP NULL;
ALTER TABLE ordens_servico ADD COLUMN forma_pagamento VARCHAR(50) NULL;
ALTER TABLE ordens_servico ADD COLUMN valor_pago DECIMAL(19,2) NULL;
ALTER TABLE ordens_servico ADD COLUMN status_entrega VARCHAR(50) DEFAULT 'PENDENTE';
ALTER TABLE ordens_servico ADD COLUMN data_entrega TIMESTAMP NULL;
ALTER TABLE ordens_servico ADD COLUMN endereco_entrega VARCHAR(500) NULL;

CREATE INDEX idx_os_status_pagamento ON ordens_servico(status_pagamento);
CREATE INDEX idx_os_status_entrega ON ordens_servico(status_entrega);
