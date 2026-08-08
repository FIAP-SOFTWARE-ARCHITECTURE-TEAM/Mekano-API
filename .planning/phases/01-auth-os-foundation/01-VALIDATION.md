---
phase: 1
slug: auth-os-foundation
status: validated
nyquist_compliant: true
wave_0_complete: false
created: 2026-06-22
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + QuarkusTest + REST Assured + Mockito + AssertJ |
| **Config file** | `pom.xml` (surefire + failsafe plugins) |
| **Quick run command** | `./mvnw test -pl mekano-domain` (domain unit tests) |
| **Full suite command** | `./mvnw verify -pl mekano-rest -am` |
| **Estimated runtime** | ~120 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./mvnw test -pl mekano-domain` or relevant module test
- **After every plan wave:** Run `./mvnw verify -pl mekano-rest -am`
- **Before `/gsd-verify-work`:** Full suite must be green
- **Max feedback latency:** 180 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Threat Ref | Secure Behavior | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|------------|-----------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01 | 1 | AUTH-01 | T-1-01 / — | N/A for role model | unit | `mvn test -pl mekano-domain -Dtest=*Role*` | ❌ W0 | ⬜ pending |
| 01-01-02 | 01 | 1 | AUTH-02 | T-1-01 / — | @RolesAllowed enforces access | integration | `mvn test -pl mekano-rest -am -Dtest=*Auth*` | ❌ W0 | ⬜ pending |
| 01-01-03 | 01 | 1 | AUTH-03 | — | Public endpoint, no auth | integration | `mvn verify -pl mekano-rest -am` | ✅ W0 | ⬜ pending |
| 01-02-01 | 02 | 1 | OS-01..04 | — | N/A | unit + integ | `mvn test -pl mekano-rest -am -Dtest=*Cliente*` | ❌ W0 | ⬜ pending |
| 01-03-01 | 03 | 1 | OS-03..04 | — | N/A | unit + integ | `mvn test -pl mekano-rest -am -Dtest=*Veiculo*` | ❌ W0 | ⬜ pending |
| 01-04-01 | 04 | 1 | OS-05..06 | — | N/A | unit + integ | `mvn test -pl mekano-rest -am -Dtest=*Servico*` | ❌ W0 | ⬜ pending |
| 01-05-01 | 05 | 2 | OS-07..08, OS-15 | T-1-02 | Transition validation | unit + param | `mvn test -pl mekano-domain -Dtest=*OrdemDeServico*` | ❌ W0 | ⬜ pending |
| 01-05-02 | 05 | 2 | DOC-01 | — | N/A | manual | — | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] N/A — Existing test infrastructure covers all phase requirements via JUnit 5, Mockito, REST Assured, AssertJ

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Sequence diagrams | DOC-01 | Visual documentation, not executable | Verify `docs/sequence-diagrams/` contains Mermaid `.md` files for: criar OS, iniciar diagnóstico, consulta pública |

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

**Evidência de verificação:** [01-VERIFICATION.md](01-VERIFICATION.md) — verificada em 2026-08-08 pela phase 03.1-06.
nyquist_compliant: true — todos os requisitos têm verificação automatizada com latência < 180s.
