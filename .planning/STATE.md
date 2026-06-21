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
| **Phase** | 0 — Foundation Review |
| **Plan** | - |
| **Status** | Not started |
| **Progress** | ░░░░░░░░░░░░░░░░░░░░ 0% |
| **Milestone** | v1 |

## Performance Metrics

*Gathering data — no metrics recorded yet.*

| Metric | Current | Target | Notes |
|--------|---------|--------|-------|
| Requirements mapped | 33/33 | 33 | ✓ Full coverage |
| Phases defined | 4 | — | Foundation → OS Core → Estoque → Pagamento |

## Accumulated Context

### Key Decisions (from research/planning)

| Decision | Rationale | Status |
|----------|-----------|--------|
| Vertical slice strategy | Avoids Pitfall 9 (parallel integration chaos) | Locked |
| OS state machine with transition matrix | Prevents Pitfall 2 (incomplete state machine) | Locked |
| CDI events for inter-context comm | Decoupled contexts without Kafka overengineering | Locked |
| Orcamento as separate aggregate | Prevents Pitfall 1 (mega-aggregate) | Locked |
| Atomic stock reservation (UPDATE ... WHERE saldo >= qtd) | Prevents Pitfall 3 (race condition) | Locked |
| Payment idempotency via processed_events table | Prevents Pitfall 4 (double-processing) | Locked |
| Partial unique indexes for soft delete | Prevents Pitfall 10 (constraint violation) | Locked |

### Pending Decisions

- [ ] **Phase 0 scope**: Which specific code smell fixes to prioritize (ArchUnit tests, Jandex, annotation processor order)?
- [ ] **MapStruct mappers**: Confirm annotation processor order in all `pom.xml` files before Phase 1 coding.
- [ ] **SLA policy values**: What default SLA window for budget expiration? (Research: `PoliticaSLA` value object)
- [ ] **Simulated bank service**: What delay profile for the mock? (Research: 2-5s simulated delay with `@Retry`/`@Timeout`)

### Open Todos

- [ ] PLAN.md: Phase 0 — Foundation Review (codebase inspection, ArchUnit setup, build verification)
- [ ] CLAUDE.md review: Ensure gotchas G1-G10 are addressed before Phase 1 expansion starts

### Blockers

None yet.

## Session Continuity

**Last session:** 2026-06-20 — Roadmap initialized (4 phases, 33 reqs mapped).
**Next action:** `/gsd-plan-phase 0` — Create execution plan for Foundation Review.

### Threads

| Thread | Phase | Status | Owner | Context |
|--------|-------|--------|-------|---------|
| Roadmap creation | 0-3 | ✅ Done | GSD | All 33 v1 mapped, 4 phases, vertical slices |

### Next Phase Brief

**Phase 0 — Foundation Review** (0.5 day, all 5 devs):
1. Run `mvn clean compile` — verify Jandex indexes exist in all non-domain modules
2. Add ArchUnit dependency and write Clean Architecture boundary tests
3. Review existing User/Auth for DDD purity (no framework leak in domain layer)
4. Verify Flyway migrations use partial unique indexes for soft-delete entities
5. Confirm annotation processor order in all POMs
6. Create CONTRIBUTING.md skeleton
7. Tag branch `gsd/phase-0-foundation-review`
