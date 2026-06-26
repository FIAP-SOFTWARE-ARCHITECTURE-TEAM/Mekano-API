# Phase 2: OS Continuation & Estoque — Research

**Researched:** 2026-06-22
**Domain:** OS lifecycle (budget → approval → execution → finalization), inventory management, scheduled SLA expiry, public client auth, admin user CRUD, OpenAPI docs
**Confidence:** HIGH (verified against official Quarkus docs, existing codebase patterns, and project conventions)

## Summary

Phase 2 extends the OS lifecycle from Phase 1 through budget generation, client approval/rejection, execution tracking, and finalization. Simultaneously implements full inventory management: parts CRUD with non-negative stock, atomic reservation on budget approval, purchase requisitions, NF entry, minimum stock alerts. Also adds admin user management (AUTH-04) and comprehensive OpenAPI documentation (DOC-02).

The codebase already has strong patterns for all of these: two-class repository, MapStruct CDI mappers, CDI event publishing (`CdiEventPublisher`), hybrid IDs, `BaseEntity` with soft delete, `@Transactional` on service layer only, Caffeine caching, and `@Retry`/`@Timeout` fault tolerance. The patterns are documented in `01-PATTERNS.md` with 89% analog coverage — every new entity class has an existing analog in the codebase.

**INC-01 changes already applied:**
- `StatusOS` enum has 7 states (no `APROVADA`). `aprovarOrcamento()` transitions to `EM_EXECUCAO` directly (no intermediate state).
- `reprovarOrcamento(String motivo)` added to `OrdemDeServico` — transitions to `CANCELADA`.
- `CancelarPorSLA()` added for auto-expiry transitions.
- Flyway Phase 2 uses V11-V17 (Phase 1 occupies V6-V10).

**Three critical technical decisions for this phase:**
1. **Atomic stock reservation** — Use `EntityManager.createNativeQuery("UPDATE peca SET saldo = saldo - :qtd WHERE uuid = :uuid AND saldo >= :qtd")` inside `@Transactional` service, then verify `executeUpdate()` result count. Avoids JPA optimistic locking overhead for this single-row operation. CDI event `OrcamentoAprovadoEvent` fires after successful reservation.
2. **SLA expiry job** — Use `quarkus-scheduler` (not Quartz) with `@Scheduled(cron = "${sla.expiry.cron}")` defaulting to `0 0 */12 * * ?`. Method annotated with `@Transactional`. Configurable via `application.properties`.
3. **OpenAPI documentation** — `quarkus-smallrye-openapi` already in pom.xml. Use `@Operation(summary = "...", description = "...")`, `@Schema(description = "...", example = "...")` on all new endpoints and DTOs. Security scheme auto-detected from `@RolesAllowed`.

**Primary recommendation:** Follow existing patterns strictly. Use native SQL for atomic stock update. Use `quarkus-scheduler` for SLA job. Reuse existing `CdiEventPublisher` for all domain events. Phase 2 has 8 plans across 4 waves, with 3 devs on OS + 2 devs on Estoque (Days 5-7).

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions (from 02-CONTEXT.md)

**Budget Pricing:** D-01 through D-08 — Simple sum pricing, no markup, no discounts, auto-generated at diagnosis finalization. Only in-stock items. Parts reserved only on approval.

**Client Authentication:** D-09 through D-17 — Client logs in with JWT (CPF). Account auto-created when client registered. Role 'cliente' reuses existing JWT auth. Default password shown to admin. Client can only see own OS's. No notification integration (simulated).

**SLA:** D-18 through D-20 — 72h default, configurable via properties. OS auto-canceled at expiry. Checked every 12h via scheduled job.

**CNPJ:** D-21 — Deferred. Only CPF in scope.

**Inventory Management:** D-22 through D-35 — Alert on minimum stock triggers auto-purchase requisition. RF quantity = fixed lot. States: ABERTA, CANCELADA, COMPRADA, RECEBIDA. NF must reference requisition. Saldo never negative (`UPDATE saldo = saldo - qtd WHERE saldo >= qtd`). Out put at execution start (not finalization). Error: 409 for insufficient stock, 404 for non-existent part.

**Execution:** D-36 through D-43 — Only mechanic can start/finish. Only one mechanic per OS. Average time endpoint. OS Detail returns full comparison. Filters on list.

**Admin User CRUD:** D-44 through D-47 — Separate `/admin/users` endpoint. Admin creates (with generated password), lists, edits, resets password, soft-deletes any user.

**Soft Delete:** D-48 through D-53 — All new entities extend `BaseEntity`. Blocks soft delete of client with open OS. Blocks soft delete of part referenced in pending OS. Restore endpoint `PATCH /{entidade}/{uuid}/restore`. Restore does NOT restore linked vehicles. CDI event on delete.

**OS Cancellation:** D-54 through D-57 — Admin and client can cancel. Only in `AGUARDANDO_APROVACAO`. Reason required. Cancellation releases stock reservations. OS retains history.

**CDI Events:** D-58 through D-60 — Six events defined (`OrcamentoGeradoEvent`, `OrcamentoAprovadoEvent`, `OrcamentoReprovadoEvent`, `OSFinalizadaEvent`, `EstoqueMinimoAtingidoEvent`, `RequisicaoCompraCriadaEvent`). Payload with main data. Naming: `EntidadeAcaoEvent`.

**Pagination:** D-61 through D-65 — Default 10, max 50. Sort: `dataCriacao` desc. Response: `totalPages` + `totalElements` + `page` + `size` + `content`. Same config for OS, peças and requisições.

**Audit:** D-66 through D-69 — Separate `os_audit_log` table. JSON snapshot at transition. `GET /os/{id}/historico`. Admin + mechanic. Immutable data (no soft delete).

**OpenAPI:** D-70 through D-71 — `@Operation` with summary/description on each endpoint. `@Schema` on DTOs. Realistic examples via `@ExampleAnnotation`.

**Names:** D-72 — Portuguese: Cliente, Veiculo, ServicoTipo, Peca, OrdemDeServico, Orcamento, RequisicaoCompra.

**Academic:** D-73 through D-75 — JaCoCo 80% LINE coverage gate on OS and Estoque domains. OWASP Dependency Check in CI. README.md with setup, patterns, usage instructions.

### the agent's Discretion
- Implementation details not covered above follow existing codebase patterns (BaseEntity, two-class repository, MapStruct CDI, etc.)
- Test structure follows established pattern (JUnit 5 + Mockito + REST Assured + AssertJ)
- Method names, Javadoc, and comments follow codebase conventions

