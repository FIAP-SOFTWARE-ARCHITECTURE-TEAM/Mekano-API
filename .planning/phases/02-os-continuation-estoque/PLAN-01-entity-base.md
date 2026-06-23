# PLAN-01: Entity Base — Domain Entities, Value Objects, Flyway Migrations

## Goal
Create all new JPA entities extending BaseEntity, domain model classes, value objects, enums, Flyway migrations V11-V17, repository port interfaces, entity mappers, and Panache repository stubs for Phase 2 aggregates. This is the infrastructure foundation for both Estoque and Orcamento subdomains.

## Dependencies
- Phase 1 complete: V1-V5 Flyway migrations applied, `BaseEntity`, `PanacheEntityBase` available
- Existing patterns: `AppException`, `Cpf`, `Placa`, `Endereco` VOs if created in Phase 1 (otherwise recreated here)

## Requirements Covered
EST-01 (entities for Peca), EST-03 (Peca entity for stock), EST-06 (NfEntrada entity), foundation for all Phase 2 aggregates

---

## Tasks

### Task 1: Value Objects — Cpf, Placa, Endereco

**Files created:**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/Cpf.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/Placa.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/Endereco.java`

**Action:**
Create three `final class` VOs with `@EqualsAndHashCode`, `private final` fields, constructor validation:
- **Cpf:** `String value` — validates 11 digits, checksum via modulo 11 algorithm (both digits). Throws `AppException(400)` on invalid. Normalizes: strip non-digits.
- **Placa:** `String value` — regex covering Mercosul (`ABC1D23`) and old format (`ABC1234`). Normalizes: uppercase, no hyphen. `getFormatted()` returns with hyphen (e.g., `ABC-1234` or `ABC-1D23`).
- **Endereco:** `String logradouro`, `String numero`, `String bairro`, `String cidade`, `String uf`, `String cep`. All fields required. CEP validated as 8 digits.
- Pattern: same as existing `Email.java` VO. Javadoc in Portuguese.

**Verification:**
```bash
./mvnw test -pl mekano-domain -Dtest="*ValueObject*"
```

---

### Task 2: Enums — UnidadeMedida, StatusRequisicao

**Files created:**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/UnidadeMedida.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/StatusRequisicao.java`

**Action:**
- **UnidadeMedida:** enum with `UN`, `KG`, `L`, `CX`, `M`, `PC`. Each with `descricao` field (e.g., `UN("Unidade")`).
- **StatusRequisicao:** enum with `ABERTA`, `CANCELADA`, `COMPRADA`, `RECEBIDA` (per D-27).
- Pattern: `public enum X { ... }` with `private final String descricao`, `@Getter`, constructor.

**Verification:**
```bash
./mvnw test -pl mekano-domain -Dtest="*Enum*"
```

---

### Task 3: Domain Model Classes — Peca, Orcamento, ItemOrcamento, RequisicaoCompra, NfEntrada

