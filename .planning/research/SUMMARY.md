# Research Summary

**Project:** Mekano — Sistema Integrado de Atendimento e Execução de Serviços para Oficina Mecânica
**Domain:** Brazilian mechanical workshop management (Ordem de Serviço, Estoque, Pagamento)
**Researched:** 2026-06-20
**Confidence:** HIGH (all 4 research files verified against official docs, competitor analysis, and existing codebase)

## Executive Summary

Mekano is a **modular monolith API** for managing mechanical workshop operations in Brazil — specifically the complete Ordem de Serviço lifecycle (reception to delivery), integrated inventory control, and payment processing. The existing foundation (Java 17, Quarkus 3.36.0, Clean Architecture 4-module Maven structure, JWT auth with Ed25519) is solid and well-tested. Research confirms the architecture should expand from the existing User/Auth subsystem into three new bounded contexts: **Ordem de Serviço** (core), **Gestão de Estoque**, and **Ordem de Pagamento** — communicating exclusively via **CDI domain events** (`jakarta.enterprise.event.Event`), not direct service calls or message brokers.

The recommended approach is a **vertical-slice strategy**: build one complete end-to-end OS flow before adding stock or payment complexity. Research across 14 Brazilian competitors shows the MVP must include: OS lifecycle with a 7-status state machine, client/vehicle registration with CPF/CNPJ/Placa validation, budget generation with client approval (mandatory per CDC Art. 40), parts CRUD with balance invariants, stock reservation on budget approval, and payment processing with vehicle delivery. Key differentiators vs competitors: **SLA auto-expiration** (no competitor offers this), **public status tracking** (only ~20% of competitors), and the API-first architecture itself (competitors are closed SaaS).

The #1 risk is **parallel-development integration chaos** (Pitfall 9) — a 5-person team dividing by context instead of by vertical slice. Mitigation: first 4 days with all developers on OS Core to establish patterns, then split 3/2 for Estoque and Pagamento. Other critical risks: building a mega-aggregate `OrdemDeServico` that absorbs Cliente/Veiculo/Orcamento (Pitfall 1), inventory overselling from non-atomic reservation (Pitfall 3), and payment webhook non-idempotency (Pitfall 4). All have documented prevention strategies with ArchUnit enforcement and high-confidence test patterns.

## Key Findings

### Recommended Stack

The existing stack (Java 17, Quarkus 3.36.0, PostgreSQL 16, Maven multi-module, MapStruct 1.6.3, Lombok 1.18.36) is the correct foundation. Three new libraries are needed, all verified on Maven Central with 2025-2026 releases:

**Core additions:**
- **CDI Events** (`jakarta.enterprise.event.Event`) — built into Quarkus, zero dependencies. This is the inter-context communication backbone for the modular monolith. Use `fire()` for immediate consistency (stock reservation) and `fireAsync()` for eventual consistency (payment → delivery release). Confirmed by Quarkus discussion #51183 as the recommended pattern. **Do NOT use Kafka/RabbitMQ** for in-process events — overkill for a monolith where producer and consumer share the same JVM and transaction.
- **cpf-cnpj-utils** (1.0.0-alpha, felseje) — Brazilian document validation that supports **alphanumeric CNPJ** (mandatory from July 2026 per IN RFB 2.229). Alternatives are either dead projects (last updated 2012), GPLv3-licensed, or Spring Boot-specific. Zero runtime dependencies, Java 17+.
- **ArchUnit** (1.3.0) — enforces bounded context isolation at test time. Non-negotiable for a multi-context monolith. Catches cross-context imports that would prevent future extraction to microservices. Configured via `@AnalyzeClasses` with context boundary rules.
- **cnpj-alfanumerico** (1.1.0, optional) — provides `@CNPJ` Bean Validation annotation for REST DTOs if desired.