### Deferred Ideas (OUT OF SCOPE)
- CNPJ (Pessoa Jurídica) — PF only, CPF scope
- Real WhatsApp/email notifications — simulated link log
- Rate limiting on public endpoints
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-04 | Admin CRUD users | Reuses existing `User` domain. New `/admin/users` resource at `/api/v1/admin/users`. Generated password pattern via `BcryptPasswordHasher`. Reuses existing `@RolesAllowed("admin")` protection. Soft delete already implemented. |
| OS-09 | Auto budget generation at diagnosis finalization | Sum of `ServicoTipo.valor * qtde` + `Peca.valor`. Separate `Orcamento` aggregate (prevents mega-aggregate). `OrcamentoService.execute()` with `@Transactional`. Validates state transition via state machine. |
| OS-10 | Client approves budget via API | Client auth with JWT (`role = "cliente"`). POST `/orcamento/{uuid}/aprovar`. Transitions OS to `EM_EXECUCAO` (via `aprovarOrcamento()`). Triggers atomic stock reservation. Fires `OrcamentoAprovadoEvent`. |
| OS-11 | Client rejects budget via API | POST `/orcamento/{uuid}/reprovar`. Calls `reprovarOrcamento(String motivo)`. OS → `CANCELADA`. Reason required. Fires `OrcamentoReprovadoEvent`. |
| OS-13 | Mechanic starts execution | OS → `EM_EXECUCAO`. Timestamp + mechanic ID + optional observation. `@RolesAllowed({"admin","mecanico"})`. Reserved stock gets debited (EST-08). |
| OS-14 | Mechanic finishes execution | OS → `FINALIZADA`. Timestamp + optional observation. Fires `OSFinalizadaEvent`. |
| OS-16 | List OS with filters | Filters: date, status, client, plate. Paginated (D-61..D-65). `@RolesAllowed({"admin","atendente"})`. |
| OS-17 | OS Detail with full data | Returns OS + client + vehicle + services + parts + history + budget. Client and vehicle embedded (by UUID ref). |
| OS-18 | Average execution time per service type | `GET /api/v1/os/metricas/tempo-medio?tipo=&inicio=&fim=`. HQL `avg()` of duration. Only `FINALIZADA` OS's. |
| EST-01 | Parts CRUD | Two-class repository pattern. `BaseEntity` with soft delete. Enum fixed units (UN, KG, L, CX, M, PC). Non-negative stock validated in domain `Peca` entity. |
| EST-02 | Edit/delete parts | Stock validation at domain level. Blocks delete if referenced in pending OS. Saldo never negative. |
| EST-03 | Atomic stock reservation on approval | Native SQL `UPDATE peca SET saldo = saldo - :qtd WHERE uuid = :uuid AND saldo >= :qtd`. Verify `executeUpdate()` returns 1. Inside `@Transactional` service. |
| EST-04 | Purchase requisition for unavailable parts | Auto-generates `RequisicaoCompra` when parts insufficient at approval. State `ABERTA` (auto-approved for OS urgency). |
| EST-05 | List/view/cancel purchase requisitions | Two-class repository. Cancel only by admin. CRUD via `RequisicaoCompraService`. |
| EST-06 | NF entry referencing requisition | NF data (number, series, supplier, emission date, CFOP). Updates stock via `saldo = saldo + qtd`. Transaction-scoped. Must reference a `RequisicaoCompra`. |
| EST-07 | Minimum stock check on saldo update | After any stock mutation, checks `saldo <= estoqueMinimo`. Auto-generates `RequisicaoCompra` if triggered. `EstoqueMinimoAtingidoEvent`. |
| EST-08 | Output of reserved parts at execution start | When `EM_EXECUCAO`, actual stock debit happens. Reservation flag cleared. `saldo = saldo - qtd` atomic. |
| EST-09 | Inventory alert system | GET `/alertas` + highlight in list. Minimum stock flag. Calculated: tempo de reposição × consumo médio (simplified: `saldo <= estoqueMinimo`). |
| DOC-02 | OpenAPI specification | `quarkus-smallrye-openapi` already in pom.xml. Annotations: `@Operation`, `@Schema`, `@APIResponse`, `@ExampleAnnotation`. All new endpoints documented. |

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Budget auto-generation | API / Backend (application service) | — | Pure server-side computation: sum of items. No client input. |
| Client auth & approval | API / Backend (rest + application) | — | Client logs in via JWT (same auth as Phase 1). Approval endpoint is POST. |
| SLA expiry job | API / Backend (scheduled) | — | Quarkus scheduler runs in-process on server. No external scheduler. |
| Atomic stock reservation | Database / Backend | — | Native SQL `UPDATE ... WHERE saldo >= qtd` is database-level atomic operation. Single-statement guarantee. |
| Minimum stock alert | Backend (domain event) | — | Triggered after any stock mutation. Synchronous CDI event. |
| Purchase requisitions | Backend | — | Pure domain logic: auto-generate on minimum stock or unavailable parts. |
| Average execution time | Backend (repository query) | — | HQL `avg()` calculation. REST endpoint exposes result. |
| Admin user CRUD | Backend | — | Reuses existing User domain and auth infrastructure. |
| OpenAPI docs | Build-time annotation scanning | — | Automatically generated from `@Operation`, `@Schema` source annotations. |
| JaCoCo / OWASP DC | CI / Build | — | Maven plugins bound to verify phase. Reports generated at build time. |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `quarkus-scheduler` | 3.36.0 | SLA expiry job (12h cron) | Lightweight in-memory scheduler, `@Scheduled` annotation, supports `@Transactional` — perfect for periodic batch jobs |
| `quarkus-hibernate-orm-panache` | 3.36.0 | JPA persistence | Already in stack, used for ALL entity persistence |
| `quarkus-smallrye-openapi` | 3.36.0 | OpenAPI / Swagger UI | Already in `mekano-rest/pom.xml`. Auto-generates OpenAPI 3.1 spec from annotations |
| `quarkus-cache` (Caffeine) | 3.36.0 | In-memory caching | Already in stack. Add caches for Peca, Orcamento lookups |
| `quarkus-smallrye-fault-tolerance` | 3.36.0 | `@Retry` / `@Timeout` | Already in stack. Apply to new repository operations |
| `quarkus-arc` | 3.36.0 | CDI container | Already in stack. Events, injection, scopes |
| `jakarta.transaction.Transactional` | — | Transaction boundary | Existing pattern: `@Transactional` ONLY on service methods, NEVER on resources |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `quarkus-jdbc-h2` | 3.36.0 | H2 test database | Tests only (already in stack) |
| `rest-assured` | — | REST integration tests | Tests only (already in stack) |
| `jacoco-maven-plugin` | 0.8.12 | Code coverage gate | Build only (D-73: 80% minimum LINE coverage) |
| `dependency-check-maven` | 12.2.2 | OWASP vulnerability scan | Build only (D-74) |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Native SQL for stock reservation | JPA `@Version` optimistic locking | Native SQL is atomic at database level; JPA version has race window between read and write |
| `quarkus-scheduler` (in-memory) | Quartz scheduler | In-memory is adequate for single-instance; Quartz adds persistent/clustered job support but unnecessary complexity for Phase 2 |
| CDI events (synchronous) | Kafka / message broker | Zero infrastructure, same-transaction consistency, adequate for this scale |

**Installation:**
```xml
<!-- mekano-rest/pom.xml — add quarkus-scheduler -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-scheduler</artifactId>
</dependency>
```

**Version verification:**
All libraries managed by Quarkus BOM 3.36.0 — no explicit version needed except for stand-alone plugins:
- `jacoco-maven-plugin:0.8.12` — latest stable as of 2026-06-22 [VERIFIED: Maven Central]
- `dependency-check-maven:12.2.2` — latest stable as of 2026-06-22 [VERIFIED: Maven Central]

## Package Legitimacy Audit

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| `io.quarkus:quarkus-scheduler` | Maven Central | 7+ yrs | Very high | `github.com/quarkusio/quarkus` | — | Approved (managed by BOM) |
| `org.jacoco:jacoco-maven-plugin:0.8.12` | Maven Central | 10+ yrs | Very high | `github.com/jacoco/jacoco` | — | Approved |
| `org.owasp:dependency-check-maven:12.2.2` | Maven Central | 10+ yrs | Very high | `github.com/dependency-check/DependencyCheck` | — | Approved |

**Packages removed due to slopcheck [SLOP] verdict:** None
**Packages flagged as suspicious [SUS]:** None
*Note: slopcheck was unavailable at research time (Python environment). All packages above are from Maven Central with established track records and are managed either by Quarkus BOM or widely used in the Java ecosystem.*

