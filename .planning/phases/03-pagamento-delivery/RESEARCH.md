# Phase 3: Pagamento & Delivery — Research

**Researched:** 2026-06-23  
**Domain:** Payment flow, delivery state machine, mock bank integration  
**Confidence:** HIGH (25 locked decisions from discussion, patterns verified in Phase 1-2 codebase)

---

## Summary

Phase 3 implements the final two steps of the OS lifecycle: (1) automatic charge emission when execution finishes, (2) payment confirmation via a simulated bank endpoint, and (3) vehicle delivery after payment is confirmed. The implementation reuses established patterns from Phase 1 (state machine, clean architecture) and Phase 2 (events, audit logs, soft delete). Payment is modeled as **fields on the OS entity** (not a separate aggregate), keeping the aggregate focused. Delivery is a guarded state transition that validates `status_pagamento == CONFIRMADO` before allowing `FINALIZADA → ENTREGUE`.

**Primary recommendation:** Payment transitions follow the OS state machine pattern; delivery adds a pre-flight guard (payment status) in the service layer before invoking `os.entregar()`.

---

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Charge emission (OSFinalizadaEvent → CobrancaEmitidaEvent) | Application/Domain | Infrastructure (event publisher) | Business logic (when to charge, what amount) belongs in domain; infrastructure publishes the event |
| Payment confirmation (mock bank call) | Application | Infrastructure (REST client) | Service orchestrates the call; infrastructure provides the HTTP client |
| Delivery state transition | Application | Domain (state machine) | Service validates payment status; domain performs the transition |
| Payment & delivery audit | Infrastructure | — | Reuse existing `os_audit_log` table; OS transitions are captured automatically |
| Idempotency enforcement | Infrastructure (database) | — | `processed_events` table with unique key `(event_type, aggregate_uuid)` prevents duplicate webhook processing |

---

## 1. Implementation Patterns

### 1.1 Payment State Transitions

Payment integrates into the existing OS state machine without creating a separate payment entity. The OS aggregate gains three payment-related fields:

```sql
status_pagamento VARCHAR(20) DEFAULT 'PENDENTE'  -- PENDENTE | CONFIRMADO
data_pagamento TIMESTAMP
valor_cobrado DECIMAL(10, 2)
```

**State diagram** (simplified):
```
OS: RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE
                                                                                  ↑
                                                                    (requires status_pagamento = CONFIRMADO)

Payment states (independent):
PENDENTE ──(mock bank call)──> CONFIRMADO
```

**Why this design:**
- Avoids mega-aggregate pitfall (D-21-02 analysis: Orcamento as separate AR; Pagamento as fields on OS prevents further explosion)
- Payment is a property of OS finalization, not a domain concept with its own lifecycle
- Simplifies queries: "give me all unpaid orders" = `SELECT * FROM ordens_de_servico WHERE status_pagamento = 'PENDENTE'`

### 1.2 Charge Emission Flow

**Trigger:** `OSFinalizadaEvent` (published by OS service when execution finishes)

**Flow:**
1. Mechanic calls `PATCH /api/v1/os/{osUuid}/finalizar-execucao` (from Phase 2)
2. Service calls `os.finalizarExecucao()` → OS transitions `EM_EXECUCAO → FINALIZADA`
3. Service publishes `OSFinalizadaEvent(osUuid, statusAnterior, motivoCancelamento?)`
4. **Listener (new in Phase 3):** `CobrancaEmitidaListener` observes `OSFinalizadoEvent`
   - Validates: no existing charge (HTTP 409 Conflict if `status_pagamento != null`)
   - Sets: `os.status_pagamento = PENDENTE`, `os.valor_cobrado = os.orcamento.valorTotal`, `os.data_pagamento = now()`
   - Publishes: `CobrancaEmitidaEvent(osUuid, valor_cobrado, dataEmissao)`
5. **Within same transaction** (synchronous CDI observer), all listeners complete before transaction commits

**Code pattern** (Application service):
```java
@ApplicationScoped
public class OrdemDeServico Service {
    @Transactional
    public void finalizarExecucao(UUID osUuid, String observacao) {
        OrdemDeServico os = repository.findById(osUuid)
            .orElseThrow(() -> new AppException(404, "OS not found"));
        
        if (os.isPagamentoPendente() || os.isPagamentoConfirmado()) {
            throw new AppException(409, "Cobrança já existe para esta OS");
        }
        
        os.finalizarExecucao(observacao);  // Transição FINALIZADA
        OrdemDeServico saved = repository.save(os);  // Persist estado FINALIZADA
        
        eventPublisher.publish(new OSFinalizadaEvent(
            saved.getUuid(), 
            saved.getOrcamento().getValorTotal(),
            LocalDateTime.now()
        ));
    }
}
```

