# Architecture Research: Mekano v2.0 — infra-docs-quality-whatsapp

**Domain:** Quarkus 3.36 Clean Architecture REST API (Java 17)
**Researched:** 2026-08-08
**Confidence:** HIGH (verified against existing source code + Context7 docs)

## Key Decisions (Answering Architecture Questions)

### Q1: Where does WhatsApp integration live?

**Decision: Adapter in `mekano-infrastructure` — no new module.**

Existing pattern puts ALL adapters in `mekano-infrastructure`:
- DB repos (14 `*RepositoryImpl`)
- Security (`BcryptPasswordHasher`)
- Events (`CdiEventPublisher`)
- Cache config

A `mekano-notification` module would only be justified with multiple notification channels (SMS, email, push), complex routing, or independent deployability. For a single WhatsApp channel, it's overkill and breaks the existing one-layer-per-module philosophy.

**Structure:**
```
mekano-domain/port/out/
  ├── NotificationOutputPort.java         ← NEW: pure interface
  │   methods: sendOrcamentoApproved(), sendOrcamentoRejected(), sendOSCompleted()
  ├── TelefoneClienteQueryPort.java       ← OPTIONAL: lookup customer WhatsApp number

mekano-infrastructure/notification/
  ├── whatsapp/
  │   ├── TwilioWhatsAppClient.java       ← NEW: Quarkus REST Client interface
  │   ├── TwilioWhatsAppAdapter.java      ← NEW: implements NotificationOutputPort
  │   ├── TwilioWhatsAppConfig.java       ← NEW: @ConfigMapping for tokens/URLs
  │   └── dto/
  │       ├── TwilioMessageRequest.java   ← NEW: DTO for request body
  │       └── TwilioMessageResponse.java  ← NEW: DTO for response body

mekano-application/service/
  ├── orcamento/OrcamentoService.java     ← MODIFIED: injects NotificationOutputPort
  │                                         calls port after approve/reject business logic
  └── ordemservico/OSService.java         ← MODIFIED: calls port after OS finalized
```

**Why infrastructure and not application?** The domain port is in domain/. The Quarkus REST Client (`io.quarkus.rest.client.reactive.QuarkusRestClientBuilder`) is a framework detail — it belongs in infrastructure per Clean Architecture rules. The application layer just calls the port interface.

### Q2: How to configure WhatsApp API tokens?

**Decision: 3-layer strategy — @ConfigMapping + K8s Secret + env fallback.**

Layer 1 — Type-safe @ConfigMapping in infrastructure (already proven pattern):
```java
@ConfigMapping(prefix = "mekano.whatsapp")
public interface TwilioWhatsAppConfig {
    String accountSid();
    String authToken();
    String fromNumber();
    String apiBaseUrl();  // dev override: simulated endpoint
    
    @WithDefault("https://api.twilio.com/2010-04-01")
    String baseUrl();
}
```

Layer 2 — Config file `whatsapp-config.yml` in mekano-rest (following existing pattern):
```yaml
# whatsapp-config.yml
mekano:
  whatsapp:
    account-sid: ${TWILIO_ACCOUNT_SID:}
    auth-token: ${TWILIO_AUTH_TOKEN:}
    from-number: ${TWILIO_FROM_NUMBER:whatsapp:+14155238886}
    base-url: ${TWILIO_BASE_URL:https://api.twilio.com/2010-04-01}
```

Layer 3 — K8s Secret + env mapping:
```yaml
# k8s/secret-whatsapp.yaml
apiVersion: v1
kind: Secret
metadata:
  name: mekano-whatsapp
type: Opaque
stringData:
  TWILIO_ACCOUNT_SID: "ACxxx"
  TWILIO_AUTH_TOKEN: "xxx"
  TWILIO_FROM_NUMBER: "whatsapp:+14155238886"
```
```yaml
# k8s/deployment.yaml (env section)
env:
  - name: TWILIO_ACCOUNT_SID
    valueFrom:
      secretKeyRef:
        name: mekano-whatsapp
        key: TWILIO_ACCOUNT_SID
```

**Reference URL:** https://github.com/quarkusio/quarkus/blob/main/docs/src/main/asciidoc/kubernetes-config.adoc (verified via Context7)

**Dev profile:** In `%dev`, tokens can be empty → adapter falls back to simulated/mock responses. No real WhatsApp call needed for local development.

### Q3: How to maintain Clean Architecture boundaries with external service calls?

**Decision: Port/Adapter + CDI events for async boundary + @Retry/@CircuitBreaker on adapter.**

The existing architecture already solves this for database (repository ports) and events (EventPublisher port). External HTTP is just another output port.