## Architecture Patterns

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT TIER                                     │
│  Browser (Swagger UI)  │  Mechanic App  │  Admin Panel  │  Client (JWT)    │
└──────────────────────────────┬──────────────────────────────────────────────┘
                               │ HTTP / JSON
                               ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  REST TIER (mekano-rest)                                                     │
│                                                                              │
│  /admin/users  │  /os/{id}/orcamento  │  /orcamento/{id}/aprovar            │
│  /os/{id}/executar/iniciar  │  /os/{id}/executar/finalizar                  │
│  /pecas  │  /requisicoes-compra  │  /nf-entrada  │  /alertas                 │
│  /os/{id}/historico  │  /os/metricas/tempo-medio                            │
│                                                                              │
│  @RequestScoped @RolesAllowed  │  @PermitAll (auth endpoints)                │
│  MapStruct DTO ↔ Domain  │  ApiExceptionMapper (RFC 7807)                   │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │ CDI
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  APPLICATION TIER (mekano-application)                                       │
│                                                                              │
│  OrcamentoService  │  PecaService  │  RequisicaoCompraService                │
│  NfEntradaService  │  OrdemDeServicoService  │  AdminUserService             │
│                                                                              │
│  @Transactional  │  @ApplicationScoped  │  EventPublisher.publish()          │
│  CDI observers: PecaOrcamentoObserver (reserve stock on approval)           │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │ Ports
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  DOMAIN TIER (mekano-domain) — Pure Java, zero deps                          │
│                                                                              │
│  model: Cliente, Veiculo, ServicoTipo, Peca, OrdemDeServico                 │
│         Orcamento, ItemOrcamento, RequisicaoCompra, NfEntrada               │
│  valueobject: Cpf, PlacaVeiculo, Telefone, Endereco, StatusOS               │
│  port/in: OrcamentoServicePort, PecaServicePort, ...                        │
│  port/out: PecaRepositoryPort, OrcamentoRepositoryPort, ...                 │
│  event: OrcamentoAprovadoEvent, EstoqueMinimoAtingidoEvent, ...             │
│  exception: SaldoInsuficienteException, ClienteComOSeAbertaException         │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │ Port implementations
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  INFRASTRUCTURE TIER (mekano-infrastructure)                                 │
│                                                                              │
│  entity: PecaEntity, OrcamentoEntity, RequisicaoCompraEntity,               │
│          NfEntradaEntity, OsAuditLogEntity (no BaseEntity — no soft delete) │
│  repository: Two-class (PanacheRepo + Impl) for each aggregate               │
│  mapper: PecaEntityMapper, OrcamentoEntityMapper, ... (MapStruct CDI)        │
│  event: CdiEventPublisher (already exists)                                   │
│  security: BcryptPasswordHasher (reuse for client passwords)                 │
│  service: SlaExpiryJob (infrastructure scheduled bean)                      │
│                                                                              │
│  @CacheResult  │  @Retry  │  @Timeout  │  Flyway migrations V11-V17         │
└───────────────────────────────────┬──────────────────────────────────────────┘
                                    │ SQL
                                    ▼
┌──────────────────────────────────────────────────────────────────────────────┐
│  DATABASE (PostgreSQL 16 / H2 for tests)                                     │
│                                                                              │
│  Tables: clientes, veiculos, servico_tipos, pecas, ordens_de_servico        │
│          orcamentos, orcamento_itens, requisicoes_compra,                   │
│          nf_entradas, os_audit_log, os_servicos_executados,                 │
│          os_pecas_usadas, os_historico_status                               │
│                                                                              │
│  Flyway V11-V17  │  Soft delete everywhere  │  UUID for public IDs          │
└──────────────────────────────────────────────────────────────────────────────┘

Entry points and data flow:
  1. POST /orcamento/{uuid}/aprovar  →  OrcamentoResource  →  OrcamentoService  →  OS state change + stock reservation
  2. @Scheduled SLA job  →  SlaExpiryJob  →  OrdemDeServicoRepository  →  Cancel expired OS's
  3. Stock mutation  →  PecaService  →  EstoqueMinimoAtingidoEvent  →  RequisicaoCompraService (auto-generate)
```

### Recommended Project Structure (new packages per module)

```
mekano-domain/src/main/java/com/fiap/mekano/domain/
├── event/
│   ├── OrcamentoGeradoEvent.java
│   ├── OrcamentoAprovadoEvent.java       # Includes list of parts + quantities
│   ├── OrcamentoReprovadoEvent.java
│   ├── OSFinalizadaEvent.java
│   ├── OSIniciadaEvent.java              # NEW: for execution start events
│   ├── EstoqueMinimoAtingidoEvent.java
│   ├── RequisicaoCompraCriadaEvent.java
│   └── SoftDeleteEvent.java
├── model/
│   ├── Cliente.java                      # New domain entity (or from Phase 1)
│   ├── Veiculo.java                      # New domain entity (or from Phase 1)
│   ├── ServicoTipo.java                  # New domain entity (or from Phase 1)
│   ├── Peca.java                         # New: stock, minimum, unit
│   ├── OrdemDeServico.java               # Phase 1 + new transitions (aprovarOrcamento, reprovarOrcamento, CancelarPorSLA)
│   ├── Orcamento.java                    # Separate aggregate (D-02)
│   ├── ItemOrcamento.java                # Orcamento child: servico + qtd + valorUnitario
│   ├── StatusOS.java                     # 7-state enum (extends Phase 1)
│   ├── RequisicaoCompra.java             # New aggregate: status, items, supplier
│   ├── NfEntrada.java                    # New: NF data with requisition reference
│   └── UnidadeMedida.java                # Enum: UN, KG, L, CX, M, PC
├── valueobject/
│   ├── Cpf.java                          # CPF with validation (if not Phase 1)
│   ├── PlacaVeiculo.java                 # Placa with Mercosul validation (if not Phase 1)
│   ├── Telefone.java                     # Brazilian phone validation
│   └── Endereco.java                     # Flattened address VO
├── port/in/
│   ├── ClienteServicePort.java           # CRUD + find
│   ├── VeiculoServicePort.java           # CRUD + find
│   ├── ServicoTipoServicePort.java       # CRUD + find
│   ├── OrcamentoServicePort.java         # aprovar, reprovar, gerar
│   ├── PecaServicePort.java              # CRUD + stock operations
│   ├── RequisicaoCompraServicePort.java  # CRUD + cancel
│   ├── NfEntradaServicePort.java         # Register NF
│   └── AdminUserServicePort.java         # Admin user management
├── port/out/
│   ├── ClienteRepositoryPort.java
│   ├── VeiculoRepositoryPort.java
│   ├── ServicoTipoRepositoryPort.java
│   ├── PecaRepositoryPort.java           # + reservarEstoque, creditarEstoque
│   ├── OrcamentoRepositoryPort.java
│   ├── RequisicaoCompraRepositoryPort.java
│   ├── NfEntradaRepositoryPort.java
│   ├── OrdemDeServicoRepositoryPort.java # + findExpiradas
│   └── OsAuditLogRepositoryPort.java
└── exception/
    ├── AppException.java                 # Reuse existing
    └── Messages.java                     # Add new messages

