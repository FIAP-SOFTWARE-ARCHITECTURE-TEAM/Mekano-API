# Requirements: Mekano

**Defined:** 2026-06-20
**Core Value:** Gerenciar o ciclo de vida completo das Ordens de Serviço — do recebimento do veículo à entrega — com rastreabilidade, controle de estoque e cobrança integrados.

## v1 Requirements

### Autenticação e Autorização

- [x] **AUTH-01**: Sistema oferece roles para cada perfil (admin, atendente, mecânico, almoxarife, financeiro)
- [x] **AUTH-02**: Endpoints administrativos são protegidos por `@RolesAllowed` com base no perfil
- [x] **AUTH-03**: Cliente pode consultar status da OS via endpoint público sem autenticação
- [x] **AUTH-04**: Admin pode gerenciar usuários do sistema (CRUD)

### Ordem de Serviço

- [x] **OS-01**: Admin/atendente pode cadastrar cliente com nome, CPF/CNPJ (único, validado), e-mail e telefone
- [x] **OS-02**: Admin/atendente pode editar, consultar e excluir clientes
- [x] **OS-03**: Admin/atendente pode cadastrar veículo com placa (única, formatos Mercosul e antigo), marca, modelo e ano vinculado a um cliente
- [x] **OS-04**: Admin/atendente pode editar, consultar e excluir veículos
- [x] **OS-05**: Admin pode cadastrar tipos de serviço com nome, descrição e valor unitário (valor > 0)
- [x] **OS-06**: Admin pode editar, consultar e excluir tipos de serviço
- [x] **OS-07**: Atendente pode criar OS identificando cliente (CPF/CNPJ) e veículo (placa), registrando entrada e serviços solicitados — status inicial Recebida
- [x] **OS-08**: Mecânico pode iniciar diagnóstico da OS (status → Em Diagnóstico) e incluir serviços e peças identificados
- [x] **OS-09**: Sistema gera orçamento automaticamente ao finalizar diagnóstico e envia para aprovação do cliente (OS → Aguardando Aprovação)
- [x] **OS-10**: Cliente pode aprovar orçamento via API pública (OS → Em Execução)
- [x] **OS-11**: Cliente pode reprovar orçamento via API pública (OS → Cancelada)
- [x] **OS-12**: Sistema cancela OS automaticamente se orçamento expirar por SLA
- [x] **OS-13**: Mecânico pode registrar início da execução (status → Em Execução)
- [x] **OS-14**: Mecânico pode finalizar execução (status → Finalizada)
- [x] **OS-15**: Cliente pode consultar status público da OS via API sem autenticação
- [x] **OS-16**: Admin/atendente pode listar OS com filtros por data, status e cliente (paginado)
- [x] **OS-17**: Admin/atendente pode ver detalhes completos de uma OS
- [x] **OS-18**: Admin pode consultar tempo médio de execução por tipo de serviço em um período

### Gestão de Estoque

- [x] **EST-01**: Admin/almoxarife pode cadastrar peça/insumo com código, descrição, unidade, saldo inicial, estoque mínimo e valor
- [x] **EST-02**: Admin/almoxarife pode editar, consultar e excluir peças/insumos (saldo não pode ficar negativo)
- [x] **EST-03**: Sistema reserva automaticamente peças disponíveis ao aprovar orçamento (reserva = flag)
- [x] **EST-04**: Sistema gera Requisição de Compra para peças indisponíveis ao aprovar orçamento
- [x] **EST-05**: Admin/almoxarife pode listar, visualizar e cancelar Requisições de Compra
- [x] **EST-06**: Almoxarife/financeiro pode registrar NF de entrada referenciando Requisição de Compra, atualizando saldo
- [x] **EST-07**: Ao atualizar saldo, sistema verifica itens abaixo do estoque mínimo e gera nova requisição se necessário
- [x] **EST-08**: Almoxarife registra saída de peças reservadas ao iniciar execução (saldo debitado, reserva encerrada)
- [x] **EST-09**: Sistema alerta quando estoque mínimo é atingido (calculado: tempo de reposição × consumo médio diário)

### Ordem de Pagamento

- [x] **PAG-01**: Sistema emite cobrança automaticamente ao finalizar execução (pagamento → Pendente)
- [x] **PAG-02**: Sistema registra confirmação de pagamento via serviço bancário simulado (pagamento → Confirmado)
- [x] **PAG-03**: Admin registra entrega do veículo após pagamento confirmado (OS → Entregue)

### Documentação

- [x] **DOC-01**: Diagramas de sequência dos fluxos principais (criar OS, aprovar orçamento, fluxo estoque, fluxo pagamento)
- [x] **DOC-02**: Especificação OpenAPI/Swagger da API documentada
- [x] **DOC-03**: Guia de contribuição (CONTRIBUTING.md) com setup, padrões e workflow do time

## v2 Requirements

