# Phase 3: Pagamento & Delivery — Implementation Plan

**Scope:** Happy-path payment & delivery flow (OS finalized → charged → paid → delivered)  
**MVP Mode:** Enabled (vertical slices, no edge cases)  
**Total Tasks:** 20  
**Estimated Effort:** 2-3 weeks (distributed across domain/app/infra/REST/events/tests/docs)  
**Requirements:** OS-12 (SLA, prerequisite from Phase 2), PAG-01, PAG-02, PAG-03, DOC-03

---

## Overview

This plan implements the final two steps of the OS lifecycle:
1. **Automatic charge emission** when OS execution finishes (`OSFinalizadaEvent` → `CobrancaEmitidaEvent`, payment status = PENDENTE)
2. **Payment confirmation** via a simulated bank endpoint (payment status = CONFIRMADO, with idempotency guard)
3. **Vehicle delivery** guarded by payment confirmation (OS transitions FINALIZADA → ENTREGUE)

All decisions from `03-CONTEXT.md` (D-01 through D-25) are integrated into task descriptions with explicit references.

---

## Task Breakdown by Work Package

### 0. Prerequisite Gate (Task 0.1)

#### Task 0.1: Validate Phase 2 Prerequisites (Including OS-12)

**Description:** Validate that Phase 2 artifacts required by Phase 3 are present and operational before implementing payment/delivery. This resolves OS-12 ownership ambiguity by treating it as a prerequisite gate (per D-05), not a new Phase 3 implementation.

**Files modified:**
- None (checkpoint task)

**Action:**
1. Verify OS-12 behavior from Phase 2 is active (SLA expiration job + cancelation flow)
2. Verify `OSFinalizadaEvent` is published by Phase 2 finalization flow
3. Verify `CdiEventPublisher` is available for injection in application/infrastructure listeners
4. Verify `os_audit_log` infrastructure is available for transition tracing

**Acceptance commands:**
- `./mvnw test -pl mekano-application -am -Dtest=*Orcamento*SLA*`
- `./mvnw test -pl mekano-rest -am -Dtest=*Ordem*Servico*Finalizar*`

**Success Criteria:** Phase 2 dependencies are confirmed and documented in execution notes before Wave 1 starts.

**Dependencies:** None.

### 1. Domain & Entity Layer (Tasks 1.1 – 1.4)

#### Task 1.1: Domain Events for Payment & Delivery

**Description:** Create immutable event records in domain layer for payment/delivery lifecycle. Per D-17, D-18: two separate events (CobrancaEmitidaEvent and PagamentoConfirmadoEvent), plus OSEntregueEvent.

**Files created:**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/CobrancaEmitidaEvent.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/PagamentoConfirmadoEvent.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/OSEntregueEvent.java`

**Action (per D-16, D-17, D-18):**
Create three record classes in domain/event:
1. `CobrancaEmitidaEvent(UUID osUuid, UUID cobrancaId, BigDecimal valor, LocalDateTime dataEmissao)` — published when charge is emitted
2. `PagamentoConfirmadoEvent(UUID osUuid, UUID transacaoId, BigDecimal valor, LocalDateTime dataConfirmacao)` — published when mock bank confirms
3. `OSEntregueEvent(UUID osUuid, LocalDateTime dataEntrega, String observacao)` — published when vehicle is delivered

All records are immutable (Java records) and contain no framework annotations.

**Success Criteria:** All three event classes compile, are serializable via Jackson (for potential audit logging), and reside in com.fiap.mekano.domain.event package.

**Dependencies:** None (foundation task).

---

#### Task 1.2: OS Domain Model Extensions — Payment & Delivery Fields

**Description:** Extend OrdemDeServico domain model with payment-related fields and delivery date. Per D-01, D-02: payment state (PENDENTE | CONFIRMADO), payment date, amount paid, delivery date, delivery observation.

**Files modified:**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/OrdemDeServico.java`

**Action (per D-01, D-02, D-03):**
Add fields to OrdemDeServico:
- `StatusPagamento statusPagamento` (enum: PENDENTE, CONFIRMADO) — default = null initially
- `BigDecimal valorCobrado` — copied from Orcamento.valorTotal at charge emission (D-03: no recalculation)
- `LocalDateTime dataPagamento` — timestamp of charge emission or confirmation
- `LocalDateTime dataEntrega` — timestamp of delivery registration
- `String observacaoEntrega` — optional delivery notes

Add methods:
- `boolean isPagamentoPendente()` — returns statusPagamento == PENDENTE
- `boolean isPagamentoConfirmado()` — returns statusPagamento == CONFIRMADO
- `void confirmarPagamento(UUID transacaoId, LocalDateTime dataConfirmacao)` — sets status = CONFIRMADO, dataPagamento = dataConfirmacao
- `void entregar(String observacao)` — sets status = ENTREGUE, dataEntrega = now(), observacaoEntrega = observacao; THROWS DomainException if status != FINALIZADA

**Success Criteria:** OrdemDeServico compiles with all new fields, methods exist, `@Test` confirms field initialization to null (payment/delivery not pre-set).

**Dependencies:** Task 1.1 (events created).

---

#### Task 1.3: StatusPagamento Enum

**Description:** Create enum for payment status values (PENDENTE, CONFIRMADO). Per D-02: no intermediate states.

