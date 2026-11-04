# 1 Prometheus metrics

- Status servidor mekano api (up | down);

- Tempo de atividade em horas do servidor;

- SLA de disponibilidade do servidor;

- Nº quedas do servidor nos ultimos dias;

- CPU USAGE;

- MEMORY USAGE;

- Garbage Collection;

- Threads (totais, em uso, bloqueadas, aguardando);

- Pool de conexões com banco de dados;

# 2 OS FLOW TRAFFIC

- Total de requisições por segundo;

- Latência média das requisições;

- TAXA HTTP RESPONSE (200, 400, 500*);

- Erros 500 por serviço de endpoint (ex: criar OS deu erro 500 3x nos ultimos dias);

- Total de OS criadas;

- Taxa diária de OS criadas;

- OS criadas HOJE;

- Tempo médio de duração de cada etapa da OS (Recebida, Em Diagnostico, Aguardando Execução, Em Execução, Finalizada, Entregue e Total);

- Transição dos status de OS;

- Falhas de Transição dos status de OS;

- Taxa de abertura de OS por segundo;

- Taxa de abertura de OS por minuto;

# 3 LOKI

### TAB: Logs Estruturados

- Logs Estruturados do Mekano API;

### TAB: Métricas

- Total de logs por Level (INFO, WARN, ERROR);

### TAB: Logs OS

- Logs da camada de OS;

### TAB: Logs Orçamento

- Logs da camada de Orçamento;

### TAB: Logs Req.Compra e NFe

- Logs da camada de Requisições de Compra e Nota Fiscal;

### TAB: Logs WhatsApp

- Logs da camada de Eventos do WhatsApp do Mekano API;