**Listeners registered in infrastructure:**

```java
@ApplicationScoped
public class CobrancaEmitidaListener {
    @Inject OrdemDeServicoRepository repository;
    @Inject CdiEventPublisher eventPublisher;
    
    void on(@Observes OSFinalizadaEvent event) {
        // Consulta a OS que transicionou para FINALIZADA
        var os = repository.findById(event.osUuid()).orElseThrow();
        
        // Registra a cobrança
        os.setStatusPagamento(StatusPagamento.PENDENTE);
        os.setValorCobrado(event.valorTotal());
        os.setDataPagamento(LocalDateTime.now());
        
        repository.save(os);  // Persiste cobrança
        
        // Publica que cobrança foi emitida
        eventPublisher.publish(new CobrancaEmitidaEvent(
            os.getUuid(),
            UUID.randomUUID(),  // transacaoId (não referencia real)
            os.getValorCobrado(),
            os.getDataPagamento()
        ));
    }
}
```

### 1.3 Payment Confirmation Flow

**Trigger:** `POST /api/v1/os/{osUuid}/confirmar-pagamento` (new endpoint in REST layer)

**Flow:**
1. Admin calls REST endpoint with OS UUID
2. **MockPaymentResource** (**@PermitAll**, **not authenticated**):
   ```java
   @Path("/os/{osUuid}/confirmar-pagamento")
   @POST
   @PermitAll
   public Response confirmarPagamento(@PathParam("osUuid") UUID osUuid) {
       // Delega para service
       return mockPaymentService.confirmarPagamento(osUuid);
   }
   ```
3. **MockPaymentService** (in infrastructure, with delay simulation):
   ```java
   @ApplicationScoped
   @Timeout(value = 3, unit = ChronoUnit.SECONDS)
   public class MockPaymentService {
       @Transactional
       public void confirmarPagamento(UUID osUuid) {
           // Simula delay do banco (2 segundos)
           Thread.sleep(2000);
           
           OrdemDeServico os = repository.findById(osUuid)
               .orElseThrow(() -> new AppException(404, "OS not found"));
           
           if (!os.isPagamentoPendente()) {
               throw new AppException(409, "Pagamento não está pendente");
           }
           
           // Gera transacaoId fictício
           UUID transacaoId = UUID.randomUUID();
           
           // Atualiza status
           os.setStatusPagamento(StatusPagamento.CONFIRMADO);
           os.setDataPagamento(LocalDateTime.now());
           repository.save(os);
           
           // Publica evento
           eventPublisher.publish(new PagamentoConfirmadoEvent(
               os.getUuid(),
               transacaoId,
               os.getValorCobrado(),
               LocalDateTime.now()
           ));
       }
   }
   ```
4. **Listener (PagamentoConfirmadoListener):** Receives event and updates audit log (via existing mechanism)
5. **Response to client:** `200 OK` with updated payment status OR `503 Service Unavailable` if mock fails

**Idempotency via processed_events table** (D-15):
- Before processing payment confirmation, check: `SELECT * FROM processed_events WHERE event_type = 'PAGAMENTO_CONFIRMADO' AND aggregate_uuid = ?`
- If found, skip processing (webhook was duplicate)
- If not found, process and INSERT into table
- This prevents double-charging if webhook is retried

### 1.4 Delivery Validation & Vehicle Handoff

**Trigger:** `PATCH /api/v1/os/{osUuid}/entregar` (new endpoint in REST layer)

**Flow:**
1. Admin/atendente calls REST endpoint with OS UUID and optional `delivery_notes`
2. **Service layer pre-flight check:**
   ```java
   @ApplicationScoped
   public class EntregaService {
       @Transactional
       public void registrarEntrega(UUID osUuid, String observacaoEntrega) {
           OrdemDeServico os = repository.findById(osUuid)
               .orElseThrow(() -> new AppException(404, "OS not found"));
           
           // GUARD: Pagamento CONFIRMADO obrigatório (D-08)
           if (!os.isPagamentoConfirmado()) {
               throw new AppException(409, 
                   "Não é possível entregar veículo sem pagamento confirmado");
           }
           
           // State machine guard: FINALIZADA → ENTREGUE
           if (!os.canTransitionTo(StatusOS.ENTREGUE)) {
               throw new AppException(400, 
                   "OS não está no estado FINALIZADA");
           }
           
           // Perform transition
           os.entregar(observacaoEntrega);  // Calls `setStatus(ENTREGUE)` + audit
           repository.save(os);
           
           eventPublisher.publish(new OSEntregueEvent(
               os.getUuid(),
               LocalDateTime.now(),
               observacaoEntrega
           ));
       }
   }
   ```

