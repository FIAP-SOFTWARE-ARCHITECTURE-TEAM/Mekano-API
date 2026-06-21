# Pitfalls Research

**Domain:** Mechanical Workshop Management System (Mekano)
**Researched:** 2026-06-20
**Confidence:** HIGH (multi-source verified)

## Critical Pitfalls

### Pitfall 1: Mega-Aggregate OS — Putting Everything Inside OrdemDeServico

**What goes wrong:**
The `OrdemDeServico` aggregate root absorbs `Cliente`, `Veiculo`, `Orcamento`, all `ItemOS` entries, and `PoliticaSLA` into a single monolithic aggregate. This creates:
- Transaction contention: every OS update locks the entire aggregate
- Performance degradation as OS history grows (hundreds of items per OS)
- Concurrent modification failures when mechanics, atendentes, and the SLA timer all touch the same OS simultaneously
- Painful loading: every OS query fetches the entire object graph

**Why it happens:**
Vaughn Vernon's "Aggregate Design" papers (DDDCommunity, 2011) identify this as the #1 DDD mistake: treating the aggregate as a "cluster" rather than a **consistency boundary**. Teams model the real-world concept of "ordem de serviço" as a single document because a physical OS paper form contains all this info. The Event Storming class diagram in this project shows `OrdemDeServico` with direct composition of `ItemOS` (0..\*), `Orcamento` (0..1), `Cliente` (1), and `Veiculo` (1) — this is the classic trap.

**How to avoid:**
- Split `OrdemDeServico` into **two aggregates**:
  - `OrdemDeServico` (AR) — identity, status, dates, references to ClienteId and VeiculoId (not the entities themselves)
  - `Orcamento` (separate AR with its own lifecycle and identity)
- `ItemOS` becomes a **value object collection** within `OrdemDeServico`, but only the subset needed for the current state (not all items ever)
- `Cliente` and `Veiculo` are separate aggregates referenced by ID only — never embedded
- Use eventual consistency: when `Orcamento` is approved, ORdemDeServico transitions via domain event, not within the same transaction

**Warning signs:**
- OS repository methods loading the entire graph for every operation
- Tests that need to construct deep object hierarchies to test simple status transitions
- Optimistic locking exceptions in production when two people work on the same OS
- `@Transactional` spans spanning multiple unrelated tables in a single transaction

**Phase to address:**
OS Foundation Phase (aggregate modeling session before writing code). Enforce with ArchUnit test: `classes().that().areAnnotatedWith(OrdemDeServico.class).should().not().dependOn(Cliente.class)`

---

### Pitfall 2: Incomplete State Machine — Missing Transitions and Illegal Flows

**What goes wrong:**
The OS lifecycle has 7 statuses (RECEBIDA → EM_DIAGNOSTICO → AGUARDANDO_APROVACAO → EM_EXECUCAO → FINALIZADA → ENTREGUE, with CANCELADA as dead end). Two specific failure modes:

1. **Missing edges**: Forgetting that OS can be cancelled from EM_DIAGNOSTICO (customer changes mind before orçamento) or that EM_EXECUCAO might need to go back to AGUARDANDO_APROVACAO if additional services are discovered mid-work (re-orçamento)
2. **Illegal transitions not enforced**: Letting a service call transition from RECEBIDA directly to FINALIZADA without going through the pipeline. Or allowing ENTREGUE → EM_EXECUCAO (re-opening delivered OS)
3. **No guard conditions**: `iniciarExecucao()` succeeds even when `orcamento` is not approved, or `finalizar()` succeeds when not all required `ItemOS` are marked complete
4. **Orcamento and OS status coupling**: The `Orcamento` (separate entity) has its own status (GERADO, ENVIADO, APROVADO, REPROVADO, EXPIRADO). When these get out of sync with the OS status, the system enters an unrecoverable state (e.g., orçamento = APROVADO but OS = AGUARDANDO_APROVACAO)

**Why it happens:**
State machines look simple on paper (7 states, ~10 transitions) but the combinatorial explosion of edge cases is deceptive. Teams implement status as a simple enum field + setters, then add `if/else` guards incrementally. Each guard is tested individually, but the **matrix** of all possible transitions is never exhaustively tested. The `Orcamento` entity sharing lifecycle with OS means changes can happen in one without the other.

**How to avoid:**
- Implement a **transition matrix** as a single source of truth:
```java
enum StatusOS {
    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    APROVADO,  // split this from AGUARDANDO_APROVACAO
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE,
    CANCELADA;

    private static final Map<StatusOS, Set<StatusOS>> ALLOWED_TRANSITIONS = Map.of(
        RECEBIDA, Set.of(EM_DIAGNOSTICO, CANCELADA),
        EM_DIAGNOSTICO, Set.of(AGUARDANDO_APROVACAO, CANCELADA),
        AGUARDANDO_APROVACAO, Set.of(APROVADO, CANCELADA),
        APROVADO, Set.of(EM_EXECUCAO, CANCELADA),
        EM_EXECUCAO, Set.of(FINALIZADA, AGUARDANDO_APROVACAO), // re-orçamento
        FINALIZADA, Set.of(ENTREGUE),
        ENTREGUE, Set.of(), // terminal
        CANCELADA, Set.of() // terminal
    );

    public boolean canTransitionTo(StatusOS target) {
        return ALLOWED_TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }
}
```
- Write a single **parameterized test** that validates EVERY possible transition in the matrix (7×7 = 49 cases, trivially covered)
- Keep `Orcamento` status as a separate concern — OS status should derive from the aggregate's own state, not duplicate the orçamento status. When orçamento expires, a domain event handler transitions OS independently
- Make `StatusOS` a value object on the aggregate, not a simple String — guard transitions in the domain entity, not in the service layer

