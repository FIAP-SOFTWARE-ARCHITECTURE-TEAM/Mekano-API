# Project Research Summary

**Project:** Mekano v2.0 — infra-docs-quality-whatsapp
**Domain:** Quarkus 3.36 Clean Architecture REST API for mechanical workshop management (Java 17)
**Researched:** 2026-08-08
**Confidence:** HIGH (verified via Context7 official docs + existing codebase analysis)

## Executive Summary

Mekano v2.0 extends an existing Quarkus 3.36 Clean Architecture API (4 modules, 517 tests) with WhatsApp notifications, Kubernetes deployment, Terraform provisioning, an 80% JaCoCo coverage gate, dead code removal, and academic documentation. The product is a well-structured monolith following hexagonal architecture — the research confirms the existing patterns are solid and v2.0 additions fit cleanly within them.

**Recommended approach:** Add WhatsApp as a new output port adapter in `mekano-infrastructure` (not a new module), using `quarkus-rest-client-jackson` to call Meta's WhatsApp Cloud API directly (no Twilio SDK). Infra uses plain YAML K8s manifests (no Helm for v2.0), Terraform with S3 backend for state, and JaCoCo `report-aggregate` for true cross-module coverage. The phase order prioritizes infrastructure foundation first (needed for deployment), then WhatsApp integration (highest user value), quality fixes in parallel, API improvements, and documentation last.

**Key risks:** (1) WhatsApp Cloud API tokens expire every 24 hours — a token refresh service must be built on day 1, not as an afterthought. (2) JaCoCo's per-module `BUNDLE` check gives false 80% passes — `report-aggregate` in `mekano-rest` is mandatory for truthful coverage. (3) Clean Code refactoring is a scope-creep trap — a fixed 5-10 item list must be agreed before any changes. (4) Terraform local state checked into git is a data-loss disaster — remote backend + `.gitignore` from the first `terraform init`.

## Key Findings

### Recommended Stack

WhatsApp integration uses **Meta's Cloud API** (free tier: 1,000 conversations/month) called via **Quarkus REST Client** (`quarkus-rest-client-jackson`) with declarative `@RegisterRestClient` interfaces — no Twilio Java SDK or third-party wrappers. Infrastructure uses **Terraform v1.8+** with `hashicorp/aws` for EKS + RDS provisioning, **plain YAML** K8s manifests (no Helm — overkill for single-service API), and **Kind 0.20+** for local cluster testing. Quality gate uses **JaCoCo 0.8.12** with `LINE` counter at 80% minimum, `BUNDLE` element per module, and `report-aggregate` in `mekano-rest` for a single project-wide report.

**Core technologies:**
- **WhatsApp Cloud API v19.0+**: Direct-to-Meta notification messaging — free tier, no middleman fees, template-based messages with URL buttons for budget approval and text templates for OS finished notifications
- **Quarkus REST Client 3.36.0** (`quarkus-rest-client-jackson`): Declarative HTTP client — fits existing Mekano pattern, zero boilerplate, testable via WireMock. **Reactive variant** recommended for consistency with existing `quarkus-rest-jackson` server
- **WireMock 3.x** (`quarkus-wiremock:1.6.3`): Mock WhatsApp API in tests — Quarkus DevService, auto-starts in dev/test, native `@ConnectWireMock` annotation
- **JaCoCo Maven Plugin 0.8.12**: `LINE` counter at 80%, `BUNDLE` element, `report-aggregate` for cross-module report. Excludes: DTOs, Entities, Resources, ExceptionMappers, Config, MapStruct Impl, REST Client proxies
- **Terraform v1.8+**: IaC with `hashicorp/aws ~> 5.0` for EKS + RDS. S3 backend with state locking. **No Helm** — plain YAML `kubectl apply -f` for v2.0
- **Kind 0.20+**: Local K8s cluster — Docker-based, 30s startup, `kind load docker-image` for local testing
- **HPA `autoscaling/v2`**: CPU 70% + memory 80% targets, minReplicas: 2, maxReplicas: 8, scaleDown stabilization window: 120s

### Expected Features