**Files created:**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/StatusPagamento.java`

**Action (per D-02):**
Create enum with two values:
```
PENDENTE("Charge emitted, awaiting confirmation"),
CONFIRMADO("Payment confirmed by mock bank")
```

**Success Criteria:** Enum compiles, has String descriptor for audit logs.

**Dependencies:** None.

---

#### Task 1.4: Domain Layer Unit Tests — Payment State Machine

**Description:** Write pure JUnit 5 tests (no framework, no DB) for payment and delivery state transitions. Per RESEARCH.md §5.1.

**Files created:**
- `mekano-domain/src/test/java/com/fiap/mekano/domain/model/PagamentoStateTransitionsTest.java`

**Action:**
Write parameterized tests covering:
1. `osCancelada_naoDeveEmitirCobranca()` — OS in CANCELADA cannot have payment
2. `osComPagamentoPendente_isPagamentoPendente()` — status query works
3. `osComPagamentoConfirmado_isPagamentoConfirmado()` — status query works
4. `osPodeTransicionarDeFINALIZADAParaENTREGUE_somenteSePagamentoConfirmado()` — delivery transition requires CONFIRMADO
5. `osPodeNaoTransicionarDeFINALIZADAParaENTREGUE_sePagamentoPENDENTE()` — delivery blocked if PENDENTE
6. `osPodeNaoTransicionarDeFINALIZADAParaENTREGUE_seNaoPaga()` — delivery blocked if null payment status (not charged)

**Success Criteria:** All 6 tests pass with `./mvnw test -pl mekano-domain`.

**Dependencies:** Tasks 1.1, 1.2, 1.3.

---

### 2. Application Layer (Tasks 2.1 – 2.4)

#### Task 2.1: CobrancaEmitidaListener — Charge Emission on OS Finalization

**Description:** CDI observer that listens for `OSFinalizadaEvent` (published from Phase 2's finalizarExecucao) and emits charge. Per D-10, D-16, D-17: automatic charge emission, publishes CobrancaEmitidaEvent.

**Files created:**
- `mekano-application/src/main/java/com/fiap/mekano/application/listener/CobrancaEmitidaListener.java`

**Action (per D-10, D-11, D-15, D-16, D-17, D-19):**
Create `@ApplicationScoped` CDI observer:
1. `void on(@Observes OSFinalizadaEvent event)` — receives event
2. Load OS from repository via UUID
3. **Idempotency guard (D-15):** check `processed_events` for `(event_type="COBRANCA_EMITIDA", aggregate_uuid=osUuid)`; if found, return without side effects
4. Validate: no existing charge (if `statusPagamento != null`, throw AppException 409 "Cobrança já existe")
5. Set `os.statusPagamento = PENDENTE`, `os.valorCobrado = event.valorTotal()`, `os.dataPagamento = now()`
6. Save OS
7. Publish `CobrancaEmitidaEvent(os.uuid, UUID.randomUUID(), os.valorCobrado, os.dataPagamento)`
8. Record processed event: INSERT `("COBRANCA_EMITIDA", os.uuid, now())`
9. Within same TX (synchronous CDI, D-19)

Inject: `OrdemDeServicoRepositoryPort`, `ProcessedEventsRepositoryPort`, `CdiEventPublisher`, `EventPublisher`.

**Success Criteria:** Listener compiles, integration test confirms event is received and OS is updated (PENDING).

**Dependencies:** Task 0.1 (Phase 2 gate), Task 1.1 (events), Task 1.2 (OS fields), Task 3.4 (processed_events repository), Phase 2 finalizarExecucao emits OSFinalizadaEvent.

---

#### Task 2.2: MockPaymentService — Simulated Bank Payment Confirmation

**Description:** Application service that simulates bank payment confirmation. Per D-12, D-14: 2-second delay, generates transactionId, updates payment status to CONFIRMADO, publishes PagamentoConfirmadoEvent. Per D-15: idempotency via processed_events table.

**Files created:**
- `mekano-application/src/main/java/com/fiap/mekano/application/service/MockPaymentService.java`

**Action (per D-12, D-14, D-15, D-18):**
Create `@ApplicationScoped` service with `@Transactional`:
1. `void confirmarPagamento(UUID osUuid)` method:
   - Load OS from repository
   - Validate OS exists (404 if not)
   - Validate payment status = PENDENTE (409 if not: "Pagamento não está pendente")
   - Check processed_events table (D-15): if event already recorded, skip (idempotent)
   - Simulate delay: `Thread.sleep(2000)` (D-14)
   - Generate transactionId = `UUID.randomUUID()`
   - Update OS: `os.statusPagamento = CONFIRMADO`, `os.dataPagamento = now()`
   - Save OS
   - Record in processed_events: INSERT `("PAGAMENTO_CONFIRMADO", os.uuid, now())`
   - Publish `PagamentoConfirmadoEvent(os.uuid, transacaoId, os.valorCobrado, now())`

Inject: `OrdemDeServicoRepositoryPort`, `ProcessedEventsRepositoryPort`, `CdiEventPublisher`.

Add `@Timeout(value = 3, unit = ChronoUnit.SECONDS)` per RESEARCH.md pattern (3-second timeout for 2-second mock).

**Success Criteria:** Service compiles, integration test with @QuarkusTest confirms delay, state update, event publication.

**Dependencies:** Task 1.1 (events), Task 1.2 (OS fields), processed_events repository (Task 3.4).

---

#### Task 2.3: EntregaService — Delivery Registration with Payment Guard

**Description:** Application service for registering vehicle delivery. Per D-06, D-07, D-08: admin+atendente can register, requires payment CONFIRMADO (service-level guard), OS must be FINALIZADA (domain-level guard).

**Files created:**
- `mekano-application/src/main/java/com/fiap/mekano/application/service/EntregaService.java`

**Action (per D-06, D-07, D-08, D-09):**
Create `@ApplicationScoped` service with `@Transactional`:
1. `void registrarEntrega(UUID osUuid, String observacaoEntrega)` method:
   - Load OS from repository
   - Validate OS exists (404 if not)
   - **Service-level guard (D-08):** if `!os.isPagamentoConfirmado()`, throw AppException 409: "Não é possível entregar veículo sem pagamento confirmado"
   - **Domain-level guard:** call `os.entregar(observacaoEntrega)` which validates status = FINALIZADA and throws DomainException if not
   - Save OS (now status = ENTREGUE)
   - Publish `OSEntregueEvent(os.uuid, os.dataEntrega, observacaoEntrega)`

Inject: `OrdemDeServicoRepositoryPort`, `CdiEventPublisher`.

**Success Criteria:** Service compiles, integration test confirms both guards work (409 if payment pending, 400 if OS not FINALIZADA).

**Dependencies:** Task 1.1 (events), Task 1.2 (OS fields + entregar method).

---

#### Task 2.4: Application Layer Unit Tests — Mocked Services

**Description:** Mockito tests for MockPaymentService and EntregaService without DB. Per RESEARCH.md §5.2.

**Files created:**
- `mekano-application/src/test/java/com/fiap/mekano/application/service/MockPaymentServiceTest.java`
- `mekano-application/src/test/java/com/fiap/mekano/application/service/EntregaServiceTest.java`

**Action:**
1. **MockPaymentServiceTest:**
   - `confirmarPagamento_atualizaStatusEPublicaEvento()` — mock OS, verify save called with CONFIRMADO, verify event published
   - `confirmarPagamento_lancaExcecao_seJaConfirmado()` — mock OS with CONFIRMADO, verify AppException 409
   - `confirmarPagamento_lancaExcecao_seNaoPendente()` — mock OS with null status, verify AppException 409

2. **EntregaServiceTest:**
   - `registrarEntrega_bloqueiaSeNaoPago()` — mock OS with FINALIZADA but PENDENTE payment, verify AppException 409
   - `registrarEntrega_sucesso_sePagoEFinalizada()` — mock OS with FINALIZADA + CONFIRMADO, verify OS saved with ENTREGUE, event published

Use `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`.

**Success Criteria:** All tests pass with `./mvnw test -pl mekano-application -am`.

**Dependencies:** Tasks 2.2, 2.3.

---

### 3. Infrastructure & Persistence (Tasks 3.1 – 3.4)

#### Task 3.1: Flyway Migration V18 — Payment Fields to OSEntity

**Description:** Database migration adding payment columns to ordens_de_servico table. Per D-24, D-01, D-02: `status_pagamento`, `data_pagamento`, `valor_cobrado`, `data_entrega`, `observacao_entrega` columns.

**Files created:**
- `mekano-infrastructure/src/main/resources/db/migration/V18__add_payment_fields_to_ordens_de_servico.sql`

**Action (per D-24, D-01, D-02):**
```sql
-- V18__add_payment_fields_to_ordens_de_servico.sql
ALTER TABLE ordens_de_servico ADD COLUMN status_pagamento VARCHAR(20) DEFAULT NULL;
ALTER TABLE ordens_de_servico ADD COLUMN data_pagamento TIMESTAMP DEFAULT NULL;
ALTER TABLE ordens_de_servico ADD COLUMN valor_cobrado DECIMAL(10, 2) DEFAULT NULL;
ALTER TABLE ordens_de_servico ADD COLUMN data_entrega TIMESTAMP DEFAULT NULL;
ALTER TABLE ordens_de_servico ADD COLUMN observacao_entrega TEXT DEFAULT NULL;

