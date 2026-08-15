---
phase: 5
slug: whatsapp-integration
status: validated
nyquist_compliant: true
wave_0_complete: true
created: 2026-08-08
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

## Wave 0 Gaps

No gaps identified. This phase introduces new external integration (Evolution API) — all components are testable with mocked HTTP.

## Validation Dependencies

- Docker Engine for compose validation (optional for unit tests)
- No real Evolution API instance needed — REST Client mocked via `@InjectMock`
- WireMock stubs for Evolution API HTTP calls in integration tests

## Verification Commands

```powershell
# Domain tests
./mvnw test -pl mekano-domain

# Infrastructure tests (REST Client + observers)
./mvnw test -pl mekano-infrastructure -am

# REST tests (webhook endpoint)
./mvnw test -pl mekano-rest -am

# Full suite
./mvnw verify -pl mekano-rest -am

# Docker compose validation
docker compose config
```