**Pattern:**
```
Domain port (pure interface)
    ↕ implements
Infrastructure adapter (Quarkus REST Client + FT annotations)
    ↕ HTTP
Twilio WhatsApp API
```

**Key boundary rules:**

1. **Domain port uses ONLY domain types.** `NotificationOutputPort` receives `Telefone` (value object), `String message`, or domain entities like `Orcamento`, `OrdemServico`. No HTTP, no JSON, no Twilio types.

2. **Application layer calls the port, but does NOT manage the HTTP call.** The use case approves the budget, publishes the event, and calls `notificationOutputPort.sendOrcamentoApproved(...)`. If Twilio is down, the use case still succeeds — the business transaction is committed before the notification attempt.

3. **@Transactional on the use case, NOT on the notification.** External calls MUST NOT be inside a database transaction. Pattern:
   ```java
   @Transactional
   public OrcamentoResponse aprovarOrcamento(UUID osUuid) {
       Orcamento orcamento = orcamentoDomainService.aprovar(os);
       repositoryPort.save(orcamento);
       // Notification is best-effort — outside @Transactional scope
       // Or fire an async CDI event
       eventPublisher.publish(new OrcamentoAprovadoEvent(osUuid));
       return mapToResponse(orcamento);
   }
   ```

4. **CDI event observer handles the actual HTTP call** — async decoupling:
   ```java
   // In infrastructure — pure observer, no @Transactional
   public class OrcamentoNotificationObserver {
       @Inject NotificationOutputPort notification;
       
       void onOrcamentoAprovado(@Observes OrcamentoAprovadoEvent event) {
           notification.sendOrcamentoApproved(event.clienteTelefone(), event);
       }
   }
   ```

5. **Fault tolerance on the adapter** (not in application):
   ```java
   @ApplicationScoped
   public class TwilioWhatsAppAdapter implements NotificationOutputPort {
       @Retry(maxRetries = 2, delay = 500,
              retryOn = {TimeoutException.class, WebApplicationException.class},
              abortOn = {IllegalArgumentException.class})
       @CircuitBreaker(requestVolumeThreshold = 5, failureRatio = 0.5, delay = 10000)
       @Timeout(3000)  // 3s max for external call
       public void sendOrcamentoApproved(Telefone to, Orcamento orcamento) {
           // HTTP call via Quarkus REST Client
       }
   }
   ```

6. **Never expose the REST Client interface outside infrastructure.** The Quarkus REST Client `@RegisterRestClient` interface is package-private or scoped to the notification package.

**Reference URL:** https://github.com/quarkusio/quarkus/blob/main/extensions/smallrye-fault-tolerance/deployment/src/main/resources/META-INF/quarkus-skill.md (verified via Context7)

### Q4: Build order for phases — documentation early or late?

**Decision: Documentation is continuous — start outlines early, finalize last.**

For academic deliverables (video, Miro, README), the **documented system must match the final state**. Writing doc first means rewriting it 3 times. But starting doc last means running out of time.

**Optimal phase order for 10 days, 5 devs:**

| Phase | Days | People | Scope | Parallel? |
|-------|------|--------|-------|-----------|
| A — Infra Foundation | 1-2 | 2-3 | Docker review, K8s manifests, Terraform, CI/CD CD update | Yes — K8s + Terraform + CI parallel |
| B — WhatsApp | 2-4 | 1-2 | Port, adapter, config, tests, @Retry/@CircuitBreaker | With Phase A (different concern) |
| C — Quality | 3-6 | 2-3 | Fix bugs (ClienteService, NfEntradaRepo), stub→real services, FT/cache gaps, Clean Code | With Phase B toward end |
| D — API Improvements | 5-7 | 1 | OS priority ordering endpoint, endpoint verification | With Phase C |
| E — Docs & Video | 7-9 | 2-3 | README, sequence diagrams, CI/CD Mermaid, Swagger, Miro, video | With Phase D |

**JaCoCo 80% is a CI GATE, not a phase.** Already configured in root POM. Tests are written continuously with each phase. The gate fails builds under 80% LINE coverage.

**Doc strategy (parallel tracks):**
- Day 1-2: README skeleton with architecture overview, ADRs
- Day 3-6: Update README as WhatsApp/infra stabilize
- Day 7-9: Finalize ALL docs (sequence diagrams, Mermaid CI/CD, Swagger, Miro, video)

**Why not doc-first?** Documenting WhatsApp before it's built means documenting the interface, not the implementation. The README's value is describing what actually runs. Doc-first works for API contracts (Swagger) but not for architecture descriptions and deployment instructions.

---

## System Overview

### Current Architecture (v1.0 — Verified)

