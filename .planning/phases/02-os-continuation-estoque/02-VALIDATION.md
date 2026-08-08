---
phase: 2
slug: os-continuation-estoque
status: validated
nyquist_compliant: true
wave_0_complete: false
created: 2026-06-23
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Mockito + REST Assured + AssertJ |
| **Config file** | `pom.xml` (Maven Surefire Plugin) |
| **Quick run command** | `./mvnw test -pl mekano-domain -q` |
| **Full suite command** | `./mvnw verify -pl mekano-rest -am` |
| **Estimated runtime** | ~180 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./mvnw test -pl mekano-domain -q` (domain) or per-module
- **After every plan wave:** Run `./mvnw test -pl mekano-rest -am` (full integration)
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Module | Test Type | Automated Command |
|---------|------|------|--------|-----------|-------------------|
| 02-01-01 | PLAN-01 | 1 | domain | unit | `./mvnw test -pl mekano-domain -Dtest="*ValueObject*"` |
| 02-01-02 | PLAN-01 | 1 | domain | unit | `./mvnw test -pl mekano-domain -Dtest="*Enum*"` |
| 02-01-03 | PLAN-01 | 1 | domain | unit | `./mvnw test -pl mekano-domain` |
| 02-01-04 | PLAN-01 | 1 | domain | compile | `./mvnw compile -pl mekano-rest -am -q` |
| 02-01-05 | PLAN-01 | 1 | infrastructure | integration | `./mvnw test -pl mekano-infrastructure -am` |
| 02-01-06 | PLAN-01 | 1 | infrastructure | integration | `./mvnw test -pl mekano-infrastructure -am` |
| 02-02-01 | PLAN-02 | 2 | infrastructure | integration | `./mvnw test -pl mekano-infrastructure -am -Dtest="*PecaRepository*"` |
| 02-02-02..07 | PLAN-02 | 2 | application | integration | `./mvnw test -pl mekano-application -am -Dtest="*Estoque*"` |
| 02-03-01..06 | PLAN-03 | 2 | application | integration | `./mvnw test -pl mekano-application -am -Dtest="*OrcamentoService*"` |
| 02-04-01..07 | PLAN-04 | 3 | application | integration | `./mvnw test -pl mekano-application -am -Dtest="*OSExec*"` |
| 02-05-01..06 | PLAN-05 | 3 | rest | integration | `./mvnw test -pl mekano-rest -am -Dtest="*AdminUser*"` |
| 02-06-01..06 | PLAN-06 | 3 | rest | e2e | `./mvnw test -pl mekano-rest -am -Dtest="*Estoque*Resource*"` |
| 02-07-01..05 | PLAN-07 | 4 | rest | e2e | `./mvnw test -pl mekano-rest -am -Dtest="*OrcamentoResource*,*OSResource*"` |
| 02-08-01..06 | PLAN-08 | 4 | all | verify | `./mvnw verify -pl mekano-rest -am` |

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements (JUnit 5, Mockito, REST Assured, DevServices PostgreSQL, @TestSecurity for JWT bypass).

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Concurrent stock debit race condition | EST-03 | Requires thread orchestration | Use `CountDownLatch` + `ExecutorService` integration test |
| SLA expiry job timing | OS-10 | Scheduled job runs at fixed intervals | Set `quarkus.scheduler.start-mode=forced` and test with mocked clock |

---

## Validation Sign-Off

- [ ] All tasks have automated verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 180s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** approved

---

**Evidência de verificação:** [02-VERIFICATION.md](02-VERIFICATION.md) — verificada em 2026-08-08 pela phase 03.1-06.
nyquist_compliant: true — todos os requisitos têm verificação automatizada com latência < 180s.
