---
phase: 5
slug: whatsapp-integration
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-08-08
updated: 2026-08-18
test_count: 544
failures: 0
---

# Phase 5: WhatsApp Integration — Validation Strategy

## Test Strategy

| Module | Type | Strategy | Tooling |
|--------|------|----------|---------|
| Domain (WhatsAppNotifierPort) | Unit | Interface existence, return types | `./mvnw test -pl mekano-domain` |
| Infra (EvolutionApiNotifier) | Integration | REST Client mock via WireMock/@InjectMock | `./mvnw test -pl mekano-infrastructure -am` |
| Infra (Observers) | Integration | CDI event firing with mocked notifier | `./mvnw test -pl mekano-infrastructure -am` |
| Rest (WebhookResource) | E2E | REST Assured with mocked service | `./mvnw test -pl mekano-rest -am` |
| Docker Compose | Integration | YAML validation + service health | `docker compose config` |

## Sampling Rates

| Area | Rate | Rationale |
|------|------|-----------|
| WhatsAppNotifierPort interface | 100% | Single file, must match contract |
| EvolutionApiRestClient | 100% | Single REST Client interface |
| Observer classes | 100% | Low count, each must be tested |
| Webhook endpoint | 100% | Security-critical (401/200) |
| Docker compose services | 100% | Must validate config |

## Wave 0 Coverage — Post Code Review

| Component | Test File | Module | Tests | Status |
|-----------|-----------|--------|-------|--------|
| WhatsAppNotifierPort | Interface contract | domain | — | COVERED (compile-time) |
| WhatsAppOrcamentoRespostaService | WhatsAppOrcamentoRespostaServiceTest | application | 8 | COVERED |
| EvolutionApiRestClient | EvolutionApiNotifierTest | infrastructure | 7 | COVERED |
| WhatsAppOrcamentoObserver | WhatsAppOrcamentoObserverTest | infrastructure | 3 | COVERED |
| ClienteRepositoryImpl.findByTelefone | ClienteRepositoryImplTest | infrastructure | 3 | COVERED |
| WebhookEvolutionResource | WebhookEvolutionResourceTest | rest | 6 | COVERED |
| docker-compose evolution services | `docker compose config` | infra | — | COVERED |

### CR Fix Regression Tests

| Finding | Test Added | Module | Status |
|---------|-----------|--------|--------|
| CR-01 formatPhone DDD-55 | EvolutionApiNotifierTest 2 new cases | infrastructure | COVERED |
| CR-02 webhook fail-closed | WebhookEvolutionResourceTest 2 401 cases | rest | COVERED |
| WR-03 SIM/NÃO exact token | WhatsAppOrcamentoRespostaServiceTest | application | COVERED |
| WR-04 findByTelefone determinístico | ClienteRepositoryImplTest 3 cases | infrastructure | COVERED |
| WR-05 observer post-commit | WhatsAppOrcamentoObserverTest | infrastructure | COVERED |

## Validation Dependencies

- Docker Engine for compose validation (optional for unit tests)
- No real Evolution API instance needed — REST Client mocked via `@InjectMock`
- WireMock stubs for Evolution API HTTP calls in integration tests

## Verification Commands

```powershell
# Domain tests
./mvnw test -pl mekano-domain

# Application tests (WhatsAppOrcamentoRespostaService)
./mvnw test -pl mekano-application -am

# Infrastructure tests (REST Client + observers + repository)
./mvnw test -pl mekano-infrastructure -am

# REST tests (webhook endpoint + auth)
./mvnw test -pl mekano-rest -am

# Full suite
./mvnw verify -pl mekano-rest -am

# Docker compose validation
docker compose config
```

## Audit Trail

### 2026-08-18 — Post Code Review Audit
| Metric | Count |
|--------|-------|
| Total tests (all modules) | 544 |
| Failures | 0 |
| WPP-01 specific tests | 27 (8 app + 10 infra + 6 rest + 3 repo) |
| Code review findings fixed | 6/6 (CR-01, CR-02, WR-03..WR-06) |
| Nyquist compliant | ✅ |