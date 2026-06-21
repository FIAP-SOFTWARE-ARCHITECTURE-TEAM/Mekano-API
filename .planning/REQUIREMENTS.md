# Requirements: Mekano

**Defined:** 2026-06-20
**Core Value:** Gerenciar o ciclo de vida completo das Ordens de Serviço — do recebimento do veículo à entrega — com rastreabilidade, controle de estoque e cobrança integrados.

## v1 Requirements

### Ordem de Serviço

- [ ] **OS-01**: Admin/atendente pode cadastrar cliente com nome, CPF/CNPJ (único, validado), e-mail e telefone
- [ ] **OS-02**: Admin/atendente pode editar, consultar e excluir clientes
- [ ] **OS-03**: Admin/atendente pode cadastrar veículo com placa (única, formatos Mercosul e antigo), marca, modelo e ano vinculado a um cliente
- [ ] **OS-04**: Admin/atendente pode editar, consultar e excluir veículos
- [ ] **OS-05**: Admin pode cadastrar tipos de serviço com nome, descrição e valor unitário (valor > 0)
- [ ] **OS-06**: Admin pode editar, consultar e excluir tipos de serviço
- [ ] **OS-07**: Atendente pode criar OS identificando cliente (CPF/CNPJ) e veículo (placa), registrando entrada e serviços solicitados — status inicial Recebida
- [ ] **OS-08**: Mecânico pode iniciar diagnóstico da OS (status → Em Diagnóstico) e incluir serviços e peças identificados
- [ ] **OS-09**: Sistema gera orçamento automaticamente ao finalizar diagnóstico e envia para aprovação do cliente (OS → Aguardando Aprovação)
- [ ] **OS-10**: Cliente pode aprovar orçamento via API pública (OS → Em Execução)
- [ ] **OS-11**: Cliente pode reprovar orçamento via API pública (OS → Cancelada)
- [ ] **OS-12**: Sistema cancela OS automaticamente se orçamento expirar por SLA
- [ ] **OS-13**: Mecânico pode registrar início da execução (status → Em Execução)
- [ ] **OS-14**: Mecânico pode finalizar execução (status → Finalizada)
- [ ] **OS-15**: Cliente pode consultar status público da OS via API sem autenticação
- [ ] **OS-16**: Admin/atendente pode listar OS com filtros por data, status e cliente (paginado)
- [ ] **OS-17**: Admin/atendente pode ver detalhes completos de uma OS
- [ ] **OS-18**: Admin pode consultar tempo médio de execução por tipo de serviço em um período

### Gestão de Estoque

- [ ] **EST-01**: Admin/almoxarife pode cadastrar peça/insumo com código, descrição, unidade, saldo inicial, estoque mínimo e valor
- [ ] **EST-02**: Admin/almoxarife pode editar, consultar e excluir peças/insumos (saldo não pode ficar negativo)
- [ ] **EST-03**: Sistema reserva automaticamente peças disponíveis ao aprovar orçamento (reserva = flag)
- [ ] **EST-04**: Sistema gera Requisição de Compra para peças indisponíveis ao aprovar orçamento
- [ ] **EST-05**: Admin/almoxarife pode listar, visualizar e cancelar Requisições de Compra
- [ ] **EST-06**: Almoxarife/financeiro pode registrar NF de entrada referenciando Requisição de Compra, atualizando saldo
- [ ] **EST-07**: Ao atualizar saldo, sistema verifica itens abaixo do estoque mínimo e gera nova requisição se necessário
- [ ] **EST-08**: Almoxarife registra saída de peças reservadas ao iniciar execução (saldo debitado, reserva encerrada)
- [ ] **EST-09**: Sistema alerta quando estoque mínimo é atingido (calculado: tempo de reposição × consumo médio diário)

### Ordem de Pagamento