-- Indices for queries (D-01: unpaid orders query)
CREATE INDEX idx_os_status_pagamento ON ordens_de_servico(status_pagamento);
CREATE INDEX idx_os_data_pagamento ON ordens_de_servico(data_pagamento);
```

**Success Criteria:** Migration compiles (flyway syntax check), test database runs with H2 and PostgreSQL.

**Dependencies:** None (foundational infrastructure change).

---

#### Task 3.2: Flyway Migration V19 — Processed Events Table for Idempotency

**Description:** Database table for idempotency tracking. Per D-15, Pitfall 4 (ROADMAP): `processed_events` with unique key `(event_type, aggregate_uuid)` prevents duplicate webhook processing.

**Files created:**
- `mekano-infrastructure/src/main/resources/db/migration/V19__create_processed_events_table.sql`

**Action (per D-15):**
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

**Success Criteria:** Migration compiles, test verifies UNIQUE constraint enforced.

**Dependencies:** None (foundational infrastructure change).

---

#### Task 3.3: OSEntity JPA Mapping — Payment & Delivery Fields

**Description:** Extend OSEntity (Panache) with JPA mappings for new payment/delivery fields. Per D-01, D-02: @Column, @Enumerated for status_pagamento.

**Files modified:**
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/OSEntity.java`

**Action (per D-01, D-02):**
Add to OSEntity:
```java
@Column(name = "status_pagamento", length = 20)
@Enumerated(EnumType.STRING)
private StatusPagamento statusPagamento;  // null initially

@Column(name = "data_pagamento")
private LocalDateTime dataPagamento;

@Column(name = "valor_cobrado")
private BigDecimal valorCobrado;

@Column(name = "data_entrega")
private LocalDateTime dataEntrega;

@Column(name = "observacao_entrega")
private String observacaoEntrega;
```

Add accessor methods (Lombok `@Getter @Setter` or explicit).

**Success Criteria:** OSEntity compiles, test confirms fields persist via H2.

**Dependencies:** Task 3.1 (migration).

---

#### Task 3.4: ProcessedEventsRepository & Entity

**Description:** Repository + entity for idempotency tracking. Per D-15: prevents duplicate event processing.

