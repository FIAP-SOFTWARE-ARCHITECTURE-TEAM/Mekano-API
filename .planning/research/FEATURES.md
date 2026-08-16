# Feature Research

**Domain:** API REST para gestão de oficina mecânica (Quarkus/Clean Architecture)
**Researched:** 2026-08-08
**Confidence:** HIGH (WhatsApp Cloud API verified via Meta docs + Quarkus K8s verified via official docs + codebase analysis)

---

## Feature Landscape

### 1. WhatsApp Notifications (WPP-01, WPP-02)

#### How WhatsApp Notification Flow Works

The WhatsApp Cloud API (v23.0) has two messaging modes:

| Mode | Window | Use Case |
|------|--------|----------|
| **Non-template messages** | Within 24h customer service window (user must have messaged business first) | Follow-ups, confirmation within active conversation |
| **Template messages** | Any time (pre-approved by Meta) | **Proactive** notifications: budget approval link, OS finished |

**For Mekano, the flow is:**

1. **Budget approval notification (WPP-01):** When orcamento is created (diagnóstico finalizado → OS transiciona para `AGUARDANDO_APROVACAO`), a **template message** with a call-to-action button is sent to the customer's phone. The button opens the public approval URL (`/orcamentos/{uuid}/aprovar` — already `@PermitAll`).

2. **OS finished notification (WPP-02):** When OS transiciona para `FINALIZADA`, a **template message** is sent informing the customer their vehicle is ready for pickup.

#### Expected Behavior

```
Flow: Diagnóstico Finalizado
1. Mecânico finaliza diagnóstico → OS → AGUARDANDO_APROVACAO
2. Sistema gera orçamento
3. Domain event [OrcamentoCriado event or similar] fired
4. Infrastructure listener (WhatsAppNotifier) catches event
5. Calls WhatsApp Cloud API: POST /v23.0/{phone-number-id}/messages
   {
     "messaging_product": "whatsapp",
     "to": "55XXXXXXXXXXX",
     "type": "template",
     "template": {
       "name": "mekano_orcamento_aprovacao",
       "language": { "code": "pt_BR" },
       "components": [{
         "type": "body",
         "parameters": [
           { "type": "text", "text": "João" },
           { "type": "text", "text": "Fiat Uno | ABC-1234" },
           { "type": "text", "text": "R$ 1.500,00" }
         ]
       }, {
         "type": "button",
         "sub_type": "url",
         "index": 0,
         "parameters": [{ "type": "text", "text": "orcamento-uuid-aqui" }]
       }]
     }
   }
6. Customer taps button → opens browser → lands on `/orcamentos/{uuid}/aprovar`
7. Customer approves/rejects → system processes → OS advances to EM_EXECUCAO or CANCELADA
```

#### Template Management Requirements

- Templates must be pre-approved by Meta (can take hours to days — **plan ahead**)
- Mekano templates needed:
  - `mekano_orcamento_aprovacao` — body with customer name, vehicle, value + URL button to approval page
  - `mekano_os_finalizada` — body with customer name, vehicle + text notification
- Template variables: use `{{1}}`, `{{2}}` placeholders in Meta Business Manager
- **For development/testing:** use WhatsApp test numbers (free, no template approval needed for test numbers)

#### Integration Points in Existing Code

| Event | Hook Point | Status |
|-------|-----------|--------|
| Orçamento criado → notificar aprovação | `OrcamentoService.aprovar()` listener | Needs new event `OrcamentoCriadoEvent` or extend existing |
| OS finalizada → notificar retirada | `OrdemDeServico.finalizar()` → `FINALIZADA` state transition | Domain event already exists via state machine |
| Cliente phone number storage | `Cliente` entity has `telefone` VO | Already exists — verify field is populated |

#### WhatsApp Service Design (Recommended)

| Layer | Component | Notes |
|-------|-----------|-------|
| **domain** | `WhatsAppNotifierPort` | Interface: `sendBudgetApproval(Cliente, Orcamento)`, `sendOsFinished(Cliente, OrdemDeServico)` |
| **infrastructure** | `WhatsAppCloudApiNotifier` | REST client calling `graph.facebook.com/v23.0` |
| **infrastructure** | `WhatsAppConfig` | `@ConfigProperties`: phoneNumberId, apiToken, template names |
| **infrastructure** | `OrcamentoCriadoListener` | CDI event observer → calls notifier |
| **infrastructure** | `OsFinalizadaListener` | CDI event observer → calls notifier |
| **test** | `MockWhatsAppNotifier` | Slf4j logger spy for integration tests |