**Domain entity guard** (in domain model):
```java
public class OrdemDeServico {
    public void entregar(String observacao) {
        if (!StatusOS.FINALIZADA.equals(this.status)) {
            throw new DomainException("Não é possível entregar veículo não finalizado");
        }
        this.status = StatusOS.ENTREGUE;
        this.dataEntrega = LocalDateTime.now();
        this.observacaoEntrega = observacao;
    }
}
```

**Why guards are in two places:**
- **Service layer:** Business policy (payment must be confirmed) — service knows about payment domain
- **Domain layer:** State machine invariants (can only transition from FINALIZADA) — domain owns the valid transitions

---

## 2. API Endpoint Design

All endpoints follow established REST conventions: `@RequestScoped`, `@RolesAllowed`, RFC 7807 error handling.

### 2.1 POST /api/v1/os/{osUuid}/confirmar-pagamento

**Permissions:** `@PermitAll` (simulated bank endpoint, any user can call)

**Request body:** (none)

**Response (200 OK):**
```json
{
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "status": "FINALIZADA",
  "statusPagamento": "CONFIRMADO",
  "valorCobrado": 1500.00,
  "dataPagamento": "2026-06-23T14:30:00"
}
```

**Error responses:**
- `404 Not Found`: OS com UUID não encontrada
- `409 Conflict`: 
  - Pagamento não está PENDENTE (já foi confirmado ou nunca foi emitido)
  - Mensagem: `"Pagamento não está pendente ou já foi confirmado"`
- `503 Service Unavailable`: Mock serviço indisponível (simulado com `Thread.sleep` timeout)
  - Mensagem (D-04): `"Pagamento indisponível no momento, tente novamente mais tarde"`

**Implementation:**
```java
@Path("/os")
@RequestScoped
@PermitAll
public class PagamentoResource {
    @Inject OrdemDeServicoService osService;
    @Inject MockPaymentService mockPaymentService;
    
    @PATCH
    @Path("/{osUuid}/confirmar-pagamento")
    public Response confirmarPagamento(@PathParam("osUuid") UUID osUuid) {
        try {
            mockPaymentService.confirmarPagamento(osUuid);
            var os = osService.buscar(osUuid);
            return Response.ok(toDtoResponse(os)).build();
        } catch (AppException e) {
            return Response.status(e.getStatus())
                .entity(ProblemDetail.of(e.getStatus(), e.getMessage(), null))
                .type("application/problem+json")
                .build();
        }
    }
}
```

### 2.2 PATCH /api/v1/os/{osUuid}/entregar

**Permissions:** `@RolesAllowed({"admin", "atendente"})`

**Request body:**
```json
{
  "observacaoEntrega": "Veículo entregue em perfeito estado"
}
```

**Response (200 OK):**
```json
{
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ENTREGUE",
  "statusPagamento": "CONFIRMADO",
  "dataEntrega": "2026-06-23T15:00:00",
  "observacaoEntrega": "Veículo entregue em perfeito estado"
}
```

**Error responses:**
- `404 Not Found`: OS não encontrada
- `409 Conflict`:
  - Pagamento não está CONFIRMADO
  - Mensagem: `"Não é possível entregar veículo sem pagamento confirmado"`
- `400 Bad Request`:
  - OS não está em estado FINALIZADA
  - Mensagem: `"OS deve estar finalizada antes da entrega"`

**Implementation:**
```java
@Path("/os")
@RequestScoped
@Authenticated
public class EntregaResource {
    @Inject EntregaService entregaService;
    
    @PATCH
    @Path("/{osUuid}/entregar")
    @RolesAllowed({"admin", "atendente"})
    public Response registrarEntrega(
        @PathParam("osUuid") UUID osUuid,
        @RequestBody EntregaRequest request
    ) {
        try {
            entregaService.registrarEntrega(osUuid, request.observacaoEntrega());
            var os = osService.buscar(osUuid);
            return Response.ok(toDtoResponse(os)).build();
        } catch (AppException e) {
            return Response.status(e.getStatus())
                .entity(ProblemDetail.of(e.getStatus(), e.getMessage(), null))
                .type("application/problem+json")
                .build();
        }
    }
}
```

### 2.3 GET /api/v1/os/{osUuid}

**Enhanced response** (reuses Phase 2 endpoint, adds payment/delivery fields):

