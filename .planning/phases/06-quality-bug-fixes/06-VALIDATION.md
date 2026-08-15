---
phase: 6
slug: quality-bug-fixes
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-08-08
---

# Phase 6: Quality & Bug Fixes — Validation Strategy

## Test Strategy

| Module | Type | Strategy | Tooling |
|--------|------|----------|---------|
| JaCoCo config | Static | Pom.xml diff + verify execution | `./mvnw verify -pl mekano-rest -am` |
| PT-BR→EN rename | Compilation | Java compiler catches all call sites | `./mvnw compile` |
| Dead code removal | Static | Grep for removed classes after deletion | `Select-String` |
| VO unification | Compilation | Java compiler + existing tests | `./mvnw test -pl mekano-domain` |
| FT/Cache additions | Compilation | Java compiler + existing tests | `./mvnw test -pl mekano-infrastructure -am` |

## Sampling Rates

| Area | Rate | Rationale |
|------|------|-----------|
| JaCoCo aggregated report | 100% | Must produce accurate coverage |
| Port rename (PT-BR→EN) | 100% | Compiler-only validation |
| Dead code removal | 100% | Each removal verified by grep |
| FT/Cache annotations | 100% | Low count, each must match pattern |

## Wave 0 Gaps

No gaps identified. All changes are mechanical refactors with compiler-level safety.

## Verification Commands

```powershell
# JaCoCo aggregated report
./mvnw verify -pl mekano-rest -am

# Compile validation after rename
./mvnw compile

# Domain tests
./mvnw test -pl mekano-domain

# Infrastructure tests
./mvnw test -pl mekano-infrastructure -am

# Full suite
./mvnw verify -pl mekano-rest -am
```