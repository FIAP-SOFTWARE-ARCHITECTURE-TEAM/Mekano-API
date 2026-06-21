# Roadmap: Mekano

**Core Value:** Gerenciar o ciclo de vida completo das Ordens de Serviço — do recebimento do veículo à entrega — com rastreabilidade, controle de estoque e cobrança integrados.

**Granularity:** standard
**Mode:** mvp
**Total v1 Requirements:** 33
**Timeline:** 10 days
**Team:** 5 developers

---

## Phases

- [ ] **Phase 0: Foundation Review** — Review existing auth codebase, fix DDD adherence, verify build config before expansion
- [ ] **Phase 1: OS Core** — Full client/vehicle/service CRUD + OS lifecycle through 7-status state machine + budget/approval flow
- [ ] **Phase 2: Estoque Integration** — Parts CRUD, atomic reservation, purchase requisitions, NF entry, minimum stock alerts
- [ ] **Phase 3: Pagamento & Delivery** — Cobrança emission, payment confirmation via simulated bank, vehicle delivery release

---

## Phase Details

### Phase 0: Foundation Review
**Goal:** Existing auth subsystem reviewed, refactored to DDD standards, and build configuration verified for expansion
**Mode:** mvp
**Depends on:** Nothing
**Requirements:** None (prerequisite — prepares codebase)
**Time allocation:** 0.5 day, all 5 devs
**Success Criteria** (what must be TRUE):
  1. ArchUnit boundary tests pass — all layers (domain, application, infrastructure, rest) respect Clean Architecture dependency rules
  2. Jandex indexes verified in `application`, `infrastructure`, and `rest` modules — no `UnsatisfiedResolutionException` risk
  3. Annotation processor order confirmed correct (Lombok → lombok-mapstruct-binding → mapstruct-processor) across all modules
  4. Partial unique indexes for soft-delete entities verified in Flyway migrations (prevents constraint violation on re-registration)
  5. Existing User/Auth aggregates reviewed for DDD purity — no framework annotations in domain layer, ports correctly isolated
**Plans:** TBD

---

### Phase 1: OS Core
**Goal:** Admin/atendente can manage clients, vehicles, and service types; create OS through complete diagnosis/budget/approval workflow; client can track status publicly
**Mode:** mvp
**Depends on:** Phase 0
**Requirements:** OS-01, OS-02, OS-03, OS-04, OS-05, OS-06, OS-07, OS-08, OS-09, OS-10, OS-11, OS-12, OS-13, OS-14, OS-15, OS-16, OS-17, DOC-01, DOC-02
**Time allocation:** Days 1-4, all 5 devs
**Success Criteria** (what must be TRUE):
  1. Admin can register, view, update, and soft-delete clients (CPF/CNPJ validated, unique), vehicles (placa unique, Mercosul+old formats), and service types (valor > 0) via REST API
  2. Atendente can create OS (RECEBIDA), mechanic can start diagnosis (EM_DIAGNOSTICO), include services and parts, and system generates budget (AGUARDANDO_APROVACAO)
  3. Client can approve or reject budget via public API link — approval transitions OS to EM_EXECUCAO, rejection to CANCELADA
  4. Client can check OS status via public endpoint without authentication; admin can list/filter OS by date, status, and client (paginated)
  5. All 7 OS status transitions enforced via state machine — illegal transitions (e.g., RECEBIDA → ENTREGUE) rejected at domain level; SLA auto-expiration cancels OS when budget approval window expires
**Plans:** TBD

---

### Phase 2: Estoque Integration
**Goal:** Stock controlled with automatic reservation on budget approval, parts withdrawal on execution start, purchase requisitions for unavailable/short stock, and NF entry registration
**Mode:** mvp
**Depends on:** Phase 1 (OrcamentoAprovadoEvent defined, OS execution flow working)
**Requirements:** OS-18, EST-01, EST-02, EST-03, EST-04, EST-05, EST-06, EST-07, EST-08, EST-09
**Time allocation:** Days 5-7, 3 devs on OS refinements + 2 devs on Estoque
**Success Criteria** (what must be TRUE):
  1. Admin/almoxarife can register, update, and query parts with current balance (saldo never negative); system enforces `saldo_atual >= 0` at database level
  2. System atomically reserves available parts upon budget approval (`OrcamentoAprovadoEvent` handler); generates purchase requisition for unavailable parts
  3. Almoxarife can register NF entry referencing purchase requisition, updating stock balance; system auto-checks minimum stock and generates new requisition if needed
  4. Parts are debited from stock (reservation consumed) when OS execution starts; system alerts when stock falls below minimum threshold
  5. Admin can query average execution time by service type (OS-18) for given period
