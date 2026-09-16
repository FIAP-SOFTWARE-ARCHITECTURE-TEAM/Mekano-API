-- V37: Adiciona timestamps para métricas de negócio de Ordens de Serviço
-- data_inicio_diagnostico: registra quando a OS entra em EM_DIAGNOSTICO
-- data_cancelamento: registra quando a OS é cancelada

ALTER TABLE ordens_de_servico
    ADD COLUMN data_inicio_diagnostico TIMESTAMP,
    ADD COLUMN data_cancelamento TIMESTAMP;
