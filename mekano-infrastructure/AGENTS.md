# mekano-infrastructure — Concrete Implementations

## Constraint
Implements domain ports. Domain NEVER knows about infrastructure. Classes here can use `jakarta.*`, `io.quarkus.*`, `org.hibernate.*`.

## Package Map (Verified Against Source)

```
com.fiap.mekano.infrastructure
├── cache/
│   └── CacheNames.java              — constants: USERS, VEHICLES, CLIENTES, PECAS, REQUISICOES, NF_ENTRADAS, SERVICOS
├── entity/                          # JPA entities (all extend BaseEntity except noted)
│   ├── BaseEntity.java              — @MappedSuperclass, extends PanacheEntityBase
│   │                                  Long id (IDENTITY), UUID uuid, createdAt, updatedAt, createdBy, updatedBy, isActive, deletedAt
│   │                                  @PreUpdate sets updatedAt
│   ├── UserEntity.java              — table "users"
│   ├── ClienteEntity.java           — table "clientes" (flattened address fields)
│   ├── VeiculoEntity.java           — table "veiculos" (clienteUuid FK)
│   ├── ServicoEntity.java           — table "servicos"
│   ├── PecaEntity.java              — table "pecas" — ⚠ uses @Data (public fields) instead of @Getter/@Setter
│   ├── RequisicaoCompraEntity.java  — table "requisicoes_compra" — ⚠ uses @Data
│   └── NfEntradaEntity.java         — table "nf_entradas" — ⚠ uses @Data
├── repository/                      # Two-class pattern: PanacheRepository + Impl
│   ├── UserPanacheRepository.java              — PanacheRepositoryBase<UserEntity, Long>
│   ├── UserRepositoryImpl.java                 — implements UserRepositoryPort
│   │                                            ✓ @Retry, @Timeout, @CacheResult, @CacheInvalidate
│   ├── ClientePanacheRepository.java           — PanacheRepositoryBase<ClienteEntity, Long>
│   ├── ClienteRepositoryImpl.java              — implements ClienteRepositoryPort
│   │                                            ✗ NO fault tolerance, NO cache annotations
│   ├── VeiculoPanacheRepository.java           — PanacheRepositoryBase<VeiculoEntity, Long>
│   ├── VeiculoRepositoryImpl.java              — implements VeiculoRepositoryPort
│   │                                            ✓ @Retry, @Timeout, @CacheResult, @CacheInvalidate
│   ├── ServicoPanacheRepository.java           — PanacheRepositoryBase<ServicoEntity, Long>
│   ├── ServicoRepositoryImpl.java              — implements ServicoRepositoryPort
│   │                                            ✓ @Retry, @Timeout, @CacheResult, @CacheInvalidate
│   ├── PecaPanacheRepository.java              — PanacheRepository<PecaEntity>
│   ├── PecaRepositoryImpl.java                 — implements PecaRepositoryPort
│   │                                            ✗ NO fault tolerance, NO cache
│   ├── RequisicaoCompraPanacheRepository.java  — PanacheRepository<RequisicaoCompraEntity>
│   ├── RequisicaoCompraRepositoryImpl.java     — implements RequisicaoCompraRepositoryPort
│   │                                            ✗ NO fault tolerance, NO cache
│   ├── NfEntradaPanacheRepository.java         — PanacheRepository<NfEntradaEntity>
│   └── NfEntradaRepositoryImpl.java            — implements NfEntradaRepositoryPort
│                                                 ✗ NO fault tolerance, NO cache
│                                                 ⚠ BUG: pecaId and requisicaoCompraId both set to nfEntrada.getId()
├── mapper/                          # Entity ↔ Domain mapping
│   ├── UserEntityMapper.java / Impl.java         — manual CDI impl (not MapStruct-generated)
│   ├── ClienteEntityMapper.java / Impl.java      — manual CDI impl
│   ├── VeiculoEntityMapper.java / Impl.java      — manual CDI impl
│   ├── ServicoEntityMapper.java / Impl.java      — manual CDI impl
│   ├── PecaEntityMapper.java                     — ⚠ EMPTY class (no methods, unused)
│   ├── RequisicaoCompraEntityMapper.java         — ⚠ EMPTY class (no methods, unused)
│   ├── NfEntradaEntityMapper.java                — ⚠ EMPTY class (no methods, unused)
│   ├── EmailMapper.java                          — @ApplicationScoped @Named
│   ├── CpfMapper.java                            — @ApplicationScoped @Named
│   ├── EnderecoMapper.java                       — @ApplicationScoped @Named (6 flatten methods)
│   ├── TelefoneMapper.java                       — @ApplicationScoped @Named
│   └── PlacaVeiculoMapper.java                   — @ApplicationScoped @Named
├── security/
│   └── BcryptPasswordHasher.java     — implements PasswordHasher, uses Quarkus BcryptUtil
└── event/
    └── CdiEventPublisher.java        — implements EventPublisher, uses CDI Event<Object>
```