```
┌──────────────────────────────────────────────────────────────────────┐
│                         mekano-rest (quarkus)                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌───────────────────┐   │
│  │Resources  │  │ DTOs     │  │DtoMapper │  │ApiExceptionMapper │   │
│  │(8)        │  │(in/out)  │  │(MapStruct│  │RFC 7807          │   │
│  └─────┬─────┘  └──────────┘  └──────────┘  └───────────────────┘   │
│        │ injects ports                                                │
├────────┴──────────────────────────────────────────────────────────────┤
│                     mekano-application (jar)                          │
│  ┌───────────────┐  ┌────────────────┐  ┌────────────────────────┐  │
│  │ UserService   │  │ VeiculoService │  │ ClienteService (bug)   │  │
│  │ ✓ implemented │  │ ✓ implemented  │  │ ServicoService ✓      │  │
│  ├───────────────┤  ├────────────────┤  ├────────────────────────┤  │
│  │ PecaService   │  │ NfEntradaSvc   │  │ RequisicaoCompraSvc    │  │
│  │ ✗ stub        │  │ ✗ stub         │  │ ✗ stub                 │  │
│  └───────┬───────┘  └───────┬────────┘  └────────┬───────────────┘  │
│          │ @Transactional    │                     │                  │
│          │ calls ports       │                     │                  │
├──────────┴──────────────────┴─────────────────────┴──────────────────┤
│                   mekano-infrastructure (jar)                        │
│  ┌──────────────┐  ┌────────────────┐  ┌────────────────────────┐  │
│  │ Repositories │  │ Entity/Domain  │  │ Security (BcryptPW)    │  │
│  │ (14)         │  │ Mappers (7+5)  │  │ Event (CDI)           │  │
│  └──────┬───────┘  └────────────────┘  └────────────────────────┘  │
│         │ Panache + PostgreSQL                                       │
├─────────┴────────────────────────────────────────────────────────────┤
│                        mekano-domain (jar)                           │
│  ┌────────┐  ┌──────────┐  ┌───────┐  ┌────────┐  ┌─────────────┐  │
│  │ Models │  │ Value    │  │ Ports │  │Events  │  │ AppException │  │
│  │ (12)   │  │ Objects  │  │in/out │  │ (5)    │  │ + Messages   │  │
│  │        │  │ (6)      │  │ (22)  │  │        │  │              │  │
│  └────────┘  └──────────┘  └───────┘  └────────┘  └─────────────┘  │
│  ZERO framework deps — pure Java SE + Lombok(provided)              │
└──────────────────────────────────────────────────────────────────────┘
```

### WhatsApp Integration Architecture (v2.0 Addition)

```
┌───────────────────────────────────────────────────────────────────┐
│                        mekano-domain                              │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ NotificationOutputPort (interface)                          │ │
│  │ ├── sendOrcamentoApproved(Telefone, Orcamento)              │ │
│  │ ├── sendOrcamentoRejected(Telefone, Orcamento)              │ │
│  │ ├── sendOSCompleted(Telefone, UUID osUuid)                  │ │
│  │ └── ⚠️ Pure Java — no framework annotations                 │ │
│  └─────────────────────────────────────────────────────────────┘ │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ EventPublisher<T> (existing)                                │ │
│  │ └── publish(OrcamentoAprovadoEvent|OrcamentoRecusadoEvent   │ │
│  │                  |OSFinalizadaEvent)                        │ │
│  └─────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────┘
                              ↕ implements               ↕ observes
┌───────────────────────────────────────────────────────────────────┐
│                     mekano-infrastructure                          │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │ notification.whatsapp                                       │ │
│  │  ┌──────────────────────────┐  ┌────────────────────────┐  │ │
│  │  │ TwilioWhatsAppAdapter    │  │OrcamentoNotification   │  │ │
│  │  │ └→ implements Notification│  │Observer                │  │ │
│  │  │   OutputPort             │  │ └→ @Observes events    │  │ │
│  │  │ └→ @Retry, @CircuitBkr   │  │ └→ calls adapter      │  │ │
│  │  │ └→ @Timeout(3000)       │  │                        │  │ │
│  │  └──────────┬───────────────┘  └────────────────────────┘  │ │
│  │             │ uses                                           │ │
│  │  ┌──────────▼───────────────┐                               │ │
│  │  │ TwilioWhatsAppClient     │ ← Quarkus REST Client         │ │
│  │  │ └→ @RegisterRestClient   │    (HTTP to Twilio API)       │ │
│  │  └──────────────────────────┘                               │ │
│  └─────────────────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────────────────┘
                              ↓ HTTP POST (Basic Auth)
┌───────────────────────────────────────────────────────────────────┐
│                Twilio WhatsApp API                                 │
│  POST /2010-04-01/Accounts/{sid}/Messages.json                   │
│  Content-Type: application/x-www-form-urlencoded                  │
│  Body: To, From, ContentSid, ContentVariables                     │
└───────────────────────────────────────────────────────────────────┘
```