**Files created:**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/Peca.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/Orcamento.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/ItemOrcamento.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/RequisicaoCompra.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/NfEntrada.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/ValorMonetario.java` (if not existing)

**Action:**
Create pure POJO domain classes (zero framework imports, only Lombok provided):

**Peca:**
- Fields: `UUID uuid`, `String nome`, `String codigo`, `UnidadeMedida unidade`, `int saldo`, `int estoqueMinimo`, `int loteCompra`, `BigDecimal valor`, `boolean ativo`
- Factory: `static Peca.create(...)` (validates `saldo >= 0`, `estoqueMinimo >= 0`, `loteCompra > 0`, `valor != null && valor.compareTo(BigDecimal.ZERO) > 0`)
- Methods: `debitarSaldo(int qtd)` — validates non-negative AFTER debit (`saldo - qtd >= 0`). `creditarSaldo(int qtd)`. `isEstoqueMinimoAtingido()` → `saldo <= estoqueMinimo`
- Javadoc in Portuguese. Annotate with `@EqualsAndHashCode(of = "uuid")`.

**Orcamento (separate aggregate per D-02):**
- Fields: `UUID uuid`, `UUID ordemDeServicoUuid`, `List<ItemOrcamento> itens`, `BigDecimal valorTotal`, `LocalDateTime dataCriacao`, `LocalDateTime dataExpiracao`, `boolean aprovado`
- Factory: `static Orcamento.create(UUID osUuid, List<ItemOrcamento> itens, Duration sla)` — computes `valorTotal` = sum of `(servico.quantidade * servico.valorUnitario) + (peca.quantidade * peca.valorUnitario)` per D-01, D-05
- Methods: `aprovar()`, `reprovar()`, `isExpirado(LocalDateTime now)`
- No soft delete on Orcamento itself (keep historical records)

**ItemOrcamento:**
- Fields: `UUID uuid`, `String tipo` (SERVICO/PECA), `String descricao`, `int quantidade`, `BigDecimal valorUnitario`, `BigDecimal valorTotal`, `UUID pecaUuid` (null for serviços)
- Record or `@EqualsAndHashCode` class. Immutable.

**RequisicaoCompra:**
- Fields: `UUID uuid`, `UUID pecaUuid`, `int quantidade`, `StatusRequisicao status`, `UUID orcamentoUuid` (nullable — auto-approved if present per D-26), `String motivo`, `LocalDateTime dataCriacao`
- Factory: `static RequisicaoCompra.criarParaOrcamento(...)` (auto-approved status) and `static RequisicaoCompra.criarParaMinimo(...)` (PENDENTE status)
- Methods: `cancelar()`, `comprar()`, `receber()` (state transition guards)

**NfEntrada:**
- Fields: `UUID uuid`, `UUID requisicaoCompraUuid`, `String numero`, `String serie`, `String fornecedor`, `LocalDate dataEmissao`, `String cfop`, `BigDecimal valorTotal`, `List<ItemNfEntrada> itens` (if needed)
- Per D-29: NF must reference a RequisicaoCompra
- Per D-30: complete NF data (number, series, supplier, emission date, CFOP)

**Verification:**
```bash
./mvnw test -pl mekano-domain
```

---

### Task 3.5: Domain Events — Orcamento, OS, Estoque, SoftDelete

**Files created:**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/OrcamentoGeradoEvent.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/OrcamentoAprovadoEvent.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/OrcamentoReprovadoEvent.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/OSFinalizadaEvent.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/EstoqueMinimoAtingidoEvent.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/RequisicaoCompraCriadaEvent.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/SoftDeleteEvent.java`

**Action:**
Per D-58..D-60: records with `static of()` factory. Include payload with main data per D-59. Naming: `EntidadeAcaoEvent`.

**OrcamentoGeradoEvent (record):**
```java
public record OrcamentoGeradoEvent(
    UUID orcamentoUuid,
    UUID ordemDeServicoUuid,
    BigDecimal valorTotal,
    LocalDateTime dataExpiracao
) {
    public static OrcamentoGeradoEvent of(Orcamento orcamento) { ... }
}
```

**OrcamentoAprovadoEvent (includes parts list per D-59):**
```java
public record OrcamentoAprovadoEvent(
    UUID orcamentoUuid,
    UUID ordemDeServicoUuid,
    List<ItemOrcamentoDTO> itens
) {
    public static OrcamentoAprovadoEvent of(Orcamento orcamento) { ... }
    public record ItemOrcamentoDTO(
        UUID pecaUuid, String tipo, int quantidade
    ) {}
}
```

**OrcamentoReprovadoEvent:**
```java
public record OrcamentoReprovadoEvent(
    UUID orcamentoUuid,
    UUID ordemDeServicoUuid,
    String motivo
) {
    public static OrcamentoReprovadoEvent of(Orcamento orcamento, String motivo) { ... }
}
```

**OSFinalizadaEvent:**
```java
public record OSFinalizadaEvent(
    UUID ordemDeServicoUuid,
    UUID orcamentoUuid,
    LocalDateTime dataFinalizacao
) {
    public static OSFinalizadaEvent of(OrdemDeServico os, Orcamento orcamento) { ... }
}
```

**EstoqueMinimoAtingidoEvent (record):**
```java
public record EstoqueMinimoAtingidoEvent(
    UUID pecaUuid, String pecaNome,
    int saldoAtual, int estoqueMinimo, int quantidadeSugerida
) {
    public static EstoqueMinimoAtingidoEvent of(Peca peca) { ... }
}
```