**Files created:**
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/ProcessedEventEntity.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/ProcessedEventRepository.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/port/ProcessedEventsRepositoryPort.java` (if port-based)

**Action (per D-15):**
1. **ProcessedEventEntity** extends BaseEntity:
   - `String eventType` (VARCHAR 100)
   - `UUID aggregateUuid` (UUID NOT NULL)
   - `LocalDateTime processedAt` (DEFAULT CURRENT_TIMESTAMP)

2. **ProcessedEventRepository** extends PanacheRepository<ProcessedEventEntity, Long>:
   - `@ApplicationScoped`
   - `boolean existsFor(String eventType, UUID aggregateUuid)` — HQL: `SELECT COUNT(*) WHERE eventType = ? AND aggregateUuid = ?`
   - `ProcessedEventEntity save(ProcessedEventEntity entity)` — persist + flush

3. **ProcessedEventsRepositoryPort** (interface in application):
   - `boolean existsFor(String eventType, UUID aggregateUuid)`
   - `void save(ProcessedEventEntity entity)`

**Success Criteria:** Entity compiles, Panache repository works, test confirms UNIQUE constraint.

**Dependencies:** Tasks 3.1, 3.2.

---

#### Task 3.5: Infrastructure Layer Integration Tests — Persistence

**Description:** @QuarkusTest tests for payment persistence, delivery fields, processed events table. Per RESEARCH.md §5.3.

**Files created:**
- `mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/repository/PaymentPersistenceTest.java`

**Action:**
Write tests:
1. `v18Migration_adicionaColunasDePaymentAOrdensDe Servico()` — Create OS, set payment fields, persist, verify all fields loaded
2. `processedEventsTable_previneProcessamentoDuplicado()` — Insert event, attempt duplicate, verify UNIQUE constraint violation
3. `statusPagamento_persiste_comEnumCorreto()` — Save OS with PENDENTE, load, verify equals PENDENTE (not string)
4. `osComEntregaRegistrada_persisteDataEObservacao()` — Set dataEntrega + observacao, persist, load, verify both present

**Success Criteria:** All tests pass with `./mvnw test -pl mekano-infrastructure -am`.

**Dependencies:** Tasks 3.1, 3.2, 3.3, 3.4.

---

### 4. REST API & Error Handling (Tasks 4.1 – 4.2)

#### Task 4.1: PagamentoResource — Payment Confirmation Endpoint

**Description:** REST endpoint for payment confirmation. Per D-12, D-13: `PATCH /api/v1/os/{osUuid}/confirmar-pagamento`, @PermitAll (simulated bank).

**Files created:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/PagamentoResource.java`

**Action (per D-12, D-13, D-14, D-04):**
Create `@Path("/os")` resource:
```java
@RequestScoped
@PermitAll  // Simulated bank endpoint
public class PagamentoResource {
    @Inject MockPaymentService mockPaymentService;
    @Inject OrdemDeServicoService osService;
    
    @PATCH
    @Path("/{osUuid}/confirmar-pagamento")
    public Response confirmarPagamento(@PathParam("osUuid") UUID osUuid) {
        try {
            mockPaymentService.confirmarPagamento(osUuid);
            var osDto = osService.buscar(osUuid);  // DTO response
            return Response.ok(osDto).build();
        } catch (AppException e) {
            // RFC 7807 Problem Details
            if (e.getStatus() == 503) {
                // D-04: Mock unavailability message
                return Response.status(503)
                    .entity(ProblemDetail.of(503, 
                        "Pagamento indisponível no momento, tente novamente mais tarde", 
                        null))
                    .type("application/problem+json")
                    .build();
            }
            return Response.status(e.getStatus())
                .entity(ProblemDetail.of(e.getStatus(), e.getMessage(), null))
                .type("application/problem+json")
                .build();
        }
    }
}
```

Return DTO includes `statusPagamento`, `valorCobrado`, `dataPagamento` (D-22).

**Success Criteria:** Endpoint compiles, REST Assured test confirms 200 response with updated status, 409 if already confirmed, 404 if OS not found.

**Dependencies:** Task 2.2 (MockPaymentService).

---

#### Task 4.2: EntregaResource — Delivery Endpoint

**Description:** REST endpoint for vehicle delivery. Per D-06, D-07: `PATCH /api/v1/os/{osUuid}/entregar`, @RolesAllowed({"admin", "atendente"}).

**Files created:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/EntregaResource.java`

**Files modified:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/EntregaRequest.java` (if not existing)

**Action (per D-06, D-07, D-08):**
Create `@Path("/os")` resource:
```java
@RequestScoped
@Authenticated
public class EntregaResource {
    @Inject EntregaService entregaService;
    @Inject OrdemDeServicoService osService;
    
    @PATCH
    @Path("/{osUuid}/entregar")
    @RolesAllowed({"admin", "atendente"})
    public Response registrarEntrega(
        @PathParam("osUuid") UUID osUuid,
        EntregaRequest request
    ) {
        try {
            entregaService.registrarEntrega(osUuid, request.observacaoEntrega());
            var osDto = osService.buscar(osUuid);
            return Response.ok(osDto).build();
        } catch (AppException e) {
            return Response.status(e.getStatus())
                .entity(ProblemDetail.of(e.getStatus(), e.getMessage(), null))
                .type("application/problem+json")
                .build();
        }
    }
}
```

**Request DTO:**
```java
public record EntregaRequest(String observacaoEntrega) {}
```

**Response DTO:** Reuses OS DTO with `dataEntrega`, `observacaoEntrega`, `status`, `statusPagamento`.

**Success Criteria:** Endpoint compiles, REST Assured test confirms 200 response, 409 if payment pending, 400 if OS not FINALIZADA, 403 if not admin/atendente.

**Dependencies:** Task 2.3 (EntregaService).

---

#### Task 4.3: REST Layer End-to-End Tests

