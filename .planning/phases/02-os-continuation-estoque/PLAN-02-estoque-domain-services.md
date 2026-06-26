# PLAN-02: Estoque Domain/Services — Peca, RequisicaoCompra, NfEntrada, Atomic Stock

## Goal
Implement full inventory management: Peca CRUD with non-negative stock validation, RequisicaoCompra lifecycle, NfEntrada registration, atomic stock reservation via native SQL, minimum stock auto-requisition, and all CDI event handling for stock mutations.

## Dependencies
- PLAN-01 complete (entities, Flyway V11-V16, Panache repositories, ports, ALL domain events)
- Phase 1 OS domain model and StateOS enum (for AGUARDANDO_APROVACAO state check)

## Requirements Covered
EST-01 (CRUD), EST-02 (edit/delete), EST-03 (atomic reservation), EST-04 (requisition for unavailable parts), EST-05 (requisition lifecycle), EST-06 (NF entry), EST-07 (minimum stock check), EST-09 (alert system)

---

## Tasks

### Task 1: PecaRepositoryImpl — Atomic Stock, CRUD, Minimum Stock Query

**Files created:**
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/PecaRepositoryImpl.java`

**Action:**
Implement `PecaRepositoryPort` following `UserRepositoryImpl` pattern exactly:

- `@ApplicationScoped`, inject `PecaPanacheRepository` + `PecaEntityMapper`
- **`save(Peca)`:** `@Timeout(5s)`, `@CacheInvalidate(cacheName = "pecas")`. Persist via Panache, flush, return mapped domain. Handle `PersistenceException` → check for constraint violation (duplicate codigo).
- **`findById(UUID)`:** `@Retry(maxRetries = 3)`, `@CacheResult(cacheName = "pecas")`. HQL: `"uuid = ?1 AND isActive = ?2"` → firstResultOptional → map to domain.
- **`findAll(page, size, sort, order)`:** Per D-61..D-65. Use Panache `Sort.by()`. Validate sort field whitelist. Default sort: `nome`, order: `asc`. Max size: 50 (clamp).
- **`countAll()`:** `count("isActive = ?1", true)`.
- **`markAsDeleted(UUID)`:** `@Transactional`, `@CacheInvalidate`. Check if part has pending OS references (D-50) via native query before soft-deleting. Throw `AppException(409)` if referenced.
- **`reservarEstoque(UUID pecaUuid, int quantidade)`:** Per Pattern 1 (atomic native SQL). `@Transactional(Transactional.TxType.MANDATORY)` — fails fast if called outside service TX. Execute `UPDATE pecas SET saldo = saldo - :qtd WHERE uuid = :uuid AND saldo >= :qtd AND is_active = true`. Check `executeUpdate()` == 1, else throw `AppException(409)` per D-35.
- **`creditarEstoque(UUID pecaUuid, int quantidade)`:** `UPDATE pecas SET saldo = saldo + :qtd WHERE uuid = :uuid AND is_active = true`. No negative check needed (only adds).
- **`findBySaldoAbaixoMinimo()`:** HQL `"saldo <= estoqueMinimo AND isActive = ?1"`. Returns `List<Peca>`.
- **`findByUuid(UUID)`:** Simple HQL lookup for existing entity (used by Observers).

**Verification:**
```bash
./mvnw test -pl mekano-infrastructure -am -Dtest="PecaRepositoryImplTest"
```

---

### Task 2: RequisicaoCompraRepositoryImpl + NfEntradaRepositoryImpl

**Files created:**
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/RequisicaoCompraRepositoryImpl.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/NfEntradaRepositoryImpl.java`

**Action:**
Follow the same two-class repository pattern as `UserRepositoryImpl`:

**RequisicaoCompraRepositoryImpl:**
- `save()`, `findByUuid(UUID)`, `findAll(page, size, sort, order)`, `countAll()`, `cancelar(UUID)` (sets status to CANCELADA)
- `findAll()` sorts by `dataCriacao desc` default per D-62
- `@CacheResult`/`@CacheInvalidate` on requisições cache
- `@Retry` on reads, `@Timeout` on writes

**NfEntradaRepositoryImpl:**
- `save()`, `findByUuid(UUID)`, `findAll(page, size, sort, order)`, `countAll()`
- `save()` includes creditarEstoque logic (increases peca saldo) — called from service layer