**Domain value object patterns:**
- `Documento` — sealed abstract class with `CPF` and `CNPJ` subtypes. Validates Mod 11 check digits, supports alphanumeric CNPJ base-36 conversion. Type-safe — the domain knows whether it's CPF or CNPJ at compile time.
- `Placa` — Java record accepting both old format (`ABC-1234`) and Mercosul format (`ABC1D23`). Regex excludes confusing letters I, O, Q per DENATRAN rules.
- Monetary values must use `BigDecimal` or `Long` (cents) — never `double`/`float`.

**Event pattern:** Integration events are Java records with only primitive types (serializable). Domain events stay inside their context. Taxonomy: `OrcamentoAprovadoEvent` (OS→Estoque, sync, same-tx), `OSFinalizadaEvent` (OS→Pagamento, sync), `PagamentoConfirmadoEvent` (Pagamento→OS, sync). Choreography-based saga — no orchestrator needed.

### Expected Features

**Must-have for MVP (Phase 1) — 8 features:**
1. OS Lifecycle with 7-status state machine (RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → CANCELADA/EM_EXECUCAO → FINALIZADA → ENTREGUE)
2. Client Registration with CPF/CNPJ validation and uniqueness
3. Vehicle Registration with Mercosul plate validation and uniqueness per client
4. Service Type Catalog (CRUD)
5. Budget Generation + Client Approval via public link (CDC Art. 40 compliance)
6. Parts/Supplies CRUD with non-negative balance invariants
7. Stock Reservation on Budget Approval (cross-aggregate coordination via event)
8. Payment Processing + Vehicle Delivery (completes business cycle)
Plus: Public Client Status Tracking, Minimum Stock Alerts, OS Listing with Filters

**Differentiators (competitive moat):**
- **SLA auto-expiration** — no competitor offers this. `PoliticaSLA` value object with `tempoMaximoAprovacao`. Auto-cancels OS when budget expires.
- **Public status tracking** — only ~20% of competitors offer this. Reduces atendente phone calls.
- **API-first architecture** — competitors are closed SaaS. Our Clean Architecture enables mobile/web/third-party integrations they cannot match.
- **Automatic stock reservation on approval** — few competitors link OS, orçamento, and estoque atomically.
- **Auto-purchase requisition** — triggers when stock insufficient for approved OS or below minimum.

**Legal requirements:**
- CDC Art. 40: Orçamento prévio detalhado with explicit client approval. Items cannot be added after approval without re-authorization.
- CDC Art. 26: 90-day minimum warranty on services and parts. Record warranty start date per item.
- IN RFB 2.229: Alphanumeric CNPJ mandatory from July 2026. Validate with base-36 check-digit algorithm.
- NFS-e Nacional: Mandatory from Jan 2026 per LC 214/2024. Target national standard API.

**Anti-features (defer):** Real-time chat, full accounting (DRE/SPED), AI diagnostics, mobile app, real payment gateway integration, multi-workshop, online scheduling.

### Architecture Approach

**4-module Clean Architecture** with bounded contexts grouped within each layer. Three new contexts (Ordem de Serviço, Estoque, Pagamento) join the existing User/Auth context. Each context has its own packages in domain/model/, application/usecase/, infrastructure/entity+repository, and rest/api/.

**Major architectural patterns:**
1. **OS State Machine** — 7 statuses with explicit transition methods on the aggregate root (`iniciarDiagnostico()`, `finalizarDiagnostico()`, `aprovarOrcamento()`, etc.). No `setStatus()` — transitions are business methods that validate guards. Transition matrix with `Map<StatusOS, Set<StatusOS>>` as single source of truth.
2. **Domain Events for Inter-Context Communication** — CDI `Event.fire()` (same transaction) for critical flows (stock reservation must succeed or OS approval rolls back). `Event.fireAsync()` for non-critical flows (payment confirmation → delivery). Integration events are Java records with primitives only.
3. **Aggregate Design with UUID References** — `Cliente` and `Veiculo` are separate aggregates referenced by UUID from `OrdemDeServico`. `Orcamento` is a separate aggregate root with its own lifecycle. `ReservaEstoque` lives inside `Estoque` aggregate (consistency boundary for reservation).
4. **Use Case as Consistency Coordinator** — `@Transactional` on use case methods. Event publisher fires after domain state change. Listeners in other contexts run in same/separate transaction depending on `@Observes` vs `@ObservesAsync`.