```json
{
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "status": "ENTREGUE",
  "statusPagamento": "CONFIRMADO",
  "valorCobrado": 1500.00,
  "dataPagamento": "2026-06-23T14:30:00",
  "dataEntrega": "2026-06-23T15:00:00",
  "observacaoEntrega": "Veículo entregue em perfeito estado",
  "cliente": { ... },
  "veiculo": { ... },
  "orcamento": { ... },
  "servicos": [ ... ]
}
```

---

## 3. Database & Persistence

### 3.1 OSEntity Schema Changes

Flyway migration **V18** (Phase 3 starts after V17 from Phase 2):

```sql
-- V18__add_payment_fields_to_ordens_de_servico.sql

ALTER TABLE ordens_de_servico ADD COLUMN status_pagamento VARCHAR(20) DEFAULT 'PENDENTE';
ALTER TABLE ordens_de_servico ADD COLUMN data_pagamento TIMESTAMP;
ALTER TABLE ordens_de_servico ADD COLUMN valor_cobrado DECIMAL(10, 2);
ALTER TABLE ordens_de_servico ADD COLUMN observacao_entrega TEXT;
ALTER TABLE ordens_de_servico ADD COLUMN data_entrega TIMESTAMP;

-- Índices para queries de pagamento pendente
CREATE INDEX idx_os_status_pagamento ON ordens_de_servico(status_pagamento);
CREATE INDEX idx_os_data_pagamento ON ordens_de_servico(data_pagamento);
```

### 3.2 Processed Events Table (for Idempotency)

Flyway migration **V19**:

```sql
-- V19__create_processed_events_table.sql

CREATE TABLE processed_events (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    aggregate_uuid UUID NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (event_type, aggregate_uuid)
);

CREATE INDEX idx_processed_events_aggregate ON processed_events(aggregate_uuid);
```

### 3.3 OSEntity JPA Mapping

Extend `OSEntity` (created in Phase 2) with new fields:

```java
@Entity
@Table(name = "ordens_de_servico")
public class OSEntity extends BaseEntity {
    // ... existing fields (uuid, status, cliente, veiculo, etc.)
    
    @Column(name = "status_pagamento", length = 20)
    @Enumerated(EnumType.STRING)
    private StatusPagamento statusPagamento = StatusPagamento.PENDENTE;
    
    @Column(name = "data_pagamento")
    private LocalDateTime dataPagamento;
    
    @Column(name = "valor_cobrado")
    private BigDecimal valorCobrado;
    
    @Column(name = "observacao_entrega")
    private String observacaoEntrega;
    
    @Column(name = "data_entrega")
    private LocalDateTime dataEntrega;
    
    // Getters/setters
    public boolean isPagamentoPendente() {
        return StatusPagamento.PENDENTE.equals(statusPagamento);
    }
    
    public boolean isPagamentoConfirmado() {
        return StatusPagamento.CONFIRMADO.equals(statusPagamento);
    }
}
```

### 3.4 Soft Delete & Audit

No changes needed — Phase 3 reuses existing mechanisms from Phase 2:
- `is_active`, `deleted_at` columns already present
- `os_audit_log` table already captures all OS transitions (via `@PrePersist` / `@PreUpdate`)
- Payment confirmation and delivery transitions are OS state changes, automatically logged

---

## 4. Async & Events

### 4.1 Event Definitions

All events are records (immutable) in the domain layer. Published via `EventPublisher` interface.

**CobrancaEmitidaEvent:**
```java
package com.fiap.mekano.domain.event;

public record CobrancaEmitidaEvent(
    UUID osUuid,
    UUID cobrancaId,           // Unique ID for this charge
    BigDecimal valor,
    LocalDateTime dataEmissao
) {}
```

**PagamentoConfirmadoEvent:**
```java
public record PagamentoConfirmadoEvent(
    UUID osUuid,
    UUID transacaoId,          // Bank transaction ID (simulated)
    BigDecimal valor,
    LocalDateTime dataConfirmacao
) {}
```

**OSEntregueEvent:**
```java
public record OSEntregueEvent(
    UUID osUuid,
    LocalDateTime dataEntrega,
    String observacao
) {}
```

### 4.2 Event Listeners

All listeners are `@ApplicationScoped` CDI observers. They participate in the **same transaction** as the event publisher (synchronous CDI events in Quarkus).