**Description:** @QuarkusTest + REST Assured tests for payment and delivery endpoints. Per RESEARCH.md §5.4.

**Files created:**
- `mekano-rest/src/test/java/com/fiap/mekano/rest/api/PagamentoResourceTest.java`
- `mekano-rest/src/test/java/com/fiap/mekano/rest/api/EntregaResourceTest.java`

**Action:**
1. **PagamentoResourceTest:**
   - `confirmarPagamento_retorna200_quandoPagamentoPendente()` — Create OS, payment PENDENTE, call endpoint, verify 200 + CONFIRMADO
   - `confirmarPagamento_retorna409_quandoJaConfirmado()` — OS already CONFIRMADO, verify 409 + error message
   - `confirmarPagamento_retorna404_quandoOSNaoExiste()` — Invalid UUID, verify 404
   - `confirmarPagamento_retorna503_quandoMockIndisponivel()` — Mock times out, verify 503 with D-04 message

2. **EntregaResourceTest:**
   - `registrarEntrega_retorna200_quandoPagamentoConfirmado()` — FINALIZADA + CONFIRMADO, verify 200 + ENTREGUE
   - `registrarEntrega_retorna409_quandoPagamentoPendente()` — FINALIZADA + PENDENTE, verify 409 with payment message
   - `registrarEntrega_retorna400_quandoNaoFinalizada()` — OS not FINALIZADA, verify 400
   - `registrarEntrega_retorna403_seNaoAutenticado()` — No JWT, verify 403
   - `registrarEntrega_retorna403_seRoleIncorreto()` — JWT but not admin/atendente, verify 403

Use `@TestSecurity` for role-based authorized scenarios and plain `given()` (without test security) for unauthenticated scenarios. Do not require manual JWT generation in these tests.

**Success Criteria:** All tests pass with `./mvnw test -pl mekano-rest -am`.

**Dependencies:** Tasks 4.1, 4.2.

---

#### Task 4.4: Happy Path Integration Test — Full Flow

**Description:** End-to-end integration test covering finalization → charge emission → payment confirmation → delivery. Per RESEARCH.md §5.5.

**Files created:**
- `mekano-rest/src/test/java/com/fiap/mekano/rest/PaymentDeliveryIntegrationTest.java`

**Action:**
Write single test method `fluxoCompleto_finalizacao_cobranca_pagamento_entrega()`:
1. Create OS in EM_EXECUCAO state
2. Call `PATCH /api/v1/os/{uuid}/finalizar-execucao` (from Phase 2) → OS transitions FINALIZADA
3. Verify CobrancaEmitidaListener fired: status_pagamento = PENDENTE, valorCobrado set
4. Call `PATCH /api/v1/os/{uuid}/confirmar-pagamento` → status_pagamento = CONFIRMADO
5. Verify PagamentoConfirmadoListener fired, processed_events recorded
6. Call `PATCH /api/v1/os/{uuid}/entregar` with auth header → OS transitions ENTREGUE
7. Verify OSEntregueEvent published
8. Call `GET /api/v1/os/{uuid}` → response includes all payment/delivery fields with correct values

**Success Criteria:** Test passes end-to-end with `./mvnw verify -pl mekano-rest -am`.

**Dependencies:** All REST, application, infrastructure, domain tasks.

---

### 5. Events & Async Workflows (Tasks 5.1 – 5.2)

#### Task 5.1: PagamentoConfirmadoListener — Event Listener for Audit

**Description:** CDI observer for PagamentoConfirmadoEvent. Per D-19: updates audit log (via existing mechanism from Phase 2).

**Files created:**
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/listener/PagamentoConfirmadoListener.java`

**Action (per D-19, D-20):**
Create `@ApplicationScoped` CDI observer:
1. `void on(@Observes PagamentoConfirmadoEvent event)` — receives event
2. OS audit log is updated automatically via Phase 2's `@PreUpdate` mechanism (D-20: no new audit logic needed)
3. Optional: add technical log statement with `osUuid` and `transacaoId` for observability

Inject: optionally `AuditLogRepository` (only if explicit audit extension is needed).

**Success Criteria:** Listener compiles, integration test confirms event is consumed and audit trail is preserved.

**Dependencies:** Task 1.1 (events), Task 2.2 (PagamentoConfirmadoEvent publication).

---

#### Task 5.2: OSEntregueListener — Event Listener for Audit

**Description:** CDI observer for OSEntregueEvent. Per D-19, D-20: audit log capture via existing mechanism.

**Files created:**
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/listener/OSEntregueListener.java`

**Action (per D-19, D-20):**
Create `@ApplicationScoped` CDI observer:
1. `void on(@Observes OSEntregueEvent event)` — receives event
2. Optional: Log event to application logs (but audit is automatic via @PreUpdate from Phase 2)

**Success Criteria:** Listener compiles, event is processed without errors.

**Dependencies:** Task 1.1 (events).

---

### 6. Tests — All Layers (Tasks 6.1 – 6.2)

#### Task 6.1: Full Test Suite Execution & Coverage

**Description:** Run complete test suite across all layers to ensure no regressions. Per CLAUDE.md test commands.

**Action:**
Run full test matrix:
```bash
# Domain tests (pure, no DB)
./mvnw test -pl mekano-domain

# Application tests (mocks, no DB)
./mvnw test -pl mekano-application -am

# Infrastructure tests (H2 in-memory, migrations)
./mvnw test -pl mekano-infrastructure -am

# REST tests (full Quarkus, DevServices PostgreSQL)
./mvnw test -pl mekano-rest -am

# Full verify (including integration tests)
./mvnw verify -pl mekano-rest -am
```

**Success Criteria:** All tests pass. Coverage targets: domain 100%, application 90%, infrastructure 85%, REST 80% (per Phase 2 patterns).