**Free-tier strategy:** WhatsApp Cloud API has free tier (1,000 conversations/month). For development, use test mode with Meta-provided test numbers — no real cost. Do NOT use paid WhatsApp Business API wrappers; the official Cloud API is free for low volume.

#### Sources

- WhatsApp Cloud API docs (Context7): `/websites/developers_facebook_business-messaging_whatsapp_v4` — **HIGH confidence**
- Template messages require pre-approval: verified via Meta docs **HIGH confidence**
- Interactive button with URL: `type: "template"` with `components[].type: "button"` and `sub_type: "url"` — **MEDIUM confidence** (not directly confirmed in Context7 output, but aligns with Meta standard pattern)

---

### 2. Ordered OS Listing by Status Priority (API-04)

#### Status Priority Logic

Based on PROJECT.md specification and existing `StatusOS` enum:

| Priority | Status | Why This Priority |
|----------|--------|-------------------|
| **P1** | `EM_EXECUCAO` | Vehicle being worked on — highest operational urgency |
| **P2** | `AGUARDANDO_APROVACAO` | Customer hasn't responded — blocking the pipeline |
| **P3** | `EM_DIAGNOSTICO` | Being diagnosed — needs mechanic attention |
| **P4** | `RECEBIDA` | Just arrived — needs triage |
| **P5** | `AGUARDANDO_EXECUCAO` | Approved but not started — lower urgency |
| — | `FINALIZADA`, `ENTREGUE`, `CANCELADA` | Excluded from active listing (terminal states) |

#### Expected Behavior

```
GET /os?sort=priority&statusFilter=active

Response ordering:
1. OS em EM_EXECUCAO (sorted by oldest first within same priority)
2. OS em AGUARDANDO_APROVACAO (sorted by oldest first)
3. OS em EM_DIAGNOSTICO (sorted by oldest first)
4. OS em RECEBIDA (sorted by oldest first)
5. OS em AGUARDANDO_EXECUCAO (sorted by oldest first)

Within same priority: oldest createdAt first (FIFO — workshop fairness)
```

#### Integration Points

- Modify `OrdemDeServicoResource.listAll()` or create new endpoint `GET /os/active`
- Add `findAllActiveOrderedByPriority()` port in `OrdemDeServicoServicePort`
- Implement in `OrdemDeServicoServiceImpl`: JPQL with `ORDER BY CASE WHEN ...` or fetch all and sort in memory
- Paginated response: `OrdemDeServicoPageResponse` already exists

#### Implementation Approaches (Recommended)

| Approach | Pros | Cons | Verdict |
|----------|------|------|---------|
| **JPQL ORDER BY CASE** | Single query, DB sorts | Complex CASE, DB-specific if complex | ✅ **Recommended** — efficient, single round-trip |
| In-memory sort | Simple Java Comparator | Loads all active OS, bad for large datasets | ❌ Not for production |
| Status priority column | Simple query | Denormalization, sync needed | ❌ Too complex for this scope |

#### Sources

- `StatusOS` enum confirmed with 8 states + transition matrix: **HIGH confidence**
- Priority order from PROJECT.md API-04: **HIGH confidence**
- Status lifecycle: RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → AGUARDANDO_EXECUCAO → EM_EXECUCAO → FINALIZADA → (cobrança) → ENTREGUE: **HIGH confidence** (verified from OrdemDeServico.java)

---

### 3. Infrastructure: Docker, K8s, Terraform, HPA (INF-01 to INF-05)

#### Current State

| Component | Status | Action Needed |
|-----------|--------|---------------|
| `docker-compose.yml` | ✅ Exists (postgres + keygen + mekano JVM) | Review, add native profile |
| `docker-compose.prod.yml` | ✅ Exists | Review, align with K8s |
| `Dockerfile.jvm` | ✅ Exists | Verify correctness |
| `Dockerfile.native` | ✅ Exists | Verify correctness |
| `.dockerignore` | ✅ Exists | Review |
| K8s manifests | ❌ Doesn't exist | Create from scratch |
| Terraform | ❌ Doesn't exist | Create from scratch |
| HPA config | ❌ Doesn't exist | Create K8s HPA manifest |
| CD pipeline | ❌ Doesn't exist | Add GitHub Actions deploy stage |

#### Docker Patterns for Quarkus

**Verified from codebase:** `Dockerfile.jvm` uses `registry.access.redhat.com/ubi8/openjdk-17-runtime:1.21` and `Dockerfile.native` uses `docker.io/library/registry.access.redhat.com/ubi9-minimal:9.5`.