**CobrancaEmitidaListener** (triggered when OSFinalizadaEvent is published):
```java
@ApplicationScoped
public class CobrancaEmitidaListener {
    @Inject OSEntityRepository repository;
    @Inject CdiEventPublisher eventPublisher;
    @Inject ProcessedEventsRepository processedEventsRepository;
    
    void on(@Observes @Priority(1000) OSFinalizadaEvent event) {
        // Retrieve OS that transitioned to FINALIZADA
        OSEntity os = repository.findById(event.osUuid())
            .orElseThrow(() -> new AppException(404, "OS not found"));
        
        // Check if charge already exists (idempotency)
        if (os.getPagamentoPendente() != null) {
            return;  // Already charged, skip
        }
        
        // Register charge (sets status_pagamento = PENDENTE)
        os.setStatusPagamento(StatusPagamento.PENDENTE);
        os.setValorCobrado(event.valorTotal());
        os.setDataPagamento(LocalDateTime.now());
        repository.persist(os);
        
        // Publish charge emitted event
        eventPublisher.publish(new CobrancaEmitidaEvent(
            os.getUuid(),
            UUID.randomUUID(),  // Simulated charge ID
            os.getValorCobrado(),
            os.getDataPagamento()
        ));
    }
}
```

**PagamentoConfirmadoListener** (triggered when PagamentoConfirmadoEvent is published):
```java
@ApplicationScoped
public class PagamentoConfirmadoListener {
    @Inject ProcessedEventsRepository processedEventsRepository;
    @Inject AuditLogRepository auditLogRepository;
    
    void on(@Observes PagamentoConfirmadoEvent event) {
        // Idempotency check
        if (processedEventsRepository.existsFor("PAGAMENTO_CONFIRMADO", event.osUuid())) {
            return;  // Already processed
        }
        
        // Mark as processed
        processedEventsRepository.save(new ProcessedEventEntity(
            "PAGAMENTO_CONFIRMADO",
            event.osUuid(),
            LocalDateTime.now()
        ));
        
        // Log to audit trail (optional, since OS.save() already triggers audit)
        // The OS status change is captured via os_audit_log @PreUpdate trigger
    }
}
```

### 4.3 Event Publishing Flow

**Synchronous CDI events** in Quarkus:
1. Service publishes event via `eventPublisher.publish(event)`
2. All listeners execute **within the same transaction**
3. If any listener throws exception, transaction rolls back (entire OS update reverted)
4. Commit only after all observers complete

**Benefits:**
- Consistency: charge and payment state changes are atomic
- Simplicity: no separate message queue needed for MVP
- Ordering: listeners execute in `@Priority` order (lower number = earlier)

---

## 5. Testing Strategy

### 5.1 Domain Layer (JUnit 5)

**Test payment state transitions** — no framework dependencies:

```java
public class PagamentoStateTransitionsTest {
    
    @Test
    void osComPagamentoEmitido_deveEstarNoEstadoPENDENTE() {
        OSBuilder.anOS()
            .withStatus(StatusOS.FINALIZADA)
            .withStatusPagamento(StatusPagamento.PENDENTE)
            .build();
        
        assertTrue(os.isPagamentoPendente());
    }
    
    @Test
    void osPodeTransicionarDeFINALIZADAParaENTREGUE_somenteSePagamentoConfirmado() {
        // Given: OS FINALIZADA com pagamento CONFIRMADO
        var os = OSBuilder.anOS()
            .withStatus(StatusOS.FINALIZADA)
            .withStatusPagamento(StatusPagamento.CONFIRMADO)
            .build();
        
        // When: entregar
        os.entregar("Entregue");
        
        // Then: transition succeeds
        assertEquals(StatusOS.ENTREGUE, os.getStatus());
    }
    
    @Test
    void osPodeNaoTransicionarDeFINALIZADAParaENTREGUE_sePagamentoPENDENTE() {
        // Given: OS FINALIZADA com pagamento PENDENTE
        var os = OSBuilder.anOS()
            .withStatus(StatusOS.FINALIZADA)
            .withStatusPagamento(StatusPagamento.PENDENTE)
            .build();
        
        // When: entregar sem pagamento confirmado
        assertThrows(DomainException.class, () -> os.entregar("Entregue"));
    }
    
    @Test
    void osCancelada_naoDeveEmitirCobranca() {
        var os = OSBuilder.anOS()
            .withStatus(StatusOS.CANCELADA)
            .build();
        
        // status_pagamento deve ser null (não PENDENTE)
        assertNull(os.getStatusPagamento());
    }
}
```

### 5.2 Application Layer (Mockito)

**Test service orchestration** — mock infrastructure ports:

