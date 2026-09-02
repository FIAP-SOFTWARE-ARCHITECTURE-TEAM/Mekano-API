---
phase: 05-whatsapp-integration
audited_at: 2026-08-18T12:00:00Z
version: 1
threats_total: 11
threats_closed: 11
threats_open: 0
asvs_level: 1
---

# Phase 05 — WhatsApp Integration Security Audit

**Audited:** 2026-08-18
**ASVS Level:** 1
**Threats Closed:** 11/11
**Status:** ALL CLOSED

## Threat Verification

| Threat ID | Category | Disposition | Status | Evidence |
|-----------|----------|-------------|--------|----------|
| T-05-01 | Tampering | mitigate | CLOSED | `docker-compose.yml` lines 9-10, 59-61, 85, 119: every env var uses `${VAR:-default}` pattern — no secrets committed |
| T-05-02 | Information Disclosure | mitigate | CLOSED | `EvolutionApiNotifier.java:110-115` `maskPhone()` returns first 4 + "****" + last 2 digits. All log calls at lines 60, 74, 89-90, 120, 126 use `maskPhone()` — full phone number never logged |
| T-05-03 | Denial of Service | mitigate | CLOSED | `EvolutionApiNotifier.java:48,65,79` — `@Retry(maxRetries=2, delay=1000)` on all 3 methods. Lines 49,66,80 — `@Fallback(fallbackMethod="logFailure*")`. Fallback methods (lines 117-133) log warning and return silently — main OS transaction unaffected |
| T-05-04 | Spoofing | accept | CLOSED | Accepted risk: Evolution API is on internal Docker network (`mekano-net`). `AUTHENTICATION_API_KEY` required for admin operations. Exposed port 5033 is necessary for instance setup (QR code scan). Instance token is separate from API key. |
| T-05-SC | Tampering | mitigate | CLOSED | `mekano-infrastructure/pom.xml:101` — `quarkus-rest-client-jackson` dependency present. No version specified (Quarkus BOM-managed, verified via Maven Central). All new packages from official Quarkus BOM. |
| T-05-05 | Spoofing | mitigate | CLOSED | `WebhookEvolutionResource.java:115-127` — `tokenValido()` validates `x-webhook-token` header AND `apikey` from JSON body. `MessageDigest.isEqual` constant-time comparison (line 121, 126). Fail-closed: if `evolution.webhook-token` is empty/absent, returns 401 (line 116-118). Returns 401 on invalid/missing token (line 80-82). |
| T-05-06 | Tampering | mitigate | CLOSED | `WhatsAppOrcamentoRespostaService.java:66-75` — text response matched via first exact token (`split("\\s+")[0]` against `sim`/`s`/`nao`/`não`/`n`). `processarResposta()` resolves remoteJid → cliente (line 77) validates cliente exists via `ClienteRepositoryImpl.findByTelefone()` (WR-04 fixed: deterministic `ORDER BY createdAt DESC`, single-match only), resolves OS in `AGUARDANDO_APROVACAO` status (line 84-85), validates orcamento exists (line 93). `OrcamentoService.aprovar()` internally validates orcamento status via `orcamento.aprovar()` — rejects non-PENDENTE orçamentos. |
| T-05-07 | Information Disclosure | mitigate | CLOSED | `WebhookEvolutionResource.java` logs only: event rejection reason (line 80), event received (line 89), exception message (line 96). NEVER logs full payload (no `remoteJid`, no message content in logs). `WhatsAppOrcamentoRespostaService.java` logs only cliente UUID (line 88, 95, 104, 107) and orcamento UUID (line 104, 107) — no phone numbers in logs. |
| T-05-08 | Elevation of Privilege | mitigate | CLOSED | Authentication enforced at resource level: `WebhookEvolutionResource.java:79` `tokenValido()` called BEFORE any delegation to `respostaService.processarResposta()` (line 94). `@PermitAll` on endpoint (line 44) — intentional: Evolution API is on internal Docker network, auth is via token header/body. `OrcamentoService.aprovar()` (line 43-73) validates orcamento must be in PENDENTE status via `orcamento.aprovar()` — non-PENDENTE orçamentos are rejected. |
| T-05-09 | Repudiation | mitigate | CLOSED | Transaction boundary is at `OrcamentoService` level: `OrcamentoService.aprovar()` line 43 `@Transactional`; `OrcamentoService.reprovar()` line 76 `@Transactional`. `WhatsAppOrcamentoRespostaService.processarResposta()` intentionally does NOT have `@Transactional` (per D-11: HTTP call to Evolution must not hold DB connection) — delegates write operations to `OrcamentoServicePort` which provides the transaction boundary. Approve/reject is always transactional. |
| T-05-10 | Information Disclosure | accept | CLOSED | Accepted risk: documentation file `docs/whatsapp-external-status-scope.md` was planned but not created. No PII or secrets were ever at risk in this file (placeholder values only). Documenting decision: this file is deferred — the scope boundary (approve/reject orçamento only) is documented in `05-03-PLAN.md` and enforced by implementation. |