---

## Infrastructure & Deployment Architecture

```
┌──────────────────────────────────────────────────────────────────┐
│                       Kubernetes Cluster                         │
│                                                                  │
│  ┌────────────┐  ┌──────────────┐  ┌─────────────────────────┐  │
│  │ Namespace: │  │ ConfigMap    │  │ Secret                  │  │
│  │ mekano     │  │ mekano-config│  │ mekano-whatsapp         │  │
│  │            │  │ (profile,    │  │ mekano-jwt-keys         │  │
│  │            │  │  logging)    │  │ mekano-db               │  │
│  └────────────┘  └──────────────┘  └─────────────────────────┘  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Deployment: mekano-api (replicas: 2, HPA: 2-10)         │   │
│  │ ┌────────────┐  ┌────────────┐  ┌────────────────────┐  │   │
│  │ │ Container  │  │ Liveness   │  │ Resource:          │  │   │
│  │ │ mekano:1.0 │  │ /q/health/ │  │ 512Mi-1Gi, 500m-  │  │   │
│  │ │            │  │ live       │  │ 2000m CPU         │  │   │
│  │ └────────────┘  └────────────┘  └────────────────────┘  │   │
│  └──────────────────────────────────────────────────────────┘   │
│                      │ Service :8080                             │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Service: mekano-api (ClusterIP :8080)                     │   │
│  └──────────────────────────────────────────────────────────┘   │
│                      │ Ingress                                    │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Ingress: mekano-api.mekano.local → Service :8080          │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ PostgreSQL (managed outside K8s via Terraform, or in-     │   │
│  │ cluster StatefulSet)                                      │   │
│  └──────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│                  Terraform (provisioning layer)                   │
│                                                                  │
│  modules/                                                        │
│  ├── cluster/        ← EKS or GKE cluster definition             │
│  ├── database/       ← PostgreSQL RDS or CloudSQL                │
│  └── networking/     ← VPC, subnets, security groups             │
│                                                                  │
│  environments/                                                   │
│  ├── dev/            ← smaller cluster, dev DB                   │
│  └── prod/           ← HA cluster, multi-AZ DB                   │
└──────────────────────────────────────────────────────────────────┘
```

---

## Component Responsibilities

| Component | Layer | Responsibility | Implementation |
|-----------|-------|----------------|----------------|
| Domain entities | domain/core | Business rules, lifecycles, invariants | POJO, `@Builder(Access.PRIVATE)`, factory methods |
| Value Objects | domain/VO | Immutable typed values, format validation | `@EqualsAndHashCode`, constructor validates |
| Input Ports | domain/port/in | Service use case contracts | Pure Java interfaces |
| Output Ports | domain/port/out | Adapter contracts (DB, notification, events) | Pure Java interfaces |
| Domain Events | domain/event | Business event records | Immutable Java records |
| Use Cases | application/service | Transactional orchestration, domain logic | `@ApplicationScoped`, `@Transactional` |
| JPA Entities | infrastructure/entity | ORM mapping to tables | Extends `PanacheEntityBase`, hybrid ID |
| Repositories | infrastructure/repository | DB access via Port interface | Two-class: PanacheRepo + Impl |
| Mappers | infrastructure/mapper | Entity ↔ Domain mapping | Manual CDI (mostly) |
| Security | infrastructure/security | Password hashing | BcryptPasswordHasher |
| Events | infrastructure/event | CDI event publishing | CdiEventPublisher |
| **WhatsApp** | infrastructure/notification | External HTTP notification | Quarkus REST Client + FT |
| Resources | rest/api | HTTP endpoints, validation, auth | `@RequestScoped`, `@RolesAllowed` |
| DTOs | rest/dto | Request/response mapping | Input: Lombok, Output: records |
| Exception | rest/exception | Error response formatting | RFC 7807 `application/problem+json` |

---

## Recommended Project Structure (v2.0 Additions)