- [ ] **PAG-01**: Sistema emite cobrança automaticamente ao finalizar execução (pagamento → Pendente)
- [ ] **PAG-02**: Sistema registra confirmação de pagamento via serviço bancário simulado (pagamento → Confirmado)
- [ ] **PAG-03**: Admin registra entrega do veículo após pagamento confirmado (OS → Entregue)

### Documentação

- [ ] **DOC-01**: Diagramas de sequência dos fluxos principais (criar OS, aprovar orçamento, fluxo estoque, fluxo pagamento)
- [ ] **DOC-02**: Especificação OpenAPI/Swagger da API documentada
- [ ] **DOC-03**: Guia de contribuição (CONTRIBUTING.md) com setup, padrões e workflow do time

## v2 Requirements

- **OS-19**: Notificações WhatsApp para status da OS e orçamento
- **OS-20**: Agendamento de serviços online
- **EST-10**: Integração NF-e XML com SEFAZ
- **PAG-04**: Integração real com gateway de pagamento (PIX, boleto)
- **PAG-05**: Múltiplos métodos de pagamento na cobrança
- **DOC-04**: Documentação de arquitetura detalhada (C4 model)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Front-end / interface gráfica | API-first, time focado no backend |
| Aplicativo mobile do cliente | Não contemplado na Fase 1 |
| Módulo financeiro/contábil completo | Apenas cobrança essencial no v1 |
| Gateway de pagamento real | Serviço bancário simulado para MVP |
| Notificações WhatsApp/email real | Envio simulado de orçamento |
| Chat em tempo real | Fora do domínio da oficina |
| Multitenancy/multi-oficina | Escopo futuro |

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| OS-01 | Phase 1 — OS Core | Pending |
| OS-02 | Phase 1 — OS Core | Pending |
| OS-03 | Phase 1 — OS Core | Pending |
| OS-04 | Phase 1 — OS Core | Pending |
| OS-05 | Phase 1 — OS Core | Pending |
| OS-06 | Phase 1 — OS Core | Pending |
| OS-07 | Phase 1 — OS Core | Pending |
| OS-08 | Phase 1 — OS Core | Pending |
| OS-09 | Phase 1 — OS Core | Pending |
| OS-10 | Phase 1 — OS Core | Pending |
| OS-11 | Phase 1 — OS Core | Pending |
| OS-12 | Phase 1 — OS Core | Pending |
| OS-13 | Phase 1 — OS Core | Pending |
| OS-14 | Phase 1 — OS Core | Pending |
| OS-15 | Phase 1 — OS Core | Pending |
| OS-16 | Phase 1 — OS Core | Pending |
| OS-17 | Phase 1 — OS Core | Pending |
| OS-18 | Phase 2 — Estoque Integration | Pending |
| EST-01 | Phase 2 — Estoque Integration | Pending |
| EST-02 | Phase 2 — Estoque Integration | Pending |
| EST-03 | Phase 2 — Estoque Integration | Pending |
| EST-04 | Phase 2 — Estoque Integration | Pending |
| EST-05 | Phase 2 — Estoque Integration | Pending |
| EST-06 | Phase 2 — Estoque Integration | Pending |
| EST-07 | Phase 2 — Estoque Integration | Pending |
| EST-08 | Phase 2 — Estoque Integration | Pending |
| EST-09 | Phase 2 — Estoque Integration | Pending |
| PAG-01 | Phase 3 — Pagamento & Delivery | Pending |
| PAG-02 | Phase 3 — Pagamento & Delivery | Pending |
| PAG-03 | Phase 3 — Pagamento & Delivery | Pending |
| DOC-01 | Phase 1 — OS Core | Pending |
| DOC-02 | Phase 1 — OS Core | Pending |
| DOC-03 | Phase 3 — Pagamento & Delivery | Pending |

**Coverage:**
- v1 requirements: 33 total
- Mapped to phases: 33
- Unmapped: 0 ✅

---
*Requirements defined: 2026-06-20*
*Last updated: 2026-06-20 after initial definition*