## Key Conventions (VERIFIED)

### JPA Entities
- All extend `BaseEntity` which extends `PanacheEntityBase`
- Hybrid ID: `Long id` (IDENTITY PK) + `UUID uuid` (unique, exposed in APIs)
- Soft delete: `Boolean isActive` + `LocalDateTime deletedAt`
- Audit: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
- Explicit `@Table(name = "...")`, `@Column(name = "...")`
- ⚠ Newer entities (Peca, RequisicaoCompra, NfEntrada) use `@Data` (public fields) — older ones use `@Getter/@Setter` (private)

### Repository Two-Class Pattern
1. `*PanacheRepository` — extends `PanacheRepositoryBase<Entity, Long>` or `PanacheRepository<Entity>`
2. `*RepositoryImpl` — implements domain port, delegates to PanacheRepository

### Fault Tolerance & Cache Coverage (INCONSISTENT)
| Repository | @Retry | @Timeout | @CacheResult | @CacheInvalidate |
|------------|--------|----------|--------------|------------------|
| User       | ✓      | ✓        | ✓            | ✓                |
| Veiculo    | ✓      | ✓        | ✓            | ✓                |
| Servico    | ✓      | ✓        | ✓            | ✓                |
| Cliente    | ✗      | ✗        | ✗            | ✗                |
| Peca       | ✗      | ✗        | ✗            | ✗                |
| Requisicao | ✗      | ✗        | ✗            | ✗                |
| NfEntrada  | ✗      | ✗        | ✗            | ✗                |

### Cache Configuration (`cache-config.yml`)
All caches: `expire-after-write=60s` (except servicos: 120s), max-size 100-200.

### Mapper Notes
- Mappers are manual CDI implementations (`@ApplicationScoped`), NOT MapStruct-generated
- 3 mapper classes are EMPTY (Peca, RequisicaoCompra, NfEntrada) — unused dead code
- VO mappers (Email, Cpf, Endereco, Telefone, PlacaVeiculo) are `@ApplicationScoped @Named` with explicit methods

## Flyway Migrations (V1-V23)

| Migration | Table(s) | Notes |
|-----------|----------|-------|
| V1 | users | Original UUID PK schema |
| V2 | refresh_tokens | JTI-based, FK to users |
| V3 | users | Soft delete columns |
| V4 | users | Audit columns |
| V5 | users, refresh_tokens | Hybrid ID migration (UUID→uuid, BIGINT PK) |
| V6 | servicos | New style: hybrid ID + audit + soft delete from start |
| V7 | clientes | Flattened address |
| V8 | veiculos | References cliente_uuid |
| V9 | pecas | Simple inventory |
| V10 | requisicoes_compra | References peca_id |
| V11 | nf_entradas | References peca_id + requisicao_compra_id |

- V6-V11 use `BIGINT GENERATED BY DEFAULT AS IDENTITY` (H2-compatible)
- All follow H2 compatibility rules (no BIGSERIAL, no multi-column ADD)
- V12: ordens_de_servico tables (superseded by V18)
- V13: user_roles
- V14: add role to refresh_tokens
- V15: add columns users_roles
- V16: insert cliente_role for existing users
- V17: fix user_roles audit columns
- V18: ordens_de_servico table
- V19: orcamentos table
- V20: add chave_acesso to nf_entradas
- V21: create os_audit_logs
- V22: fix nf_entradas columns
- V23: fix pecas/requisicoes columns

## Known Issues
1. **Empty mapper stubs**: `PecaEntityMapper`, `RequisicaoCompraEntityMapper`, `NfEntradaEntityMapper` are dead code
2. **NfEntradaRepositoryImpl bug**: `entity.pecaId` and `entity.requisicaoCompraId` both set to `nfEntrada.getId()` (copy-paste error)
3. **Inconsistent FT/cache**: Cliente, Peca, RequisicaoCompra, NfEntrada repos lack fault tolerance and caching
4. **Entity style inconsistency**: Peca/RequisicaoCompra/NfEntrada use `@Data` (public fields) — others use `@Getter/@Setter` (private)
5. **Panache interface inconsistency**: Some repos use `PanacheRepositoryBase<E, Long>`, others use `PanacheRepository<E>`
6. **No `RefreshTokenEntity`, `RefreshTokenRepository`, `TokenBucketRateLimiter`, `RefreshTokenService`** — these were documented but deleted

## Dependencies (compile)
mekano-domain, quarkus-hibernate-orm-panache, quarkus-arc, quarkus-jdbc-postgresql, quarkus-flyway, quarkus-smallrye-fault-tolerance, quarkus-elytron-security-common, quarkus-cache, quarkus-config-yaml, mapstruct, lombok (provided)

## Testing
- `@QuarkusTest` + `@TestTransaction` (DevServices PostgreSQL)
- AssertJ fluent assertions
- 2 test files: `UserRepositoryImplTest`, `VeiculoRepositoryImplTest`