```
mekano/
├── pom.xml                            ← MODIFIED: add mekano-rest-client? No — keep in infra
│
├── mekano-domain/
│   └── src/main/java/.../domain/
│       └── port/out/
│           └── NotificationOutputPort.java   ← NEW
│
├── mekano-application/
│   └── src/main/java/.../application/
│       ├── service/orcamento/
│       │   └── OrcamentoService.java         ← MODIFIED: inject NotificationOutputPort
│       ├── service/ordemservico/
│       │   └── OrdemServicoService.java      ← NEW: OS finalization notification
│       └── (existing services remain)
│
├── mekano-infrastructure/
│   └── src/main/java/.../infrastructure/
│       ├── notification/
│       │   └── whatsapp/
│       │       ├── TwilioWhatsAppConfig.java     ← NEW: @ConfigMapping
│       │       ├── TwilioWhatsAppClient.java     ← NEW: REST Client interface
│       │       ├── TwilioWhatsAppAdapter.java    ← NEW: port implementation
│       │       ├── dto/
│       │       │   ├── TwilioMessageRequest.java ← NEW
│       │       │   └── TwilioMessageResponse.java ← NEW
│       │       └── observer/
│       │           └── WhatsAppNotificationObserver.java ← NEW: CDI event listener
│       └── (existing packages remain)
│
├── mekano-rest/
│   └── src/main/resources/
│       └── whatsapp-config.yml                  ← NEW: config (referenced in app.properties)
│
├── k8s/                                         ← NEW directory
│   ├── deployment.yml
│   ├── service.yml
│   ├── hpa.yml
│   ├── configmap.yml
│   ├── secret-database.yml
│   ├── secret-whatsapp.yml                      ← NEW
│   └── ingress.yml
│
├── terraform/                                   ← NEW directory
│   ├── main.tf
│   ├── variables.tf
│   ├── outputs.tf
│   └── modules/
│       ├── cluster/
│       ├── database/
│       └── networking/
│
└── .github/workflows/
    └── ci.yml                                   ← MODIFIED: add CD + JaCoCo gate
```

---

## Data Flow

### WhatsApp Notification Flow (Approved Budget)

```
1. POST /api/v1/ordens-servico/{uuid}/aprovar-orcamento
    ↓
2. UserResource.aprovarOrcamento(uuid)
    ↓ @RolesAllowed("atendente")
3. OrcamentoService.aprovarOrcamento(uuid)
    ↓ @Transactional
    ├── 3a. Find OS → validate state → approve budget
    ├── 3b. Orcamento.aprovar() (domain logic, timestamp set)
    ├── 3c. repositoryPort.save(orcamento)
    ├── 3d. eventPublisher.publish(new OrcamentoAprovadoEvent(...))
    └── 3e. Return response ← TRANSACTION COMMITTED HERE
    ↓
4. (async in same request or fire-and-forget)
   WhatsAppNotificationObserver.onOrcamentoAprovado(event)
    ↓ @Observes(during = TransactionPhase.AFTER_SUCCESS)
5. TwilioWhatsAppAdapter.sendOrcamentoApproved(telefone, orcamento)
    ↓ @Retry(maxRetries=2) @CircuitBreaker @Timeout(3000)
6. TwilioWhatsAppClient.sendMessage(url, auth, formBody)
    ↓ HTTP POST
7. Twilio API /2010-04-01/Accounts/{sid}/Messages.json
    ↓ 200 OK
8. Return success (logged, but no rollback on failure)
```

### OS Finalized Flow

```
1. PUT /api/v1/ordens-servico/{uuid}/finalizar
    ↓
2. UserResource.finalizarOS(uuid)
    ↓ @RolesAllowed("admin")
3. OrdemServicoService.finalizar(uuid)
    ↓ @Transactional
    ├── 3a. Validate OS state → mark finalized
    ├── 3b. Save OS + calculate charges
    ├── 3c. eventPublisher.publish(new OSFinalizadaEvent(clienteTelefone, osUuid))
    └── 3d. Return response
    ↓
4. WhatsAppNotificationObserver.onOSFinalizada(event)
    ↓ @Observes
5. TwilioWhatsAppAdapter.sendOSCompleted(telefone, osId)
    ↓ HTTP POST with template variables
6. Twilio API
```

---

## Architectural Patterns

### Pattern 1: Port/Adapter (Hexagonal)

**What:** Domain defines pure interfaces (ports). Infrastructure implements them (adapters). Domain has zero knowledge of the implementation technology.

**When to use:** Every external system boundary — always.

**Already in use for:** DB repositories, password hashing, event publishing.
**Now adding:** WhatsApp notification.

**Trade-offs:**
- + Domain stays pure, testable without infrastructure
- + Switch Twilio for another provider by swapping adapter
- - One extra indirection layer per boundary

**Example (new for WhatsApp):**
```java
// mekano-domain/port/out/ — pure Java, no framework
public interface NotificationOutputPort {
    void sendOrcamentoApproved(Telefone to, Orcamento orcamento);
    void sendOrcamentoRejected(Telefone to, Orcamento orcamento, String motivo);
    void sendOSCompleted(Telefone to, UUID osUuid);
}
```