```java
@ExtendWith(MockitoExtension.class)
public class MockPaymentServiceTest {
    
    @Mock OSEntityRepository repository;
    @Mock CdiEventPublisher eventPublisher;
    
    @InjectMocks MockPaymentService service;
    
    @Test
    void confirmarPagamento_atualizaStatusEPublicaEvento() {
        // Given
        UUID osUuid = UUID.randomUUID();
        OSEntity os = new OSEntity();
        os.setUuid(osUuid);
        os.setStatusPagamento(StatusPagamento.PENDENTE);
        os.setValorCobrado(new BigDecimal("1500.00"));
        
        when(repository.findById(osUuid)).thenReturn(Optional.of(os));
        
        // When
        service.confirmarPagamento(osUuid);
        
        // Then
        verify(repository).save(argThat(savedOs -> 
            savedOs.getStatusPagamento().equals(StatusPagamento.CONFIRMADO)
        ));
        
        verify(eventPublisher).publish(any(PagamentoConfirmadoEvent.class));
    }
    
    @Test
    void confirmarPagamento_lancaExcecao_seJaConfirmado() {
        // Given
        UUID osUuid = UUID.randomUUID();
        OSEntity os = new OSEntity();
        os.setStatusPagamento(StatusPagamento.CONFIRMADO);
        
        when(repository.findById(osUuid)).thenReturn(Optional.of(os));
        
        // When/Then
        assertThrows(AppException.class, () -> service.confirmarPagamento(osUuid));
    }
}
```

### 5.3 Infrastructure Layer (@QuarkusTest)

**Test database persistence and migrations:**

```java
@QuarkusTest
@TestTransaction
public class PaymentPersistenceTest {
    
    @Inject OSRepository osRepository;
    @Inject ProcessedEventsRepository processedEventsRepository;
    
    @Test
    void v18Migration_adicionaColunasDePaymentAOrdensDe Servico() {
        // Given: OS criada em FINALIZADA
        var os = new OSEntity();
        os.setUuid(UUID.randomUUID());
        os.setStatus(StatusOS.FINALIZADA);
        os.setValorCobrado(new BigDecimal("1500.00"));
        os.setStatusPagamento(StatusPagamento.PENDENTE);
        
        // When: persiste
        osRepository.persist(os);
        
        // Then: lê do banco
        var loaded = osRepository.findByIdOptional(os.getId()).orElseThrow();
        assertEquals(StatusPagamento.PENDENTE, loaded.getStatusPagamento());
        assertNotNull(loaded.getDataPagamento());
    }
    
    @Test
    void processedEventsTable_previneProcessamentoDuplicado() {
        // Given: evento já processado
        UUID osUuid = UUID.randomUUID();
        var event = new ProcessedEventEntity("PAGAMENTO_CONFIRMADO", osUuid, LocalDateTime.now());
        processedEventsRepository.persist(event);
        
        // When: tenta processar novamente
        var duplicate = new ProcessedEventEntity("PAGAMENTO_CONFIRMADO", osUuid, LocalDateTime.now());
        
        // Then: violação de UNIQUE constraint
        assertThrows(PersistenceException.class, () -> processedEventsRepository.persist(duplicate));
    }
}
```

### 5.4 REST Layer (@QuarkusTest + REST Assured)

**Test end-to-end scenarios:**

```java
@QuarkusTest
public class PagamentoResourceTest {
    
    @Test
    void confirmarPagamento_retorna200_quandoPagamentoPendente() {
        // Given: OS com pagamento PENDENTE
        var osUuid = createOS(StatusOS.FINALIZADA, StatusPagamento.PENDENTE, 1500.00);
        
        // When
        var response = given()
            .pathParam("osUuid", osUuid)
            .when()
            .patch("/api/v1/os/{osUuid}/confirmar-pagamento")
            .then()
            .statusCode(200);
        
        // Then
        response.body("statusPagamento", equalTo("CONFIRMADO"));
    }
    
    @Test
    void confirmarPagamento_retorna409_quandoJaConfirmado() {
        // Given: OS com pagamento CONFIRMADO
        var osUuid = createOS(StatusOS.FINALIZADA, StatusPagamento.CONFIRMADO, 1500.00);
        
        // When
        var response = given()
            .pathParam("osUuid", osUuid)
            .when()
            .patch("/api/v1/os/{osUuid}/confirmar-pagamento")
            .then()
            .statusCode(409);
        
        // Then
        response.body("detail", containsString("não está pendente"));
    }
    
    @Test
    void registrarEntrega_retorna200_quandoPagamentoConfirmado() {
        // Given: OS FINALIZADA com pagamento CONFIRMADO
        var osUuid = createOS(StatusOS.FINALIZADA, StatusPagamento.CONFIRMADO, 1500.00);
        
        // When
        var response = given()
            .auth().oauth2("admin-token")  // @RolesAllowed admin
            .body("""
                {
                  "observacaoEntrega": "Veículo entregue"
                }
            """)
            .when()
            .patch("/api/v1/os/{osUuid}/entregar")
            .then()
            .statusCode(200);
        
        // Then
        response.body("status", equalTo("ENTREGUE"));
    }
    
    @Test
    void registrarEntrega_retorna409_quandoPagamentoPendente() {
        // Given: OS FINALIZADA com pagamento PENDENTE
        var osUuid = createOS(StatusOS.FINALIZADA, StatusPagamento.PENDENTE, 1500.00);
        
        // When
        var response = given()
            .auth().oauth2("admin-token")
            .body("""
                {
                  "observacaoEntrega": "Veículo entregue"
                }
            """)
            .when()
            .patch("/api/v1/os/{osUuid}/entregar")
            .then()
            .statusCode(409);
        
        // Then
        response.body("detail", containsString("pagamento confirmado"));
    }
}
```