**Dependencies:** All test tasks (1.4, 2.4, 3.5, 4.3, 4.4).

---

#### Task 6.2: Test Report & Documentation

**Description:** Generate test reports and document test coverage per layer.

**Action:**
1. Run `./mvnw jacoco:report -pl mekano-rest` (from Phase 1 build config)
2. Verify report shows coverage by layer
3. Document findings in `.planning/phases/03-pagamento-delivery/TEST_REPORT.md` with:
   - Total test count by layer
   - Coverage percentages
   - Any gaps or critical paths not covered
   - Performance metrics (test execution time)

**Success Criteria:** Test report exists and shows >80% overall coverage.

**Dependencies:** Task 6.1.

---

### 7. Documentation (Tasks 7.1 – 7.2)

#### Task 7.1: CONTRIBUTING.md — Payment & Delivery Sections

**Description:** Add Phase 3 patterns and workflow to CONTRIBUTING.md. Per D-23: setup, test commands, gotchas, sequence diagrams.

**Files created/modified:**
- `CONTRIBUTING.md` (root of mekano project)

**Action (per D-23):**
Add to CONTRIBUTING.md (if not existing, create it with Phase 1-3 sections):

1. **Setup Section (if not present):**
   - Prerequisites: Java 17, Maven, Docker
   - Commands: `docker-compose up -d`, `./mvnw quarkus:dev`

2. **Phase 3: Pagamento & Delivery Section (NEW):**
   - **Overview:** Payment state machine, mock bank simulation, delivery guards
   - **Architecture:** Diagram showing CobrancaEmitidaEvent → PagamentoConfirmadoEvent → OSEntregueEvent flow
   - **Key Patterns:**
     - "Payment is modeled as fields on OS, not separate entity (D-01)"
     - "Two-phase validation: service checks business rules (D-08), domain checks invariants (D-09)"
     - "Idempotency via processed_events table (D-15)"
   - **Common Gotchas (new for Phase 3):**
     - G11: Forgetting to check processed_events → duplicate charges
     - G12: Payment guard only in service, not domain → allows invalid transitions
     - G13: Mock delay too short → doesn't catch timeout edge cases
   - **Test Commands:**
     ```bash
     ./mvnw test -pl mekano-domain              # Payment state transitions
     ./mvnw test -pl mekano-application -am     # Service orchestration
     ./mvnw test -pl mekano-infrastructure -am  # Persistence & events
     ./mvnw test -pl mekano-rest -am            # REST endpoints
     ```
   - **Sequence Diagrams (Mermaid):**
     - Diagram 1: Finalization → Charge Emission (OSFinalizadaEvent → CobrancaEmitidaEvent)
     - Diagram 2: Payment Confirmation (PATCH /confirmar-pagamento → MockPaymentService → PagamentoConfirmadoEvent)
     - Diagram 3: Delivery (PATCH /entregar → guard check → OSEntregueEvent)
     - Diagram 4: Full flow (Finalization through Delivery)

3. **Quick Reference Table (add Payment/Delivery rows):**
   - | Concept | Decide | Constraint |
   - | Payment Status | PENDENTE, CONFIRMADO | D-02 |
   - | Charge Trigger | OSFinalizadaEvent | D-10 |
   - | Delivery Guard | Payment CONFIRMADO (service) + Status FINALIZADA (domain) | D-08, D-09 |

**Success Criteria:** CONTRIBUTING.md is readable in plain text + renders as Markdown in GitHub, Mermaid diagrams render without syntax errors.

**Dependencies:** All tasks (documents work completed).

---

#### Task 7.2: Inline Code Comments & API Documentation

**Description:** Add JavaDoc and API documentation comments for payment/delivery classes. Per CLAUDE.md conventions.

**Files modified:**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/CobrancaEmitidaEvent.java` (JavaDoc record)
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/PagamentoConfirmadoEvent.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/OSEntregueEvent.java`
- `mekano-application/src/main/java/com/fiap/mekano/application/service/MockPaymentService.java`
- `mekano-application/src/main/java/com/fiap/mekano/application/service/EntregaService.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/PagamentoResource.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/EntregaResource.java`

**Action:**
1. Add JavaDoc comments to all public classes/methods explaining:
   - Purpose (one-line summary)
   - Parameters with @param tags
   - Return value with @return tag
   - Thrown exceptions with @throws tag
   - Example usage (for key services)
   - Decision reference (D-XX)

2. Add inline comments for complex logic (e.g., idempotency check, guard validation)

3. OpenAPI annotations already present from Phase 1 (ApiExceptionMapper); reuse for response documentation

**Success Criteria:** `./mvnw clean compile` produces no JavaDoc warnings, exported Javadoc is readable.

**Dependencies:** None (documentation-only).

---

## Dependency Graph