**Plans:** TBD

---

### Phase 3: Pagamento & Delivery
**Goal:** Payment processed via simulated bank service and vehicle delivery controlled — completing the OS business cycle end-to-end
**Mode:** mvp
**Depends on:** Phase 1 (OSFinalizadaEvent defined), Phase 2 (stock withdrawal working)
**Requirements:** PAG-01, PAG-02, PAG-03, DOC-03
**Time allocation:** Days 8-10, 3 devs on OS finalization + 2 devs on Pagamento
**Success Criteria** (what must be TRUE):
  1. System automatically emits cobrança (OrdemDePagamento → Pendente) when OS execution is finalized (`OSFinalizadaEvent` handler)
  2. System records payment confirmation via simulated bank service with idempotency key — duplicate webhooks produce no side effects
  3. Vehicle delivery is registered only after payment confirmed — OS transitions to ENTREGUE; system blocks delivery if payment is pending
  4. Team can set up local development environment from scratch using CONTRIBUTING.md guide with clear setup steps, branch workflow, and test instructions
**Plans:** TBD

---

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 0. Foundation Review | 0/0 | Not started | - |
| 1. OS Core | 0/0 | Not started | - |
| 2. Estoque Integration | 0/0 | Not started | - |
| 3. Pagamento & Delivery | 0/0 | Not started | - |

---

## Day-by-Day Allocation

| Day | Phase | Team Split | Deliverable |
|-----|-------|------------|-------------|
| 0 | 0 | 5/5 Foundation | Codebase reviewed, ArchUnit tests passing, build verified |
| 1-2 | 1 | 5/5 OS Core | Cliente/Veiculo/Servico CRUD + OS create + state machine core |
| 3-4 | 1 | 5/5 OS Core | Budget/approval flow, SLA, public status, list/filter |
| 5-7 | 2 | 3/5 OS refinements + 2/5 Estoque | Parts CRUD, reservation, req. compra, NF entry, min stock, avg time |
| 8-10 | 3 | 3/5 OS finalization + 2/5 Pagamento | Cobrança, payment mock, delivery, contributing guide |

---

## Risk Mitigation

| Risk | Impact | Mitigation | Phase |
|------|--------|------------|-------|
| Mega-aggregate OS (Pitfall 1) | Transaction contention, perf degradation | Split Orcamento as separate AR; reference Cliente/Veiculo by UUID only; ArchUnit enforcement | 1 |
| Incomplete state machine (Pitfall 2) | Illegal transitions corrupt OS | Transition matrix `Map<StatusOS, Set<StatusOS>>` as single source of truth; parameterized test covering all 49 transitions | 1 |
| Inventory race condition (Pitfall 3) | Overselling stock | Atomic `UPDATE saldo = saldo - qtd WHERE saldo >= qtd`; `@Version` + `PESSIMISTIC_WRITE`; concurrent test | 2 |
| Payment idempotency (Pitfall 4) | Double-processing on duplicate webhook | Idempotency key on every cobrança; `processedEvents` table check before processing | 3 |
| Concurrent OS writes (Pitfall 5) | Lost updates | `@Version` on OS aggregate; explicit transition methods (no `setStatus()`); SLA timer checks guard before transition | 1 |
| Parallel dev integration chaos (Pitfall 9) | Day-8 integration scramble | Vertical slices: first 4 days all-hands on OS Core; day 5 split 3/2; daily working demo | All |
| Soft delete + unique (Pitfall 10) | Constraint violation on re-registration | Partial unique indexes: `CREATE UNIQUE INDEX ... WHERE is_active = true` | 0 |

---

## Requirement Coverage

| Category | Total | Phase 0 | Phase 1 | Phase 2 | Phase 3 |
|----------|-------|---------|---------|---------|---------|
| OS | 18 | 0 | 17 | 1 | 0 |
| Estoque | 9 | 0 | 0 | 9 | 0 |
| Pagamento | 3 | 0 | 0 | 0 | 3 |
| Documentação | 3 | 0 | 2 | 0 | 1 |
| **Total** | **33** | **0** | **19** | **10** | **4** |

✓ **33/33 v1 requirements mapped to phases**
✓ **No orphaned requirements**