```java
// mekano-infrastructure/notification/whatsapp/ — framework-aware
@ApplicationScoped
public class TwilioWhatsAppAdapter implements NotificationOutputPort {
    private final TwilioWhatsAppClient client;
    private final TwilioWhatsAppConfig config;
    
    @Inject
    public TwilioWhatsAppAdapter(TwilioWhatsAppClient client, TwilioWhatsAppConfig config) {
        this.client = client;
        this.config = config;
    }
    
    @Override
    @Retry(maxRetries = 2, delay = 500, retryOn = WebApplicationException.class)
    @Timeout(3000)
    public void sendOrcamentoApproved(Telefone to, Orcamento orcamento) {
        client.sendMessage(
            config.accountSid(),
            config.fromNumber(),
            "whatsapp:+" + to.getValue(),
            config.approvedTemplateSid(),
            "{\"1\": \"" + orcamento.getValorTotal() + "\"}"
        );
    }
}
```

### Pattern 2: CDI Event-Driven Notification (Async Boundary)

**What:** Business events (`OrcamentoAprovadoEvent`, `OSFinalizadaEvent`) are published synchronously inside `@Transactional`. Observers listen after transaction commit (`TransactionPhase.AFTER_SUCCESS`). External HTTP calls happen outside the transaction.

**When to use:** Every time a use case needs to trigger side effects (notifications, email, webhooks) that should not roll back the main transaction.

**Already in use for:** `ClienteCriadoEvent`, `OrcamentoAprovadoEvent` (existing event infrastructure via `CdiEventPublisher`).

**Trade-offs:**
- + Transaction is never held open by slow HTTP calls
- + If Twilio is down, the business operation still succeeds
- + Same JVM — no message broker needed for this scale
- - If the JVM crashes between commit and observer execution, the notification is lost (acceptable for notifications — the business transaction is safe)
- - Observers run in the same HTTP request thread unless explicitly made async (`@Asynchronous`)

**Example:**
```java
// Use case publishes event
@Transactional
public OrcamentoResponse aprovarOrcamento(UUID osUuid) {
    // ... business logic ...
    eventPublisher.publish(new OrcamentoAprovadoEvent(osUuid, clienteTelefone, orcamento));
    return response;
}

// Observer in infrastructure — fires after transaction commits
@ApplicationScoped
public class WhatsAppNotificationObserver {
    @Inject NotificationOutputPort notification;
    
    void onAprovado(@Observes(during = TransactionPhase.AFTER_SUCCESS) OrcamentoAprovadoEvent event) {
        notification.sendOrcamentoApproved(event.clienteTelefone(), event.orcamento());
    }
}
```

### Pattern 3: Segregated Configuration Files

**What:** Instead of one monolithic `application.properties`, each concern has its own YAML file, all referenced via `quarkus.config.locations`.

**Already in use:** `datasource-config.yml`, `api-config.yml`, `openapi-config.yml`, `logging-config.yml`, `auth-config.yml`, `cache-config.yml`.
**Now adding:** `whatsapp-config.yml`.

**Trade-offs:**
- + Team can work on different config files without merge conflicts
- + Easy to find/review specific config
- + Profile-specific overrides stay in the same file
- - Must remember to add new files to the `quarkus.config.locations` list

### Pattern 4: @ConfigMapping for Type-Safe Config

**What:** `@ConfigMapping(prefix = "mekano.whatsapp")` on an interface provides type-safe, injectable configuration with automatic validation.

**When to use:** Any non-trivial configuration group (more than 2 related properties).

**Trade-offs:**
- + Compile-time validation instead of runtime String lookup
- + Automatically supports `@WithDefault`, `@WithConverter`, nesting
- + Testable — can provide alternate mock implementations
- - One extra file per config group

---

## Anti-Patterns

### Anti-Pattern 1: Notification inside @Transactional

**What people do:** Call an HTTP API inside a `@Transactional` method, then wonder why the database connection pool is exhausted.

```java
// WRONG — HTTP call inside transaction
@Transactional
public void aprovarOrcamento(UUID osUuid) {
    // ... save to DB ...
    httpClient.sendMessage(...);  // Transaction held open during HTTP call
}
```

**Why it's wrong:**
- Database connection held during network latency (potentially seconds)
- If Twilio times out (e.g., 30s), the transaction stays open
- Connection pool exhaustion under load
- @Retry on the HTTP call compounds the problem

**Do this instead:** Publish a CDI event and let the observer handle the HTTP call. Or call the notification port AFTER (outside) the `@Transactional` method returns — but CDI events with `AFTER_SUCCESS` are the cleanest approach in this codebase.

### Anti-Pattern 2: Leaking HTTP Framework into Domain

**What people do:** Import `jakarta.ws.rs.*` or `io.quarkus.rest.client.reactive.*` in the domain module or the port interface.

```java
// WRONG — domain port importing HTTP annotations
public interface NotificationOutputPort {
    void send(@org.jboss.resteasy.reactive.RestQuery String to);  // HTTP in domain!
}
```