**Verification:**
```bash
./mvnw test -pl mekano-infrastructure -am
```

---

### Task 3: PecaService — Application Layer (CRUD + Business Rules)

**Files created:**
- `mekano-application/src/main/java/com/fiap/mekano/application/service/peca/PecaService.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/CriarPecaCommand.java` (if needed)

**Action:**
- `@ApplicationScoped`, inject `PecaRepositoryPort`, `EventPublisher`
- **`criar(Peca peca)`:** Delegates to `PecaRepositoryPort.save(peca)`. Validates `saldo >= 0` in domain constructor already. Returns saved Peca.
- **`atualizar(UUID uuid, Peca dadosAtualizados)`:** Find existing by UUID, update mutable fields (nome, codigo, unidade, estoqueMinimo, loteCompra, valor). Saldo NOT directly mutable (changed only via reservation/NF entry).
- **`deletar(UUID uuid)`:** Checks domain rule (D-50: blocks if referenced in pending OS) — the repository handles this check. Delegates to `markAsDeleted()`.
- **`buscarPorId(UUID uuid)`:** `findById(uuid)` or throw `AppException(404)`.
- **`listar(page, size, sort, order)`:** Delegates to `findAll()` / `countAll()`. Builds paginated response.
- **`verificarMinimoEPublicarEvento(Peca peca)`:** After any stock mutation, if `peca.isEstoqueMinimoAtingido()`, publish `EstoqueMinimoAtingidoEvent` via `EventPublisher.publish()` (sync CDI event, per D-22/D-58).

**Verification:**
```bash
./mvnw test -pl mekano-application -am -Dtest="PecaServiceTest"
```

---

### Task 4: RequisicaoCompraService + NfEntradaService — Application Layer

**Files created:**
- `mekano-application/src/main/java/com/fiap/mekano/application/service/requisicao/RequisicaoCompraService.java`
- `mekano-application/src/main/java/com/fiap/mekano/application/service/nf/NfEntradaService.java`

**Action:**

**RequisicaoCompraService:**
- `@ApplicationScoped`, `@Transactional`
- **`criarParaOrcamento(UUID pecaUuid, int quantidade, UUID orcamentoUuid)`:** Per D-26, auto-aprovada (status ABERTA). Publish `RequisicaoCompraCriadaEvent`.
- **`criarParaMinimo(Peca peca)`:** Per D-23/D-24, uses `peca.loteCompra` as quantity. Status ABERTA (PENDENTE per D-25 but Phase 2 uses ABERTA only per D-27). Publish `RequisicaoCompraCriadaEvent`.
- **`listar(page, size, sort, order)`:** Paginated list.
- **`buscarPorId(UUID uuid)`:** Find by UUID.
- **`cancelar(UUID uuid)`:** Only admin can cancel (D-28). Validates can cancel. Delegates to repository.
- **`comprar(UUID uuid)`:** Admin marks requisition as COMPRADA.
- **`receber(UUID uuid, NfEntrada nfEntrada)`:** Register NF, mark REQUISICAO as RECEBIDA, credit peca stock.

**NfEntradaService:**
- `@ApplicationScoped`, `@Transactional`
- **`registrar(NfEntrada nfEntrada, UUID requisicaoUuid)`:** Validate NF data per D-30 (numero, serie, fornecedor, dataEmissao, cfop all required). Check requisicao exists and status != CANCELADA. Save NF. Update requisicao status → RECEBIDA. Credit peca stock via `PecaRepository.creditarEstoque()`. After credit, call `PecaService.verificarMinimoEPublicarEvento()` (EST-07).

**Verification:**
```bash
./mvnw test -pl mekano-application -am
```

---

### Task 5: CDI Event Observers — Stock Reservation and Auto-Requisition

**Files created:**
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/service/PecaOrcamentoObserver.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/service/EstoqueMinimoObserver.java`

**Action:**

**PecaOrcamentoObserver:**
- `@ApplicationScoped`
- `void onOrcamentoAprovado(@Observes OrcamentoAprovadoEvent event)` (reference — will exist after PLAN-03):
  - For each item in event.itens():
    - If item is a peca (tipo == "PECA"), call `pecaRepository.reservarEstoque(item.pecaUuid(), item.quantidade())`
    - If stock insufficient (AppException 409), call `requisicaoService.criarParaOrcamento(...)` per EST-04
    - Per D-08: reservation happens ONLY on budget approval
  - After all reservations, call `pecaService.verificarMinimoEPublicarEvento(peca)` for affected pecas

**EstoqueMinimoObserver:**
- `@ApplicationScoped`
- `void onEstoqueMinimoAtingido(@Observes EstoqueMinimoAtingidoEvent event)`:
  - Calls `requisicaoService.criarParaMinimo(...)` per D-23
  - Logs the event

**Verification:**
```bash
./mvnw compile -pl mekano-infrastructure -am
```

---

### Task 6: Cache Configuration for New Caches

**Files modified:**
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/cache/CacheNames.java`
- `mekano-rest/src/main/resources/cache-config.yml`