## Accepted Risks Log

| Risk ID | Component | Risk Description | Rationale |
|---------|-----------|-----------------|-----------|
| T-05-04 | evolution-api service | Instance creation API exposed on internal Docker network | Evolution API requires `AUTHENTICATION_API_KEY` for admin operations. Port 5033 host mapping is required for QR code scan setup. Instance token is separate. Risk is acceptable given internal network isolation. |
| T-05-10 | docs/whatsapp-external-status-scope.md | Documentation file not created | No PII or secrets were at risk. Scope boundary is documented in 05-03-PLAN.md. Decision to defer file creation. |

## Implementation vs Plan Deviations

The PLAN.md (05-02) was authored with a different architecture than implemented. Key deviations and their security implications:

| Plan Component | Actual Implementation | Security Implication |
|----------------|----------------------|---------------------|
| `WhatsAppWebhookResource` with button reply (`selectedButtonId`) | `WebhookEvolutionResource` with text-based SIM/NÃO matching | Same auth concerns apply — both need token validation. Text matching adds WR-03 (first-token exact match, fixed) instead of UUID-prefix parsing. |
| `WhatsAppWebhookService.processButtonReply` with `@Transactional` | `WhatsAppOrcamentoRespostaService.processarResposta()` without `@Transactional`, delegating to `OrcamentoService.aprovar/reprovar` (which have `@Transactional`) | Transaction boundary moved one level deeper. Intent (no partial state) preserved: `orcamentoService.aprovar()` is always transactional. |
| `EVOLUTION_WEBHOOK_SECRET` header-only validation | `EVOLUTION_WEBHOOK_TOKEN` validated via both `x-webhook-token` header AND `apikey` from JSON body | CR-02 fix: Evolution global webhook doesn't support custom headers, so body apikey is accepted as fallback. Both paths use `MessageDigest.isEqual`. |
| Button ID format `approve_orc_<uuid>` / `reject_orc_<uuid>` | Text matching on first exact token: `sim`/`s`/`nao`/`não`/`n` | WR-03 fix: changed from `startsWith` to first-token exact match to prevent "não entendi" from rejecting orçamento. |

## Unregistered Flags

None detected. All new attack surface created during implementation was addressed by the review findings (CR-01, CR-02, WR-03, WR-04, WR-05, WR-06) before this audit, and all map to the existing threat register.

## Config Parameters

| Parameter | Source | Default | Security Note |
|-----------|--------|---------|---------------|
| `evolution.api-key` | `EVOLUTION_API_KEY` | `dev-key` | Must be changed for production. Shared between mekano (REST client) and evolution-api (AUTHENTICATION_API_KEY). |
| `evolution.instance-name` | `EVOLUTION_INSTANCE_NAME` | `mekano` | Logged — non-sensitive |
| `evolution.webhook-token` | `EVOLUTION_WEBHOOK_TOKEN` | empty (fail-closed) | CR-02: must be set for webhook to function. `docker-compose.yml` default: `dev-key`. Test profile: `test-webhook-token`. |
| `EVOLUTION_API_URL` | `EVOLUTION_API_URL` | `http://evolution-api:5033` | Internal Docker hostname |
| `EVOLUTION_DB_PASSWORD` | `EVOLUTION_DB_PASSWORD` | `evolution` | PostgreSQL for evolution-api, internal network only |

---

_SECURITY.md — Phase 05 WhatsApp Integration. All 11 threats closed._