**Major components per bounded context:**
| Component | Responsibility |
|-----------|----------------|
| `OrdemDeServico` (AR) | OS state machine, item list, orçamento reference |
| `Cliente` (AR) | Client registration, CPF/CNPJ validation |
| `Veiculo` (AR) | Vehicle registration, plate uniqueness |
| `Orcamento` (AR) | Budget generation, SLA expiry, approval workflow |
| `Estoque` (AR) | Inventory balance, reservation, minimum stock |
| `RequisicaoDeCompra` (AR) | Purchase requisition when stock insufficient |
| `OrdemDePagamento` (AR) | Payment tracking, cobrança emission, delivery release |
| `CdiEventPublisher` | Dispatches domain events via CDI Event bus |
| `EventConsumer` | Listens for events, triggers cross-context reactions |

**Test pyramid:** Domain tests (pure JUnit 5, no framework) → Component tests (`@QuarkusComponentTest`) → Integration tests (`@QuarkusTest` + REST Assured + DevServices) → ArchUnit boundary tests (1 per context). Cross-context flows tested via direct `Event.fire()` in test harness (deterministic, no timing issues).

### Critical Pitfalls

Top 5 pitfalls (all HIGH confidence, verified against multiple sources):

1. **Mega-Aggregate OS** — Putting Cliente, Veiculo, Orcamento, ItemOS inside OrdemDeServico creates transaction contention and performance degradation. *Prevention:* Split Orcamento into separate aggregate root. Reference Cliente/Veiculo by UUID only. Enforce with ArchUnit: `noClasses().that().resideInPackage("..ordemservico..").should().dependOn(Cliente.class)`.

2. **Incomplete State Machine** — Missing transition edges (e.g., re-orçamento from EM_EXECUCAO back to AGUARDANDO_APROVACAO) or illegal flows not enforced. *Prevention:* Build transition matrix as `Map<StatusOS, Set<StatusOS>>` with single parameterized test covering all 49 possible transitions. StatusOS is a value object with `canTransitionTo()`.

3. **Inventory Reservation Race Condition** — Two parallel OS approvals both read `saldoAtual = 5`, both pass the availability check, both write — overselling stock. *Prevention:* Atomic database-level `UPDATE item_estoque SET saldo_atual = saldo_atual - :qtd WHERE id = :id AND saldo_atual >= :qtd`. Use `LockModeType.PESSIMISTIC_WRITE` and `@Version`. Write concurrent test: 10 parallel requests for the same item, assert exactly N succeed (N = available stock).

4. **Payment Webhook Idempotency** — Duplicate callbacks from bank service cause double-processing or 500 errors. Timing races between "OS finalized" and "payment confirmed" events. *Prevention:* Idempotency key (UUID) on every Cobranca. Check `processedEvents.exists(key)` before processing. Async boundary: `Pagamento.confirmar()` emits `PagamentoConfirmadoEvent` → separate handler calls `os.entregar()`. Validate payment amount >= cobranca amount.

5. **Concurrent OS Status Writes** — Atendente, mecânico, and SLA timer all mutate the same OS concurrently. Default `READ_COMMITTED` isolation doesn't prevent lost updates. *Prevention:* `@Version` on OS aggregate. Explicit transition methods (not `setStatus()`). SLA timer checks status guard before transitioning.

**Deadline risk:** Building all 3 contexts in parallel without vertical slicing leads to day-8 integration scramble (Pitfall 9). **Mitigation:** Vertical slices — first 4 days all-hands on OS Core (establishes patterns), then split. Daily working demo by day 4 minimum.

