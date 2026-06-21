# STATE: Mekano

**Init:** 2026-06-20
**Core Value:** Gerenciar o ciclo de vida completo das Ordens de Serviço — do recebimento do veículo à entrega — com rastreabilidade, controle de estoque e cobrança integrados.

---

## Project Reference

| Key | Value |
|-----|-------|
| Stack | Java 17, Quarkus 3.36.0, PostgreSQL 16, Maven multi-module |
| Architecture | Clean Architecture (domain → application → infrastructure → rest) |
| Bounded Contexts | User/Auth (existing), OS (Phase 1), Estoque (Phase 2), Pagamento (Phase 3) |
| Team | 5 developers |
| Timeline | 10 days |
| Granularity | standard |
| Mode | mvp |

## Current Position

| Attribute | Value |
|-----------|-------|
| **Phase** | 1 — Auth & OS Foundation |
| **Plan** | - |
| **Status** | Not started |
| **Progress** | ░░░░░░░░░░░░░░░░░░░░ 0% |
| **Milestone** | v1 |

## Performance Metrics

*Gathering data — no metrics recorded yet.*

| Metric | Current | Target | Notes |
|--------|---------|--------|-------|
| Requirements mapped | 37/37 | 37 | ✓ Full coverage |
| Phases defined | 3 | — | Auth & OS → OS Cont. & Estoque → Pagamento & Delivery |

## Accumulated Context

### Key Decisions (from research/planning)

| Decision | Rationale | Status |
|----------|-----------|--------|
| Vertical slice strategy | Avoids integration chaos with 5 devs in parallel | Locked |
| OS state machine with transition matrix | Prevents illegal state transitions | Locked |
| CDI events for inter-context comm | Decoupled contexts without Kafka overengineering | Locked |
| Orcamento as separate aggregate | Prevents mega-aggregate (Pitfall 1) | Locked |
| Atomic stock reservation (UPDATE ... WHERE saldo >= qtd) | Prevents race condition (Pitfall 3) | Locked |
| Payment idempotency via processed_events table | Prevents double-processing (Pitfall 4) | Locked |
| Auth construído como task da Phase 1 | Não revisar codebase existente, tratar como task | Locked |

### Pending Decisions

- [ ] **SLA policy values**: What default SLA window for budget expiration? (PoliticaSLA value object)
- [ ] **Simulated bank service**: What delay profile for the mock? (@Retry/@Timeout)
- [ ] **Roles**: Confirm list of roles (admin, atendente, mecanico, almoxarife, financeiro)
- [ ] **JWT strategy**: Reutilizar Ed25519 existente ou nova config?

### Open Todos

- [ ] PLAN.md: Phase 1 — Auth & OS Foundation
- [ ] Criar tasks individuais para GitHub Issues

### Blockers

None yet.

## Session Continuity

**Last session:** 2026-06-20 — Roadmap restructured (3 phases, 37 reqs mapped, auth como task).
**Next action:** `/gsd-plan-phase 1` — Create execution plan for Auth & OS Foundation.

### Threads

| Thread | Phase | Status | Owner | Context |
|--------|-------|--------|-------|---------|
| Roadmap restructured | 1-3 | ✅ Done | GSD | All 37 v1 mapped, 3 phases, vertical slices |

### Next Phase Brief

**Phase 1 — Auth & OS Foundation** (Days 1-4, all 5 devs):
1. Implementar auth JWT com roles (admin, atendente, mecanico, almoxarife, financeiro)
2. CRUD Cliente (CPF/CNPJ validado), Veículo (placa única), Serviço (valor > 0)
3. OS lifecycle: RECEBIDA → EM_DIAGNOSTICO com state machine
4. Endpoint público de consulta de status da OS
5. Diagramas de sequência dos fluxos principais
