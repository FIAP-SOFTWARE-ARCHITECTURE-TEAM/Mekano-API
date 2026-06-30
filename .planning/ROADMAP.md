# Roadmap: Mekano

**Core Value:** Gerenciar o ciclo de vida completo das Ordens de Serviço — do recebimento do veículo à entrega — com rastreabilidade, controle de estoque e cobrança integrados.

**Granularity:** standard
**Mode:** mvp
**Total v1 Requirements:** 37
**Timeline:** 10 days
**Team:** 5 developers

---

## Phases

- [x] **Phase 1: Auth & OS Foundation** — Auth/roles + Cliente/Veiculo/Serviço CRUD + OS criação/diagnóstico + consulta pública
- [x] **Phase 2: OS Continuation & Estoque** — Fluxo de orçamento/aprovação + execução/finalização + estoque completo + métricas
- [ ] **Phase 3: Pagamento & Delivery** — Cobrança + pagamento simulado + entrega + docs finais (parcial: 50%)

---

## Phase Details

### Phase 1: Auth & OS Foundation

**Goal:** Sistema com autenticação JWT por perfis; admin gerencia clientes, veículos e serviços; atendente cria OS e mecânico realiza diagnóstico com consulta pública de status
**Mode:** mvp
**Depends on:** Nothing (auth construído do zero como parte da fase)
**Requirements:** AUTH-01, AUTH-02, AUTH-03, OS-01, OS-02, OS-03, OS-04, OS-05, OS-06, OS-07, OS-08, OS-15, DOC-01
**Time allocation:** Days 1-4, all 5 devs
**Success Criteria** (what must be TRUE):

  1. Sistema tem roles (admin, atendente, mecanico, almoxarife, financeiro) com JWT — endpoints protegidos por `@RolesAllowed`
  2. Admin/atendente pode cadastrar, editar, consultar e excluir clientes (CPF/CNPJ único validado), veículos (placa única, Mercosul+antigo) e serviços (valor > 0)
  3. Atendente pode criar OS (RECEBIDA) identificando cliente e veículo; mecânico inicia diagnóstico (EM_DIAGNOSTICO) com inclusão de serviços e peças
  4. Cliente pode consultar status público da OS via endpoint sem autenticação
  5. Diagramas de sequência dos fluxos principais documentados

**Plans:** 6 plansPlans:
**Wave 1**

- [x] 01-01-PLAN.md — Auth Foundation: JWT Ed25519, roles, refresh rotation, walking skeleton

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 01-02-PLAN.md — Cliente CRUD: domain, infra, REST com update
- [x] 01-03-PLAN.md — Veiculo CRUD: domain, infra, REST com update
- [x] 01-04-PLAN.md — Servico CRUD: domain, infra, REST (admin-only)

**Wave 3** *(blocked on Wave 2 completion)*

- [x] 01-05-PLAN.md — OrdemDeServico: state machine, child entities, public status

**Wave 4** *(blocked on Wave 3 completion)*

- [x] 01-06-PLAN.md — Sequence diagrams: Mermaid docs dos fluxos da OS

---

### Phase 2: OS Continuation & Estoque

**Goal:** Orçamento gerado e aprovado/reprovado pelo cliente, OS executada até finalização; estoque controlado com reserva atômica, requisições de compra e entrada NF
**Mode:** mvp
**Depends on:** Phase 1 (OS Aggregate root, Cliente/Veiculo/Servico endpoints)
**Requirements:** AUTH-04, OS-09, OS-10, OS-11, OS-13, OS-14, OS-16, OS-17, OS-18, EST-01, EST-02, EST-03, EST-04, EST-05, EST-06, EST-07, EST-08, EST-09, DOC-02
**Time allocation:** Days 5-7, 3 devs on OS + 2 devs on Estoque
**Success Criteria** (what must be TRUE):

   1. Sistema gera orçamento automaticamente ao finalizar diagnóstico (AGUARDANDO_APROVACAO); cliente aprova (EM_EXECUCAO) ou reprova (CANCELADA) via API pública
   2. Mecânico inicia execução (EM_EXECUCAO) e finaliza (FINALIZADA); admin lista/filtra OS com paginação e consulta tempo médio de execução
   3. Admin/almoxarife cadastra peças/insumos com saldo não-negativo; sistema reserva peças atomicamente ao aprovar orçamento (`OrcamentoAprovadoEvent`)
   4. Sistema gera Requisição de Compra para peças indisponíveis; almoxarife registra NF de entrada atualizando saldo; sistema verifica estoque mínimo e gera nova requisição se necessário
   5. Admin gerencia usuários do sistema (CRUD); especificação OpenAPI/Swagger documentada

**Plans:** 8 plans

**Wave 1** *(infrastructure foundation)*

- [x] PLAN-01-entity-base.md — Entities, VOs, Flyway V6-V11, domain models, ports, mappers

**Wave 2** *(application services — parallel: 3 devs OS + 2 devs Estoque)*