```
┌─────────────────────────────────────────────────────────────────────┐
│ WAVE 0: Prerequisites + Foundation                                  │
├─────────────────────────────────────────────────────────────────────┤
│ 0.1 (Phase 2 Gate)   ──┐                                            │
│ 1.1 (Domain Events)  ──┼─→ 1.2 (OS Model Extensions) ──┐           │
│ 1.3 (StatusPagamento) ──┼─→ 1.2 (OS Model Extensions) ──┐          │
│ 3.1 (V18 Migration)  ──┼─→ 3.3 (OSEntity Mapping)      │          │
│ 3.2 (V19 Migration)  ──┘                               │          │
│                                                         └─→ 3.4    │
│                                                                │    │
│                                                                └─→ 1.4 │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ WAVE 1: Application + Event Layer (ordered inside wave)             │
├─────────────────────────────────────────────────────────────────────┤
│ 3.4 (ProcessedEvents) ──┐                                          │
│ 1.2 (OS Model)       ───┼─→ 2.1 (CobrancaEmitidaListener) ──┐     │
│ 1.2 (OS Model)       ───┼─→ 2.3 (EntregaService)            ├─→2.4│
│ 3.4 (ProcessedEvents) ──┘  2.2 (MockPaymentService) ────────┘     │
│                                        │                            │
│                                        └─→ 5.1 (PagamentoConfirmadoListener) │
│                                            5.2 (OSEntregueListener)          │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ WAVE 2: REST API Layer                                              │
├─────────────────────────────────────────────────────────────────────┤
│ 2.2 (MockPaymentService) ──┐                                        │
│ 2.4 (App Tests)          ──┼─→ 4.1 (PagamentoResource) ──┐        │
│                            │                              │        │
│ 2.3 (EntregaService)     ──┼─→ 4.2 (EntregaResource)   ──┼─→ 4.3 │
│ 2.4 (App Tests)          ──┘                              │  (E2E) │
│                                                          ▼        │
│                                                   4.4 (Happy Path)  │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│ WAVE 3: Test & Documentation                                        │
├─────────────────────────────────────────────────────────────────────┤
│ 3.5 (Infra Tests) ──┐                                              │
│ 4.3 (REST Tests)  ──┼─→ 6.1 (Full Test Suite) ──→ 6.2 (Report)   │
│ 4.4 (Happy Path)  ──┤                                              │
│                      └─→ 7.1 (CONTRIBUTING.md)                    │
│                      └─→ 7.2 (Code Comments)                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Wave Structure & Parallelism

| Wave | Tasks | Autonomous | Parallel Potential |
|------|-------|-----------|-------------------|
| 0 | 0.1, 1.1, 1.3, 3.1, 3.2 | Partial (0.1 gate first) | 4 foundation tasks after gate |
| 1 | 3.3, 3.4, 1.2, 1.4, 2.1, 2.2, 2.3, 2.4, 5.1, 5.2 | Partial (ordered dependencies inside wave) | 3 devs on application + 2 on infra/events |
| 2 | 4.1, 4.2, 4.3, 4.4 | Yes | REST layer + E2E |
| 3 | 6.1, 6.2, 7.1, 7.2 | Yes | Test + doc (can overlap with execution) |

**Estimated timeline (with 5 developers):**
- Wave 0: 2-3 hours (prereq gate + foundation)
- Wave 1: 5-6 hours (ordered infra/app/events within same wave)
- Wave 2: 3-4 hours (2 devs on REST, 2 on integration tests)
- Wave 3: 2-3 hours (1 dev on tests, 1 on docs)
- **Total: ~13-16 hours of wall-clock time (1-2 development days)**

---

## Success Criteria (Phase-Level)

- [ ] Phase 2 prerequisite gate executed (Task 0.1), including OS-12 validation
- [ ] CobrancaEmitidaEvent published when OS finalization completes
- [ ] Payment status PENDENTE set automatically with correct amount from Orcamento
- [ ] Mock bank endpoint responds in ~2 seconds (D-14)
- [ ] Payment confirmation endpoint returns 200 + updated OS with CONFIRMADO status
- [ ] Idempotency prevents duplicate charges (processed_events table enforced)
- [ ] Delivery endpoint blocked if payment not CONFIRMADO (409 response)
- [ ] Delivery endpoint blocked if OS not FINALIZADA (domain guard)
- [ ] OSEntregueEvent published when delivery registered
- [ ] All tests pass: domain (100%), application (90%), infrastructure (85%), REST (80%)
- [ ] GET /api/v1/os/{uuid} returns payment/delivery fields in response
- [ ] CONTRIBUTING.md includes Phase 3 setup, gotchas, diagrams
- [ ] Requirement coverage: OS-12 (Phase 2, checked), PAG-01, PAG-02, PAG-03, DOC-03 ✓

---

## Risk Mitigation

| Risk | Impact | Mitigation | Status |
|------|--------|-----------|--------|
| Double-charge on webhook retry | $$ loss | processed_events UNIQUE constraint (D-15, Task 3.2) | Planned |
| Payment confirmation timeout | 503 error | @Timeout(3s) on service (D-14) | Planned |
| Missing payment guard | Invalid delivery | Domain + service guards (D-08, D-09, Task 2.3) | Planned |
| Concurrent OS writes | Lost update | @Version on OSEntity (Phase 1 pattern) | Existing |
| SLA expiry job overlap | Duplicate cancellations | ConcurrentExecution.SKIP on @Scheduled (Phase 2 pattern) | Existing |
| Mega-aggregate (payment state) | Contention | Payment fields on OS, no separate entity (D-01) | Locked |

---

## Decision Traceability

| Decision ID | Topic | Task(s) | Implementation |
|------------|--------|---------|-----------------|
| D-01 | Payment modeled on OS entity | 1.2, 3.1, 3.3 | Fields on OSEntity, no separate Pagamento aggregate |
| D-02 | Status: PENDENTE, CONFIRMADO | 1.3, 1.2 | StatusPagamento enum, no intermediate states |
| D-03 | Copy valor from Orcamento | 2.1 | CobrancaEmitidaListener sets valorCobrado = event.valorTotal() |
| D-04 | Mock failure response 503 | 4.1 | AppException(503, "Pagamento indisponível...") |
| D-06 | Delivery roles: admin, atendente | 4.2 | @RolesAllowed({"admin", "atendente"}) |
| D-07 | Delivery endpoint: PATCH /entregar | 4.2 | REST resource path configuration |
| D-08 | Service-level payment guard | 2.3 | EntregaService validates isPagamentoConfirmado() |
| D-09 | Domain-level state guard | 1.2, 2.3 | OS.entregar() validates status = FINALIZADA |
| D-10 | Automatic charge emission | 2.1 | CobrancaEmitidaListener on OSFinalizadaEvent |
| D-11 | Single charge emission with conflict | 2.1 | 409 when charge already exists |
| D-12 | Mock endpoint path | 4.1 | /api/v1/os/{osUuid}/confirmar-pagamento |
| D-14 | Mock delay 2 seconds | 2.2 | Thread.sleep(2000) + @Timeout(3s) |
| D-15 | Idempotency via processed_events | 3.2, 3.4, 2.2 | UNIQUE(event_type, aggregate_uuid) constraint |
| D-16 | Two separate events | 1.1 | CobrancaEmitidaEvent, PagamentoConfirmadoEvent separate |
| D-17 | CobrancaEmitidaEvent fields | 1.1 | (UUID osUuid, UUID cobrancaId, BigDecimal valor, LocalDateTime dataEmissao) |
| D-18 | PagamentoConfirmadoEvent fields | 1.1 | (UUID osUuid, UUID transacaoId, BigDecimal valor, LocalDateTime dataConfirmacao) |
| D-19 | Synchronous CDI events | 2.1, 2.2, 2.3, 5.1, 5.2 | @Observes within same @Transactional TX |
| D-20 | Reuse os_audit_log | 5.1, 5.2 | Phase 2's @PreUpdate mechanism, no new audit logic |
| D-22 | Payment data in GET /os | 4.1 | Response DTO includes statusPagamento, valorCobrado, dataPagamento |
| D-23 | CONTRIBUTING.md with diagrams | 7.1 | Mermaid sequence diagrams for 4 flows |
| D-24 | Flyway V18-V19 | 3.1, 3.2 | Migrations for payment fields and processed_events |
| D-25 | MockPaymentResource in mekano-rest | 4.1 | REST resource with @PermitAll |

---

## Requirements Coverage

| Requirement | Phase | Task(s) | Validation |
|-------------|-------|---------|------------|
| OS-12 (SLA expiry) | Phase 2 | 0.1 (prerequisite gate) | Validate SLA tests and finalization dependencies before Wave 1 |
| PAG-01 (Auto charge emission) | 3 | 2.1 (CobrancaEmitidaListener) | Integration test: OS finalization triggers charge |
| PAG-02 (Mock payment confirmation) | 3 | 2.2, 4.1 (MockPaymentService + Resource) | REST test: 200 response with CONFIRMADO status |
| PAG-03 (Delivery guard) | 3 | 2.3, 4.2 (EntregaService + Resource) | REST test: 409 if payment pending, 400 if not FINALIZADA |
| DOC-03 (CONTRIBUTING.md) | 3 | 7.1 (CONTRIBUTING.md update) | Checklist: setup section, test commands, Mermaid diagrams, gotchas |

---

## Notes for Execution

1. **Phase 2 Dependency (Task 0.1):** Ensure Phase 2 is complete (OS finalization working, events framework established) before starting Phase 3. Specifically:
   - `OSFinalizadaEvent` must be published by Phase 2's finalizarExecucao service
   - Audit log infrastructure (`os_audit_log` table) must exist
   - CdiEventPublisher must be working

2. **Test Database:** H2 in-memory during `mvn test`, PostgreSQL via DevServices during `mvn verify`. Flyway migrations V18-V19 execute in both contexts.

3. **Mock Delay:** 2-second `Thread.sleep` is intentional to simulate bank latency. Do not optimize away; tests verify delay is present.

4. **Idempotency Testing:** Critical to test duplicate webhook scenario. ProcessedEventsRepository.existsFor() must be checked before any state change.

5. **Developer Handoff:** Each wave should have clear exit criteria (all tests pass, no manual verification needed) before moving to next wave.

6. **Configuration:** Ensure application.properties includes:
   - `sla.expiry.cron` (from Phase 2)
   - Mock timeout config (new in Phase 3)

---

## Output & Artifacts

Upon Phase 3 completion:
- ✅ 20 tasks implemented across 8 work packages (including prerequisite gate)
- ✅ 2 new Flyway migrations (V18, V19)
- ✅ 3 event classes (CobrancaEmitidaEvent, PagamentoConfirmadoEvent, OSEntregueEvent)
- ✅ 3 application services (CobrancaEmitidaListener, MockPaymentService, EntregaService)
- ✅ 2 REST resources (PagamentoResource, EntregaResource)
- ✅ 2 event listeners (PagamentoConfirmadoListener, OSEntregueListener)
- ✅ ProcessedEventsRepository + Entity for idempotency
- ✅ 15+ test classes across all layers (JUnit 5, Mockito, REST Assured, @QuarkusTest)
- ✅ CONTRIBUTING.md updated with Phase 3 architecture, gotchas, diagrams
- ✅ All 37 v1 requirements satisfied (Auth ✓, OS ✓, Estoque ✓, Pagamento ✓)

---

**Next Action:** `/gsd-execute-phase 3` — Execute Phase 3 plans in wave order with 3 devs on payment/delivery, 2 devs on events/tests.

---

## Issues Criadas (Fase 3)

- [x] Task 0.1 — issue #32 criada
- [x] Task 1.1, 1.2, 1.3, 1.4 — issue #36 criada
- [x] Task 2.1, 2.2, 2.3, 2.4 — issue #34 criada
- [x] Task 3.1, 3.2, 3.3, 3.4, 3.5 — issue #33 criada
- [x] Task 5.1, 5.2 — issue #35 criada
- [x] Task 4.1, 4.2, 4.3, 4.4 — issue #37 criada
- [x] Task 6.1, 6.2, 7.1, 7.2 — issue #38 criada