**Quarkus-native note (from Context7):** Native executables built via container are 64-bit Linux binaries based on UBI 10 (default builder). **UBI 10 executable will NOT run on UBI 8/9 base images.** The existing Dockerfile.native uses ubi9-minimal — this needs verification against current Quarkus 3.36 builder image.

**Container image options (from Quarkus docs):**
- `quarkus-container-image-jib` — builds image without Docker daemon (best for CI)
- `quarkus-container-image-docker` — requires Docker daemon
- **Recommendation:** Jib for CI pipelines, Docker for local dev

#### K8s Patterns for Quarkus (Standard)

| Resource | Purpose | Key Config |
|----------|---------|------------|
| `Deployment` | App instance(s) | Port 8080, health probes, env from ConfigMap/Secret |
| `Service` | Internal load balancing | ClusterIP, port 8080 |
| `ConfigMap` | Non-sensitive config | DB URL, Quarkus profile, log level |
| `Secret` | Sensitive config | DB password, JWT keys, WhatsApp API token |
| `HPA` | Autoscaling | CPU > 70% or memory > 80% |
| `Ingress` | External access | Path-based routing to Service |

**Health probe pattern (Quarkus standard):**
```yaml
livenessProbe:
  httpGet:
    path: /q/health/live
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 30
readinessProbe:
  httpGet:
    path: /q/health/ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 10
startupProbe:
  httpGet:
    path: /q/health/started
    port: 8080
  initialDelaySeconds: 0
  periodSeconds: 5
  failureThreshold: 30
```

**Quarkus-kubernetes extension** auto-generates manifests from `application.properties` — can be used as starting point but **hand-crafted manifests are more maintainable** for Terraform integration. Use the extension for initial generation, then customize.

**HPA Recommendation:**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: mekano-api
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: mekano-api
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 70
```

#### Terraform Design

| Module | Resources | Inputs |
|--------|-----------|--------|
| `gke_cluster` | GKE cluster, node pool (spot) | project_id, region, node_count |
| `postgres` | Cloud SQL PostgreSQL 16 | db_name, db_user, db_password, tier |
| `mekano_k8s` | K8s manifests (via kubectl_manifest or helm) | image_tag, db_url, secrets |

**Free-tier constraint:** For academic demo, Terraform can target Minikube or kind locally, or GKE free-tier (1 e2-medium node). Avoid Cloud SQL costs by using same-node PostgreSQL pod for demo.

#### CI/CD Pipeline (GitHub Actions)

Current: CI only (build + test + OWASP + coverage check).

Add CD stage:
```yaml
# After verify passes
deploy:
  needs: verify
  if: github.ref == 'refs/heads/main'
  steps:
    - Build container image (Jib or Docker)
    - Push to registry (GHCR or Docker Hub)
    - Update K8s deployment image tag
    - kubectl apply -f k8s/manifests/