mekano-application/src/main/java/com/fiap/mekano/application/
└── service/
    ├── cliente/
    │   └── ClienteService.java           # CRUD + auto-create User account
    ├── veiculo/
    │   └── VeiculoService.java
    ├── servico/
    │   └── ServicoTipoService.java
    ├── orcamento/
    │   ├── OrcamentoService.java         # Generate + approve + reject
    │   └── PecaOrcamentoObserver.java    # CDI observer: reserves stock on approval
    ├── peca/
    │   ├── PecaService.java              # CRUD
    │   └── EstoqueObserver.java          # CDI observer: checks minimum stock
    ├── requisicao/
    │   └── RequisicaoCompraService.java  # CRUD + auto-generate
    ├── nf/
    │   └── NfEntradaService.java         # Register NF + update stock
    ├── os/
    │   ├── OrdemDeServicoService.java    # Start/finish execution, list, detail, metrics
    │   └── OsAuditService.java           # Audit logging
    └── admin/
        └── AdminUserService.java         # Admin CRUD + password generation

mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/
├── entity/
│   ├── ClienteEntity.java               (extends BaseEntity)
│   ├── VeiculoEntity.java               (extends BaseEntity)
│   ├── ServicoTipoEntity.java           (extends BaseEntity)
│   ├── PecaEntity.java                  (extends BaseEntity)
│   ├── OrdemDeServicoEntity.java        (extends BaseEntity + @Version)
│   ├── OrcamentoEntity.java             (extends BaseEntity)
│   ├── ItemOrcamentoEntity.java         (no BaseEntity — owned by Orcamento)
│   ├── RequisicaoCompraEntity.java      (extends BaseEntity)
│   ├── NfEntradaEntity.java             (extends BaseEntity)
│   └── OsAuditLogEntity.java            (NO BaseEntity — not soft-deletable, D-69)
├── repository/
│   ├── ClientePanacheRepository.java + ClienteRepositoryImpl.java
│   ├── VeiculoPanacheRepository.java + VeiculoRepositoryImpl.java
│   ├── ServicoTipoPanacheRepository.java + ServicoTipoRepositoryImpl.java
│   ├── PecaPanacheRepository.java + PecaRepositoryImpl.java
│   ├── OrcamentoPanacheRepository.java + OrcamentoRepositoryImpl.java
│   ├── RequisicaoCompraPanacheRepository.java + RequisicaoCompraRepositoryImpl.java
│   ├── NfEntradaPanacheRepository.java + NfEntradaRepositoryImpl.java
│   └── OsAuditLogPanacheRepository.java + OsAuditLogRepositoryImpl.java
├── mapper/
│   ├── ClienteEntityMapper.java + ClienteEntityMapperImpl.java
│   ├── VeiculoEntityMapper.java + VeiculoEntityMapperImpl.java
│   ├── ServicoTipoEntityMapper.java + ServicoTipoEntityMapperImpl.java
│   ├── PecaEntityMapper.java + PecaEntityMapperImpl.java
│   ├── OrcamentoEntityMapper.java + OrcamentoEntityMapperImpl.java
│   └── ... (manual impls, same pattern as UserEntityMapperImpl)
├── cache/
│   └── CacheNames.java                  # Add PECAS, ORCAMENTOS, CLIENTES, etc.
└── service/
    └── SlaExpiryJob.java                # @Scheduled SLA expiry job

mekano-rest/src/main/java/com/fiap/mekano/rest/
├── api/
│   ├── OrcamentoResource.java           # /orcamento — client approval/rejection
│   ├── PecaResource.java                # /pecas — CRUD peças/insumos
│   ├── RequisicaoCompraResource.java    # /requisicoes-compra — list, cancel
│   ├── NfEntradaResource.java           # /nf-entrada — register NF
│   ├── AdminUserResource.java           # /admin/users — admin user management
│   ├── AlertaResource.java              # /alertas — minimum stock alerts
│   └── OrdemDeServicoResource.java      # extended with execution, detail, metrics, history
├── dto/
│   ├── orcamento/
│   │   ├── OrcamentoResponse.java
│   │   ├── OrcamentoAprovarRequest.java
│   │   └── OrcamentoReprovarRequest.java
│   ├── peca/
│   │   ├── PecaRequest.java
│   │   ├── PecaResponse.java
│   │   └── PecaPageResponse.java
│   ├── requisicao/
│   │   ├── RequisicaoCompraRequest.java
│   │   └── RequisicaoCompraResponse.java
│   ├── nf/
│   │   └── NfEntradaRequest.java
│   ├── admin/
│   │   ├── AdminCreateUserResponse.java  # includes generatedPassword
│   │   ├── AdminUserUpdateRequest.java
│   │   └── AdminUserResponse.java
│   ├── os/
│   │   ├── OSDetailResponse.java
│   │   ├── OSMetricaResponse.java
│   │   ├── OsHistoricoResponse.java
│   │   ├── IniciarExecucaoRequest.java
│   │   └── FinalizarExecucaoRequest.java
│   └── alerta/
│       └── AlertaResponse.java
└── mapper/
    ├── OrcamentoDtoMapper.java
    ├── PecaDtoMapper.java
    ├── RequisicaoCompraDtoMapper.java
    ├── AdminUserDtoMapper.java
    ├── OrdemDeServicoDtoMapper.java      # Extended for new DTOs
    └── AlertaDtoMapper.java
```

### Pattern 1: Atomic Stock Reservation via Native SQL
**What:** `UPDATE saldo = saldo - qtd WHERE uuid = ? AND saldo >= qtd` executed atomically at database level, inside `@Transactional`. No JPA version column or pessimistic lock overhead.

**When to use:** Any operation that decrements stock: budget approval (reservation/flag), execution start (actual debit).

**Why not JPA `@Version`:** The reservation is a single-row arithmetic operation. Loading entity → setter → merge introduces a race window. Native SQL `UPDATE ... WHERE saldo >= :qtd` is atomic, single-statement, and lets the database enforce the constraint.

**Example:**
```java
// PecaRepositoryImpl.java
@Transactional(Transactional.TxType.MANDATORY) // must be inside service TX
@Override
public void reservarEstoque(UUID pecaUuid, int quantidade) {
    int updated = panacheRepository.getEntityManager()
        .createNativeQuery("UPDATE pecas SET saldo = saldo - :qtd " +
                           "WHERE uuid = :uuid AND saldo >= :qtd AND is_active = true")
        .setParameter("qtd", quantidade)
        .setParameter("uuid", pecaUuid)
        .executeUpdate();

    if (updated == 0) {
        throw new AppException(409,
            "Saldo insuficiente para peça " + pecaUuid);
    }
}
```
[CITED: Quarkus Hibernate ORM Panache guide — `PanacheRepositoryBase.getEntityManager()` via `entityManager` field]

### Pattern 2: @Scheduled SLA Expiry Job
**What:** Quarkus `@Scheduled` with cron expression, annotated with `@Transactional`. Runs every 12 hours, checks all OS's in `AGUARDANDO_APROVACAO` with expiration past.

**When to use:** Any periodic batch job within the same application process.

**Key details:**
- `@Scheduled(cron = "${sla.expiry.cron:0 0 */12 * * ?}")` — configurable, defaults to every 12 hours
- Method must be `void` and non-private
- `@Transactional` is supported on `@Scheduled` methods
- If the job needs to run immediately on app startup + every 12h, use `@Scheduled(cron = "...", delayed = "2m")` to give app time to initialize
- Tests disable scheduler with `quarkus.scheduler.enabled=false`
- Use `@Scheduled(concurrentExecution = SKIP)` to prevent overlapping runs

**Example:**
```java
@ApplicationScoped
public class SlaExpiryJob {

    @Inject
    OrdemDeServicoRepositoryPort osRepository;