**RequisicaoCompraCriadaEvent (record):**
```java
public record RequisicaoCompraCriadaEvent(
    UUID requisicaoUuid, UUID pecaUuid,
    int quantidade, StatusRequisicao status, String origem
) {
    public static RequisicaoCompraCriadaEvent of(RequisicaoCompra requisicao, String origem) { ... }
}
```

**SoftDeleteEvent (record):** Per D-53, published when any entity is soft-deleted.
```java
public record SoftDeleteEvent(
    UUID entityUuid,
    String entityType,
    UUID deletedBy
) {
    public static SoftDeleteEvent of(UUID entityUuid, String entityType, UUID deletedBy) { ... }
}
```

Pattern: same as existing `UserCreatedEvent`. Records with `static of()` factory.

**Verification:**
```bash
./mvnw compile -pl mekano-domain
```

---

### Task 4: Repository Port Interfaces in Domain

**Files created:**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/PecaRepositoryPort.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/OrcamentoRepositoryPort.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/RequisicaoCompraRepositoryPort.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/NfEntradaRepositoryPort.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/OsAuditLogRepositoryPort.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/PecaServicePort.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/OrcamentoServicePort.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/RequisicaoCompraServicePort.java`
- `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/NfEntradaServicePort.java`

**Action:**
Follow existing `UserRepositoryPort` pattern:
- **PecaRepositoryPort:** `save(Peca)`, `findById(UUID)`, `findByUuid(UUID)`, `findAll(page, size, sort, order)`, `countAll()`, `markAsDeleted(UUID)`, `reservarEstoque(UUID, int)`, `creditarEstoque(UUID, int)`, `findBySaldoAbaixoMinimo()`
- **OrcamentoRepositoryPort:** `save(Orcamento)`, `findByUuid(UUID)`, `findByOrdemDeServicoUuid(UUID)`
- **RequisicaoCompraRepositoryPort:** `save(RequisicaoCompra)`, `findByUuid(UUID)`, `findAll(page, size, sort, order)`, `countAll()`, `cancelar(UUID)`
- **NfEntradaRepositoryPort:** `save(NfEntrada)`, `findByUuid(UUID)`, `findAll(page, size, sort, order)`, `countAll()`
- **OsAuditLogRepositoryPort:** `save(OsAuditLog)`, `findByOrdemDeServicoUuid(UUID)`
- Service ports: `PecaServicePort`, `OrcamentoServicePort`, `RequisicaoCompraServicePort`, `NfEntradaServicePort` — mirror the `UserServicePort` pattern with `execute()` methods

**Verification:**
```bash
./mvnw compile -pl mekano-domain
```

---

### Task 5: JPA Entities + Entity Mappers + Panache Repositories

**Files created:**
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/PecaEntity.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/OrcamentoEntity.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/ItemOrcamentoEntity.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/RequisicaoCompraEntity.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/NfEntradaEntity.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/OsAuditLogEntity.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/PecaEntityMapper.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/OrcamentoEntityMapper.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/RequisicaoCompraEntityMapper.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/NfEntradaEntityMapper.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/PecaPanacheRepository.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/OrcamentoPanacheRepository.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/RequisicaoCompraPanacheRepository.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/NfEntradaPanacheRepository.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/OsAuditLogPanacheRepository.java`

**Action:**
Follow existing `UserEntity` → `BaseEntity` pattern strictly:

**PecaEntity (extends BaseEntity):**
- `@Table(name = "pecas")`, `@Entity`
- Fields: `UUID uuid` (unique, not null), `String nome`, `String codigo` (unique), `@Enumerated(STRING) UnidadeMedida unidade`, `int saldo`, `int estoqueMinimo`, `int loteCompra`, `BigDecimal valor`
- `@Column(name = "uuid", unique = true, nullable = false)`

**OrcamentoEntity (extends BaseEntity):**
- `@Table(name = "orcamentos")`, `@Entity`
- Fields: `UUID uuid`, `UUID ordemDeServicoUuid`, `BigDecimal valorTotal`, `LocalDateTime dataCriacao`, `LocalDateTime dataExpiracao`, `boolean aprovado`, `@OneToMany List<ItemOrcamentoEntity> itens`

