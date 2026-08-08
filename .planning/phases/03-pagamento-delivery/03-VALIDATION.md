---
phase: 3
slug: pagamento-delivery
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-08-08
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + QuarkusTest + REST Assured + Mockito + AssertJ |
| **Config file** | `pom.xml` (surefire + failsafe plugins) |
| **Quick run command** | `./mvnw test -pl mekano-domain` (domain unit tests) |
| **Full suite command** | `./mvnw verify -pl mekano-rest -am` |
| **Estimated runtime** | ~180 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./mvnw test -pl mekano-domain` or relevant module test
- **After every plan wave:** Run `./mvnw verify -pl mekano-rest -am`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Requirement | Test Type | Automated Command | Status |
|-------------|-----------|-------------------|--------|
| PAG-01 — Cobrança automática ao finalizar execução | integration | `./mvnw test -pl mekano-rest -am -Dtest=*Pagamento*` | ✅ green |
| PAG-02 — Confirmação de pagamento com idempotência | integration | `./mvnw test -pl mekano-rest -am -Dtest=*Pagamento*` | ✅ green |
| PAG-03 — Entrega do veículo após pagamento | integration | `./mvnw test -pl mekano-rest -am -Dtest=*Pagamento*` | ✅ green |
| OS-12 — SLA cancela OS automaticamente | integration | `./mvnw test -pl mekano-infrastructure -am -Dtest=*SlaExpiryJob*` | ✅ green |
| DOC-03 — CONTRIBUTING.md | manual | — | ✅ green |
| Idempotência de pagamento (2ª chamada = 200 no-op) | integration | `./mvnw verify -pl mekano-rest -am` | ✅ green |
| Auditoria de transições (PAGAMENTO_CONFIRMADO, ENTREGA_REALIZADA) | integration | `./mvnw verify -pl mekano-rest -am` | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] Existing test infrastructure covers all phase requirements via JUnit 5, Mockito, REST Assured, AssertJ

---

## Validation Sign-Off

- [x] All tasks have automated verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 180s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** approved

---

**Evidência de verificação:** [03-VERIFICATION.md](03-VERIFICATION.md) — verificada em 2026-08-08 pela phase 03.1-06.
nyquist_compliant: true — todos os requisitos têm verificação automatizada com latência < 180s.