### 5.5 Happy Path Scenario (Integration Test)

**Full flow: finalization → charge emission → payment confirmation → delivery:**

```java
@QuarkusTest
public class PaymentDeliveryIntegrationTest {
    
    @Inject OSRepository osRepository;
    @Inject MockPaymentService mockPaymentService;
    @Inject EntregaService entregaService;
    
    @Test
    @TestTransaction
    void fluxoCompleto_finalizacao_cobranca_pagamento_entrega() {
        // 1. Given: OS em EM_EXECUCAO
        var os = createOS(StatusOS.EM_EXECUCAO);
        
        // 2. When: Finaliza execução
        var osService = new OrdemDeServicoService(osRepository, eventPublisher);
        osService.finalizarExecucao(os.getUuid(), "Execução completa");
        
        // 3. Then: Cobrança foi emitida (status_pagamento = PENDENTE)
        var updated = osRepository.findByIdOptional(os.getId()).orElseThrow();
        assertEquals(StatusOS.FINALIZADA, updated.getStatus());
        assertEquals(StatusPagamento.PENDENTE, updated.getStatusPagamento());
        assertNotNull(updated.getDataPagamento());
        
        // 4. When: Pagamento é confirmado
        mockPaymentService.confirmarPagamento(os.getUuid());
        
        // 5. Then: Status atualizado para CONFIRMADO
        updated = osRepository.findByIdOptional(os.getId()).orElseThrow();
        assertEquals(StatusPagamento.CONFIRMADO, updated.getStatusPagamento());
        
        // 6. When: Registra entrega
        entregaService.registrarEntrega(os.getUuid(), "Entregue ao cliente");
        
        // 7. Then: OS transicionou para ENTREGUE
        updated = osRepository.findByIdOptional(os.getId()).orElseThrow();
        assertEquals(StatusOS.ENTREGUE, updated.getStatus());
        assertNotNull(updated.getDataEntrega());
    }
}
```

---

## 6. Validation Architecture

Validation architecture is described in `.planning/config.json` under `workflow.nyquist_validation`. For Phase 3, the following applies:

### 6.1 Test Requirements Map

| Req ID | Behavior | Test Type | Command | Coverage |
|--------|----------|-----------|---------|----------|
| PAG-01 | OS emits charge (PENDENTE) when finalization completes | Integration | `@QuarkusTest` with `@TestTransaction` | Payment persistence test |
| PAG-02 | Payment confirmed via mock bank, status updated (CONFIRMADO) | Integration | `@QuarkusTest` + REST Assured | `PagamentoResourceTest::confirmarPagamento_retorna200` |
| PAG-03 | Vehicle delivery only after payment confirmed (guard) | Unit + Integration | Domain test + REST test | `EntregaService` guard + `registrarEntrega_retorna409_quandoPagamentoPendente` |
| DOC-03 | CONTRIBUTING.md with setup, patterns, diagrams | Manual | N/A (documentation) | Checklist in planning |

### 6.2 Quick Test Commands

```bash
# Unit tests (domain layer, no DB)
./mvnw test -pl mekano-domain

# Application tests (mocks, no DB)
./mvnw test -pl mekano-application -am

# Infrastructure tests (H2 in-memory DB, Flyway migrations)
./mvnw test -pl mekano-infrastructure -am

# REST tests (full Quarkus container, DevServices)
./mvnw test -pl mekano-rest -am

# Full integration suite
./mvnw verify -pl mekano-rest -am
```

### 6.3 Phase 3 Test Additions

Wave 0 gaps:
- [ ] `mekano-domain/src/test/java/.../event/PagamentoStateTransitionsTest.java` — Domain guards
- [ ] `mekano-application/src/test/java/.../service/MockPaymentServiceTest.java` — Service orchestration
- [ ] `mekano-infrastructure/src/test/java/.../entity/PaymentPersistenceTest.java` — DB migrations + persistence
- [ ] `mekano-rest/src/test/java/.../api/PagamentoResourceTest.java` — REST endpoints
- [ ] `mekano-rest/src/test/java/.../PaymentDeliveryIntegrationTest.java` — Happy path E2E