**Warning signs:**
- `if (status == RECEBIDA)` scattered across Services instead of centralized
- Switch statements without `default` that throws for illegal states
- Integration tests that test "happy path" status transitions but never test illegal transition rejection (400 response)
- Two boolean fields that together encode 4 states but don't map cleanly to the domain

**Phase to address:**
OS Foundation Phase — build transition matrix before implementing any Service.

---

### Pitfall 3: Inventory Reservation Race Conditions (Double-Reservation / Overselling)

**What goes wrong:**
Two OS are approved simultaneously for the same rare part. Both pass the `if (estoque.saldoAtual >= quantidade)` check because both read `saldoAtual = 5` before either writes. Result: both OS reserve the same 3 units, committing 6 when only 5 exist. The system allows negative stock — or worse, silently oversells.

**Why it happens:**
The "read-check-write" pattern (read stock → check if enough → decrement) is inherently non-atomic. In a monolith with Panache and implicit optimistic locking, two threads can read the same version of `ItemEstoque`, both pass the check, then both write. The second write succeeds because Hibernate's optimistic locking operates at the `ItemEstoque` entity level, but the business logic check was done on stale data. NET: two transactions both see `saldoAtual >= quantidade`, both decrement, and both commit if they loaded the entity with the same version.

**How to avoid:**
- Use **atomic database-level operations** for stock deduction, not application-level read-check-write:
```sql
UPDATE item_estoque 
SET saldo_atual = saldo_atual - :quantidade, 
    version = version + 1 
WHERE id = :itemId 
  AND saldo_atual >= :quantidade
  AND version = :expectedVersion
```
If `rowCount == 0`, the reservation failed — roll back the entire OS approval transaction.
- In JPA/Hibernate: use `@Version` on `ItemEstoque` and perform the update with a **pessimistic lock** (`LockModeType.PESSIMISTIC_WRITE`) on the specific `ItemEstoque` entities within the reservation transaction. This serializes access to that specific SKU.
- Keep reservations as a simple **flag** model: `reservas` table with OS ID, item ID, quantity, status. The authoritative stock calculation is always `saldoAtual - SUM(quantidade WHERE status = 'ATIVA')`, computed at query time or through a materialized view.
- Use `SERIALIZABLE` isolation level for the reservation transaction only — forces the DB to detect conflicting read/write sets.

**Warning signs:**
- `ItemEstoque.saldoAtual` being decremented in application code after `findById()` instead of in a single JPQL `UPDATE SET saldo = saldo - :qtd WHERE saldo >= :qtd`
- No `@Version` column on `ItemEstoque` entity
- Unit tests that never test concurrent reservation (race conditions are invisible in sequential tests)
- `saldoAtual` can go negative

**Phase to address:**
Estoque Foundation Phase. Must include a concurrent test that fires 10 parallel reservation requests for the same item and asserts exactly N reservations succeed (where N = available stock).