    @Scheduled(cron = "${sla.expiry.cron:0 0 */12 * * ?}",
               identity = "sla-expiry-job",
               concurrentExecution = ConcurrentExecution.SKIP)
    @Transactional
    void verificarExpiracaoSLA() {
        List<OrdemDeServico> expiradas = osRepository
            .findExpiradasEmAguardandoAprovacao(LocalDateTime.now().minusHours(slaHoras));

        for (OrdemDeServico os : expiradas) {
            os.cancelar("SLA expirado", "sistema");
            osRepository.save(os);
        }
    }
}
```
[CITED: Quarkus Scheduler Reference Guide — `@Scheduled` CRON, `@Transactional` support, `concurrentExecution`]

### Pattern 3: CDI Events for Cross-Aggregate Communication (Synchronous)
**What:** Existing `CdiEventPublisher` fires `OrcamentoAprovadoEvent`. A CDI observer method (`@Observes`) in the relevant service handles the side effect (stock reservation).

**When to use:** Any cross-aggregate operation where the primary command should not be coupled to the side effect, but both must succeed or fail atomically.

**Key considerations:**
- CDI events are **synchronous** by default (`eventBus.fire()`). Both publisher and observer run within the same transaction. If the observer throws, the entire transaction rolls back.
- This is **desirable** for Phase 2: stock reservation MUST succeed for budget approval to be valid.
- Naming: `EntidadeAcaoEvent` (D-60). Payload: `OrcamentoAprovadoEvent` includes `List<ItemOrcamentoDTO>` (D-59).

**Example:**
```java
// OrcamentoService — publishes event after approval
@Transactional
public Orcamento aprovarOrcamento(UUID orcamentoUuid, UUID clienteId) {
    Orcamento orcamento = orcamentoRepo.findById(orcamentoUuid)
        .orElseThrow(() -> new AppException(404, "Orçamento não encontrado"));

    orcamento.aprovar(clienteId);
    orcamentoRepo.save(orcamento);

    // OS state transition inside same TX
    OrdemDeServico os = osRepository.findById(orcamento.getOsUuid())
        .orElseThrow(() -> new AppException(404, "OS não encontrada"));
    os.aprovarOrcamento();
    osRepository.save(os);

    eventPublisher.publish(OrcamentoAprovadoEvent.of(orcamento));
    return orcamento;
}

// PecaOrcamentoObserver — observes event and reserves stock
@ApplicationScoped
public class PecaOrcamentoObserver {