---

## Key Learnings & Constraints

### From Phase 1 & 2 (Patterns to Reuse)

1. **State machine as single source of truth:** Phase 1's `Map<StatusOS, Set<StatusOS>>` transition matrix already covers all valid OS state changes. Phase 3 adds delivery as a guarded transition (payment must be confirmed).

2. **Event-driven architecture without Kafka:** Phase 2 established CDI events for inter-context communication (budget approval, stock reservation). Phase 3 extends with payment and delivery events, all within same transaction (synchronous).

3. **Service layer owns business policy, domain owns invariants:**
   - Service validates: "is payment confirmed?" (business rule)
   - Domain validates: "is OS in FINALIZADA state?" (invariant)
   - Both guards must pass before transition

4. **Audit log automatically captures transitions:** Phase 2's `os_audit_log` captures OS state changes via `@PreUpdate`. Payment and delivery transitions are OS state changes, so they're logged automatically. No new audit logic needed.

5. **Soft delete is pervasive:** Phase 2 extended `BaseEntity` with `is_active` and `deleted_at`. Phase 3 queries `WHERE is_active = true` by default. No special handling for payment/delivery entities.

### Specific Constraints for Phase 3

1. **Payment is not an entity** — it's a property of OS. Avoids Pitfall 1 (mega-aggregate). Simplifies: "unpaid orders" = one column query.

2. **Idempotency is mandatory** — mock bank endpoint may be retried. `processed_events` table with UNIQUE key `(event_type, aggregate_uuid)` prevents duplicate charges. Checked before any state change.

3. **Two guards for delivery** — service checks payment (business), domain checks state machine (invariant). Both are necessary: domain is reusable for other contexts, service implements specific business policy.

4. **No RefreshToken-like complexity** — payment confirmation is one-shot, not rotated like JWT refresh tokens. No rotation table needed.

5. **Mock bank simulation** — real delay (2 seconds) via `Thread.sleep` or `@Timeout`. Phase 2 pattern: `@Retry(maxRetries=3)` on reads, `@Timeout` on writes. Mock is classified as external service (write), so `@Timeout(3s)` applies.

### MVP Prioritization

- ✅ **Thin vertical slice:** Charge emission → payment confirmation → delivery (happy path only)
- ✅ **Reuse existing patterns:** No new frameworks, no new architectural concepts
- ✅ **Guard transitions:** Payment status is prerequisite for delivery
- ⏭️ **Deferred:** Refunds, payment reversals, multiple payment methods (v2)

---

## Sources

### Primary (HIGH confidence)
- **Phase 1 Context** (`01-CONTEXT.md`): State machine matrix, transition methods (D-25, D-26)
- **Phase 2 Context** (`02-CONTEXT.md`): SLA implementation (D-18..20), event patterns (D-58..60), audit log (D-66..69)
- **Phase 3 Context** (`03-CONTEXT.md`): 25 locked decisions (D-01..25), payment flow details, mock bank design
- **Codebase inspection**: BaseEntity structure, UserService pattern, CdiEventPublisher implementation, ApiExceptionMapper RFC 7807

### Secondary (MEDIUM confidence)
- **ROADMAP.md**: Phase 3 success criteria and risk mitigation (Pitfall 4: payment idempotency)
- **REQUIREMENTS.md**: PAG-01, PAG-02, PAG-03 requirement specifications
- **CLAUDE.md**: Project conventions, existing gotchas (G1-G10), build commands

---

## Metadata

**Confidence breakdown:**
- **Implementation patterns:** HIGH — verified in Phase 1-2 codebase and context files
- **API design:** HIGH — follows established REST conventions from Phase 1
- **Database schema:** HIGH — Flyway migration pattern from Phase 1-2
- **Event architecture:** HIGH — CDI event pattern established in Phase 2
- **Testing strategy:** HIGH — test layers follow Phase 1-2 structure exactly
- **Validation architecture:** HIGH — existing `nyquist_validation` framework applies

**Research date:** 2026-06-23  
**Valid until:** 2026-07-07 (stable API, 2 weeks)

**Research completed:** All 6 research goals addressed
1. ✅ Implementation patterns (payment state machine, charge flow, delivery guard, events)
2. ✅ API endpoint design (3 endpoints with request/response DTOs, error handling)
3. ✅ Database & persistence (V18-V19 migrations, OSEntity schema)
4. ✅ Async & events (3 event definitions, listener patterns, CDI flow)
5. ✅ Testing strategy (domain, application, infrastructure, REST layers)
6. ✅ Validation architecture (test map, quick commands, wave 0 gaps)
