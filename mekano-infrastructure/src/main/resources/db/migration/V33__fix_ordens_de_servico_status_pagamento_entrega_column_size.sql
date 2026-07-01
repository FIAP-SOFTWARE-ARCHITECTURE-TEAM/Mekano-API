-- V33: Fix column sizes for status_pagamento and status_entrega
-- V27 created these as VARCHAR(20) which is too small for 'LIBERADA_PARA_ENTREGA' (21 chars)
-- V29 and V31 attempted to fix but used IF NOT EXISTS so existing columns kept VARCHAR(20)

ALTER TABLE ordens_de_servico ALTER COLUMN status_pagamento TYPE VARCHAR(50);
ALTER TABLE ordens_de_servico ALTER COLUMN status_entrega TYPE VARCHAR(50);