```

#### Sources

- Dockerfiles confirmed in `mekano-rest/src/main/docker/`: **HIGH confidence**
- Quarkus native container-build doc (Context7): `/quarkusio/quarkus` — **HIGH confidence**
- Quarkus K8s extension auto-generation (Context7): `/quarkusio/quarkus` — **HIGH confidence**
- Quarkus health endpoints (`/q/health/live/ready/started`) confirmed in mekano-rest dependencies (SmallRye Health): **HIGH confidence**

---

### 4. Quality: 80% Test Coverage + Clean Code/SOLID (QLD-01, QLD-02)

#### Current Test Inventory (517 tests — verified)

| Module | Test Count (approx) | What's Covered |
|--------|-------------------|----------------|
| `mekano-domain` | ~262 | All entities, VOs, events, state machine (8×8 transitions = 64 tests alone) |
| `mekano-application` | ~70 | Service tests (Mockito), auth, NfEntrada, OS, orcamento, client |
| `mekano-infrastructure` | ~50+ | Repository impl tests (H2), mappers, observers, listeners, JWT |
| `mekano-rest` | ~135+ | REST Assured E2E for all existing resources, fault tolerance, observability |

#### JaCoCo Configuration (already set — verified in parent pom.xml)

| Setting | Value |
|---------|-------|
| Counter | `LINE` |
| Target | `0.80` (80%) |
| Element | `BUNDLE` |
| Exclusions | `**/*Dto.class`, `**/*DTO.class`, `**/*Request.class`, `**/*Response.class`, `**/*ExceptionMapper.class`, `**/*Config.class`, `**/*Resource.class`, `**/*Entity.class` |

**⚠ Critical note:** The current exclusion list excludes `**/*Resource.class` (controllers) and `**/*Entity.class` — these are large classes. Removing these exclusions would make 80% much harder. The exclusions are reasonable (test frameworks like REST Assured test resources end-to-end, and entities have domain tests).

#### Realistic 80% Coverage Path

| Module | Current Est. Coverage | Gap | Strategy |
|--------|----------------------|-----|----------|
| `mekano-domain` | ~90%+ | Minimal | Domain model heavily tested (262 tests). Add tests for any untested edge cases. |
| `mekano-application` | ~70% | Medium | Stub services `ClienteService.updateCliente` has known bug (doesn't apply updates). `NfEntradaService` needs more scenarios. |
| `mekano-infrastructure` | ~50-60% | **Largest gap** | Repository impl tests exist but only for User, Veiculo, Peca, Orcamento, OS, OsAuditLog. Missing: NfEntrada, RequisicaoCompra. Missing: WhatsApp notification tests (new). |
| `mekano-rest` | ~60-70% | Medium | Missing: ClienteResourceTest (no controller yet for rest — controller exists actually in ClienteResource.java with DTOs confirmed). Missing: AuthResource tests for login/refresh/logout endpoints. |

**Estimated current overall coverage: ~60-65%** (domain heavy, infra lighter)

**To reach 80%:**

1. **Domain:** Likely already above 80%. No action needed.
2. **Application services:** Add tests for missing service methods. Fix `ClienteService.updateCliente` bug. Add tests for edge cases in NfEntradaService.
3. **Infrastructure repositories:** Add H2-based integration tests for NfEntradaRepositoryImpl, RequisicaoCompraRepositoryImpl.
4. **Infrastructure new code:** WhatsApp notifier tests (mock HTTP calls via WireMock or Quarkus Mockito).
5. **REST resources:** Add tests for ClienteResource (controller already exists). Add AuthResource tests. Add ordered listing endpoint tests.

#### Clean Code / SOLID Refactoring Targets

Known issues from AGENTS.md:

| Issue | Priority | Impact |
|-------|----------|--------|
| PT-BR naming in 3 repos (`salvar`, `buscarPorId`) | LOW | Consistency only |
| Field injection in 3 stub services | MEDIUM | Should use constructor injection |
| Mixed entity style (`@Data` vs `@Getter/@Setter`) | LOW | Consistency only |
| `Placa.java` and `PlacaVeiculo.java` duplicate VOs | MEDIUM | Merge into one |
| `ItemOrcamento` in `model/` not `valueobject/` | LOW | Package organization |
| 3 empty mappers (dead code) | LOW | Remove or implement |
| `NfEntradaRepositoryImpl` copy-paste bug (`pecaId` and `requisicaoCompraId` both set to `nfEntrada.getId()`) | **HIGH** | **Data corruption risk** |
| `ClienteService.updateCliente` not applying updates | **HIGH** | **Functional bug** |

#### Sources

- Test count confirmed via `./mvnw test -pl mekano-domain`: **262 domain tests HIGH confidence**
- Total 517 tests from PROJECT.md: **HIGH confidence** (confirmed across modules)
- JaCoCo config from parent pom.xml: **HIGH confidence**
- Known bugs from AGENTS.md codebase analysis: **HIGH confidence**

---

### 5. Documentation (DOC-04 to DOC-11)

#### What Each Deliverable Requires

| ID | Deliverable | Format | Audience | Effort |
|----|-------------|--------|----------|--------|
| DOC-04 | Demo video (≤15 min) | MP4/screen recording | Professor, evaluators | **HIGH** — needs script, clean environment, narration |
| DOC-05 | README.md | Markdown | Developers, evaluators | MEDIUM — comprehensive description |
| DOC-06 | Sequence diagrams (API flow) | Mermaid in README | Developers | MEDIUM |
| DOC-07 | CI/CD flow diagram | Mermaid in README | Developers, DevOps | LOW |
| DOC-08 | API spec | Swagger UI (built-in via `quarkus-smallrye-openapi`) + Postman collection | Developers, testers | LOW (already exists — `Mekano API v1.0.postman_collection.json`) |
| DOC-09 | Miro board | External link | Team, stakeholders | MEDIUM |
| DOC-10 | Architecture documentation | README or docs/ | Developers | MEDIUM |
| DOC-11 | HPA + load simulation | README section | DevOps, evaluators | MEDIUM |

#### Swagger Status

Already configured: `quarkus-smallrye-openapi` dependency in `mekano-rest/pom.xml`. OpenAPI annotations present on all resources. Swagger UI available at `/q/swagger-ui/` in dev mode.

**Need to verify in production:** `quarkus.swagger-ui.always-include=true` for prod deployment (defaults to dev-only).

#### Sources

- `quarkus-smallrye-openapi` dependency confirmed in mekano-rest pom.xml: **HIGH confidence**
- Postman collection exists at root: `Mekano API v1.0.postman_collection.json`: **HIGH confidence**

---

### Feature Dependencies

```
WhatsApp Notifications (WPP-01, WPP-02)
    ├──requires──> Domain events for orcamento criado + OS finalizada (partially exist)
    ├──requires──> WhatsApp Cloud API account + pre-approved templates
    └──requires──> Cliente.telefone populated (existing VO — verify)