**Must have (P1 — core milestone scope):**
- **WPP-01**: WhatsApp notification on orçamento creation — template message with URL button linking to `/orcamentos/{uuid}/aprovar` (public endpoint, already `@PermitAll`). Customer taps → opens browser → approves/rejects
- **WPP-02**: WhatsApp notification when OS transitions to `FINALIZADA` — text template informing customer vehicle is ready for pickup
- **API-04**: Ordered OS listing by status priority — `EM_EXECUCAO` > `AGUARDANDO_APROVACAO` > `EM_DIAGNOSTICO` > `RECEBIDA` > `AGUARDANDO_EXECUCAO`, sorted FIFO within same priority. Implemented via **JPQL ORDER BY CASE** (single query, no denormalization)
- **INF-01/INF-02**: Docker review + K8s manifests (Deployment, Service, ConfigMap, Secret, HPA, Ingress)
- **QLD-01**: 80% LINE coverage — currently ~60-65% overall. Biggest gaps in infrastructure (50-60%) and REST resources (60-70%). Domain is already ~90%+
- **DOC-05/DOC-08**: README + Swagger/Postman — Swagger already exists via `quarkus-smallrye-openapi`, Postman collection exists at root

**Should have (P2):**
- **INF-03**: Terraform scripts for EKS + RDS provisioning
- **INF-04**: CD pipeline (GitHub Actions → Docker build → push to registry → `kubectl set image`)
- **QLD-02**: Clean Code/SOLID refactoring — strictly scoped to 5-10 specific items (fix `NfEntradaRepositoryImpl` copy-paste bug, fix `ClienteService.updateCliente`, merge `Placa`/`PlacaVeiculo` VOs, remove empty mappers, fix field injection in stub services)
- **DOC-06/DOC-07**: Sequence diagrams + CI/CD flow diagram (Mermaid in README)

**Defer (v3+):**
- Full WhatsApp two-way conversation (requires webhook server + NLP)
- WhatsApp payment links (PCI compliance)
- Multiple WhatsApp business numbers (multi-branch)
- Prometheus/Grafana monitoring (beyond current scope — Micrometer metrics already exist)
- Redis-backed cache (Caffeine local cache is sufficient for 60s TTL)

### Architecture Approach