**ItemOrcamentoEntity (extends PanacheEntityBase — child, no soft delete):**
- `@Table(name = "orcamento_itens")`, `@Entity`, own `@Id Long id` with `@GeneratedValue(IDENTITY)`
- Fields: `UUID uuid`, `@ManyToOne OrcamentoEntity orcamento`, `String tipo`, `String descricao`, `int quantidade`, `BigDecimal valorUnitario`, `BigDecimal valorTotal`, `UUID pecaUuid`
- `@JsonIgnore` on orcamento to avoid circular serialization

**RequisicaoCompraEntity (extends BaseEntity):**
- `@Table(name = "requisicoes_compra")`, `@Entity`
- Fields: `UUID uuid`, `UUID pecaUuid`, `int quantidade`, `@Enumerated(STRING) StatusRequisicao status`, `UUID orcamentoUuid`, `String motivo`

**NfEntradaEntity (extends BaseEntity):**
- `@Table(name = "nf_entradas")`, `@Entity`
- Fields: `UUID uuid`, `UUID requisicaoCompraUuid`, `String numero`, `String serie`, `String fornecedor`, `LocalDate dataEmissao`, `String cfop`, `BigDecimal valorTotal`

**OsAuditLogEntity (NO BaseEntity — immutable, per D-69):**
- `extends PanacheEntityBase`, own `@Id @GeneratedValue(IDENTITY) Long id`
- `@Table(name = "os_audit_log")`
- Fields: `UUID uuid`, `UUID ordemDeServicoUuid`, `String statusOrigem`, `String statusDestino`, `String usuario`, `@Column(columnDefinition = "TEXT") String snapshotJson`, `LocalDateTime dataCriacao`
- NO setters for mutable fields (immutable after creation)
- NO `isActive`/`deletedAt` fields

**Entity Mappers (MapStruct `componentModel = "cdi"`):**
- Use existing `EmailMapper` pattern for VO flattening
- `@Mapper(componentModel = "cdi", unmappedTargetPolicy = ReportingPolicy.IGNORE)`
- `toEntity(Domain)` and `toDomain(Entity)` for each aggregate
- Ignore `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deletedAt`, `isActive` on target domain

**Panache Repositories:**
- Each extends `PanacheRepositoryBase<Entity, Long>` (PK is `Long id`)
- Empty bean class (delegation happens in RepositoryImpl in Wave 2)

**CacheNames update:**
- Add `PECAS = "pecas"`, `ORCAMENTOS = "orcamentos"`, `REQUISICOES_COMPRA = "requisicoesCompra"` to `CacheNames.java`

**Verification:**
```bash
./mvnw compile -pl mekano-infrastructure -am
```

---

### Task 6: Flyway Migrations V11-V16

> Phase 1 já ocupa V1-V5 (existentes) + V6 (user_roles) + V7 (clientes) + V8 (veiculos) + V9 (servicos) + V10 (ordens_de_servico). Phase 2 começa em V11.

**Files created:**
- `mekano-infrastructure/src/main/resources/db/migration/V11__create_pecas_table.sql`
- `mekano-infrastructure/src/main/resources/db/migration/V12__create_orcamentos_table.sql`
- `mekano-infrastructure/src/main/resources/db/migration/V13__create_orcamento_itens_table.sql`
- `mekano-infrastructure/src/main/resources/db/migration/V14__create_requisicoes_compra_table.sql`
- `mekano-infrastructure/src/main/resources/db/migration/V15__create_nf_entradas_table.sql`
- `mekano-infrastructure/src/main/resources/db/migration/V16__create_os_audit_log_table.sql`

**Action:**
Per H2 compatibility rules from CLAUDE.md:
- Use `BIGINT GENERATED BY DEFAULT AS IDENTITY` (never `BIGSERIAL`)
- Separate `ALTER TABLE` statements (never comma-separated `ADD COLUMN`)
- Types OK: `UUID`, `TIMESTAMP`, `BOOLEAN`, `VARCHAR`, `INT`, `BIGINT`, `DECIMAL`, `NOW()`, `DEFAULT`, `CREATE INDEX`

**V11: pecas**
```sql
CREATE TABLE pecas (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    nome VARCHAR(255) NOT NULL,
    codigo VARCHAR(100) NOT NULL UNIQUE,
    unidade VARCHAR(10) NOT NULL,
    saldo INT NOT NULL DEFAULT 0,
    estoque_minimo INT NOT NULL DEFAULT 0,
    lote_compra INT NOT NULL DEFAULT 1,
    valor DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
```
Include indexes on `uuid`, `codigo`, `(saldo, is_active)` for minimum stock queries.