**Why it's wrong:** The domain module's constraint is ZERO framework dependencies (verified in source). Any framework import makes the domain untestable without the framework, breaks the dependency rule, and couples business logic to transport concerns.

**Do this instead:** The port uses only domain types (Telefone, String, UUID). The Quarkus REST Client interface lives entirely in infrastructure, never exposed outside.

### Anti-Pattern 3: Synchronous Notification in REST Request Thread

**What people do:** Wait for the WhatsApp HTTP response before returning the HTTP response to the user. The API call takes 2-3 seconds for the user to see "200 OK".

```java
// WRONG — blocking the HTTP response
@POST
public Response aprovarOrcamento(UUID osUuid) {
    useCase.aprovar(osUuid);          // 10ms
    whatsAppAdapter.send(...);        // 2000ms — user waits!
    return Response.ok().build();     // 2010ms total
}
```

**Why it's wrong:** Degrades API response time from ~50ms to ~2500ms. User experience suffers. Under load, HTTP request threads block waiting for Twilio.

**Do this instead:** Fire-and-forget via CDI events (existing pattern). The HTTP response returns immediately after the business transaction commits. The notification happens in the same thread but after the response is committed, or use `@Asynchronous` if available. At minimum, log failures and let an external process retry.

### Anti-Pattern 4: Separate Notification Module Overkill

**What people do:** Create `mekano-notification` as a new Maven module because "WhatsApp is a different concern."

**Why it's wrong for this project:**
- Single channel (WhatsApp only) → no routing logic
- Small surface area (~3 interfaces, ~5 classes, ~200 lines)
- Every new module adds: pom.xml, jandex plugin, CI build time, inter-module dependency management
- The existing `mekano-infrastructure` already contains all adapters (security, events, DB)
- The project already has 4 modules — adding a 5th for 200 lines is premature

**Do this instead:** Keep WhatsApp in `mekano-infrastructure/notification/whatsapp/`. If a second channel (e.g., email, SMS) is added later, extract notification into its own module at that point.

---

## Scaling Considerations

| Concern | Current (1 office, <100 users) | With HPA (target: CPU 70%) |
|---------|-------------------------------|---------------------------|
| API throughput | ~100 req/s (single instance) | Auto-scale 2-10 pods |
| DB connections | Single PostgreSQL, pool=20 | Same DB, higher pool per pod → tune max_connections |
| WhatsApp API | 1 msg/sec (Twilio free tier) | Same adapter, no rate limit logic needed yet |
| Cache | Caffeine local (per pod) | Stale data as pods scale up — acceptable for 60s TTL |
| CI build time | ~3-5 min (4 modules) | Same — JaCoCo + OWASP add ~1-2 min |
| CD to K8s | Not configured | GitHub Actions → Docker build → push to registry → kubectl apply |

### Scaling Priorities

1. **First bottleneck:** DB connection pool under HPA. With 10 pods × pool=20 = 200 connections. PostgreSQL default is 100. **Mitigation:** Set `quarkus.datasource.jdbc.max-size=10` in prod profile, monitor connections.

2. **Second bottleneck:** Local Caffeine cache = cache miss on every pod. **Mitigation:** Acceptable for 60s TTL. Redis-backed cache only if needed.

3. **Third bottleneck:** No rate limiting on Twilio calls. **Mitigation:** Already avoided by design — not critical. Add `@Bulkhead` to adapter if needed.

---

## Integration Points

### External Services

| Service | Integration Pattern | Authentication | Gotchas |
|---------|---------------------|---------------|---------|
| Twilio WhatsApp API | REST Client (QuarkusRestClientBuilder) | Basic Auth (AccountSID:AuthToken) Base64 | POST with form-encoded body, not JSON! |
| PostgreSQL (dev) | Panache + JDBC | user/password via env vars | Already established |
| PostgreSQL (prod) | Same + HikariCP pool | via K8s Secret | Tune pool max-size for HPA scale |
| Docker registry | GitHub Container Registry or Docker Hub | GitHub token | Build in CI, push, deploy |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Resource → Use Case | Direct method call (CDI injection) | `@RequestScoped` resource injects `@ApplicationScoped` service |
| Use Case → Repository Port | Direct method call (CDI injection) | Always through domain port interface |
| Use Case → Notification Port | Direct or via EventPublisher | Use CDI events for async notification |
| Use Case → Domain Entity | Static factory methods | `create()` or `reconstitute()`, never `new` |
| Infrastructure Adapter → External API | Quarkus REST Client | `@RegisterRestClient` or `QuarkusRestClientBuilder` |
| Infrastructure → Domain | Entity↔Domain mappers (manual CDI) | No MapStruct for entity mappers (existing convention) |