**Action:**
- Add to `CacheNames.java`:
  ```java
  public static final String PECAS = "pecas";
  public static final String REQUISICOES_COMPRA = "requisicoesCompra";
  ```
- Add to `cache-config.yml`:
  ```yaml
  "pecas":
    expire-after-write: 60s
    maximum-size: 100
    initial-capacity: 10
  "requisicoesCompra":
    expire-after-write: 60s
    maximum-size: 50
    initial-capacity: 5
  ```
- Pattern: matches existing `"users"` cache config.

**Verification:**
```bash
./mvnw compile -pl mekano-rest -am
```

---

### Task 7: Unit + Integration Tests for Estoque Services

**Files created:**
- `mekano-application/src/test/java/com/fiap/mekano/application/service/peca/PecaServiceTest.java`
- `mekano-application/src/test/java/com/fiap/mekano/application/service/requisicao/RequisicaoCompraServiceTest.java`
- `mekano-application/src/test/java/com/fiap/mekano/application/service/nf/NfEntradaServiceTest.java`
- `mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/repository/PecaRepositoryImplTest.java`
- `mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/repository/PecaConcurrentTest.java`

**Action:**
- **PecaServiceTest:** Mockito `@ExtendWith(MockitoExtension.class)`. Test `criar()` with valid/invalid data, `deletar()` with pending OS reference (expect AppException 409), `verificarMinimoEPublicarEvento()` publishes event when saldo <= estoqueMinimo.
- **RequisicaoCompraServiceTest:** Mockito. Test `criarParaOrcamento()` (auto-approved), `criarParaMinimo()` (ABERTA), `cancelar()` state validation.
- **NfEntradaServiceTest:** Mockito. Test `registrar()` credits stock, updates requisition status.
- **PecaRepositoryImplTest:** `@QuarkusTest` + `@TestTransaction`. Test `save()` + `findById()` + `findAll()` + `countAll()`. Test `reservarEstoque()` decrements correctly. Test `reservarEstoque()` throws 409 when saldo < qtd. Test `creditarEstoque()` increments correctly. Test `markAsDeleted()` blocks when referenced.
- **PecaConcurrentTest:** `@QuarkusTest`. Two concurrent threads reserve stock for same peca. Assert final saldo is correct (both reservations succeed if stock sufficient, or one fails with 409). Verify atomicity with `CountDownLatch`.

**Verification:**
```bash
# Infrastructure integration tests (DevServices PostgreSQL)
./mvnw test -pl mekano-infrastructure -am
# Application service tests (Mockito, fast)
./mvnw test -pl mekano-application -am
```

---

## Verification (Plan-Level)

```bash
# Full compile
./mvnw compile -pl mekano-rest -am

# Application service tests
./mvnw test -pl mekano-application -am

# Infrastructure integration tests (includes atomic stock + concurrent)
./mvnw test -pl mekano-infrastructure -am
```

## Risk Mitigation
- **Atomic stock race condition:** Native SQL `UPDATE ... WHERE saldo >= qtd` is single-statement atomic. The `executeUpdate()` return value check prevents silent failures. Concurrent access test (PecaConcurrentTest) validates this.
- **H2 compatibility:** Standard SQL arithmetic UPDATE works on both PostgreSQL and H2. Avoid `RETURNING`, `FOR UPDATE`, `WITH`.
- **CDI event + TX propagation:** Observers run in publisher's TX. If observer throws, entire TX rolls back. This is correct for stock reservation (approval + reservation = atomic). For minimum stock observer, errors should be caught and logged (not rolled back).
- **Part soft delete with pending OS (D-50):** Implementation must check Phase 1's OS entity/service for pending OS references. If Phase 1 OS not yet implemented, stub the check.