**V12: orcamentos**
```sql
CREATE TABLE orcamentos (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    ordem_de_servico_uuid UUID NOT NULL,
    valor_total DECIMAL(12,2) NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT NOW(),
    data_expiracao TIMESTAMP NOT NULL,
    aprovado BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
```
Include index on `ordem_de_servico_uuid`.

**V13: orcamento_itens**
```sql
CREATE TABLE orcamento_itens (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    orcamento_id BIGINT NOT NULL REFERENCES orcamentos(id),
    tipo VARCHAR(20) NOT NULL,
    descricao VARCHAR(255) NOT NULL,
    quantidade INT NOT NULL,
    valor_unitario DECIMAL(12,2) NOT NULL,
    valor_total DECIMAL(12,2) NOT NULL,
    peca_uuid UUID
);
```
Include index on `orcamento_id`.

**V14: requisicoes_compra**
```sql
CREATE TABLE requisicoes_compra (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    peca_uuid UUID NOT NULL,
    quantidade INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTA',
    orcamento_uuid UUID,
    motivo VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
```

**V15: nf_entradas**
```sql
CREATE TABLE nf_entradas (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    requisicao_compra_uuid UUID NOT NULL,
    numero VARCHAR(50) NOT NULL,
    serie VARCHAR(10) NOT NULL,
    fornecedor VARCHAR(255) NOT NULL,
    data_emissao DATE NOT NULL,
    cfop VARCHAR(10) NOT NULL,
    valor_total DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
```

**V16: os_audit_log**

**V17: seed_role_cliente** (seed data, não DDL)
```sql
INSERT INTO user_roles (uuid, user_uuid, role, created_at)
SELECT gen_random_uuid(), u.uuid, 'cliente', NOW()
FROM users u WHERE u.role = 'cliente' AND NOT EXISTS (
    SELECT 1 FROM user_roles ur WHERE ur.user_uuid = u.uuid AND ur.role = 'cliente'
);
```
Nota: esta migration é segura para reexecução (idempotente via `NOT EXISTS`). O role `cliente` é inserido na user_roles para cada usuário com role='cliente' que ainda não tenha o registro.

Também adicionar `CLIENTE` ao enum `Role.java` em `mekano-domain/src/main/java/com/fiap/mekano/domain/model/Role.java`:
```java
public enum Role {
    admin, atendente, mecanico, almoxarife, financeiro, cliente
}
```
```sql
CREATE TABLE os_audit_log (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    ordem_de_servico_uuid UUID NOT NULL,
    status_origem VARCHAR(30),
    status_destino VARCHAR(30) NOT NULL,
    usuario VARCHAR(100) NOT NULL,
    snapshot_json TEXT NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT NOW()
);
```
Include index on `ordem_de_servico_uuid`.

**Verification:**
```bash
# Build and verify migrations compile
./mvnw compile -pl mekano-infrastructure -am
# Integration test with DevServices (runs H2 with MODE=PostgreSQL)
./mvnw test -pl mekano-infrastructure -am -Dtest="*Migration*"
```

---

## Verification (Plan-Level)

```bash
# Full compile check
./mvnw compile -pl mekano-rest -am

# Domain unit tests (value objects + domain models)
./mvnw test -pl mekano-domain

# Infrastructure compile + entity mapping
./mvnw compile -pl mekano-infrastructure -am
```

## Risk Mitigation
- **Flyway numbering:** Phase 1 ocupa V6-V10. Phase 2 usa V11-V16. Se Phase 1 ainda não foi executada, as migrations de Phase 1 (V6-V10) e Phase 2 (V11-V16) convivem no mesmo diretório — Flyway aplica em ordem numérica, tudo correto.
- **H2 compatibility:** All SQL uses standard types. No `BIGSERIAL`, `RETURNING`, `FOR UPDATE`. Test with H2 `MODE=PostgreSQL`.
- **Entity mapping:** MapStruct `componentModel = "cdi"` (never "spring"). Annotation processor order: Lombok → binding → MapStruct (G3).