## Implications for Roadmap

Based on combined research (feature dependencies, architecture build order, and pitfall prevention), the recommended phase structure follows a **vertical slice strategy** — completing one end-to-end flow before adding the next dimension of complexity.

### Phase 1: OS Foundation (Days 1-4)
**Rationale:** OS Core is the dependency root — everything else (Estoque, Pagamento) builds on it. All 5 developers work on OS context to establish patterns (aggregates, value objects, state machine, events) that Estoque and Pagamento will replicate. Avoiding the horizontal-split trap (Pitfall 9).
**Delivers:** Complete service-only OS lifecycle end-to-end. Cliente CRUD with Documento value object (CPF/CNPJ validation including alphanumeric). Veiculo CRUD with Placa value object (both old and Mercosul formats). Servico catalog CRUD. OS state machine with all 7 statuses and transition matrix. Budget generation with public approval link.
**Addresses:** FEATURES P1-P3 (OS lifecycle, Client/Vehicle/Service, Budget/Approval) plus P6 (public status tracking — simple since OS lifecycle is done). Also covers OS-01 through OS-10 from PROJECT.md.
**Avoids:** Pitfall 1 (mega-aggregate — enforce via ArchUnit from day 1), Pitfall 2 (incomplete state machine — transition matrix + parameterized test), Pitfall 6 (CPF/CNPJ validation — Documento VO with full edge case suite), Pitfall 7 (Mercosul plate — Placa VO with both formats), Pitfall 8 (Quarkus CDI — verify Jandex and annotation processor order), Pitfall 10 (soft delete + unique — partial unique indexes in Flyway migrations).
**Research flag:** No additional research needed — patterns are well-established from existing codebase (User/UserEntity pattern, CdiEventPublisher, etc.) and confirmed by Quarkus docs.