Ordered OS Listing (API-04)
    ├──requires──> StatusOS enum (exists)
    └──requires──> OrdemDeServico.findAllActiveOrderedByPriority() port + impl

K8s Infrastructure (INF-01 to INF-05)
    ├──requires──> Docker image build working (exists)
    ├──requires──> PostgreSQL connection via environment (exists)
    ├──enhances──> HPA (INF-02 — needs Deployment to exist first)
    └──enhances──> Terraform (INF-03 — needs K8s manifest design)

80% Coverage (QLD-01)
    ├──requires──> Fix existing bugs (data corruption in NfEntradaRepo, updateCliente)
    ├──enhances──> New feature tests for WhatsApp + ordered listing
    └──enhances──> Clean Code refactoring removes dead code → fewer untested branches

Documentation (DOC-04 to DOC-11)
    ├──requires──> All features completed before demo video
    └──requires──> Swagger annotations already present on all resources
```

### Dependency Notes

- **OrcamentoCriado domain event may not exist yet.** The existing state machine transitions (`finalizarDiagnostico()` → `AGUARDANDO_APROVACAO`) should fire an event. Verify if `OrcamentoCriadoEvent` exists or need to create it.
- **WhatsApp templates must be created in Meta Business Manager first** — can take hours to days for approval. Register templates on day 1 of the milestone.
- **Ordered listing** is additive — doesn't change existing `listAll()` behavior, just adds new endpoint or query param.

---

## MVP Definition (For v2.0 Milestone)

### Must Ship (P1)

- [ ] **WPP-01**: WhatsApp notification on orçamento approval/refusal — core external integration
- [ ] **WPP-02**: WhatsApp notification when OS is finished — second notification flow
- [ ] **API-04**: Ordered OS listing by status priority — quick win, high visibility
- [ ] **INF-01/INF-02**: Docker review + K8s manifests — required for deployment
- [ ] **QLD-01**: 80% test coverage — quality gate
- [ ] **DOC-05/DOC-08**: README + Swagger/Postman — minimum documentation

### Should Ship (P2)

- [ ] **INF-03**: Terraform scripts — needed for cloud provisioning demo
- [ ] **INF-04**: CD pipeline — needed for deployment automation
- [ ] **QLD-02**: Clean Code/SOLID refactoring — quality improvement
- [ ] **DOC-06/DOC-07**: Sequence + CI/CD diagrams — documentation depth

### Nice to Have (P3)

- [ ] **INF-05**: HPA + load simulation — advanced infra demo
- [ ] **DOC-04**: Demo video — requires all other features done
- [ ] **DOC-09/DOC-10/DOC-11**: Miro, architecture doc, HPA explanation — polish

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority | Phase |
|---------|------------|---------------------|----------|-------|
| WhatsApp orcamento notification (WPP-01) | HIGH | MEDIUM (new HTTP client + template setup) | P1 | Phase 1 |
| WhatsApp OS finished (WPP-02) | HIGH | LOW (reuses same infrastructure) | P1 | Phase 1 |
| Ordered OS listing (API-04) | HIGH | LOW (single repository method + endpoint param) | P1 | Phase 1 |
| Docker review (INF-01) | MEDIUM | LOW | P1 | Phase 1 |
| K8s manifests (INF-02) | HIGH | MEDIUM (3-5 YAML files) | P1 | Phase 2 |
| 80% coverage (QLD-01) | MEDIUM | MEDIUM (fix bugs + add ~150 tests) | P1 | Phase 2 |
| README + docs (DOC-05, DOC-08) | MEDIUM | MEDIUM | P1 | Phase 2 |
| Clean Code refactoring (QLD-02) | MEDIUM | MEDIUM (fix known bugs) | P2 | Phase 2 |
| CD pipeline (INF-04) | MEDIUM | LOW | P2 | Phase 3 |
| Terraform (INF-03) | MEDIUM | MEDIUM | P2 | Phase 3 |
| HPA + load sim (INF-05) | MEDIUM | LOW | P3 | Phase 3 |
| Demo video (DOC-04) | HIGH (academic) | HIGH (editing) | P3 | Phase 3 |
| Miro + architecture doc (DOC-09/10/11) | MEDIUM | LOW | P3 | Phase 3 |

---

## Key Design Decisions

| Decision | Option Chosen | Rationale |
|----------|--------------|-----------|
| WhatsApp provider | **Official Cloud API** (free tier) | No third-party cost, official Meta API, 1,000 free conversations/month. Avoids Twilio/SDK paid tiers. |
| WhatsApp template approach | **URL button template** for orçamento, **text template** for OS finished | URL button allows one-click approval. Text notification sufficient for pickup info. |
| Ordered listing implementation | **JPQL ORDER BY CASE** | Single query, correct ordering, no denormalization needed. |
| Container image strategy | **Jib for CI**, **Docker for local dev** | Jib doesn't require Docker daemon — ideal for GitHub Actions. Docker is simpler for local testing. |
| K8s manifest approach | **Hand-crafted** (not Quarkus auto-generation) | More maintainable, explicit, Terraform-ready. Auto-generation as reference only. |
| Test strategy for WhatsApp | **MockWhatsAppNotifier** in unit tests, **WireMock** for integration | No actual WhatsApp calls in CI. Verify message format and content via mocks. |
| 80% coverage exclusions | **Keep current exclusions** (DTOs, Resources, Entities, Mappers, ExceptionMappers, Config) | These classes are tested end-to-end via REST Assured + H2. Excluding them from JaCoCo check prevents double-counting issues. |

---

## Anti-Features

| Feature | Why Requested | Why Problematic | Alternative |
|---------|---------------|-----------------|-------------|
| Full WhatsApp two-way conversation | "Customer can reply and chat" | Requires 24h session management, webhook server, message parsing, NLP. Massively complex. | One-way notification only. Customer action is via URL click in browser. |
| Real-time OS status via WhatsApp | "Customer asks 'where's my car'" | Would require webhook handling + query processing. Not justified for MVP. | Customer uses existing `/os/{id}/status` public endpoint. Add WhatsApp query in future. |
| WhatsApp payment link | "Customer pays via WhatsApp" | PCI compliance, payment processing in chat. High risk. | Use existing idempotent payment endpoint. Notify via WhatsApp to check app/web. |
| Multiple WhatsApp business numbers | "Each branch has its own number" | Multiple phone number IDs, template management complexity. Not needed for MVP. | Single number. Multi-branch support is v3+ scope. |
| Prometheus/Grafana monitoring | "Production monitoring" | Beyond current scope (PROJECT.md explicit: "além do escopo atual"). | Use Quarkus Micrometer metrics (`/q/metrics`) which already exist. Add monitoring later. |

---

## Sources

- **WhatsApp Cloud API**: Meta official docs via Context7 library `/websites/developers_facebook_business-messaging_whatsapp_v4` — **HIGH confidence**
- **Quarkus K8s deployment**: Context7 `/quarkusio/quarkus` — Kubernetes extension, container-image, native build — **HIGH confidence**
- **Quarkus health probes**: SmallRye Health extension in mekano-rest dependencies — **HIGH confidence**
- **JaCoCo coverage check**: Verified in parent pom.xml — `LINE:COVEREDRATIO:0.80` — **HIGH confidence**
- **Existing test structure**: Full file listing across all 4 modules — **HIGH confidence**
- **StatusOS enum**: Read from `mekano-domain/src/main/java/.../StatusOS.java` — **HIGH confidence**
- **OrcamentoResource endpoints**: `@PermitAll` on aprovar/reprovar — **HIGH confidence**
- **OS lifecycle**: Read from `OrdemDeServico.java` — **HIGH confidence**
- **Known bugs**: AGENTS.md analysis of codebase — **HIGH confidence**
- **PROJECT.md**: All requirements and decisions — **HIGH confidence**

---

*Feature research for: Mekano v2.0 infra-docs-quality-whatsapp milestone*
*Researched: 2026-08-08*