- **OS-19**: Notificações WhatsApp para status da OS e orçamento
- **OS-20**: Agendamento de serviços online
- **EST-10**: Integração NF-e XML com SEFAZ
- **PAG-04**: Integração real com gateway de pagamento (PIX, boleto)
- **PAG-05**: Múltiplos métodos de pagamento na cobrança
- **DOC-04**: Documentação de arquitetura detalhada (C4 model)
- **AUTH-05**: Autenticação multifator (2FA)
- **AUTH-06**: OAuth com Google/GitHub

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
| AUTH-01 | Phase 1 — Auth & OS Foundation | Complete (fase 03.1 — ver 01-VERIFICATION.md) |
| AUTH-02 | Phase 1 — Auth & OS Foundation | Complete (fase 03.1 — ver 01-VERIFICATION.md) |
| AUTH-03 | Phase 1 — Auth & OS Foundation | Complete |
| AUTH-04 | Phase 2 — OS Continuation & Estoque | Complete |
| OS-01 | Phase 1 — Auth & OS Foundation | Complete (fase 03.1 — ver 01-VERIFICATION.md) |
| OS-02 | Phase 1 — Auth & OS Foundation | Complete |
| OS-03 | Phase 1 — Auth & OS Foundation | Complete (fase 03.1 — ver 01-VERIFICATION.md) |
| OS-04 | Phase 1 — Auth & OS Foundation | Complete (fase 03.1 — ver 01-VERIFICATION.md) |
| OS-05 | Phase 1 — Auth & OS Foundation | Complete (fase 03.1 — ver 01-VERIFICATION.md) |
| OS-06 | Phase 1 — Auth & OS Foundation | Complete (fase 03.1 — ver 01-VERIFICATION.md) |
| OS-07 | Phase 1 — Auth & OS Foundation | Complete (fase 03.1 — ver 01-VERIFICATION.md) |
| OS-08 | Phase 1 — Auth & OS Foundation | Complete (fase 03.1 — ver 01-VERIFICATION.md) |
| OS-09 | Phase 2 — OS Continuation & Estoque | Complete (fase 03.1 — ver 02-VERIFICATION.md) |
| OS-10 | Phase 2 — OS Continuation & Estoque | Complete |
| OS-11 | Phase 2 — OS Continuation & Estoque | Complete |
| OS-12 | Phase 3 — Pagamento & Delivery | Complete |
| OS-13 | Phase 2 — OS Continuation & Estoque | Complete (fase 03.1 — ver 02-VERIFICATION.md) |
| OS-14 | Phase 2 — OS Continuation & Estoque | Complete (fase 03.1 — ver 02-VERIFICATION.md) |
| OS-15 | Phase 1 — Auth & OS Foundation | Complete |
| OS-16 | Phase 2 — OS Continuation & Estoque | Complete (fase 03.1 — ver 02-VERIFICATION.md) |
| OS-17 | Phase 2 — OS Continuation & Estoque | Complete (fase 03.1 — ver 02-VERIFICATION.md) |
| OS-18 | Phase 2 — OS Continuation & Estoque | Complete (fase 03.1 — ver 02-VERIFICATION.md) |
| EST-01 | Phase 2 — OS Continuation & Estoque | Complete (fase 03.1 — ver 02-VERIFICATION.md) |
| EST-02 | Phase 2 — OS Continuation & Estoque | Complete |
| EST-03 | Phase 2 — OS Continuation & Estoque | Complete |
| EST-04 | Phase 2 — OS Continuation & Estoque | Complete (fase 03.1 — ver 02-VERIFICATION.md) |
| EST-05 | Phase 2 — OS Continuation & Estoque | Complete (fase 03.1 — ver 02-VERIFICATION.md) |
| EST-06 | Phase 2 — OS Continuation & Estoque | Complete (fase 03.1 — ver 02-VERIFICATION.md) |
| EST-07 | Phase 2 — OS Continuation & Estoque | Complete |
| EST-08 | Phase 2 — OS Continuation & Estoque | Complete |
| EST-09 | Phase 2 — OS Continuation & Estoque | Complete |
| PAG-01 | Phase 3 — Pagamento & Delivery | Complete (fase 03.1 — ver 03-VERIFICATION.md) |
| PAG-02 | Phase 3 — Pagamento & Delivery | Complete (fase 03.1 — ver 03-VERIFICATION.md) |
| PAG-03 | Phase 3 — Pagamento & Delivery | Complete (fase 03.1 — ver 03-VERIFICATION.md) |
| DOC-01 | Phase 1 — Auth & OS Foundation | Complete (fase 03.1 — ver 01-VERIFICATION.md) |
| DOC-02 | Phase 2 — OS Continuation & Estoque | Complete (fase 03.1 — ver 02-VERIFICATION.md) |
| DOC-03 | Phase 3 — Pagamento & Delivery | Complete (fase 03.1 — ver 03-VERIFICATION.md) |

**Coverage:**

- v1 requirements: 37 total — 37/37 Complete ✅
- Mapped to phases: 37
- Unmapped: 0 ✅

---
*Requirements defined: 2026-06-20*
*Last updated: 2026-08-08 — 37/37 Complete (fase 03.1 — ver VERIFICATION.md de cada fase)*