**Confidence:** HIGH (well-documented distributed systems race condition pattern — sources: O'Reilly "Designing Data-Intensive Applications", Vaughn Vernon IDDD)

---

### Pitfall 4: Payment Confirmation Webhook — Idempotency and Race with OS Status

**What goes wrong:**
The system calls an external bank service (Pagamento context) to process payment. When the bank confirms (webhook callback or polling), multiple failure modes emerge:

1. **Duplicate webhook**: Bank sends the same "payment confirmed" notification twice (common in production). First call updates OS to ENTREGUE. Second call throws 500 because OS is already ENTREGUE — or worse, tries to double-deduct inventory
2. **Timing race**: "Finalizar OS" (OS→FINALIZADA) and "Payment confirmed callback" arrive simultaneously. The payment confirms before cobranca is emitted. The cobrança emission sees OS=FINALIZADA but Pagamento status is already CONFIRMADO (inconsistent)
3. **Payment confirmed for wrong OS**: Bank callback doesn't carry the OS ID explicitly, only a transaction reference. If the mapping is stored in a local table and two payments for different OS process concurrently, the callback can match the wrong OS
4. **Partial confirmations**: Pix or boleto payments sometimes arrive as partial amounts (bank fees deducted, installments). System assumes full payment or nothing, creating reconciliation nightmares

**Why it happens:**
Payment integration is treated as a simple synchronous call rather than an asynchronous distributed workflow. The architecture assumes "request → response" when real bank integrations are "request → (hours/days later) → callback with webhook". The transaction boundary is unclear: does payment confirmation belong in the OS aggregate or the Pagamento aggregate?

**How to avoid:**
- **Idempotency key on payment callback**: Each Cobranca has a UUID `idempotencyKey`. The bank returns this key in the callback. Before processing any callback, check if this key was already processed. If yes, return 200 OK (idempotent replay) without side effects.
```java
// Payment confirmation handler
@Transactional
public void handlePaymentConfirmation(PaymentConfirmationEvent event) {
    if (processedEvents.exists(event.idempotencyKey())) {
        return; // Already processed, idempotent
    }
    OrdemDePagamento pagamento = pagamentoRepository.findByCobrancaId(event.cobrancaId());
    pagamento.confirmar(event.valor(), event.dataConfirmacao());
    // The pagamento aggregate emits PagamentoConfirmadoEvent
    // which the OS bounded context consumes asynchronously
    processedEvents.record(event.idempotencyKey());
}
```
- **Async boundary between Pagamento and OS**: `Pagamento.confirmar()` emits a `PagamentoConfirmadoEvent`. A separate handler (in the infrastructure event layer) listens for this event and calls `OrdemDeServico.registrarEntrega()`. This decouples the transaction — payment confirmation doesn't need OS to be in the same transaction.
- **Valor validation**: Always validate that `event.valor() >= pagamento.valorTotal()`. If partial, flag the payment as `PARCIAL` and require manual reconciliation (do not release the vehicle).
- **Reference mapping**: Store the external transaction reference in the Cobranca entity. Always query by `cobrancaId` (our domain ID) not the external reference.
- **Timeout + fallback**: If payment is not confirmed within 7 days (or defined SLA), emit `PagamentoVencidoEvent` and notify admin.

**Warning signs:**
- Payment handler directly calls `ordemDeServico.entregar()` in the same transaction
- No `processed_events` or idempotency table
- Payment endpoint is HTTP POST that mutates state without checking for duplicates
- No validation comparing payment amount to cobranca amount

**Phase to address:**
Pagamento Foundation Phase. Design the async event flow between Pagamento and OS before implementing any endpoint.

---

### Pitfall 5: Concurrent Status Writes on the Same OS (Atendente + Mecânico + SLA Timer)

**What goes wrong:**
Three actors can mutate the same OS concurrently:
- **Atendente** calls `iniciarDiagnostico()` (RECEBIDA → EM_DIAGNOSTICO)
- **SLA timer** (scheduled job) checks if orçamento expired and calls `cancelar()` (AGUARDANDO_APROVACAO → CANCELADA)
- **Mecânico** calls `incluirServicosInsumos()` while diagnosing

Without proper locking, the following scenario corrupts state: Atendente transitions OS to EM_DIAGNOSTICO while SLA timer simultaneously tries to cancel the same OS. Both read `RECEBIDA`, both pass their guard conditions, both write. The last writer wins — OS ends up in CANCELADA even though the atendente already started diagnosis.

**Why it happens:**
The team assumes that because it's a monolith, "transactions will serialize." But `@Transactional` with default `READ_COMMITTED` isolation does NOT prevent the lost update problem. Two transactions can both read the same version of OS, pass their guards, and commit — last writer wins.

**How to avoid:**
- **Optimistic locking with `@Version`**: Add a `version` column to the OS table. Hibernate's `@Version` forces `UPDATE ... WHERE id = ? AND version = ?`. If the version has changed since the aggregate was loaded, the transaction throws `OptimisticLockException` → catch and retry or return 409 Conflict.
- **Decompose OS operations into fine-grained commands**: Each status transition should be an explicit method on the aggregate root that takes a lock on the aggregate. Do NOT use generic `atualizarStatus(novoStatus)` — this hides the intent and bypasses business logic.
- **SLA timer design**: Instead of a batch job that scans for expired OS, use a **scheduled deadline** stored on the aggregate (`dataExpiracaoOrcamento`). The SLA expiration check should only succeed if the OS is still in AGUARDANDO_APROVACAO:
```java
public void cancelarPorExpiracaoSLA() {
    if (this.status != AGUARDANDO_APROVACAO) {
        throw new IllegalStateException("Só pode cancelar OS em aguardo de aprovação");
    }
    if (LocalDateTime.now().isBefore(this.dataExpiracaoOrcamento)) {
        throw new IllegalStateException("SLA ainda não expirou");
    }
    this.status = CANCELADA;
    // register event
}
```

**Warning signs:**
- OS entity does not have `@Version` column
- Generic `setStatus()` or `atualizarStatus()` method instead of explicit business methods
- Scheduled job that does bulk `UPDATE ordem_servico SET status = 'CANCELADA'` without per-row guard checks
- No tests for concurrent status transitions

**Phase to address:**
OS Foundation Phase (include `@Version` and explicit transition methods in the first OS implementation).

---

### Pitfall 6: Brazilian Document Validation — Regex-Only CPF/CNPJ and the July 2026 Alphanumeric CNPJ

**What goes wrong:**
The system validates CPF and CNPJ using:
1. **Regex only** (e.g., `\d{11}` or `\d{14}`) — accepts "000.000.000-00" (all zeros, valid format but invalid document) and rejects valid documents with formatting
2. **Check-digit algorithm incorrect** — CPF uses modulo 11 with specific weights; copying from the internet often gets the weight sequence wrong, especially for CNPJ's second check digit
3. **Alphanumeric CNPJ ignored** — Instrução Normativa RFB 2.229 (October 2024) mandates alphanumeric CNPJ from **July 2026**. New CNPJs will contain letters A-Z in the first 12 positions. Any validator using `\d{14}` will silently reject valid CNPJs from July 2026 onward. The existing codebase is being built during the transition period.
4. **CNPJ and CPF mixed up** — The `Cliente` entity allows either CPF or CNPJ. The system tries to validate both fields, or requires both, or doesn't enforce that at least one must be present.

**Why it happens:**
Brazilian document validation is deceptively simple — the modulo 11 algorithm is well-documented but the implementation details (weight factors, digit calculation for 10/11 remainder = 0) have edge cases. Most developers copy-paste from StackOverflow or GitHub gists without testing edge cases. The alphanumeric CNPJ change is recent (announced October 2024, effective July 2026) and many libraries haven't updated. The project documentation (`MEKANO_DOCUMENTATION.md` RF01) says "validar formato de CPF e CNPJ" but doesn't specify alphanumeric.

**How to avoid:**
- **Use a maintained library, not custom code**: For Java, use `io.github.felseje:cpf-cnpj-utils` (v1.0.0-alpha supports alphanumeric CNPJ, BSD 3-Clause, available on Maven Central). Alternatively, implement the algorithm as a Value Object in the domain layer with full test coverage.
- **Implement CPF/CNPJ as a single `Documento` value object** that detects the type based on length (11=CPF, 14=CNPJ) and validates check digits using the Módulo 11 algorithm. Include alphanumeric mapping: letters A-Z map to values 17-42 before the weighted sum (ASCII value - 48).
- **Accept formatted input**: Allow dots, dashes, and slashes (`123.456.789-09`, `11.222.333/0001-81`). Strip formatting before validation. Store normalized (digits/letters only).
- **Edge case tests**:
  - All same digits (000.000.000-00) → INVALID
  - Valid CPF with known check digits → VALID
  - CPF with swapped digits → INVALID
  - Alphanumeric CNPJ (e.g., `12.ABC.345/01DE-35`) → VALID (from July 2026)
  - CNPJ with formatting → VALID

**Warning signs:**
- Validation regex: `\d{11}` or `\d{14}` (will break July 2026)
- No test for "all same digit" edge case
- `Long.parseLong()` on CNPJ without catching `NumberFormatException` (will throw July 2026)
- CPF validation in the infrastructure layer instead of domain value object
- No `Documento` value object — CPF and CNPJ stored as raw strings everywhere

**Phase to address:**
OS Foundation Phase (Cliente aggregate includes Documento value object). Create and test the Documento VO before any Service that accepts CPF/CNPJ.

---

### Pitfall 7: Mercosul License Plate — Only Supporting One Format

**What goes wrong:**
Brazil has two active license plate formats:
- **Old format** (pre-2018): `ABC-1234` (3 letters + hyphen + 4 digits)
- **Mercosul format** (2018+): `ABC1D23` (3 letters + 1 digit + 1 letter + 2 digits)

The system only supports the old format. When a user enters a Mercosul plate, validation fails. Or the system only supports Mercosul, rejecting older vehicles still in circulation. The regex `[A-Z]{3}-\d{4}` matches neither format fully.

**Why it happens:**
The migration to Mercosul plates is gradual — new vehicles get Mercosul, existing vehicles keep old format until re-registration. Both formats are valid and will coexist for years. Developers often design for "the new standard" and forget the transition period.

**How to avoid:**
- Create a `PlacaVeiculo` value object that accepts both formats:
  - Old: `[A-Z]{3}-\d{4}` (or `[A-Z]{3}\d{4}` without hyphen)
  - Mercosul: `[A-Z]{3}\d[A-Z]\d{2}` (e.g., ABC1D23)
- Normalize to **uppercase without formatting** for storage — always store as `ABC1234` (old) or `ABC1D23` (Mercosul)
- Validate that the plate is unique (requirement RF02: "Placa deve ser única no sistema")
- Combined regex: `^[A-Z]{3}[-\s]?\d{4}$|^[A-Z]{3}\d[A-Z]\d{2}$`
- Test data should include both format examples

**Warning signs:**
- Plate regex only matches old format (`ABC-1234`)
- Plate regex only matches Mercosul format (`ABC1D23`)
- No normalization before uniqueness check — `ABC-1234` and `ABC1234` treated as different plates
- Plate stored with hyphen or formatting in database

**Phase to address:**
OS Foundation Phase (Veiculo aggregate includes PlacaVeiculo value object).

---

### Pitfall 8: Quarkus Multi-Module CDI Failures (Compile-OK, Runtime-Break)

**What goes wrong:**
The application compiles and starts in dev mode, but tests fail or the production build produces `UnsatisfiedResolutionException` or `ClassNotFoundException`. Examples:
- Beans in `mekano-application` not discovered by CDI in `mekano-rest`
- MapStruct mappers returning null fields at runtime because annotation processor order is wrong
- Jandex index not generated for new modules
- `@ApplicationScoped` on REST resources with JWT injection causes ClassCastException with `_ClientProxy`

**Why it happens:**
Quarkus uses build-time CDI bytecode processing, not runtime classpath scanning. This means:
1. Classes in non-root modules are invisible unless indexed by Jandex
2. Annotation processor order in `pom.xml` is strict: Lombok → MapStruct binding → MapStruct processor
3. `@ApplicationScoped` creates a client proxy; JWT claims injection requires `@RequestScoped` or the proxy fails class cast
The project already has documented gotchas (G1-G10 in CLAUDE.md) but these are easy to reintroduce when adding the 3 new contexts.

**How to avoid:**
- **Verify Jandex on every module**: Run `mvn compile` and check that `target/classes/META-INF/jandex.idx` exists in `mekano-application`, `mekano-infrastructure`, and `mekano-rest`. Add `mekano-domain` if it contains CDI producers.
- **ArchUnit test**: Write a test that asserts every module (except domain) has a `jandex.idx` file:
```java
@Test
void allModulesExceptDomainShouldHaveJandex() {
    assertThat(Files.exists(Paths.get("mekano-application/target/classes/META-INF/jandex.idx"))).isTrue();
    // ...
}
```
- **Resource scoping**: Verify ALL new JAX-RS resources use `@RequestScoped`, not `@ApplicationScoped`. Enforce with ArchUnit: `classes().that().areAnnotatedWith(Path.class).should().notBeAnnotatedWith(ApplicationScoped.class)`
- **Annotation processor order**: When adding MapStruct mappers for new entities (e.g., `OrdemDeServicoEntity`, `ItemEstoqueEntity`), verify the `<annotationProcessorPaths>` order in the POM matches the required sequence.
- **Integration test in CI**: At least one `@QuarkusTest` that loads the full application context (all beans) and verifies all injections resolve. A simple "GET /health" test suffices.

**Warning signs:**
- `mvn compile quarkus:dev` works but `mvn package` fails
- New module added without `jandex-maven-plugin`
- Resource classes annotated `@ApplicationScoped` instead of `@RequestScoped`
- MapStruct mappers not placed in the `infrastructure` module alongside JPA entities

**Phase to address:**
OS Foundation Phase (initial build setup verification). Must be caught before any business logic is written.

**Confidence:** HIGH (confirmed by Quarkus documentation, StackOverflow multi-module CDI issues \#55513502, and project's own G1-G10 gotcha list)

---

### Pitfall 9: Deadline Rush — Building All 3 Contexts in Parallel Without Vertical Slicing

**What goes wrong:**
With 10 days and 5 developers, the team splits into 3 groups (OS, Estoque, Pagamento) and works in parallel from day 1. By day 8:
- OS context: 70% done but has no items/peças working because Estoque API is not ready
- Estoque context: 80% done but has no way to test reservation because OS approval (which triggers reservation) isn't fully implemented
- Pagamento context: 60% done, cobrança endpoint works but OS → Pagamento integration is incomplete
- Integration is 0% — nothing connects, no end-to-end flow works for demo
- The last 2 days are a frantic integration scramble producing fragile, untested code

**Why it happens:**
"Divide and conquer" seems logical for 5 people in 10 days. But in a Clean Architecture monolith, the **integration points** between contexts (domain events, transaction boundaries, shared repositories) are where 80% of the complexity lives. Building contexts in isolation defers the hardest problems to the end. The team optimizes for parallel work ("everyone has something to do") instead of optimizing for **delivered value** ("a single working flow end-to-end").

**How to avoid:**
- **Vertical slice strategy**: Deliver ONE complete flow from start to finish, then iterate:
  - **Slice 1 (Days 1-4)**: Full OS lifecycle for a simple service-only OS (no parts). Recebida → Em Diagnóstico → Aguardando Aprovação → Aprovado auto → Em Execução → Finalizada → Entregue. No estoque, no pagamento real (auto-confirm).
  - **Slice 2 (Days 5-7)**: Add Estoque (reservation on approval, deduction on execução start, NF-e registration).
  - **Slice 3 (Days 8-10)**: Add Pagamento (cobrança emission on finalização, payment confirmation, integration with simulated bank).
- **Team allocation**: First 2 days: ALL 5 developers work on OS context to establish patterns. Then split: 3 people continue OS improvements, 2 people start Estoque. On day 5, the OS people are familiar enough to guide Estoque integration, share patterns, and write the integration tests.
- **Integration-first testing**: Write the integration contract (domain events, interfaces) BEFORE implementing either side. "Orçamento Aprovado" event is the contract between OS and Estoque — define its shape on day 1, implement both sides on day 3-7.
- **Daily working demo**: Every day at 5pm, the system should be deployable and demonstrate something. If no new flow works end-to-end by day 4, the parallel plan is failing — regroup.

**Warning signs:**
- Task assignments split strictly by bounded context on day 1
- No end-to-end integration test passing by day 4
- "We'll integrate everything on the last 2 days" attitude
- PRs that only touch a single module (rest, application, OR infrastructure — but not all three for a single feature)
- Shared domain events are designed independently by each subteam without a sync session

**Phase to address:**
Project Planning (before any coding). The roadmap must define vertical slices, not horizontal layers.

---

### Pitfall 10: Soft Delete and Unique Constraints — Inconsistent Behavior

**What goes wrong:**
The project uses soft delete (`isActive` + `deletedAt`) on entities. When a user soft-deletes a `Cliente` and then tries to register a new client with the same CPF, the unique constraint on `cpf` in the database fires — the new registration fails with a 500 error instead of being allowed (because the old record's CPF is still in the table, just soft-deleted).

**Why it happens:**
Soft delete and unique constraints are fundamentally in tension. The entity is "logically deleted" but the database row still exists. The constraint was designed for the logical model ("CPF must be unique") but implemented on the physical model where deleted rows still exist.

**How to avoid:**
- **Composite unique constraint**: Make the unique constraint include `isActive`:
```sql
CREATE UNIQUE INDEX idx_cliente_cpf_active ON cliente(cpf) WHERE is_active = true;
```
This is a **partial index** — only rows with `is_active = true` participate in uniqueness. Deleted rows are ignored.
- **Application-level check**: If partial indexes aren't available (or not portable), the application must do a soft-delete-aware uniqueness check before persisting:
```java
if (clienteRepository.existsByCpfAndIsActive(cpf, true)) {
    throw new AppException(409, "CPF já cadastrado");
}
```
But this has a race condition (see Pitfall 3) — use a partial unique index as the source of truth.
- Apply this pattern consistently: `Veiculo` plate uniqueness, `User` email uniqueness (already using soft delete on User).

**Warning signs:**
- Unique indexes on columns without `WHERE is_active = true` filter
- Unit tests that soft-delete then re-create the same natural key — and fail with constraint violation
- `@Column(unique = true)` annotation on JPA entity instead of manual schema migration

**Phase to address:**
OS Foundation Phase (database schema design). Review all Flyway migrations for partial unique index usage.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Generic `atualizarStatus()` instead of explicit business methods | Faster to write OS transitions | Opaque state machine, no audit trail of "what triggered this transition" | NEVER — explicit business methods are the same LOC count and provide domain clarity |
| Skipping `@Version` on aggregates because "it's a monolith" | Saves 1 annotation per entity | Lost update bugs in production as soon as any concurrent access occurs | NEVER — Hibernate `@Version` is one line per entity and prevents a whole class of bugs |
| CPF/CNPJ validation as static utility method in infrastructure | Fast to implement | Can't be enforced in domain layer; easy to forget validation in new endpoints; hard to test | NEVER — must be a domain Value Object (Documento) |
| No domain events between contexts (direct service call) | Simpler initial code | Tight coupling: OS calls Estoque directly, changes in one force changes in the other | MVP only — but must refactor to events by the end of the 10-day sprint |
| Storing monetary values as `Double` | Faster to code | Floating-point rounding errors in invoice totals, NF-e values (1.99 becomes 1.9899999) | NEVER — use `BigDecimal` or `long` (cents) for all monetary amounts |
| Single `application.properties` for all contexts | One file to manage | Merge conflicts, hard to find context-specific config, accidental cross-module config leaks | Acceptable for MVP (10 days) but must split by context in a fast-follow |
| Sharing the same DB schema for `domain` entities and `infrastructure` entities (no mapping) | No MapStruct mapper to write | Domain model leaks JPA annotations; infrastructure changes force domain changes | NEVER — the project already has a clean separation pattern (User ↔ UserEntity); maintain it |

---

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| **OS → Estoque (reservar peças ao aprovar orçamento)** | OS directly calls Estoque service synchronously in the same `@Transactional` | OS publishes `OrcamentoAprovadoEvent` via EventPublisher. Estoque handler listens, reserves parts, publishes `ReservaEfeturadaEvent` or `EstoqueInsuficienteEvent`. OS reacts to event. |
| **OS → Pagamento (emitir cobrança ao finalizar)** | Pagamento endpoint called directly from OS Service | OS → FINALIZADA triggers `OSTerminadaEvent`. Pagamento handler emits cobrança. Pagamento doesn't need to know about OS internals. |
| **Mock/ServicoBancario (payment gateway)** | No retry logic; assumes bank always responds in < 1 second | Implement `@Retry`, `@Timeout`, and a simulated delay (2-5s) in the mock. Test with network failures. |
| **Nota Fiscal de Entrada** | Trying to validate NF-e XML locally against SEFAZ | Validate NF-e by verifying the SEFAZ authorization protocol (chave de acesso + protocolo). Do NOT re-implement SEFAZ validation. |
| **Flyway migrations across contexts** | One monolithic migration file per version (e.g., `V5__create_all_tables.sql`) | One migration per table/feature. Use naming: `V5__create_ordem_servico.sql`, `V6__create_item_os.sql`, `V7__create_estoque.sql`. Makes rollbacks and understanding history possible. |

---

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| **OS aggregate loads ALL items every time** | OS detail endpoint takes >500ms for OS with 50+ items | Add pagination for items within OS; load only active items when status list is requested; separate "OS summary" query from "OS detail" query | ~200 items per OS |
| **Estoque saldo calculated by SUM over reservas table** | Stock check takes >100ms for high-turnover parts | Use a materialized or cached saldo_atual column, updated via DB trigger or application event; recalculate periodically | ~10,000 reservas per item |
| **Soft-delete filter on every query** | `WHERE is_active = true` added to every query, even when not needed | Use separate tables or schemas for historical data; add index on `(is_active, id)` combination; Hibernate `@Where` annotation | General performance degradation; breakpoint is DB-specific and query-pattern-specific |
| **No caching on Veiculo/Cliente lookups** | Repeated DB calls for the same customer/vehicle data in a single request | Use Caffeine cache (already configured for User — extend pattern to Cliente and Veiculo) with `@CacheResult` | >3 repeated lookups per request (this project won't hit scale issues; this is about response time consistency) |
| **NF-e entry scanning entire estoque** | After registering a NF-e, the system recalculates all stock alerts | Trigger stock alert check only for items that were in the NF-e, not the entire catalog | ~1000+ items in inventory |

Note: For a workshop-scale system (1-5 mechanics, <100 OS/month), most performance traps won't manifest. The traps above are listed for awareness when the system scales. Do NOT pre-optimize — but DO use patterns that don't paint you into a corner (pagination, selective loading, proper indexing).

---

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| **Exposing OS public endpoint without rate limiting** | Malicious client can enumerate OS by UUID (even UUIDs are guessable if using sequential or time-based UUID v1) | Use UUID v4 (random) for public OS identifiers. Implement rate limiting at the HTTP level via Quarkus filter or reverse proxy. |
| **Returning `Cliente` CPF/CNPJ in public OS status response** | Customer privacy leak — anyone with the OS number can see the customer's CPF | Mask CPF/CNPJ in public endpoints: show only last 3 digits (e.g., `***.123.456-**`). Expose full document only on authenticated admin endpoints. |
| **SLA timer job with broad update scope** | SQL injection or mass-update failure corrupts all OS | Each SLA expiration check should be a scoped query: `SELECT FROM ordem_servico WHERE status = 'AGUARDANDO_APROVACAO' AND data_expiracao < NOW()`. Update one by one within `@Transactional` with version check. |
| **Payment webhook unauthenticated** | Anyone can call the payment callback endpoint and change OS status | Protect the webhook endpoint with HMAC signature verification (shared secret between our system and the bank service). For the mock, use a static token in the request header. |
| **Soft delete revealing deleted records** | Deleted clients/vehicles could be queried via direct API calls | Ensure ALL repository methods include `is_active = true` filter by default. Do NOT expose a "list deleted" endpoint without explicit admin authentication. |
| **No input sanitization on Veiculo placa** | Cross-site scripting (XSS) if placa is later rendered in a management UI | Validate placa format strictly (only letters and digits in uppercase). Strip any HTML/special characters. Already mitigated by regex validation. |

---

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| **Orçamento approval flow has no estimated completion time** | Customer can't decide because they don't know when the car will be ready | Include estimated completion date/time in the orçamento response. Calculate based on service duration estimates. |
| **OS status doesn't communicate nuance** | Customer sees "Em Execução" but doesn't know if it's 10% or 90% done | Add optional percent-complete to OS status response (managed by mechanic). Even a rough indicator helps. |
| **Error messages in English** | Brazilian mechanics don't read English | All error messages in the RFC 7807 Problem Details response must be in Brazilian Portuguese. The project uses `Messages` resource bundle — maintain it. |
| **API returns 500 instead of 400 for domain validation** | Client can't fix their request because the error is opaque | Always throw `AppException` with specific status code and message. The existing `ApiExceptionMapper` handles this — use it consistently. |
| **Orçamento values shown without discounts** | Customer sees the raw total and rejects because it's too expensive | Consider adding optional "discount" field to OS (percentage or fixed amount) for negotiation. This is a differentiator, not MVP. |

---

## "Looks Done But Isn't" Checklist

- [ ] **OS Status Transitions**: Verify ALL 49 transitions in the matrix — not just the happy path. Test that `CANCELADA` cannot transition to anything. Test that `ENTREGUE` cannot be re-opened.
- [ ] **CPF/CNPJ Validation**: Test with all-zero, valid known numbers, alphanumeric CNPJ (from July 2026), formatted with mask, unformatted. Test that `Long.parseLong()` is NOT used.
- [ ] **Mercosul Plate**: Test with old format (`ABC-1234`), Mercosul format (`ABC1D23`), lowercase input (`abc1d23`), with/without hyphen. Normalize all to uppercase without separators.
- [ ] **Inventory Reservation**: Run concurrent test: 10 parallel requests for the same single-unit item. Assert exactly 1 succeeds, 9 fail. Then assert the item is reserved for the successful OS.
- [ ] **Payment Confirmation Idempotency**: Send the same webhook twice. Assert payment processed once. Assert OS delivered once. Assert no exception on second call.
- [ ] **Soft Delete + Unique**: Soft-delete a Cliente, then create a new Cliente with the same CPF. Must succeed. Delete a Veiculo, create with same plate. Must succeed.
- [ ] **Event Flow End-to-End**: Create OS with parts → approve orçamento → verify reservation in estoque → finalize OS → verify cobrança emitted → confirm payment → verify OS becomes ENTREGUE. All in one integration test.
- [ ] **Cache Invalidation**: Create user, query user (cache hit), update user, query again (must return updated data, not stale cache). Extend same pattern to Cliente, Veiculo, Estoque.
- [ ] **SLA Expiration**: Create OS with approval deadline 1 minute in the future. Wait 61 seconds. Assert SLA timer cancels the OS. Assert no double-cancel if timer runs again.
- [ ] **Jandex Index**: After `mvn clean compile`, verify `jandex.idx` exists in `application`, `infrastructure`, and `rest` modules.

---

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| OS aggregate too large | HIGH (requires schema migration, data migration, API versioning) | 1. Identify which sub-entities are accessed independently. 2. Split into separate aggregates (Orcamento gets its own repository). 3. Add migration script to backfill references. 4. Deploy with backward-compatible API for 1 release cycle. |
| Inventory oversell due to race condition | MEDIUM (requires manual order cancellation + customer notification) | 1. Run reconciliation query: `WHERE reservation > actual_parts_received`. 2. Contact affected customers for affected OS. 3. Add optimistic locking. 4. Implement the atomic UPDATE pattern. |
| Payment confirmed but OS not delivered | MEDIUM (manual delivery release) | 1. Admin endpoint to force-release delivery for confirmed payments. 2. Add idempotency table retroactively. 3. Write a one-time reconciliation script. |
| CPF/CNPJ validator breaks for alphanumeric CNPJ | LOW (update one Value Object + re-test) | 1. Update regex from `\d{14}` to alphanumeric-compatible. 2. Update check-digit algorithm for letter mapping. 3. Redeploy. No data migration needed (new CNPJs only). |
| Soft-delete unique constraint violation | MEDIUM (need to migrate constraint) | 1. Add partial unique index with `WHERE is_active = true`. 2. Remove the old unique constraint. 3. Run deduplication script if conflicts exist. |
| Quarkus CDI UnsatisfiedResolutionException | LOW (add missing Jandex) | 1. Add `jandex-maven-plugin` to the module. 2. `mvn clean compile`. 3. Redeploy. No code changes needed. |

---

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| Mega-Aggregate OS | OS Foundation (modeling) | ArchUnit test checking entity references |
| Incomplete State Machine | OS Foundation (transition matrix) | Parameterized test covering all 49 transitions |
| Inventory Reservation Race Condition | Estoque Foundation | Concurrent reservation test (10 parallel requests) |
| Payment Webhook Idempotency | Pagamento Foundation | Duplicate webhook test (send same callback twice) |
| Concurrent OS Status Writes | OS Foundation | Concurrent status transition test (3 parallel actors) |
| CPF/CNPJ Validation (incl. alphanumeric) | OS Foundation (Documento VO) | Edge case test suite (all-zero, alpha CNPJ, etc.) |
| Mercosul Plate Format | OS Foundation (PlacaVeiculo VO) | Format acceptance test (old + Mercosul) |
| Quarkus Multi-Module CDI | Build Setup / OS Foundation | `@QuarkusTest` full context load + ArchUnit resource scope check |
| Deadline Rush / Wrong Prioritization | Project Planning (vertical slice definition) | Day-4 working demo check |
| Soft Delete + Unique Constraints | OS Foundation (Flyway migrations) | Integration test: delete → recreate with same natural key |

---

## Sources

- **DDD Aggregate Design**: Vaughn Vernon "Effective Aggregate Design" (DDDCommunity, 2011) — Parts I, II, III. HIGH confidence. [https://www.dddcommunity.org/library/vernon_2011/](https://www.dddcommunity.org/library/vernon_2011/)
- **Inventory Reservation Race Conditions**: James Hickey "DDD Aggregates: Consistency Boundary" (2020). HIGH confidence. [https://www.jamesmichaelhickey.com/consistency-boundary/](https://www.jamesmichaelhickey.com/consistency-boundary/)
- **Quarkus Multi-Module CDI**: Quarkus CDI Reference Guide + StackOverflow: "How to create a Jandex index in Quarkus for classes in a external module." HIGH confidence. [https://stackoverflow.com/questions/55513502](https://stackoverflow.com/questions/55513502)
- **Brazilian CNPJ Alphanumeric**: IN RFB 2.229 (Oct 2024), effective July 2026. HIGH confidence. [https://www.gov.br/receitafederal/pt-br/assuntos/noticias/2024/outubro/cnpj-tera-letras-e-numeros-a-partir-de-julho-de-2026](https://www.gov.br/receitafederal/pt-br/assuntos/noticias/2024/outubro/cnpj-tera-letras-e-numeros-a-partir-de-julho-de-2026)
- **cpf-cnpj-utils Java library**: Maven Central, supports alphanumeric CNPJ. MEDIUM confidence (pre-release 1.0.0-alpha). [https://mvnrepository.com/artifact/io.github.felseje/cpf-cnpj-utils/1.0.0-alpha](https://mvnrepository.com/artifact/io.github.felseje/cpf-cnpj-utils/1.0.0-alpha)
- **Mercosul Plate Regex**: GitHub Gist (leonardortlima) + Regex101 community regex. MEDIUM confidence (community-maintained). [https://gist.github.com/leonardortlima/9f0be71bca4e505d3c0f58f98b73bec0](https://gist.github.com/leonardortlima/9f0be71bca4e505d3c0f58f98b73bec0)
- **Payment Idempotency Patterns**: Stripe API documentation, Razorpay integration patterns. HIGH confidence.
- **NF-e/SEFAZ Integration**: Portal da Nota Fiscal Eletrônica — Manual de Orientação ao Contribuinte v.7.0. HIGH confidence. [https://www.nfe.fazenda.gov.br/portal/listaConteudo.aspx?tipoConteudo=ndIjl+iEFdE=](https://www.nfe.fazenda.gov.br/portal/listaConteudo.aspx?tipoConteudo=ndIjl+iEFdE=)
- **Project Gotchas (G1-G10)**: Project's own CLAUDE.md — derived from actual experience building the auth subsystem. HIGH confidence. [https://github.com/fiap-mekano/mekano/CLAUDE.md](https://github.com/fiap-mekano/mekano/CLAUDE.md)
- **Software Project Deadline Mistakes**: Corcodia Blog "Common Pitfalls in Software Projects" (2026). MEDIUM confidence. [https://corcodia.com/blog/common-pitfalls-in-software-projects-and-how-to-avoid-them](https://corcodia.com/blog/common-pitfalls-in-software-projects-and-how-to-avoid-them)

---

*Pitfalls research for: Mekano — Mechanical Workshop Management System*
*Researched: 2026-06-20*
