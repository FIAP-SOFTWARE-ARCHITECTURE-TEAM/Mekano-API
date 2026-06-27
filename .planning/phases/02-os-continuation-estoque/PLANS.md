# Phase 2: OS Continuation & Estoque — Plan Index

**Team:** 3 devs OS + 2 devs Estoque (Days 5-7)
**Mode:** mvp
**Total Requirements:** 20 (AUTH-04, OS-09/10/11/13/14/16/17/18, EST-01..09, DOC-02)

---

## Requirements Matrix

| Req ID | PLAN-01 | PLAN-02 | PLAN-03 | PLAN-04 | PLAN-05 | PLAN-06 | PLAN-07 | PLAN-08 |
|--------|---------|---------|---------|---------|---------|---------|---------|---------|
| AUTH-04 | | | | | ✓ | | | |
| OS-09 | | | ✓ | | | | ✓ | |
| OS-10 | | | ✓ | | | | ✓ | |
| OS-11 | | | ✓ | | | | ✓ | |
| OS-13 | | | | ✓ | | | ✓ | |
| OS-14 | | | | ✓ | | | ✓ | |
| OS-16 | | | | ✓ | | | ✓ | |
| OS-17 | | | | ✓ | | | ✓ | |
| OS-18 | | | | ✓ | | | ✓ | |
| EST-01 | ✓ | ✓ | | | | ✓ | | |
| EST-02 | | ✓ | | | | ✓ | | |
| EST-03 | | ✓ | | | | | | |
| EST-04 | | ✓ | | | | | | |
| EST-05 | | ✓ | | | | ✓ | | |
| EST-06 | | ✓ | | | | ✓ | | |
| EST-07 | | ✓ | | | | | | |
| EST-08 | | | | ✓ | | | | |
| EST-09 | | ✓ | | | | ✓ | | |
| DOC-02 | | | | | | | | ✓ |

---

## Dependency Graph

```
PLAN-01 (Entity Base + Events) ← ALL domain events here
  ├──> PLAN-02 (Estoque Domain/Services)
  ├──> PLAN-03 (Orcamento Domain/Services)
  │     └──> PLAN-04 (OS Execution/Services)
  │           └──> PLAN-07 (Orcamento REST + OS REST)
  ├──> PLAN-06 (Estoque REST)
  ├──> PLAN-08 (Audit/OpenAPI/Build)
  └── (no dep) PLAN-05 (Admin Users)

No cross-dependency between PLAN-02 and PLAN-03 — both consume events from PLAN-01.
```

**Legend:**
- `──>` = depends on

---

## Execution Waves

| Wave | Plans | Issue | Status | Description | Est. Effort |
|------|-------|-------|--------|-------------|-------------|
| **1** | PLAN-01 | #21 ✅, #22 🔄 | em andamento | Infrastructure foundation: entities, VOs, Flyway V11-V17, domain models, repository ports | 3-4h |
| **2** | PLAN-02, PLAN-03 | #23 ✅, #24 🔄 | em andamento | Application services: Peca/Req/Nf services + Orcamento/OS services + CDI events + SLA job | 4-5h each |
| **3** | PLAN-04, PLAN-05, PLAN-06 | #25 🔄, #26 🔄, #27 🔄 | em andamento | OS execution/metrics + Admin user CRUD + Estoque REST | 3-4h each |
| **4** | PLAN-07, PLAN-08 | #28 🔄, #29 🔄 | em andamento | Orcamento REST, OS extension REST, audit log, OpenAPI, JaCoCo, OWASP DC, README | 3-4h each |

---

## Plan Summary

| Plan | Name | Tasks | Issue | Files Created | Key Requirements |
|------|------|-------|-------|--------------|------------------|
| 01 | Entity Base + Events | 7 | #21 (domain), #22 (events+infra) | ~42 | EST-01 (foundation), ALL events |
| 02 | Estoque Domain/Services | 7 | #23 | ~23 | EST-01..07, EST-09 |
| 03 | Orcamento Domain/Services | 6 | #24 | ~16 | OS-09, OS-10, OS-11 |
| 04 | OS Execution/Services | 7 | #25 | ~12 | OS-13, OS-14, OS-16, OS-17, OS-18, EST-08 |
| 05 | Admin Users | 6 | #26 | ~8 | AUTH-04 |
| 06 | Estoque REST | 6 | #27 | ~14 | EST-01, EST-02, EST-05, EST-06, EST-09 |
| 07 | Orcamento REST | 5 | #28 | ~12 | OS-09..11, OS-13..14, OS-16..18 |
| 08 | Audit/OpenAPI/Build | 6 | #29 | ~10 | DOC-02, D-73..D-75 |

---

## Parallelization Strategy

**Wave 2 parallel:** PLAN-02 (estoque) and PLAN-03 (orcamento) are independent (both consume events from PLAN-01) — can be done simultaneously by 2 devs each.

**Wave 3 parallel:** PLAN-04 (OS execution), PLAN-05 (admin users), and PLAN-06 (estoque REST) are independent — 3 devs can work simultaneously.

**Wave 4 serialization:** PLAN-07 needs PLAN-04's services wired to REST. PLAN-08 needs all implementations annotated.

---

## Risk Register

| Risk | Plan(s) Affected | Mitigation |
|------|------------------|------------|
| Atomic stock race condition | PLAN-02 | Native SQL `UPDATE ... WHERE saldo >= qtd`; integration test with concurrent threads |
| H2/PostgreSQL compat in native SQL | PLAN-02 | Use standard SQL arithmetic UPDATE; H2 MODE=PostgreSQL handles `saldo = saldo - :qtd` |
| CDI event + TX rollback | PLAN-02, PLAN-03 | Keep observers synchronous (same TX); document TX propagation expectations |
| JaCoCo double-instrumentation | PLAN-08 | Use stand-alone `jacoco-maven-plugin` (not `quarkus-jacoco` extension) |
| Flyway version conflict w/ Phase 1 | PLAN-01 | Phase 1 ocupa V6-V10 (user_roles, clientes, veiculos, servicos, ordens_de_servico). Phase 2 usa V11-V16 + V17 (seed role cliente). Ambos convivem no mesmo diretório — Flyway aplica em ordem. |

---

## Sync Log

- 2026-06-23 21:49: sincronizado 9 issues — concluídos: 0, em andamento: 9, pendentes: 0
- 2026-06-26 16:59: sincronizado 9 issues — concluídos: 2, em andamento: 7, pendentes: 0
