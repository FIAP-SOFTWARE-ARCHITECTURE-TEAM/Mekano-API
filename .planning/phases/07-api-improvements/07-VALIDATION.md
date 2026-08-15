---
phase: 7
slug: api-improvements
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-08-08
---

# Phase 7: API Improvements — Validation Strategy

## Test Strategy

| Module | Type | Strategy | Tooling |
|--------|------|----------|---------|
| Repository (ordering) | Integration | ORM query assertion | `./mvnw test -pl mekano-infrastructure -am` |
| Documentation | Static | File existence + content | `Test-Path`, `Select-String` |

## Sampling Rates

| Area | Rate | Rationale |
|------|------|-----------|
| ORDER BY CASE implementation | 100% | Single method change, tested |
| API verification doc | 100% | Single doc file |
| Test update | 100% | Must reflect new exclusion behavior |

## Wave 0 Gaps

No gaps identified. Implementation is contained in a single repository method.

## Verification Commands

```powershell
# Infrastructure tests (ordering change)
./mvnw test -pl mekano-infrastructure -am

# Verify doc file exists
Test-Path docs/api/api-endpoint-verification.md

# Full suite
./mvnw verify -pl mekano-rest -am
```