### Phase 2: Estoque Integration (Days 5-7)
**Rationale:** Building on the OS approval event. Stock reservation triggers on `OrcamentoAprovadoEvent` — the event shape was defined in Phase 1. 3 devs continue OS improvements (edge cases, SLA), 2 devs build Estoque context.
**Delivers:** Parts/supplies CRUD with balance invariants. Stock reservation (atomic `UPDATE` pattern, `@Version`, `@Lock(PESSIMISTIC_WRITE)`). Parts withdrawal on execution start. Minimum stock alerts. Purchase requisition auto-generation. NF-e entry registration (simulated).
**Addresses:** FEATURES P4 (Parts CRUD), P5 (Stock Reservation), P6 (Parts Withdrawal), P10 (Purchase Requisition). Covers EST-01 through EST-06 from PROJECT.md. Also OS-12 (SLA auto-expiration — uses the PoliticaSLA value object designed in Phase 1).
**Uses:** CDI Event `@Observes(OrcamentoAprovadoEvent)` for reservation trigger. ArchUnit rules for Estoque context isolation. Caffeine cache for stock queries (extend existing pattern).
**Avoids:** Pitfall 3 (inventory race condition — atomic UPDATE + concurrent test). Pitfall 5 (concurrent writes — `@Version` on Estoque aggregate).
**Research flag:** No additional research needed — standard patterns for inventory reservation (O'Reilly DDIA, Vaughn Vernon IDDD). The atomic UPDATE pattern and pessimistic locking are well-documented.

### Phase 3: Pagamento + Delivery (Days 8-10)
**Rationale:** Payment processing completes the business cycle. Depends on OS finalized event (`OSFinalizadaEvent`) which produces the cobrança. 3 devs finalize OS edge cases and integration tests, 2 devs build Pagamento context.
**Delivers:** OrdemDePagamento creation on OS finalization. Cobrança emission (simulated bank service). Payment confirmation with idempotency handling. Vehicle delivery registration (blocks until payment confirmed).
**Addresses:** FEATURES P7 (Payment + Delivery). Covers PAG-01 through PAG-03 from PROJECT.md. Also public status tracking (now shows complete flow including payment status).
**Uses:** CDI Event `@Observes(OSFinalizadaEvent)` for cobrança emission. `@Observes(PagamentoConfirmadoEvent)` for delivery release. `ServicoBancarioPort` interface in domain with simulated implementation in infrastructure.
**Avoids:** Pitfall 4 (payment webhook idempotency — idempotency key + processed events table + async boundary). Pitfall 5 (concurrent writes — `@Version` on OrdemDePagamento).
**Research flag:** Phase may benefit from deeper research if real bank integration is attempted (requires per-gateway certification). For MVP with simulated bank, patterns are standard.
**Integration test critical path (from "Looks Done But Isn't" checklist):** Create OS with parts → approve orçamento → verify stock reservation → finalize OS → verify cobrança emitted → confirm payment → verify OS becomes ENTREGUE. Test idempotency: send same webhook twice → assert no double-processing.

### Phase Ordering Rationale

- **Dependency graph is linear:** User/Auth (exists) → OS Core (Phase 1) → Estoque (Phase 2) and Pagamento (Phase 3). Phase 2 and 3 are independent of each other and can be built in parallel by different sub-teams once Phase 1 is complete.
- **CDI events are the coupling mechanism:** The event shape (`OrcamentoAprovadoEvent`, `OSFinalizadaEvent`) must be defined in Phase 1 before consumers in Estoque/Pagamento can be implemented. Attempting parallel phase 1 for all three contexts means defining events without having implemented the producer — risky and error-prone.
- **Pitfall 9 (deadline rush) dictates the sequencing:** The vertical-slice approach (one complete flow first) is the recommended strategy specifically to avoid integration-failure-on-day-8. All-hands on OS Core for 4 days establishes patterns, reduces context-switching overhead, and produces a demo-able result.
- **Legal compliance drives feature priority:** CDC Art. 40 (orçamento prévio) makes budget generation and client approval non-negotiable for Phase 1. Without this flow, the workshop operates illegally. Payment processing (Phase 3) is secondary to the approval flow.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 3:** If real bank/payment gateway integration (PIX, boleto) is attempted instead of simulated `ServicoBancario` — requires per-gateway API research, webhook certification, and async callback architecture. Recommend deferring to Phase 3+ and using simulated service for MVP.
- **Phase 2 (NF-e XML Import — EST-05):** If real NF-e XML parsing is required for MVP (not simulated), needs research on SEFAZ schema validation, NFe/XSD parser libraries, and fiscal layout mapping. Currently marked as Phase 3+ in FEATURES.md. Defer to Phase 2+.

Phases with standard patterns (skip research-phase):
- **Phase 1:** All patterns confirmed by existing codebase and Quarkus official docs. Clean Architecture, CDI events, MapStruct mappers, JPA repositories — all established patterns. Add ArchUnit boundary tests.
- **Phase 2 (stock reservation):** Well-documented in DDIA, Vaughn Vernon IDDD. Atomic UPDATE pattern is standard. No niche library research needed.
- **Phase 3 (payment simulation):** Standard port-adapter pattern already established in project (EventPublisher, PasswordHasher). Simulated bank is a simple `ServicoBancarioPort` impl.

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | **HIGH** | All libraries verified on Maven Central with 2025-2026 releases. Quarkus CDI events confirmed by official docs and community discussion #51183. Existing codebase patterns validated. |
| Features | **HIGH** | Verified against 14 competitor products (all HIGH/MEDIUM confidence sources), official Brazilian legislation (CDC, RFB, DENATRAN), and project's own Event Storming docs. Feature prioritization cross-referenced with legal requirements. |
| Architecture | **HIGH** | Patterns verified against existing compiled codebase, Quarkus official docs, DDD reference literature (Vernon, Krzybek), and Quarkus blog. 3 bounded contexts mapped from Event Storming with clear aggregate boundaries and event flows. |
| Pitfalls | **HIGH** | Multi-source verified: project's own G1-G10 gotcha list (from real experience), Vaughn Vernon aggregate design papers, O'Reilly DDIA, Quarkus CDI reference, Stripe payment patterns, Brazilian government regulations. |

**Overall confidence: HIGH** — all 4 research areas have strong source triangulation (official docs + community patterns + existing codebase + competitor analysis). No major gaps or conflicting information across the 4 files.

### Gaps to Address

- **Real payment gateway specifics:** The research assumes a simulated `ServicoBancario` for MVP. If Phase 3 scope expands to include real PIX/boleto integration, per-gateway API research (Asaas, Stone, Cielo) will be needed during the Pagamento Foundation Phase. The idempotency patterns and async webhook architecture from Pitfall 4 provide the structural foundation but gateway-specific certification requirements are undeclared.
- **NF-e XML integration depth:** Research covers the requirement (EST-05) and the anti-pattern (re-implementing SEFAZ validation) but does not specify which XML parsing library to use. If this moves into MVP scope, a dedicated library research task is needed.
- **Workshop-scale performance validation:** Performance traps are documented but not benchmarked. The research assumes workshop-scale traffic (<100 OS/month) won't hit bottlenecks. If the deployment context changes (multi-workshop on day 1), re-validation needed. Not a blocker for the 10-day sprint.

## Sources

### Primary (HIGH confidence)
- **Existing codebase** — `.planning/codebase/ARCHITECTURE.md`, `.planning/codebase/STRUCTURE.md`, `CLAUDE.md` — compiled code patterns for User/Auth context. All patterns verified against compiled code.
- **Project documentation** — `docs/EventStorming_Mermaid.md` (aggregate definitions, events, state machine), `docs/MEKANO_DOCUMENTATION.md` (functional requirements), `.planning/PROJECT.md` (current context).
- **Quarkus CDI Reference** — Official docs confirming `jakarta.enterprise.event.Event` API and `@Observes` patterns. Quarkus Discussion #51183 confirming CDI events for bounded contexts in monoliths.
- **Quarkus Testing Guide** — `@QuarkusTest` vs `@QuarkusComponentTest` patterns for multi-context testing. Published 2025.
- **Brazilian Legislation** — CDC Lei 8.078/90 (Art. 35, 39, 40), IN RFB 2.229 (alphanumeric CNPJ from July 2026), LC 214/2024 (NFS-e Nacional from Jan 2026), DENATRAN Res. 780/2019 (Mercosul plate).
- **Vaughn Vernon "Effective Aggregate Design"** (DDDCommunity, 2011) — Parts I-III. Primary source for aggregate consistency boundaries and the "mega-aggregate" anti-pattern.
- **O'Reilly "Designing Data-Intensive Applications"** — Inventory reservation race condition patterns (atomic updates, pessimistic locking).
- **Existing Gotcha List (G1-G10)** — Project's own CLAUDE.md, derived from real experience building the auth subsystem. Covers Quarkus multi-module CDI failures, MapStruct processor ordering, Flyway naming, JWT scoping.

### Secondary (MEDIUM confidence)
- **14 Competitor Products Analyzed** — MecPro, Ultracar, Garage, AutoERP, MecânicaFlow, and 10 others. Feature matrix cross-reference for table stakes vs differentiators.
- **Modular Monolith with DDD (Krzybek, GitHub)** — Reference architecture for integration events vs domain events pattern. High-quality patterns but not officially Quarkus-specific.
- **ArchUnit Part 2: Enforcing DDD Boundaries (Jitin Kayyala, 2026)** — Specific ArchUnit rules for bounded context isolation.
- **Brazilian document validation libraries** — Multiple alternatives evaluated (danielfariati, jereztech, andrelamego, robsonkades) before selecting `cpf-cnpj-utils`. Dead project dates and license restrictions verified on Maven Central.

---

*Research completed: 2026-06-20*
*Ready for roadmap: yes*