### Existing Event Flow (WhatsApp-relevant)

| Event | Publisher | Observer (to add) |
|-------|-----------|-------------------|
| `OrcamentoAprovadoEvent` | OrcamentoService (existing) | WhatsAppNotificationObserver.onAprovado ← NEW |
| `OrcamentoRecusadoEvent` | OrcamentoService (existing) | WhatsAppNotificationObserver.onRecusado ← NEW |
| `OSFinalizadaEvent` | OrdemServicoService (new) | WhatsAppNotificationObserver.onFinalizada ← NEW |

---

## JaCoCo Coverage Gate

Already configured in root `pom.xml` with LINE coverage minimum 0.80 (80%). The current exclusions cover DTOs, entities, resources, exception mappers, and config classes:

```xml
<excludes>
    <exclude>**/*Dto.class</exclude>
    <exclude>**/*DTO.class</exclude>
    <exclude>**/*Request.class</exclude>
    <exclude>**/*Response.class</exclude>
    <exclude>**/*ExceptionMapper.class</exclude>
    <exclude>**/*Config.class</exclude>
    <exclude>**/*Resource.class</exclude>
    <exclude>**/*Entity.class</exclude>
</excludes>
```

**Note:** The `**/*Config.class` exclusion will cover `TwilioWhatsAppConfig` automatically — no change needed.

**Recommendation for v2.0:** Add exclusion for auto-generated REST Client implementations (Quarkus generates them):
```xml
<exclude>**/*_ClientProxy.class</exclude>
<exclude>**/*$RestClient*class</exclude>
```

---

## Dependencies to Add

### mekano-infrastructure

```xml
<!-- Quarkus REST Client — for Twilio HTTP calls -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-client-reactive</artifactId>
</dependency>
<!-- Or, if using the non-reactive variant: -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-client</artifactId>
</dependency>
```

**Note on which variant:** The project uses `quarkus-rest-jackson` (reactive REST server). For consistency, use `quarkus-rest-client-reactive`. Both are RESTEasy Reactive under the hood, same programming model. The `@RegisterRestClient` interface is identical.

### mekano-rest (test scope)

```java
<!-- WireMock for testing WhatsApp HTTP calls -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-wiremock</artifactId>
    <scope>test</scope>
</dependency>
```

**Alternative:** Test the adapter by injecting a mock `TwilioWhatsAppClient` (the REST Client proxy). Since Quarkus generates the client implementation, `@InjectMock` with `@RestClient` works:
```java
@InjectMock
@RestClient
TwilioWhatsAppClient mockClient;
```

---

## Configuration Files to Add

### `whatsapp-config.yml` (in mekano-rest/src/main/resources/)

```yaml
# WhatsApp integration configuration
mekano:
  whatsapp:
    account-sid: ${TWILIO_ACCOUNT_SID:}
    auth-token: ${TWILIO_AUTH_TOKEN:}
    from-number: ${TWILIO_FROM_NUMBER:whatsapp:+14155238886}
    base-url: ${TWILIO_BASE_URL:https://api.twilio.com/2010-04-01}
    approved-template-sid: ${TWILIO_APPROVED_TPL:}
    rejected-template-sid: ${TWILIO_REJECTED_TPL:}
    os-completed-template-sid: ${TWILIO_COMPLETED_TPL:}
```

### Updated `application.properties`

Add to `quarkus.config.locations`:
```properties
quarkus.config.locations=...,whatsapp-config.yml
```

---

## Sources

- **Existing source code:** Verified module structure, dependency graph, patterns, conventions — HIGH confidence
- **Context7 Quarkus REST Client docs:** `/quarkusio/quarkus` — verified programmatic client builder, `@RegisterRestClient`, timeout/config patterns — HIGH confidence
- **Context7 Quarkus FT docs:** `/quarkusio/quarkus` — `@Retry`, `@CircuitBreaker`, `@Timeout` annotations — HIGH confidence
- **Context7 JaCoCo docs:** `/websites/jacoco_jacoco_trunk_doc` — `jacoco:check` goal, rules configuration, ratio limits — HIGH confidence
- **Context7 Twilio API docs:** `/llmstxt/twilio_llms_txt` — WhatsApp message POST endpoint, form-encoded body, Basic Auth — HIGH confidence
- **Context7 Quarkus K8s Config docs:** `/quarkusio/quarkus` — `quarkus.kubernetes.env.secrets`, secret env mapping — HIGH confidence
- **Context7 Quarkus @ConfigMapping docs:** `/quarkusio/quarkus` — type-safe configuration interface pattern — HIGH confidence

---

*Architecture research for: Mekano v2.0 — infra-docs-quality-whatsapp*
*Researched: 2026-08-08*