    void onOrcamentoAprovado(@Observes OrcamentoAprovadoEvent event) {
        for (ItemOrcamentoDTO item : event.itens()) {
            try {
                pecaRepository.reservarEstoque(item.pecaUuid(), item.quantidade());
            } catch (AppException e) {
                if (e.getStatus() == 409) {
                    // Stock insufficient — auto-generate purchase requisition
                    requisicaoService.criarParaOrcamento(
                        item.pecaUuid(), item.quantidade(), event.orcamentoUuid());
                } else {
                    throw e;
                }
            }
        }
    }
}
```
[CITED: Existing CdiEventPublisher.java, Quarkus CDI Events guide]

### Pattern 4: Client Auth with Role 'cliente'
**What:** Client is a `User` with role `cliente`. Account created automatically when `Cliente` is registered. Login = CPF (stored as `email` field or new `cpf` field in User). Password generated by system and displayed once.

**When to use:** Any new role-based authentication.

**Key implementation details:**
- Reuse the existing `UserService.create()` flow, but set `role = "cliente"` and `email = cliente.getCpf().getValue()` (CPF as login identifier)
- Generate random password via `SecureRandom` + Base64 encoding + hash via `BcryptPasswordHasher`
- Return generated password in `CreateUserResponse` only on creation (`@JsonInclude(Include.NON_NULL)`)
- `@RolesAllowed("cliente")` on `/orcamento/{id}/aprovar` and `/orcamento/{id}/reprovar`
- `@RequestScoped` on resources (keeps G8 compliance)
- Resource enforces: `os.getCliente().getUuid() == loggedInUser.getId()` (client sees only own OS's)

**Default password generation:**
```java
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int PASSWORD_LENGTH = 12;

    public static String generate() {
        byte[] bytes = new byte[PASSWORD_LENGTH];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```

### Pattern 5: Pagination Compliance (D-61..D-65)
**What:** All list endpoints follow a consistent pagination contract.

**When to use:** Every list endpoint (OS, peças, requisições, users, admin users).

**Contract:**
```
GET /api/v1/pecas?page=0&size=10&sort=nome&order=asc

Response:
{
  "content": [...],
  "page": 0,
  "size": 10,
  "totalElements": 42,
  "totalPages": 5
}
```
Default: `page=0, size=10, sort=dataCriacao, order=desc`
Max size: 50. Clamp to 50 if higher requested.

### Pattern 6: Soft Delete with Restore (D-48..D-53)
**What:** All new entities extend `BaseEntity` (which has `isActive`, `deletedAt`). Queries filter `isActive = true`. Restore endpoint: `PATCH /{entidade}/{uuid}/restore`.

**When to use:** Every new aggregate root entity.

**Protection rules:**
- Block soft delete of client with OS in non-final state (`status != ENTREGUE && status != CANCELADA`)
- Block soft delete of part referenced in pending OS
- Soft delete publishes `SoftDeleteEvent` via CDI
- Restore does NOT restore linked vehicles (D-52)

### Pattern 7: Audit Log for OS Transitions (D-66..D-69)
**What:** Separate `os_audit_log` table records every state transition. JSON snapshot of OS items at transition time.

**Key details:**
- Table: `os_audit_log` — no soft delete (immutable data)
- Fields: `id`, `os_uuid`, `status_origem`, `status_destino`, `usuario`, `observacao`, `snapshot_itens` (JSON), `created_at`
- All transitions logged (including automatic ones, with user = "sistema")
- Endpoint: `GET /api/v1/os/{uuid}/historico` — accessible to admin + mechanic

### Anti-Patterns to Avoid
- **setStatus() on OS entity:** Use explicit transition methods only (`aprovarOrcamento()`, `reprovarOrcamento()`, `CancelarPorSLA()`)
- **@Transactional on resource layer:** Transaction should be on service layer only
- **Mega-aggregate OS:** Never embed `Cliente`/`Veiculo`/`Orcamento` inside `OrdemDeServico` — reference by UUID
- **$BIGSERIAL in migrations:** Use `BIGINT GENERATED BY DEFAULT AS IDENTITY` (H2 compatibility)
- **Hand-rolled pagination:** Use Panache `Page.of()` + `Sort.by()` consistently
- **Putting SLA job logic in domain:** Scheduled job infrastructure belongs in infrastructure layer, not domain

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Scheduled job execution | Custom Timer thread | `quarkus-scheduler` with `@Scheduled` | Managed lifecycle, CDI integration, configurable disable, testing support |
| Stock race condition | Application-level synchronized block | Native SQL `UPDATE ... WHERE saldo >= qtd` | Database-level atomic operation, no cluster-aware issues, single statement |
| Password hashing | Custom hash algorithm | `BcryptPasswordHasher` (existing) | Already implemented and tested. Reuses Quarkus `BcryptUtil` |
| Event publishing | Kafka/RabbitMQ/JMS | CDI Events (`CdiEventPublisher`) | In-process, same transaction, zero infra. Adequate for this scale |
| REST API documentation | Hand-written OpenAPI YAML | `quarkus-smallrye-openapi` annotations | Auto-generated from source, always in sync, Swagger UI included |
| Code coverage enforcement | Manual review | `jacoco-maven-plugin` with `check` goal | Automated gating in `verify` phase, configurable thresholds |
| Vulnerability scanning | Manual CVE audit | `dependency-check-maven` | Automated NVD scanning, CI integration |
| Entity↔Domain mapping | Hand-written boilerplate | MapStruct with `componentModel = "cdi"` | Already established pattern. Ordem annotationProcessorPaths é crítica (G3) |

**Key insight:** The existing codebase already has battle-tested solutions for all cross-cutting concerns. Every new feature should plug into the existing infrastructure (CDI events, MapStruct CDI, BaseEntity, CacheNames, two-class repository) — not create new infrastructure.

## Common Pitfalls

### Pitfall 1: JPA Optimistic Locking Race on Stock Reservation
**What goes wrong:** Using `@Version` on `PecaEntity`, loading entity, checking `saldo >= qtd`, decrementing, and saving. Two concurrent approvals for the same part both pass the Java-level check, both commit — one overwrites the other's decrement.

**Why it happens:** JPA version increments on UPDATE, but the read+check+build happens outside the database's atomic scope.

**How to avoid:** Single-statement native SQL `UPDATE pecas SET saldo = saldo - :qtd WHERE uuid = :uuid AND saldo >= :qtd` with `executeUpdate()` returning the affected row count. If 0, throw `AppException(409)`.

**Warning signs:** Stock goes negative despite Java-level `if (saldo >= qtd)` checks.

### Pitfall 2: @Transactional on Scheduled Job + CDI Observer
**What goes wrong:** If `SlaExpiryJob` has `@Transactional` and the observer of `OSCanceladaEvent` also has `@Transactional`, the transaction propagates by default (REQUIRED). If the observer throws, the job's transaction rolls back.

**Why it happens:** CDI events are synchronous by default. Both publisher and observer run in the same transaction unless `TransactionPropagation.REQUIRES_NEW` is used.

**How to avoid:** Keep the synchronous pattern for stock reservation (correct: approval + reservation is one atomic unit). For the SLA job, the observer should be lightweight (just log the cancellation). No rollback-prone logic in observers.

### Pitfall 3: H2 Compatibility in Native SQL for Atomic Update
**What goes wrong:** Native SQL uses PostgreSQL-specific syntax that fails in H2 tests.

**How to avoid:** The `UPDATE ... SET saldo = saldo - :qtd WHERE saldo >= :qtd` pattern is standard SQL and works on both PostgreSQL and H2. Avoid `RETURNING`, `FOR UPDATE`, or `WITH` clauses. H2's `MODE=PostgreSQL` handles standard arithmetic UPDATEs correctly.

### Pitfall 4: Flyway Migration Naming for Phase 2
**What goes wrong:** Phase 1 occupies V1-V5 (original) + V6-V10 (Phase 1 entities). Phase 2 uses V11-V17. If Phase 1 and Phase 2 migrations are developed simultaneously, numbering collision can occur.

**How to avoid:** Phase 2 = V11-V17. Verify `db/migration/` directory before creating migrations. Do NOT renumber; Flyway applies in numeric order. If Phase 1 is incomplete, place Phase 2 migrations after whatever Phase 1's last migration is.

**Migration sequence for Phase 2:**
| Migration | Tables | Notes |
|-----------|--------|-------|
| `V11__create_clientes_table.sql` | `clientes` | Endereco flatten, CPF unique |
| `V12__create_veiculos_table.sql` | `veiculos` | Placa unique, FK logical to clientes |
| `V13__create_servico_tipos_table.sql` | `servico_tipos` | CHECK valor > 0 |
| `V14__create_pecas_table.sql` | `pecas` | Unidade enum, non-negative stock |
| `V15__create_requisicoes_compra_table.sql` | `requisicoes_compra` | Status, lot, supplier |
| `V16__create_nf_entradas_table.sql` | `nf_entradas` | FK to requisicoes_compra |
| `V17__create_os_audit_log_table.sql` | `os_audit_log` | JSON snapshot, no soft delete |

### Pitfall 5: Orcamento as Separate Aggregate — Transaction Boundaries
**What goes wrong:** Operations span both `OrdemDeServico` and `Orcamento` aggregates (e.g., approve budget → updates OS state + reserves stock). Two different `@Transactional` methods called sequentially — if the second fails, the first already committed.

**How to avoid:** Both operations are in the same `@Transactional` method `OrcamentoService.aprovar()`. The service coordinates both aggregate changes and publishes the event. Push changes to both aggregates within the same transaction boundary.

### Pitfall 6: JaCoCo quarkus-jacoco vs Stand-alone Plugin
**What goes wrong:** Using `quarkus-jacoco` extension AND `jacoco-maven-plugin` simultaneously causes "classes already instrumented" errors.

**How to avoid:** Use the **stand-alone** `jacoco-maven-plugin` (not `quarkus-jacoco` extension) for multi-module projects. Use Surefire system properties with `quarkus.jacoco.data-file` pointing to a shared location. Use `<argLine>@{argLine}</argLine>` in Surefire config.

### Pitfall 7: OWASP DC NVD API Key
**What goes wrong:** Dependency Check downloads NVD data on every build. Without an API key, NIST rate-limits anonymous access, causing builds to fail or data to be stale.

**How to avoid:** Require `NVD_API_KEY` environment variable. Document in README. The plugin has `<nvdApiKey>${env.NVD_API_KEY}</nvdApiKey>` config. Provide a setup step in docs.

### Pitfall 8: MapStruct annotationProcessorPaths in New Modules
**What goes wrong:** If new modules need MapStruct mappers and `annotationProcessorPaths` is not configured in their `pom.xml`, mappers compile but produce null fields at runtime.

**How to avoid:** Add `annotationProcessorPaths` with order Lombok → lombok-mapstruct-binding → mapstruct-processor in every module that uses MapStruct (same as `mekano-rest/pom.xml` lines 177-198).

## Code Examples

### Atomic Stock Reservation (Repository Layer)

```java
// Source: https://quarkus.io/guides/hibernate-orm-panache
// Pattern: native UPDATE via EntityManager within existing transaction

@ApplicationScoped
public class PecaRepositoryImpl implements PecaRepositoryPort {

    @Inject
    PecaPanacheRepository panacheRepository;

    @Transactional(Transactional.TxType.MANDATORY)
    @Override
    public void reservarEstoque(UUID pecaUuid, int quantidade) {
        int updated = panacheRepository.getEntityManager()
            .createNativeQuery(
                "UPDATE pecas SET saldo = saldo - :qtd " +
                "WHERE uuid = :uuid AND saldo >= :qtd AND is_active = true")
            .setParameter("qtd", quantidade)
            .setParameter("uuid", pecaUuid)
            .executeUpdate();

        if (updated == 0) {
            throw new AppException(409,
                "Saldo insuficiente para a peça ou peça não encontrada");
        }
    }

    @Override
    public void creditarEstoque(UUID pecaUuid, int quantidade) {
        panacheRepository.getEntityManager()
            .createNativeQuery(
                "UPDATE pecas SET saldo = saldo + :qtd " +
                "WHERE uuid = :uuid AND is_active = true")
            .setParameter("qtd", quantidade)
            .setParameter("uuid", pecaUuid)
            .executeUpdate();
    }
}
```

### SLA Expiry Job

```java
// Source: Quarkus Scheduler Reference Guide

@ApplicationScoped
public class SlaExpiryJob {

    private static final Logger LOG = Logger.getLogger(SlaExpiryJob.class);

    @Inject
    OrdemDeServicoRepositoryPort osRepository;

    @Scheduled(cron = "${sla.expiry.cron:0 0 */12 * * ?}",
               identity = "sla-expiry-job",
               concurrentExecution = ConcurrentExecution.SKIP)
    @Transactional
    void verificarExpiracaoSLA() {
        LOG.info("Verificando OS's com SLA expirado...");

        List<OrdemDeServico> expiradas =
            osRepository.findExpiradasEmAguardandoAprovacao(LocalDateTime.now());

        for (OrdemDeServico os : expiradas) {
            os.CancelarPorSLA();
            osRepository.save(os);
        }

        LOG.infof("%d OS(s) cancelada(s) por SLA expirado", expiradas.size());
    }
}
```

### CDI Event Observer for Stock Reservation

```java
@ApplicationScoped
public class PecaOrcamentoObserver {

    @Inject
    PecaRepositoryPort pecaRepository;

    @Inject
    RequisicaoCompraServicePort requisicaoService;

    void onOrcamentoAprovado(@Observes OrcamentoAprovadoEvent event) {
        for (ItemOrcamentoDTO item : event.itens()) {
            try {
                pecaRepository.reservarEstoque(item.pecaUuid(), item.quantidade());
            } catch (AppException e) {
                if (e.getStatus() == 409) {
                    // Stock insufficient — auto-generate purchase requisition
                    requisicaoService.criarParaOrcamento(
                        item.pecaUuid(), item.quantidade(), event.orcamentoUuid());
                } else {
                    throw e;
                }
            }
        }
    }
}
```

### OpenAPI Documentation Pattern

```java
// Source: https://quarkus.io/guides/openapi-swaggerui

@Path("/api/v1/pecas")
@RequestScoped
@RolesAllowed({"admin", "almoxarife"})
@Tag(name = "Peças", description = "Gestão de peças e insumos")
public class PecaResource {

    @GET
    @Operation(summary = "Listar peças",
               description = "Retorna lista paginada de peças disponíveis no estoque, " +
                             "com suporte a filtros e ordenação.")
    @APIResponse(responseCode = "200", description = "Lista de peças")
    public Response listar(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") @Max(50) int size,
            @QueryParam("sort") @DefaultValue("nome") String sort,
            @QueryParam("order") @DefaultValue("asc") String order) {
        // ... implementation
    }

    // DTO with Schema
    @Schema(description = "Requisição para cadastro de peça")
    public class PecaRequest {
        @Schema(description = "Nome da peça", example = "Filtro de óleo")
        @NotBlank String nome;

        @Schema(description = "Unidade de medida", example = "UN",
                allowableValues = {"UN", "KG", "L", "CX", "M", "PC"})
        @NotNull UnidadeMedida unidade;

        @Schema(description = "Saldo inicial", example = "10", minimum = "0")
        @Min(0) int saldoInicial;
    }
}
```

### JaCoCo 80% Gate Configuration

```xml
<!-- Source: https://quarkus.io/guides/tests-with-coverage -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
            <configuration>
                <exclClassLoaders>*QuarkusClassLoader</exclClassLoaders>
                <destFile>${project.build.directory}/jacoco-quarkus.exec</destFile>
                <append>true</append>
            </configuration>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <dataFile>${project.build.directory}/jacoco-quarkus.exec</dataFile>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
            <configuration>
                <dataFile>${project.build.directory}/jacoco-quarkus.exec</dataFile>
                <outputDirectory>${project.build.directory}/jacoco-report</outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### OWASP Dependency Check with Aggregate Report

```xml
<!-- Source: https://dependency-check.github.io/DependencyCheck/dependency-check-maven/ -->
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>12.2.2</version>
    <configuration>
        <format>ALL</format>
        <failBuildOnCVSS>11</failBuildOnCVSS>
        <nvdApiKey>${env.NVD_API_KEY}</nvdApiKey>
        <skipProvidedScope>false</skipProvidedScope>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `quarkus-jacoco` extension | Stand-alone `jacoco-maven-plugin` | Quarkus 3.3+ | Stand-alone plugin needed for multi-module aggregation and threshold enforcement |
| Quartz for scheduler | `quarkus-scheduler` (in-memory) | Always | For Phase 2, in-memory is sufficient. Quartz needed only for persistent/clustered jobs |
| JPA `@Version` for stock | Native SQL `UPDATE ... WHERE saldo >= qtd` | This phase | Native SQL provides true atomicity that JPA optimistic locking cannot guarantee for arithmetic operations |

**Deprecated/outdated:**
- `javax.persistence` vs `jakarta.persistence`: Quarkus 3.x uses `jakarta.*` namespace. All imports must use `jakarta.*`.
- `quarkus-resteasy-*` vs `quarkus-rest-*`: Quarkus 3.36 uses RESTEasy Reactive (`quarkus-rest-jackson`), not the deprecated resteasy-classic. Already using `quarkus-rest-jackson` in pom.xml.

## Runtime State Inventory

> Rename/refactor phases only. Phase 2 is greenfield extensions to existing code — no runtime state migration required.

| Category | Items Found | Action Required |
|----------|-------------|------------------|
| Stored data | None — Phase 2 adds new tables via Flyway V11-V17 | None (new tables) |
| Live service config | None — new config in application.properties | Add properties for SLA cron, expiry hours |
| OS-registered state | None — no OS-level registrations involved | None |
| Secrets/env vars | `NVD_API_KEY` needed for OWASP DC | Document in README. Add to CI secrets. |
| Build artifacts | None — JaCoCo and OWASP DC are standard Maven plugins | Add to pom.xml, no migration needed |

**Nothing found in category:** Verified — Phase 2 does not rename or migrate existing runtime state.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | `quarkus-scheduler` supports `@Transactional` on `@Scheduled` methods | SLA Expiry Job | Low — verified in Quarkus docs. Fallback: open transaction manually via `Panache.getEntityManager().getTransaction()` |
| A2 | Native SQL `UPDATE ... WHERE saldo >= :qtd` works on PostgreSQL and H2 | Atomic Stock Reservation | LOW — SQL standard. Verified H2 MODE=PostgreSQL handles this |
| A3 | Phase 1 completes V1-V10 Flyway migrations before Phase 2 | Migration numbering | MEDIUM — if Phase 1 not done, need to coordinate numbering. Verify `db/migration/` before creating V11-V17 |
| A4 | `entityManager` from PanacheRepository delegates to same TX | RepositoryImpl atomic update | MEDIUM — `@Transactional(MANDATORY)` ensures it fails at startup if wrong. Add integration test. |
| A5 | JaCoCo 80% LINE coverage gate requires stand-alone plugin not `quarkus-jacoco` extension | Build configuration | LOW — Quarkus docs confirm using both causes double-instrumentation. |
| A6 | Phase 1 has applied `StatusOS` with `aprovarOrcamento()` → `EM_EXECUCAO` | State machine integration | HIGH — INC-01 changes already documented as applied. Verify during Wave 2. |

## Open Questions (RESOLVED)

1. **[OQ-01] Flyway migration numbering conflict with Phase 1**
   - What we know: Phase 1 ocupa V1-V5 (originais) + V6-V10 (auth). Phase 2 começa em V11.
   - Recommendation: Phase 1 = V1-V10. Phase 2 = V11-V17. Both coexist in same directory — Flyway applies in numeric order. Verify `db/migration/` before creating.

2. **[OQ-02] Password generation UX for admin**
   - What we know: D-46 says system generates password on creation, shown once to admin.
   - Recommendation: `AdminCreateUserResponse` includes `generatedPassword` field. This field is `null` on all other endpoints. Use `@JsonInclude(Include.NON_NULL)`.

3. **[OQ-03] Average execution time endpoint scope**
   - What we know: OS-18 says admin can query average execution time per service type over a date range.
   - Recommendation: Only `FINALIZADA` OS's. Add validation in service layer.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 17 | All code | ✓ | 17+ | — |
| Maven 3.9+ | Build | ✓ | 3.9.15 (wrapper) | — |
| PostgreSQL 16 | Dev/Prod | ✓ (docker-compose) | 16-alpine | H2 for tests |
| Docker | PostgreSQL dev | ✓ | — | DevServices auto-provisions PG in tests |
| `NVD_API_KEY` | OWASP DC | ✗ | — | Build passes without it (CVSS 11 = never fail). Document as optional but recommended. |

**Missing dependencies with no fallback:** None
**Missing dependencies with fallback:** `NVD_API_KEY` — OWASP DC works anonymously with rate limits.

## Validation Architecture

> `workflow.nyquist_validation` is `true` in config.json — this section is included.

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Quarkus JUnit5 + Mockito + REST Assured + AssertJ |
| Config file | None — test profiles in application code |
| Quick run command | `./mvnw test -pl mekano-domain` (domain only, < 3s) |
| Full suite command | `./mvnw verify -pl mekano-rest -am` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| EST-03 | Atomic stock reservation decrements correctly | Integration | `mvn test -pl mekano-infrastructure -am -Dtest=PecaRepositoryImplTest` | ❌ Wave 0 |
| EST-03 | Race condition: concurrent approvals produce correct stock | Integration | `mvn test -pl mekano-infrastructure -am -Dtest=PecaConcurrentTest` | ❌ Wave 0 |
| OS-09 | Budget auto-generated from service items | Unit + REST | `mvn test -pl mekano-rest -am -Dtest=OrcamentoResourceTest` | ❌ Wave 0 |
| OS-10 | Client can approve budget (JWT with role cliente) | REST | `mvn test -pl mekano-rest -am -Dtest=OrcamentoResourceTest` | ❌ Wave 0 |
| OS-12 | SLA job cancels expired OS's | Integration | `mvn test -pl mekano-infrastructure -am -Dtest=SlaExpiryJobTest` | ❌ Wave 0 |
| AUTH-04 | Admin CRUD user operations | REST | `mvn test -pl mekano-rest -am -Dtest=AdminUserResourceTest` | ❌ Wave 0 |
| DOC-02 | OpenAPI spec generated at /q/openapi | Smoke | `curl http://localhost:8080/q/openapi \| grep -q "openapi"` | ❌ Wave 0 |
| D-73 | JaCoCo >= 80% LINE coverage | Build | `mvn verify -pl mekano-rest -am` (jacoco:check) | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./mvnw test -pl mekano-domain` (domain unit tests)
- **Per wave merge:** `./mvnw verify -pl mekano-rest -am`
- **Phase gate:** Full suite green + JaCoCo 80% gate + OWASP DC report before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `tests/PecaRepositoryImplTest.java` — covers EST-03 atomic reservation, concurrent access
- [ ] `tests/SlaExpiryJobTest.java` — covers OS-12 scheduled cancellation
- [ ] `tests/OrcamentoResourceTest.java` — covers OS-09, OS-10, OS-11
- [ ] `tests/OrdemDeServicoResourceTest.java` — covers extended OS states
- [ ] `tests/AdminUserResourceTest.java` — covers AUTH-04
- [ ] `tests/PecaResourceTest.java` — covers EST-01, EST-02
- [ ] JaCoCo plugin in `mekano-rest/pom.xml` — jacoco:check with 80% rule
- [ ] OWASP DC plugin in `mekano-rest/pom.xml` — dependency-check:check

## Security Domain

> `security_enforcement` is enabled in config.json — this section is required.

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | Yes | JWT Ed25519 (Phase 1 reuse). Role 'cliente' extends existing auth. Password generated, shown once, hashed via BCrypt. |
| V3 Session Management | Yes | Existing JWT access + refresh token from Phase 1. No change. |
| V4 Access Control | Yes | `@RolesAllowed` on all new endpoints. Client sees only own OS's (UUID filter). Admin separate endpoint. |
| V5 Input Validation | Yes | Bean Validation on all DTOs. Domain VOs validate in constructor. `@Valid` on resource parameters. |
| V6 Cryptography | Yes | BCrypt for passwords (existing). Ed25519/EdDSA for JWT (Phase 1). No hand-rolled crypto. |

### Known Threat Patterns for Quarkus REST + JPA
| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Stock overselling via race condition | Tampering | Native SQL `UPDATE ... WHERE saldo >= qtd` (atomic single-statement). CDI event-based side effects within same TX. |
| Resource enumeration via sequential IDs | Information Disclosure | Hybrid ID pattern: UUID public IDs exposed, Long PK internal. Always use UUID in URLs. |
| Unauthorized budget approval | Spoofing | `@RolesAllowed("cliente")` + `@RequestScoped` + UUID ownership filter: verify `os.cliente.uuid == loggedInUser.id` |
| JWT token reuse after logout | Repudiation | Phase 1 refresh rotation. No change needed for Phase 2. |
| SQL injection in native queries | Injection | Parameterized queries only (`:param` syntax). Never concatenate values. |
| Admin user enumeration | Information Disclosure | Same protection as Phase 1: generic error messages. 404 for not found, not "user exists". |

## Sources

### Primary (HIGH confidence)
- [Quarkus Scheduler Reference Guide](https://quarkus.io/guides/scheduler-reference) — `@Scheduled` cron/interval, `@Transactional` on scheduled methods, concurrent execution, identity, config
- [Quarkus OpenAPI & Swagger UI Guide](https://quarkus.io/guides/openapi-swaggerui) — `@Operation`, `@Schema`, `@APIResponse`, auto-add security, Swagger UI config
- [Quarkus Tests with Coverage Guide](https://quarkus.io/guides/tests-with-coverage) — JaCoCo integration, multi-module setup, threshold enforcement
- [OWASP Dependency Check Maven Plugin](https://dependency-check.github.io/DependencyCheck/dependency-check-maven/configuration.html) — Goals, formats, NVD API key, failBuildOnCVSS
- [Existing codebase files](..) — BaseEntity.java, CdiEventPublisher.java, CacheNames.java, UserRepositoryImpl.java, UserPanacheRepository.java, pom.xml files, `01-PATTERNS.md`
- `02-CONTEXT.md` — All 75 locked decisions (D-01 through D-75)

### Secondary (MEDIUM confidence)
- [Baeldung — Pessimistic Locking in JPA](https://www.baeldung.com/jpa-pessimistic-locking) — LockModeType patterns, though we use native SQL instead
- [Integrating OWASP DC into Maven Verify](https://medium.com/meetcyber/integrating-owasp-dependency-check-into-maven-verify-goal-69219ed364ab) — Practical Maven config for aggregate reports
- [Quarkus Scheduler Guide (intro)](https://quarkus.io/guides/scheduler) — Getting started, basic cron/every examples

### Tertiary (LOW confidence)
- None — all critical claims verified against official documentation or existing code

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — all libraries verified in official Quarkus docs and existing codebase
- Architecture: HIGH — patterns match existing codebase (BaseEntity, CDI events, two-class repository, MapStruct CDI)
- Pitfalls: HIGH — G1-G10 from CLAUDE.md, PLUS Phase-2-specific pitfalls verified against Quarkus docs
- Atomic stock reservation: HIGH — native SQL pattern documented in Hibernate ORM docs; alternative JPA approaches verified but not recommended

**Research date:** 2026-06-22
**Valid until:** 2026-07-22 (Quarkus 3.36 is stable; scheduler/openapi APIs are mature)