- [x] PLAN-02-estoque-domain-services.md — Peca/Req/Nf domain + services + atomic stock + CDI events
- [x] PLAN-03-orcamento-domain-services.md — Orcamento aggregate + OS state machine + SLA job + CDI events
- [x] PLAN-05-admin-users.md — Admin user CRUD service + role 'cliente' auth (fits in Wave 2)

**Wave 3** *(services + REST — parallel)*

- [x] PLAN-04-ordemservico-execucao.md — OS execution start/finish + metrics + detail + list filters
- [x] PLAN-06-estoque-rest.md — REST endpoints for Peca, RequisicaoCompra, NfEntrada, Alertas

**Wave 4** *(REST + cross-cutting)*

- [x] PLAN-07-orcamento-rest.md — REST for Orcamento approval/rejection + OS execution endpoints
- [x] PLAN-08-audit-openapi-build.md — Audit log, OpenAPI annotations, JaCoCo 80%, OWASP DC, README

---

### Phase 3: Pagamento & Delivery

**Goal:** Cobrança emitida ao finalizar OS, pagamento confirmado via banco simulado, veículo entregue — ciclo completo da OS finalizado
**Mode:** mvp
**Depends on:** Phase 1 (OS Aggregate root), Phase 2 (OS finalization working, estoque integrado)
**Requirements:** OS-12, PAG-01, PAG-02, PAG-03, DOC-03
**Time allocation:** Days 8-10, 3 devs on OS finalization + 2 devs on Pagamento
**Success Criteria** (what must be TRUE):

  1. SLA de orçamento expira automaticamente e cancela OS sem aprovação no prazo
  2. Sistema emite cobrança automaticamente ao finalizar execução (`OSFinalizadaEvent` → OrdemDePagamento Pendente)
  3. Sistema registra pagamento via banco simulado com idempotência (webhooks duplicados não geram efeito colateral)
  4. Admin registra entrega do veículo somente após pagamento confirmado (OS → ENTREGUE); sistema bloqueia entrega se pendente
  5. Guia de contribuição (CONTRIBUTING.md) com setup, padrões e workflow do time

**Plans:** TBD

---

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Auth & OS Foundation | 6/6 | ✅ Complete | 2026-06-28 |
| 2. OS Continuation & Estoque | 8/8 | ✅ Complete | 2026-06-29 |
| 3. Pagamento & Delivery | TBD (parcial) | 🔄 In progress — 3/6 issues | - |

---

## Day-by-Day Allocation

| Day | Phase | Team Split | Deliverable |
|-----|-------|------------|-------------|
| 1-4 | 1 | 5/5 Auth & OS | Auth JWT + roles + Cliente/Veiculo/Servico CRUD + OS create/diagnose + public status |
| 5-7 | 2 | 3/5 OS + 2/5 Estoque | Budget/approval + execution/finalization + full Estoque CRUD + reservation + NF |
| 8-10 | 3 | 3/5 OS + 2/5 Pagamento | SLA, cobrança, payment mock, delivery, CONTRIBUTING.md |

---

## Risk Mitigation

| Risk | Impact | Mitigation | Phase |
|------|--------|------------|-------|
| Mega-aggregate OS (Pitfall 1) | Transaction contention, perf degradation | Split Orcamento as separate AR; reference Cliente/Veiculo by UUID only; ArchUnit enforcement | 1 |
| Incomplete state machine (Pitfall 2) | Illegal transitions corrupt OS | Transition matrix `Map<StatusOS, Set<StatusOS>>` as single source of truth; parameterized test covering all 49 transitions | 1 |
| Inventory race condition (Pitfall 3) | Overselling stock | Atomic `UPDATE saldo = saldo - qtd WHERE saldo >= qtd`; `@Version` + `PESSIMISTIC_WRITE`; concurrent test | 2 |
| Payment idempotency (Pitfall 4) | Double-processing on duplicate webhook | Idempotency key on every cobrança; `processedEvents` table check before processing | 3 |
| Concurrent OS writes (Pitfall 5) | Lost updates | `@Version` on OS aggregate; explicit transition methods (no `setStatus()`) | 1 |
| Parallel dev integration chaos (Pitfall 9) | Day-8 integration scramble | Vertical slices: first 4 days all-hands on OS Core; day 5 split 3/2 | All |

---

## Requirement Coverage

| Category | Total | Phase 1 | Phase 2 | Phase 3 |
|----------|-------|---------|---------|---------|
| Auth | 4 | 3 | 1 | 0 |
| OS | 18 | 7 | 9 | 2 |
| Estoque | 9 | 0 | 9 | 0 |
| Pagamento | 3 | 0 | 0 | 3 |
| Documentação | 3 | 1 | 1 | 1 |
| **Total** | **37** | **11** | **20** | **6** |

✓ **37/37 v1 requirements mapped to phases**
✓ **No orphaned requirements**