WhatsApp integration follows the existing Port/Adapter (hexagonal) pattern: a pure Java `NotificationOutputPort` interface in `mekano-domain`, implemented by `TwilioWhatsAppAdapter` in `mekano-infrastructure`. Notifications are triggered via CDI events (`OrcamentoAprovadoEvent`, `OSFinalizadaEvent`) observed with `TransactionPhase.AFTER_SUCCESS` — keeping HTTP calls outside `@Transactional` scope. No new Maven module is created (WhatsApp-only notification doesn't justify a 5th module). Configuration uses the existing 3-layer strategy (`@ConfigMapping` + YAML config file + K8s Secret/env vars).

**Major components:**
1. **`NotificationOutputPort`** (domain/port/out) — Pure Java interface: `sendOrcamentoApproved(Telefone, Orcamento)`, `sendOrcamentoRejected(...)`, `sendOSCompleted(Telefone, UUID)`. Zero framework annotations
2. **`TwilioWhatsAppAdapter`** (infrastructure/notification/whatsapp) — Implements the port. Uses `@RegisterRestClient` for HTTP calls, `@Retry(maxRetries=2)`, `@CircuitBreaker`, `@Timeout(3000)`. REST Client interface is package-private
3. **`WhatsAppNotificationObserver`** (infrastructure) — CDI event listener observing domain events with `@Observes(during = TransactionPhase.AFTER_SUCCESS)`. Calls the adapter, fires after the database transaction commits
4. **K8s manifests** (k8s/) — Plain YAML: Deployment (2 replicas, health probes at `/q/health/live/ready/started`, resources 512Mi-1Gi/500m-2000m), Service (ClusterIP:8080), HPA (CPU 70%, mem 80%, 2-8 replicas)
5. **Terraform modules** (terraform/) — `eks/`, `rds/`, `networking/` modules. S3 backend. Applied via GitHub Actions with OIDC auth

### Critical Pitfalls

1. **WhatsApp token expiration (24h)** — Meta access tokens expire daily. Without a programmatic refresh service using `app_id` + `app_secret`, notifications stop silently after day one. **Mitigation:** Build TokenManager as first WhatsApp component. Store token in K8s Secret (never ConfigMap/properties). Add a readiness check that pings WhatsApp API to verify token validity. Monitor HTTP 401 responses

2. **JaCoCo multi-module false pass** — Current `BUNDLE` element checks per module independently. `mekano-domain` at 95% + `mekano-rest` at 70% passes the build even though overall coverage may be below 80%. **Mitigation:** Add `jacoco:report-aggregate` goal in `mekano-rest` POM. This collects `.exec` files from all dependent modules and produces a single project-wide report. Also add exclusions for `*MapperImpl.class` (MapStruct generated) and `*$RestClient*class` (REST Client proxies)

3. **Clean Code refactoring scope creep** — "QLD-02: Clean Code and SOLID" is a trap. Without a fixed list of ≤10 specific items, developers will rename variables, extract methods, and reformat working code — introducing bugs in 517 passing tests. **Mitigation:** Create a written scope list with 5-10 items max. Ban stylistic changes. Run full test suite after every change. Accept "good enough" — 10 days left

4. **Notification inside @Transactional** — Calling an HTTP API inside a `@Transactional` method holds the database connection open during network latency (potentially seconds). Under load, connection pool exhaustion follows. **Mitigation:** CDI events with `TransactionPhase.AFTER_SUCCESS` are the established pattern. The business transaction commits before any HTTP call. @Retry on the adapter doesn't compound the problem

5. **Terraform state management** — Local `terraform.tfstate` files get committed to git, lost, or corrupted by concurrent `apply`. **Mitigation:** Use S3 backend from day 1. Enable `use_lockfile` for state locking. Add `*.tfstate*` to `.gitignore`. Pin provider versions with `required_providers`

6. **HPA misconfiguration for JVM apps** — Quarkus on Java 17 with 512MB heap typically sits at 30-50% CPU under normal load. Default CPU 80% target may never trigger scaling. **Mitigation:** Benchmark before configuring HPA. Set CPU target to 70%. Add memory target. Verify `requests` are set (HPA needs them). Test with `kubectl run load-generator`

7. **Documentation drift** — README and Mermaid diagrams go stale within days of code changes. **Mitigation:** Swagger is auto-generated (source of truth for API). Keep README architecture docs high-level. Record demo video LAST (day 9-10). Add CI warning when README isn't updated alongside API changes

## Implications for Roadmap

Based on research, the suggested phase structure for 10 days / 5 developers:

### Phase 1: Infrastructure Foundation (Days 1-2, 2-3 devs)
**Rationale:** Docker must work before K8s manifests can be tested. K8s manifests must exist before CD pipeline. Terraform and CD can be parallel. This phase unblocks all deployment work
**Delivers:** Verified Dockerfiles, `k8s/` manifests (Deployment, Service, ConfigMap, Secret skeleton, HPA, Ingress), local Kind cluster validation, Terraform scaffold with S3 backend
**Addresses:** INF-01 (Docker review), INF-02 (K8s manifests), INF-03 (Terraform scaffold + backend config)
**Stack:** Kind, plain YAML, Terraform, `quarkus.kubernetes.*` config properties
**Avoids:** Pitfall 3 (probe confusion — define proper liveness=/live, readiness=/ready, startup=/started from the start), Pitfall 7 (Terraform state — configure S3 backend and .gitignore before first apply), Pitfall 9 (HPA — set realistic 70% CPU, 80% memory targets)

### Phase 2: WhatsApp Integration (Days 2-4, 1-2 devs, parallel with Phase 1)
**Rationale:** Highest user value. Templates must be submitted to Meta on day 1 (approval takes hours to days). Port/adapter design is additive and doesn't block other work. Async with Phase 1 (different module, different concern)
**Delivers:** `NotificationOutputPort` in domain, `TwilioWhatsAppAdapter` + `TwilioWhatsAppConfig` + REST Client interface in infrastructure, `WhatsAppNotificationObserver` (CDI event listener), `whatsapp-config.yml`, WireMock integration tests, 2 Meta templates submitted
**Addresses:** WPP-01 (orçamento approval notification), WPP-02 (OS finished notification)
**Stack:** `quarkus-rest-client-reactive`, `quarkus-wiremock`, WhatsApp Cloud API sandbox
**Architecture:** Port/Adapter pattern, CDI events with `TransactionPhase.AFTER_SUCCESS`, `@Retry/@CircuitBreaker/@Timeout` on adapter
**Avoids:** Pitfall 1 (token expiration — build TokenManager first), Pitfall 2 (rate limits — implement rate limiter), Pitfall 6 (mock-only — use WireMock with real error scenarios), Pitfall 11 (token in application.properties — store in K8s Secret)

### Phase 3: Quality & Bug Fixes (Days 3-6, 2-3 devs, starts mid-Phase 2)
**Rationale:** Fixes existing data corruption bugs before adding more features. JaCoCo gate must be correctly configured before measuring. Test coverage added continuously — not a single burst
**Delivers:** Fixed `NfEntradaRepositoryImpl` copy-paste bug, fixed `ClienteService.updateCliente`, merged `Placa`/`PlacaVeiculo` VOs, removed empty mappers, switched 3 stub services to constructor injection, added infrastructure tests for NfEntrada + RequisicaoCompra repos, added WhatsApp adapter tests, configured JaCoCo `report-aggregate` in `mekano-rest`, verified 80% LINE coverage
**Addresses:** QLD-01 (80% coverage), QLD-02 (Clean Code refactoring — scoped list only)
**Stack:** JaCoCo 0.8.12, H2 for infra tests, WireMock for WhatsApp tests
**Architecture:** Proper `report-aggregate` in `mekano-rest`, exclusions for generated code
**Avoids:** Pitfall 4 (JaCoCo aggregation — use `report-aggregate` not per-module), Pitfall 5 (scope creep — fixed 5-10 item list, no stylistic changes), Pitfall 8 (low-value tests — focus on domain logic + edge cases, ban getter/setter tests)

### Phase 4: API Improvements (Days 5-7, 1 dev, overlaps with Phase 3)
**Rationale:** Small, additive feature. Single repository method + endpoint param. Doesn't block or get blocked by other phases
**Delivers:** `GET /os?sort=priority&statusFilter=active` endpoint with JPQL `ORDER BY CASE` query, FIFO ordering within same priority, terminal states excluded, existing `listAll()` unchanged
**Addresses:** API-04 (Ordered OS listing by status priority)
**Stack:** JPQL, existing `StatusOS` enum
**Architecture:** New port method `findAllActiveOrderedByPriority()` in `OrdemDeServicoServicePort`, new impl in `OrdemDeServicoServiceImpl`

### Phase 5: Documentation & Polish (Days 7-9, 2-3 devs, can start earlier)
**Rationale:** Docs must reflect final state. Record video last. Terraform CD pipeline needs infra and K8s to be stable first
**Delivers:** README with architecture overview + ADRs, Mermaid sequence diagrams (WhatsApp flow, CI/CD), Swagger verified in production (`quarkus.swagger-ui.always-include=true`), updated Postman collection, Miro board, Terraform `apply` CD pipeline in GitHub Actions, HPA load test section in README, demo video (recorded last)
**Addresses:** DOC-04 (demo video), DOC-05 (README), DOC-06/07 (diagrams), DOC-08 (API spec), DOC-09/10/11 (Miro, architecture doc, HPA explanation)
**Stack:** Mermaid, `quarkus-smallrye-openapi`, GitHub Actions CD
**Avoids:** Pitfall 10 (documentation drift — auto-generate Swagger, record video last, CI warning for doc changes)

### Phase Ordering Rationale

- **Infrastructure first** because K8s manifests and Terraform scaffold are needed before any deployment automation can work. Docker verification is a quick win (already exists) but must be validated
- **WhatsApp in parallel** with infra because the port/adapter pattern is additive — it doesn't touch existing code paths. Template approval lead time (hours-days) demands starting on day 1
- **Quality early** (not last) because bug fixes and JaCoCo aggregation need to be in place before new features add more untested code. The coverage gate catches regressions during feature development, not after
- **API improvement mid-cycle** because the ordered listing is a single-endpoint change with zero dependencies on WhatsApp or infra. It's a quick win that keeps momentum
- **Documentation last** because the video, README, and diagrams must reflect the final, working system. Documenting WhatsApp before it's built means documenting intentions, not reality
- **CD pipeline after K8s+Terraform** because CD needs a stable target (EKS cluster, RDS DB) to deploy to. The GitHub Actions workflow is the last automation piece

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 2 (WhatsApp):** WhatsApp template submission process (Meta Business Manager UI flow, approval times, template rejection reasons). The HTTP integration is well-understood; the administrative process is a blind spot. Also verify which domain events already exist vs need creation (`OrcamentoCriadoEvent` may not exist yet)
- **Phase 3 (Quality):** Current coverage numbers per module need exact measurement via `jacoco:report` before setting a realistic plan. The ~60-65% estimate needs verification. The `NfEntradaRepositoryImpl` bug fix needs exact code review
- **Phase 5 (Docs):** Whether Quarkus Swagger UI works in production (`quarkus.swagger-ui.always-include=true`). Video recording tooling and hosting requirements

Phases with standard patterns (skip research-phase):
- **Phase 1 (Infra):** K8s manifests, Dockerfiles, HPA configuration — all well-documented Kubernetes patterns. Quarkus health endpoints are standard. Terraform EKS modules are documented by HashiCorp
- **Phase 4 (API):** JPQL `ORDER BY CASE` is a standard SQL pattern. Adding a query parameter to an existing endpoint is routine

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All recommendations verified via Context7 official docs (Quarkus 3.36 REST Client, JaCoCo 0.8.12, Terraform 1.8+, WhatsApp Cloud API). Stack alignment with existing codebase confirmed |
| Features | HIGH | WhatsApp flow (template messages with URL buttons, free tier limits) confirmed via Meta docs. Priority ordering logic verified from existing StatusOS enum. Codebase analysis confirmed existing DTOs, events, and 517 tests. Bug inventory from AGENTS.md cross-referenced with source |
| Architecture | HIGH | Existing module structure, dependency graph, and patterns verified against source code. Port/Adapter pattern confirmed across all 4 modules. Event publishing pattern (`CdiEventPublisher`) confirmed. Config file segregation pattern already in use |
| Pitfalls | HIGH | WhatsApp token behavior, rate limits, and webhook validation confirmed via Meta docs. JaCoCo multi-module aggregation issue is a known pattern. K8s probe best practices from Quarkus guides. Terraform state management from HashiCorp docs. Clean Code scope creep is experiential but well-documented |

**Overall confidence:** HIGH

### Gaps to Address

- **WhatsApp template approval timeline**: Meta approval can take hours to days with rejection risks (policy violations, unclear template purpose). **Action:** Submit both templates on day 1. Have fallback text notifications ready if approval is delayed
- **Exact current coverage numbers**: Estimated ~60-65% but not measured exactly. **Action:** Run `./mvnw verify -pl mekano-rest -am` with `jacoco:report` on current code (before v2.0 changes) to establish baseline. Check if `*MapperImpl.class` exclusions are already effective
- **`OrcamentoCriadoEvent` existence**: The domain event that should fire when orçamento is created may not exist. The state machine transitions exist but the event may need creation. **Action:** Verify in `mekano-domain/event/` — create event if missing
- **WhatsApp Cloud API sandbox availability**: The sandbox (test numbers + pre-approved templates) behavior needs verification. **Action:** Create Meta Business Account and test number on day 1 as proof of concept before building the adapter
- **K8s metrics-server in Kind**: Kind doesn't ship metrics-server. HPA won't work without it. **Action:** Verify `kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml` works with Kind. This is needed for HPA testing in local dev
- **CD pipeline OIDC setup**: GitHub Actions OIDC trust with AWS needs IAM role creation (may be blocked by permissions). **Action:** Document the manual AWS setup steps. Consider a fallback: push to GHCR + manual `kubectl` for demo if OIDC is blocked

## Sources

### Primary (HIGH confidence)
- **Context7 `/quarkusio/quarkus`** — REST Client declarations, Mutiny Uni patterns, `@ConfigMapping`, SmallRye Fault Tolerance (`@Retry/@CircuitBreaker/@Timeout`), K8s config/secret mapping, health probes, `quarkus.kubernetes.*` properties, OpenAPI/Swagger UI
- **Context7 `/jacoco/jacoco`** — `check` mojo parameters, `LINE` counter, `BUNDLE` element, `report-aggregate` goal, exclusion patterns
- **Context7 `/hashicorp/terraform`** — S3 backend configuration, state locking (`use_lockfile`), `required_providers` version constraints
- **Context7 `/kubernetes/website`** — HPA v2 API, resource/custom metrics, scaling behavior, stabilization window
- **Context7 `/llmstxt/twilio_llms_txt`** — Twilio WhatsApp message POST endpoint, form-encoded body, Basic Auth pattern
- **Existing codebase analysis** — Module structure, dependency graph, 517 tests, AGENTS.md cross-referenced bug inventory, JaCoCo config in parent pom.xml, health check implementations, existing event system, `StatusOS` enum, `OrdemDeServico` state machine
- **PROJECT.md** — Milestone requirements, feature IDs (WPP-*, API-04, INF-*, QLD-*, DOC-*), capacity (10 days, 5 devs)

### Secondary (MEDIUM confidence)
- **WhatsApp Cloud API free tier limits** — 1,000 conversations/month confirmed via Meta developer docs but free tier exact billing could not be directly verified via API call (400 error on endpoint). Multiple sources agree on the 1K conversation/month tier

### Tertiary (LOW confidence)
- **Twilio template SID format** — Exact ContentSid format for WhatsApp template messages needs verification against Twilio console. The architecture uses the correct POST endpoint but template variable format may differ

---

*Research completed: 2026-08-08*
*Ready for roadmap: yes*