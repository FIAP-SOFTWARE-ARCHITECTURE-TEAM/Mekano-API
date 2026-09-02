# Phase 5: WhatsApp Integration — Research

**Researched:** 2026-08-08
**Domain:** WhatsApp messaging integration via Evolution API (self-hosted)
**Confidence:** HIGH

## Summary

This phase integrates WhatsApp notifications into Mekano using the **Evolution API** (self-hosted via Docker). Two trigger points fire notifications: (1) `DiagnosticoFinalizadoEvent` → orçamento criado, cliente decide aprovar/recusar (WPP-01); (2) `PagamentoConfirmadoEvent` → OS paga, veículo pronto para retirada (WPP-02). A third integration (API-05) lets the client approve/recuse the orçamento via WhatsApp using interactive buttons, with responses received through a webhook endpoint.

**Primary recommendation:** Create domain port `WhatsAppNotifierPort`, implement `EvolutionApiNotifier` in infrastructure using Quarkus REST Client (`quarkus-rest-client-jackson`). Add two CDI observers in `infrastructure/observer/` that listen to existing events and call the notifier. Add a webhook receiver `POST /api/webhooks/whatsapp` in mekano-rest. Add Evolution API + PostgreSQL + Redis to `docker-compose.yml`.

---

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Evolution API (self-hosted, https://evolution-api.com) como provedor — mais flexível que Cloud API, sem necessidade de templates pré-aprovados
- **D-02:** Evolution API roda em container Docker junto com o Mekano (docker-compose), com referência de implementação em `C:\Users\victo\Desktop\Empresas\Paperclip-Organization\Groom\API`
- **D-03:** Token de instância fixo via environment variable (Evolution não tem expiração de 24h como Cloud API)
- **D-04:** WPP-01: Notificar quando orçamento é criado (após finalizarDiagnostico, status AGUARDANDO_APROVACAO) — cliente precisa decidir. Usar link para endpoints públicos `@PermitAll` (`POST /orcamentos/{uuid}/aprovar` e `reprovar`)
- **D-05:** WPP-02: Notificar sobre retirada quando pagamento for confirmado (`PagamentoConfirmadoEvent`) — OS finalizada + paga = veículo pronto
- **D-06:** Cliente pode interagir via WhatsApp para aprovar/recusar orçamento (não apenas notificação unilateral). Sistema expõe webhook para Evolution API chamar quando cliente responde
- **D-07:** Mensagem de orçamento: texto com resumo do orçamento + link para aprovar + link para recusar (ou interação via WhatsApp pelo webhook)
- **D-08:** Mensagem de retirada: texto informando que veículo está pronto para retirada, com link para consultar status (`GET /os/{uuid}/status`)
- **D-09:** Seguir o padrão existente: domain port (`WhatsAppNotifier`) → infra implementation (`EvolutionApiNotifier`) usando REST Client do Quarkus
- **D-10:** Webhook receiver: novo endpoint REST em mekano-rest (`POST /api/v1/webhooks/whatsapp`) para Evolution API chamar
- **D-11:** Notificações disparam via CDI events (AFTER_SUCCESS) — observer escuta evento existente e chama o notifier
- **D-12:** Cliente pode aprovar/recusar orçamento via WhatsApp (webhook processa resposta e chama endpoint de aprovação interno). WhatsApp não substitui a API pública — é um canal adicional

### the agent's Discretion
- Formato exato das mensagens (texto, emojis, número do pedido)
- Webhook security (token de validação para garantir que chamadas são da Evolution API)
- Estrutura do container Evolution (MongoDB + Evolution API + Mekano no mesmo docker-compose? Ou Evolution externo?)

### Deferred Ideas (OUT OF SCOPE)
- Lembrete automático se cliente não agir no orçamento após X horas — v2.x
- Notificação para múltiplos contatos (cliente + financeiro) — v2.x
- Relatório de entregas via WhatsApp — v2.x
</user_constraints>

---

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| WPP-01 | Enviar notificação via WhatsApp para aprovação/recusa de orçamento | Observer on `DiagnosticoFinalizadoEvent` → resolve OS → cliente → telefone → `sendButtons` with approve/deny buttons. Text fallback with links. |
| WPP-02 | Notificar cliente via WhatsApp quando OS for finalizada (retirada) | Observer on `PagamentoConfirmadoEvent` → resolve OS → cliente → telefone → `sendText` with retirada message. |
| API-05 | Verificar escopo de atualização de status via ferramenta externa (WhatsApp/e-mail) — aplicar somente a aprovar/recusar orçamento? | Webhook receiver handles interactive button response (`selectedRowId` or `buttonReply.id`). Maps to internal approve/reject endpoints. WhatsApp is additional channel, NOT replacing API. |
</phase_requirements>

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Send WhatsApp message | Infrastructure | — | Calls external Evolution API via REST Client. Domain port in domain/, impl in infrastructure/ |
| Receive WhatsApp webhook | REST (mekano-rest) | Application | New `POST /api/v1/webhooks/whatsapp` endpoint. Validates auth, delegates to application service |
| Process incoming message | Application | — | Maps button replies to internal approve/reject actions. Coordinates between webhook payload and internal services |
| Trigger notification on event | Infrastructure (observer) | — | CDI `@Observes` on existing domain events. Pattern: `PecaOrcamentoObserver`, `EstoqueMinimoObserver` |
| Resolve OS → Cliente → Telefone | Infrastructure (repository) | Application | Events carry `osUuid` only. Need repository lookups to get cliente phone number |
| Approve/reject orçamento | REST (mekano-rest) | — | Existing `POST /orcamentos/{uuid}/aprovar` and `reprovar` endpoints with `@PermitAll` |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `quarkus-rest-client-jackson` | 3.36.0 (BOM) | REST Client for calling Evolution API | Quarkus standard REST Client with JSON support — follows D-09 pattern |
| `evoapicloud/evolution-api` | latest | Self-hosted WhatsApp API gateway | Locked decision D-01. Manages WhatsApp connection, message sending, webhooks |
| PostgreSQL 16 | 16-alpine | Evolution API storage | Standard storage for Evolution API (Context7 docs show PostgreSQL + Redis setup) |
| Redis 7 | 7-alpine | Evolution API cache/session | Required by Evolution API for session management |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `com.fasterxml.jackson.datatype:jsr310` | (via BOM) | LocalDateTime serialization for messages | If formatting dates in message text |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Evolution API self-hosted | Meta WhatsApp Cloud API | Cloud API requires Meta Business Account, template approval (hours-days), 24h token refresh. Evolution self-hosted gives full control, no template limits. Chosen per D-01. |
| REST Client (`@RegisterRestClient`) | `HttpClient` (Java 17) | REST Client integrates with Quarkus config, fault tolerance, CDI scopes. Aligns with D-09 port/adapter pattern. |

**Installation:**
```bash
# Maven dependency for mekano-infrastructure pom.xml
# Already managed by Quarkus BOM 3.36.0
# Only need to add:
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-rest-client-jackson</artifactId>
</dependency>
```

**Version verification:** Quarkus 3.36.0 BOM manages `quarkus-rest-client-jackson` version. Verified via Quarkus docs — REST Client is part of the official Quarkus platform BOM. `evoapicloud/evolution-api` latest tag confirmed via Context7 docs.

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| `io.quarkus:quarkus-rest-client-jackson` | Maven Central | ~4 yrs | Very high | github.com/quarkusio/quarkus | [OK] | Approved — official Quarkus extension |
| `evoapicloud/evolution-api` | Docker Hub | ~2 yrs | High | github.com/evolution-foundation/evolution-api | [OK] | Approved — confirmed via Context7 docs |

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

*slopcheck unavailable at research time (pip not found on PATH). All packages above are verified via Context7 documentation and official registries.*

## Architecture Patterns

### System Architecture Diagram

```
                    ┌─────────────────────────────────────────────┐
                    │              Mekano Application              │
                    │                                              │
  ┌──────────┐      │  ┌──────────────┐    ┌───────────────────┐  │
  │ Domain    │      │  │ Application  │    │ Infrastructure    │  │
  │ Event     │──────┼─>│  Layer       │    │ (Observer Layer)  │  │
  │           │      │  │              │    │                   │  │
  │ • Diag-   │      │  │ (no change)  │    │ ┌───────────────┐ │  │
  │   nostico │      │  │              │    │ │ WppNotifObser │ │  │
  │   Finali- │      │  │              │    │ │ @Observes     │─┼──┼──┐
  │   zadoEv  │──────┼──┼──────────────┼───>│ │ DiagFimEv     │ │  │  │
  │           │      │  │              │    │ └───────────────┘ │  │  │
  │ • Pagto-  │      │  │              │    │ ┌───────────────┐ │  │  │
  │   Confir- │──────┼──┼──────────────┼───>│ │ WppNotifObser │ │  │  │
  │   madoEv  │      │  │              │    │ │ @Observes     │─┼──┼──┤
  └──────────┘      │  │              │    │ │ PagtoConfEv   │ │  │  │
                    │  │              │    │ └───────────────┘ │  │  │
                    │  │              │    │                   │  │  │
                    │  │              │    │ ┌───────────────┐ │  │  │
                    │  │              │    │ │ EvolutionApi  │ │  │  │
                    │  │              │    │ │ Notifier      │◄┼──┼──┘
                    │  │              │    │ │ (REST Client) │ │     │
                    │  │              │    │ └───────┬───────┘ │     │
                    │  └──────────────┘    └─────────┼─────────┘     │
                    │                               │               │
                    └───────────────────────────────┼───────────────┘
                                                    │ HTTP POST
                                                    │ apikey header
                    ┌───────────────────────────────┼───────────────┐
                    │                     Docker    │               │
                    │  ┌────────────────────────────┴──────────┐    │
                    │  │        Evolution API Container        │    │
                    │  │  POST /message/sendButtons/{inst}     │    │
                    │  │  POST /message/sendText/{inst}        │    │
                    │  │  POST /webhook/set/{inst}             │    │
                    │  └────────────────┬──────────────────────┘    │
                    │                   │                           │
                    │  ┌────────────────┴──────────────────────┐    │
                    │  │    PostgreSQL (evolution data)         │    │
                    │  └───────────────────────────────────────┘    │
                    │  ┌───────────────────────────────────────┐    │
                    │  │    Redis 7 (sessions/cache)            │    │
                    │  └───────────────────────────────────────┘    │
                    └───────────────────────────────────────────────┘

  ┌───────────────────────┐     ┌──────────────────────────────┐
  │  Evolution API        │     │  Mekano REST (mekano-rest)  │
  │  (incoming msg)       │────>│                              │
  │  POST webhook         │     │  POST /api/v1/webhooks/     │
  │  MESSAGES_UPSERT      │     │  whatsapp                    │
  │                       │     │  (validates apikey header)   │
  └───────────────────────┘     └──────────────┬───────────────┘
                                               │
                                               ▼
                                    ┌──────────────────────┐
                                    │ Process button reply │
                                    │ → orcamento UUID     │
                                    │ → approve/reject     │
                                    └──────────────────────┘
```

### Component Responsibilities

| Component | Module | Responsibility |
|-----------|--------|----------------|
| `WhatsAppNotifierPort` | domain/port/out | Interface: `sendOrcamentoMessage(phone, orcamentoUuid, ...)`, `sendRetiradaMessage(phone, ...)` |
| `EvolutionApiNotifier` | infrastructure | Implements `WhatsAppNotifierPort` using `@RegisterRestClient` to call Evolution API |
| `EvolutionApiRestClient` | infrastructure | REST Client interface (`@Path`, `@POST`, `@HeaderParam("apikey")`) |
| `WhatsAppOrcamentoObserver` | infrastructure/observer | `@Observes DiagnosticoFinalizadoEvent` → resolve OS → cliente → telefone → call notifier |
| `WhatsAppPagamentoObserver` | infrastructure/observer | `@Observes PagamentoConfirmadoEvent` → resolve OS → cliente → telefone → call notifier |
| `WhatsAppWebhookResource` | mekano-rest/api | `POST /api/v1/webhooks/whatsapp` — receives Evolution API callback, validates auth |
| `WhatsAppWebhookService` | application | Processes incoming message — extracts button ID, resolves to orcamento UUID, calls approve/reject |

### Pattern 1: CDI Event Observer (Follow Existing)
**What:** Listen to domain events and trigger side effects (WhatsApp notification) without coupling the domain to external services.
**When to use:** All notification triggers. Follows D-11 exactly.
**Example:**
```java
// Source: mekano-infrastructure observer pattern (verified in PecaOrcamentoObserver.java)
@ApplicationScoped
public class WhatsAppOrcamentoObserver {
    private final OrdemDeServicoRepositoryPort osRepository;
    private final ClienteRepositoryPort clienteRepository;
    private final WhatsAppNotifierPort notifier;

    public WhatsAppOrcamentoObserver(
            OrdemDeServicoRepositoryPort osRepository,
            ClienteRepositoryPort clienteRepository,
            WhatsAppNotifierPort notifier) {
        this.osRepository = osRepository;
        this.clienteRepository = clienteRepository;
        this.notifier = notifier;
    }

    void aoFinalizarDiagnostico(@Observes DiagnosticoFinalizadoEvent event) {
        // 1. Resolve OS → cliente UUID
        // 2. Resolve cliente → Telefone
        // 3. Build message with buttons/links
        // 4. Call notifier.sendOrcamento(...)
        // Note: @Transactional is NOT needed here — HTTP call to Evolution API
        //       should not be inside a DB transaction (prevents pool exhaustion)
    }
}
```

### Pattern 2: Quarkus REST Client for Evolution API
**What:** Interface-based HTTP client with `@RegisterRestClient`. Configuration via `application.properties`.
**When to use:** Calling any Evolution API endpoint from infrastructure.
**Example:**
```java
// Source: Quarkus REST Client docs (Context7) + Mekano port/adapter pattern
@RegisterRestClient(configKey = "evolution-api")
@Path("/message")
public interface EvolutionApiRestClient {
    @POST
    @Path("/sendText/{instanceName}")
    @Produces(MediaType.APPLICATION_JSON)
    SendMessageResponse sendText(
            @PathParam("instanceName") String instanceName,
            @HeaderParam("apikey") String apiKey,
            SendTextRequest request);

    @POST
    @Path("/sendButtons/{instanceName}")
    @Produces(MediaType.APPLICATION_JSON)
    SendMessageResponse sendButtons(
            @PathParam("instanceName") String instanceName,
            @HeaderParam("apikey") String apiKey,
            SendButtonsRequest request);
}
```

**Configuration:**
```properties
# application.properties or api-config.yml
quarkus.rest-client.evolution-api.url=http://evolution-api:8080
quarkus.rest-client.evolution-api.scope=jakarta.inject.Singleton
quarkus.rest-client.evolution-api.connect-timeout=5000
quarkus.rest-client.evolution-api.read-timeout=10000
```

### Pattern 3: Webhook Receiver with Auth Validation
**What:** REST endpoint with API key validation in header. Inspired by Groom reference (`WhatsAppController.cs`).
**When to use:** Receiving callbacks from Evolution API.
**Example:**
```java
// Pattern derived from Groom API WhatsAppController.cs + Mekano resource conventions
@Path("/webhooks/whatsapp")
@RequestScoped
public class WhatsAppWebhookResource {
    @POST
    @PermitAll
    public Response receiveMessage(
            @HeaderParam("apikey") String apiKey,
            WebhookPayload payload) {
        // 1. Validate apikey matches EVOLUTION_WEBHOOK_SECRET
        // 2. Parse payload — extract button ID (selectedRowId)
        // 3. Map to orcamento UUID
        // 4. Call internal approve/reject service
        return Response.ok().build();
    }
}
```

### Anti-Patterns to Avoid
- **Putting `@Transactional` on the observer:** The observer will call Evolution API via HTTP. HTTP inside a DB transaction blocks the connection pool. Follow D-11 pattern — observers that call external services should NOT have `@Transactional`.
- **Hardcoding instance name:** Evolution API supports multiple instances. Use config property for `instanceName`.
- **Storing Evolution API token in code:** Use environment variable `EVOLUTION_API_KEY` via `@ConfigProperty`.
- **Assuming phone number format:** Evolution API expects full international format (`5511999999999`). Telefone VO stores just DDD+number (10-11 digits). Must prepend `55` country code before sending.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| HTTP calls to Evolution API | Raw `HttpURLConnection` / `HttpClient` | Quarkus REST Client (`@RegisterRestClient`) | Automatic JSON serialization, CDI integration, config-based URLs, timeout management, fault tolerance integration |
| WhatsApp connection management | Custom WhatsApp protocol client | Evolution API (self-hosted) | Evolution manages the Baileys WebSocket connection, QR code pairing, reconnection, session persistence |
| Webhook payload parsing | Manual JSON parsing with hacks | Jackson POJO (`@JsonProperty`) | Type-safe, validation-friendly. Quarkus REST already uses Jackson |
| Button reply mapping | Complex regex on message text | Evolution API `sendButtons` with `id` field | Built-in interactive buttons. `id` maps directly to actions (e.g., "approve_orc_X") |

**Key insight:** Evolution API handles the hardest parts of WhatsApp integration — connection management, QR pairing, session persistence, reconnection. Mekano only needs simple HTTP calls.

## Common Pitfalls

### Pitfall 1: Missing Country Code in Phone Number
**What goes wrong:** Evolution API returns error when number is sent without country code. Message never reaches client.
**Why it happens:** Telefone VO stores only DDD + number (e.g., `11999999999`). Evolution API expects `5511999999999`.
**How to avoid:** Always prepend `55` to Telefone's `getValue()` before sending. Document this in the notifier implementation.
**Warning signs:** Evolution API returns 400 or "invalid number" response.

### Pitfall 2: Blocking HTTP Inside Transaction
**What goes wrong:** Observer has `@Transactional` and calls Evolution API → DB connection held during HTTP call → connection pool exhaustion under load.
**Why it happens:** The existing `PecaOrcamentoObserver` uses `@Transactional` because it does DB writes. WhatsApp observer does NOT need DB writes.
**How to avoid:** Do NOT annotate WhatsApp observers with `@Transactional`. If a DB write is needed (e.g., logging), use a separate `@Transactional` method or fire a CDI event.
**Warning signs:** HikariPool timeout warnings, `Connection is not available` errors.

### Pitfall 3: Webhook Processing Outside Transactional Boundaries
**What goes wrong:** Webhook receives button click → processes approve/reject → but no transaction → partial update or data inconsistency if subsequent operation fails.
**Why it happens:** The webhook resource is `@RequestScoped` and `@PermitAll`. The approve/reject logic needs a transaction.
**How to avoid:** The webhook controller delegates to an `@ApplicationScoped` service method that has `@Transactional`. The controller itself remains non-transactional (per D-01: transactions in use case, never in resource).
**Warning signs:** Orçamento approved in memory but not persisted. 200 response returned but DB unchanged.

### Pitfall 4: Evolution API Container Network Configuration
**What goes wrong:** Mekano cannot reach Evolution API (or vice versa for webhook) due to Docker network isolation.
**Why it happens:** Evolution API runs in a separate service. If webhook URL uses `localhost`, it refers to the Evolution container, not the host.
**How to avoid:** Use Docker service name (`evolution-api`) for Mekano → Evolution calls. For webhook URL (Evolution → Mekano), use `http://mekano:8080/api/v1/webhooks/whatsapp` (Docker internal network). All services must be on the same Docker network.
**Warning signs:** Connection timeout, `Connection refused`.

## Code Examples

### Evolution API: Send Button Message (Orçamento Approval)
```json
// Source: Context7 docs for /evolution-foundation/evolution-api — sendButtons endpoint
POST /message/sendButtons/mekano
Content-Type: application/json
apikey: INSTANCE_TOKEN

{
  "number": "5511999999999",
  "title": "Orçamento #OS-123",
  "description": "Olá João, seu orçamento ficou em R$ 1.234,56. Deseja aprovar?",
  "footer": "Oficina Mekano",
  "buttons": [
    { "type": "reply", "displayText": "✅ Aprovar", "id": "approve_orc_abc123" },
    { "type": "reply", "displayText": "❌ Recusar", "id": "reject_orc_abc123" }
  ]
}
```

### Evolution API: Send Text Message (Retirada)
```json
// Source: Context7 docs for /evolution-foundation/evolution-api — sendText endpoint
POST /message/sendText/mekano
Content-Type: application/json
apikey: INSTANCE_TOKEN

{
  "number": "5511999999999",
  "text": "🚗 Olá João! Seu veículo Placa ABC-1234 já está pronto para retirada na Oficina Mekano.\n\n📍 Rua das Oficinas, 100 - Centro\n📅 Seg-Sex: 08h-18h\n\nAcompanhe o status: https://mekano.app/os/abc123/status",
  "linkPreview": true
}
```

### Evolution API: Webhook Payload (Button Reply Received)
```json
// Source: Context7 docs — MESSAGES_UPSERT event with button reply
{
  "event": "messages.upsert",
  "instance": "mekano",
  "data": {
    "key": {
      "remoteJid": "5511999999999@s.whatsapp.net",
      "fromMe": false,
      "id": "BAE5E90A1C2D1234"
    },
    "pushName": "João Silva",
    "message": {
      "buttonsResponseMessage": {
        "selectedButtonId": "approve_orc_abc123",
        "displayText": "✅ Aprovar"
      }
    },
    "messageType": "buttonsResponseMessage",
    "messageTimestamp": 1703001234
  },
  "sender": "5511999999999@s.whatsapp.net",
  "apikey": "INSTANCE_API_KEY"
}
```

### Mekano: Domain Port Interface
```java
// Source: Existing port/adapter pattern in Mekano (EventPublisher.java, UserRepositoryPort.java)
package com.fiap.mekano.domain.port.out;

import java.util.UUID;

public interface WhatsAppNotifierPort {
    /**
     * Envia mensagem com botões de aprovar/recusar orçamento.
     * @param telefone Número do cliente (apenas dígitos, sem máscara)
     * @param nomeCliente Nome para personalizar a mensagem
     * @param orcamentoUuid UUID do orçamento para construir links/IDs
     * @param valorTotal Valor do orçamento para exibir
     */
    void notificarOrcamento(String telefone, String nomeCliente, UUID orcamentoUuid, String valorTotal);

    /**
     * Envia mensagem informando que veículo está pronto para retirada.
     * @param telefone Número do cliente
     * @param nomeCliente Nome para personalizar
     * @param placa Placa do veículo
     * @param osUuid UUID da OS para link de status
     */
    void notificarRetirada(String telefone, String nomeCliente, String placa, UUID osUuid);
}
```

### Mekano: Observer Pattern (Follow Existing)
```java
// Source: PecaOrcamentoObserver.java pattern (verified) — constructor injection + @Observes
@ApplicationScoped
public class WhatsAppOrcamentoObserver {
    // Repository lookups and notifier injected via constructor
    // NO @Transactional — external HTTP call must not block DB connection

    void aoFinalizarDiagnostico(@Observes DiagnosticoFinalizadoEvent event) {
        // 1. osRepository.findByUuid(event.osUuid()) → get clienteUuid
        // 2. clienteRepository.findByUuid(clienteUuid) → get telefone
        // 3. notifier.notificarOrcamento(telefone.getValue(), ...)
    }
}
```

### Quarkus REST Client Interface
```java
// Source: Quarkus REST Client documentation (Context7)
@RegisterRestClient(configKey = "evolution-api")
public interface EvolutionApiRestClient {

    @POST
    @Path("/message/sendText/{instanceName}")
    @Produces(MediaType.APPLICATION_JSON)
    JsonObject sendText(
            @PathParam("instanceName") String instanceName,
            @HeaderParam("apikey") String apiKey,
            JsonObject request);

    @POST
    @Path("/message/sendButtons/{instanceName}")
    @Produces(MediaType.APPLICATION_JSON)
    JsonObject sendButtons(
            @PathParam("instanceName") String instanceName,
            @HeaderParam("apikey") String apiKey,
            JsonObject request);
}
```

```properties
# application.properties
quarkus.rest-client.evolution-api.url=${EVOLUTION_API_URL:http://localhost:8080}
quarkus.rest-client.evolution-api.scope=jakarta.inject.Singleton
quarkus.rest-client.evolution-api.connect-timeout=5000
quarkus.rest-client.evolution-api.read-timeout=15000
```

```yaml
# docker-compose environment variables
EVOLUTION_API_URL=http://evolution-api:8080
EVOLUTION_API_KEY=your_global_api_key
EVOLUTION_INSTANCE_NAME=mekano
EVOLUTION_INSTANCE_TOKEN=mekano_instance_token
```

### Docker Compose: Evolution API + PostgreSQL + Redis
```yaml
# Add to existing docker-compose.yml
services:
  evolution-postgres:
    image: postgres:16-alpine
    container_name: mekano-evolution-postgres
    environment:
      POSTGRES_DB: evolution
      POSTGRES_USER: evolution
      POSTGRES_PASSWORD: ${EVOLUTION_DB_PASSWORD:-evolution}
    volumes:
      - evolution_postgres_data:/var/lib/postgresql/data
    networks:
      - mekano-net
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U evolution -d evolution"]
      interval: 10s
      timeout: 5s
      retries: 5

  evolution-redis:
    image: redis:7-alpine
    container_name: mekano-evolution-redis
    volumes:
      - evolution_redis_data:/data
    networks:
      - mekano-net
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 5

  evolution-api:
    image: evoapicloud/evolution-api:latest
    container_name: mekano-evolution-api
    ports:
      - "8081:8080"
    environment:
      SERVER_URL: http://localhost:8081
      AUTHENTICATION_API_KEY: ${EVOLUTION_API_KEY}
      DATABASE_PROVIDER: postgresql
      DATABASE_CONNECTION_URI: postgresql://evolution:${EVOLUTION_DB_PASSWORD:-evolution}@evolution-postgres:5432/evolution?schema=public
      REDIS_ENABLED: "true"
      CACHE_REDIS_URI: redis://evolution-redis:6379
    depends_on:
      evolution-postgres:
        condition: service_healthy
      evolution-redis:
        condition: service_healthy
    networks:
      - mekano-net

volumes:
  evolution_postgres_data:
  evolution_redis_data:
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Meta WhatsApp Cloud API (Groom ref uses this) | Evolution API self-hosted | Phase 5 decision (D-01) | No template pre-approval needed. No 24h token expiry. Full control over connection. Docker-native. |
| Groom's `@HttpClient` + manual JSON | Quarkus REST Client `@RegisterRestClient` | Mekano architecture | CDI integration, config-based URLs, fault tolerance, cleaner code |

**Deprecated/outdated:**
- **Groom's `SHA256` webhook signature validation:** This is for Meta Cloud API's `X-Hub-Signature-256`. Evolution API uses `apikey` header instead. Do not copy the HMAC validation — use simple API key comparison.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Evolution API uses PostgreSQL + Redis for storage (not MongoDB) | Docker Compose | MongoDB image would need different config. Both are supported by Evolution via Prisma ORM. The Context7 docs show PostgreSQL as standard. If MongoDB is preferred, swap the docker-compose. |
| A2 | `DiagnosticoFinalizadoEvent` is the correct trigger for WPP-01 (orçamento criado) | Events | CONTEXT.md says "após finalizarDiagnostico". If there's a separate OrcamentoCriadoEvent (not found in events directory), that would be more precise. The DiagnosticoFinalizadoEvent carries `osUuid` which can be used to fetch the orçamento. |
| A3 | OS → cliente UUID resolution requires repository lookups | Events | `PagamentoConfirmadoEvent` has `osUuid` only. No cliente info in event. Need to query OS repository to get `clienteUuid`, then query cliente repository to get `telefone`. If events were richer, lookups would be unnecessary. |
| A4 | Evolution API Docker setup uses `evoapicloud/evolution-api:latest` | Docker | Image tag confirmed via Context7 docs. Specific version pinning may be needed for stability. |

## Open Questions (RESOLVED)

1. **How to resolve OS UUID to Cliente Telefone efficiently?**
   - What we know: `PagamentoConfirmadoEvent` has `osUuid`. `DiagnosticoFinalizadoEvent` has `osUuid`.
   - What's unclear: Does the OS entity expose `clienteUuid` directly? Do we need a repository method `findByUuid(UUID)` that returns OS with cliente reference?
   - Recommendation: Check `OrdemDeServico` entity for `clienteUuid` field. Implement `OrdemDeServicoRepositoryPort.findByUuid(UUID)` and `ClienteRepositoryPort.findByUuid(UUID)` if not yet available.

2. **What is the webhook security mechanism for Evolution → Mekano calls?**
   - What we know: Evolution API sends `apikey` in webhook payload. The webhook can also include custom headers.
   - What's unclear: Should we validate against a separate `WEBHOOK_SECRET` env var, or reuse the instance token?
   - Recommendation: Use a separate `EVOLUTION_WEBHOOK_SECRET` env var. Configure it as a custom header in the Evolution webhook config. Mekano validates this header on every webhook POST.

3. **Should WhatsApp notifications be synchronous or async (fire-and-forget)?**
   - What we know: HTTP calls to Evolution API are fast (~100-500ms).
   - What's unclear: If Evolution API is down, should the observer fail (blocking the OS flow) or log and continue?
   - Recommendation: Use `@Retry(2)` + `@Fallback(fallbackMethod = "logFailure")` from SmallRye Fault Tolerance. Log and continue — WhatsApp notification is a side effect, not part of the core domain transaction.

## Environment Availability

> For this phase, dependencies are containerized (Docker). No additional CLI tools required beyond what the project already uses.

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker | Evolution API, PostgreSQL, Redis | ✓ (project already uses) | — | — |
| Maven (`quarkus-rest-client-jackson`) | EvolutionApiRestClient | ✓ (Quarkus BOM manages) | 3.36.0 | — |
| Evolution API image | WhatsApp integration | Not pulled yet | latest | Must add to docker-compose |
| PostgreSQL for Evolution | Evolution API storage | Not configured | 16-alpine | Must add to docker-compose |
| Redis for Evolution | Evolution API sessions | Not configured | 7-alpine | Evolution may work without Redis (degraded) |

**Missing dependencies with no fallback:**
- None — all dependencies are Docker containers that will be added to docker-compose.yml

**Missing dependencies with fallback:**
- Redis — Evolution API may function without Redis (session persistence degraded). Add as optional health check dependency.

## Validation Architecture

> nyquist_validation enabled in config.json. Include this section.

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + REST Assured + QuarkusTest |
| Config file | Inherited from existing mekano-rest setup |
| Quick run command | `./mvnw test -pl mekano-infrastructure -am -Dtest=WhatsApp*` |
| Full suite command | `./mvnw verify -pl mekano-rest -am` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| WPP-01 | Observer sends WhatsApp on DiagnosticoFinalizadoEvent | Integration (Mock Evolution API) | `./mvnw test -pl mekano-infrastructure -am -Dtest=WhatsAppOrcamentoObserverTest` | ❌ Wave 0 |
| WPP-02 | Observer sends WhatsApp on PagamentoConfirmadoEvent | Integration (Mock Evolution API) | `./mvnw test -pl mekano-infrastructure -am -Dtest=WhatsAppPagamentoObserverTest` | ❌ Wave 0 |
| API-05 | Webhook receives button reply → approves orçamento | E2E (REST Assured) | `./mvnw test -pl mekano-rest -am -Dtest=WhatsAppWebhookTest` | ❌ Wave 0 |
| — | EvolutionApiRestClient serializes request correctly | Unit (Mock @RestClient) | `./mvnw test -pl mekano-infrastructure -am -Dtest=EvolutionApiRestClientTest` | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** Quick run command for affected module
- **Per wave merge:** `./mvnw verify -pl mekano-rest -am`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `mekano-infrastructure/src/test/java/.../observer/WhatsAppOrcamentoObserverTest.java` — covers WPP-01 (mock repository + mock notifier)
- [ ] `mekano-infrastructure/src/test/java/.../observer/WhatsAppPagamentoObserverTest.java` — covers WPP-02
- [ ] `mekano-rest/src/test/java/.../api/WhatsAppWebhookTest.java` — covers API-05 (REST Assured with mocked service)
- [ ] Mock services/quarkus-test evolution-api — use `@InjectMock` or WireMock for Evolution API HTTP calls

## Security Domain

> security_enforcement enabled. Include this section.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | No | N/A — webhook is @PermitAll (intentionally public) |
| V3 Session Management | No | N/A — no session in webhook flow |
| V4 Access Control | Yes | Webhook validates apikey header before processing. Orcamento approve/reject checks orcamento ownership by UUID (existing) |
| V5 Input Validation | Yes | Webhook payload parsed via Jackson POJO — validation against schema. API key header validated before processing |
| V6 Cryptography | No | N/A — Evolution API runs on internal Docker network. No sensitive data in transit between containers |
| V8 Data Protection | Yes | WhatsApp messages contain PII (name, phone, vehicle plate). Logging must NOT expose full phone numbers |

### Known Threat Patterns for {Quarkus + Evolution API}

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Unauthenticated webhook (attacker sends fake button clicks) | Spoofing | Validate `apikey` header against `EVOLUTION_WEBHOOK_SECRET`. Reject with 401 if missing or wrong. |
| Phone number leakage in logs | Information Disclosure | Mask phone numbers in log statements: `log.info("Sending WhatsApp to {}", mask(telefone))` |
| Evolution API key leakage in config | Tampering | Use environment variables (not committed files). Existing pattern: `docker-compose.yml` uses `${VAR:-default}` |
| Unauthorized orçamento approval via webhook | Tampering | The button `id` includes orcamento UUID (`approve_orc_<uuid>`). Validate UUID exists and is in AGUARDANDO_APROVACAO status before processing |

## Sources

### Primary (HIGH confidence)
- [/evolution-foundation/evolution-api](https://context7.com/evolution-foundation/evolution-api/llms.txt) — All endpoints: sendText, sendButtons, sendList, webhook set/find, instance create/connect, auth guard
- [Quarkus REST Client docs](https://github.com/quarkusio/quarkus/blob/main/docs/src/main/asciidoc/rest-client.adoc) — @RegisterRestClient, configKey, quarkus.rest-client properties
- [Mekano codebase] — PecaOrcamentoObserver.java, EstoqueMinimoObserver.java (observer pattern), CdiEventPublisher.java (event pattern), Cliente.java (Telefone VO), DiagnosticoFinalizadoEvent.java, PagamentoConfirmadoEvent.java (event structures)

### Secondary (MEDIUM confidence)
- [Groom API WhatsAppController.cs](file://C:/Users/victo/Desktop/Empresas/Paperclip-Organization/Groom/API/Groom.Web/Controllers/WhatsAppController.cs) — Webhook pattern: GET for verification, POST with payload, HMAC signature validation (Meta-specific — not reused for Evolution)
- [Groom API .env.example](file://C:/Users/victo/Desktop/Empresas/Paperclip-Organization/Groom/API/.env.example) — Evolution API env vars pattern: `EVOLUTION_API_BASE_URL`, `EVOLUTION_API_KEY`, `EVOLUTION_GLOBAL_INSTANCE`
- [Groom API WhatsAppService.cs](file://C:/Users/victo/Desktop/Empresas/Paperclip-Organization/Groom/API/Groom.Infrastructure/Services/WhatsAppService.cs) — HttpClient-based WhatsApp integration (Meta Cloud API, not Evolution — architectural reference only)

### Tertiary (LOW confidence)
- Evolution API Docker image tag `evoapicloud/evolution-api:latest` — verified via Context7 docs but not pulled/tested locally. Pin to specific version if `latest` causes issues.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — Evolution API endpoints and Quarkus REST Client verified via Context7/official docs
- Architecture: HIGH — CDI observer pattern verified in codebase. Port/adapter pattern verified.
- Pitfalls: MEDIUM — Phone format and transaction issues are common but untested in this codebase.

**Research date:** 2026-08-08
**Valid until:** 2026-09-08 (30 days — Evolution API is stable, Quarkus 3.x is LTS)