ALTER TABLE ordens_servico
    ADD COLUMN status_pagamento VARCHAR(40) DEFAULT 'NAO_COBRADO' NOT NULL;

ALTER TABLE ordens_servico
    ADD COLUMN status_entrega VARCHAR(40) DEFAULT 'NAO_LIBERADA' NOT NULL;

ALTER TABLE ordens_servico
    ADD COLUMN cobranca_gerada_em TIMESTAMP;

ALTER TABLE ordens_servico
    ADD COLUMN pagamento_confirmado_em TIMESTAMP;

ALTER TABLE ordens_servico
    ADD COLUMN referencia_pagamento VARCHAR(120);

ALTER TABLE ordens_servico
    ADD COLUMN entregue_em TIMESTAMP;

ALTER TABLE ordens_servico
    ADD COLUMN recebido_por VARCHAR(120);