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
│   │                                  @EntityListeners(AuditoriaListener.class) — auto-fills createdBy/updatedBy
│   ├── UserEntity.java              — table "users"
│   ├── ClienteEntity.java           — table "clientes" (flattened address fields)
│   ├── VeiculoEntity.java           — table "veiculos" (clienteUuid FK)
│   ├── ServicoEntity.java           — table "servicos"
│   ├── PecaEntity.java              — table "pecas" — ⚠ uses @Data (public fields) instead of @Getter/@Setter
│   ├── RequisicaoCompraEntity.java  — table "requisicoes_compra" — ⚠ uses @Data
│   ├── NfEntradaEntity.java         — table "nf_entradas" — ⚠ uses @Data
│   ├── OrdemDeServicoEntity.java    — table "ordens_de_servico" — hybrid ID, audit, soft delete
│   ├── ItemOsEntity.java            — table "os_itens" — junction table for OS items (peças/serviços)
│   ├── OrcamentoEntity.java         — table "orcamentos"
│   ├── OsAuditLogEntity.java        — table "os_audit_logs"
│   ├── ProcessedEventsEntity.java   — table "processed_events" — idempotency control
│   ├── RefreshTokenEntity.java      — table "refresh_tokens"
│   ├── UserRoleEntity.java          — table "user_roles"
│   └── CobrancaEntity.java          — table "cobrancas"
├── audit/                           # Auditoria automática de createdBy/updatedBy (D-16)
│   ├── AuditoriaOrigem.java         — enum: PUBLICO (00000000-0000-0000-0000-000000000001),
│   │                                  SISTEMA (00000000-0000-0000-0000-000000000002);
│   │                                  estático resolver(String principalName)
│   ├── AuditoriaContext.java        — @RequestScoped @Unremovable; injeta Instance<SecurityIdentity>;
│   │                                  principalName() = subject do JWT (ou null se anônimo)
│   └── AuditoriaListener.java       — JPA listener de BaseEntity: @PrePersist → createdBy;
│                                      @PreUpdate → updatedBy; resolve via Arc.container() com
│                                      fallback SISTEMA em caso de erro/sem request context
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
│   ├── NfEntradaRepositoryImpl.java            — implements NfEntradaRepositoryPort
│   │                                            ✗ NO fault tolerance, NO cache
│   │                                            ⚠ BUG: pecaId and requisicaoCompraId both set to nfEntrada.getId()
│   ├── OrdemDeServicoPanacheRepository.java    — PanacheRepositoryBase<OrdemDeServicoEntity, Long>
│   ├── OrdemDeServicoRepositoryImpl.java       — implements OrdemDeServicoRepositoryPort
│   ├── ItemOsPanacheRepository.java            — PanacheRepository<ItemOsEntity>
│   ├── ItemOsRepositoryImpl.java               — implements ItemOsRepositoryPort
│   │                                            ✗ NO fault tolerance, NO cache
│   │                                            save(), findByOsUuid(), deleteByOsUuid()
│   ├── OrcamentoPanacheRepository.java         — PanacheRepository<OrcamentoEntity>
│   ├── OrcamentoRepositoryImpl.java            — implements OrcamentoRepositoryPort
│   ├── OsAuditLogPanacheRepository.java        — PanacheRepository<OsAuditLogEntity>
│   ├── OsAuditLogRepositoryImpl.java           — implements OsAuditLogRepositoryPort
│   ├── ProcessedEventsPanacheRepository.java   — PanacheRepository<ProcessedEventsEntity>
│   ├── ProcessedEventsRepositoryImpl.java      — implements ProcessedEventsRepositoryPort
│   ├── RefreshTokenPanacheRepository.java      — PanacheRepository<RefreshTokenEntity>
│   ├── RefreshTokenRepositoryImpl.java         — implements RefreshTokenRepositoryPort
│   ├── UserRolePanacheRepository.java          — PanacheRepository<UserRoleEntity>
│   ├── UserRoleRepositoryImpl.java             — implements UserRoleRepositoryPort
│   ├── CobrancaPanacheRepository.java          — PanacheRepository<CobrancaEntity>
│   └── CobrancaRepositoryImpl.java             — implements CobrancaRepositoryPort
├── mapper/                          # Entity ↔ Domain mapping
│   ├── UserEntityMapper.java / Impl.java         — manual CDI impl (not MapStruct-generated)
│   ├── ClienteEntityMapper.java / Impl.java      — manual CDI impl
│   ├── VeiculoEntityMapper.java / Impl.java      — manual CDI impl
│   ├── ServicoEntityMapper.java / Impl.java      — manual CDI impl
│   ├── OrdemDeServicoEntityMapper.java / Impl.java — manual CDI impl
│   ├── ItemOsEntityMapper.java                   — manual toDomain() and toEntity() methods
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
├── event/
│   └── CdiEventPublisher.java        — implements EventPublisher, uses CDI Event<Object>
└── listener/                        # CDI event listeners
    ├── PagamentoConfirmadoListener.java
    ├── OSEntregueListener.java
    ├── WhatsAppPagamentoObserver.java
    ├── WhatsAppOrcamentoObserver.java
    └── WhatsAppCancelamentoObserver.java
```

## Key Conventions (VERIFIED)

### JPA Entities
- All extend `BaseEntity` which extends `PanacheEntityBase`
- Hybrid ID: `Long id` (IDENTITY PK) + `UUID uuid` (unique, exposed in APIs)
- Soft delete: `Boolean isActive` + `LocalDateTime deletedAt`
- Audit: `createdAt`, `updatedAt`, `createdBy`, `updatedBy`
- Explicit `@Table(name = "...")`, `@Column(name = "...")`
- ⚠ Newer entities (Peca, RequisicaoCompra, NfEntrada) use `@Data` (public fields) — older ones use `@Getter/@Setter` (private)

### Audit Auto-Fill (`audit/` package — D-16)
- `AuditoriaListener` está registrado via `@EntityListeners` em `BaseEntity` — cobrindo TODAS as entidades de uma vez
- `@PrePersist` preenche apenas `createdBy` (nunca `updatedBy`, que fica NULL no create)
- `@PreUpdate` preenche `updatedBy` (via dirty checking no commit, sem calls extras)
- Resolução do usuário: request ativo + principal → subject do JWT (UUID); request ativo + anônimo → `PUBLICO`; sem request context (jobs, unit tests) → `SISTEMA`
- Usa `SecurityIdentity` (não `JsonWebToken`) — funciona com `@TestSecurity` e com JWT real
- `@Unremovable` em `AuditoriaContext` é OBRIGATÓRIO (lookup programático via `Arc.container()` não é detectado pela remoção de beans)
- Queries nativas de estoque (`PecaRepositoryImpl`) estampam `updated_by = SISTEMA` explicitamente
- Nenhuma mudança de schema nem backfill — apenas registros novos são auditados

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

## Flyway Migrations (V1-V35)

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
| V12 | ordens_de_servico tables | Superseded by V18/V25 |
| V13 | user_roles | Role-based access |
| V14 | refresh_tokens | Add role column |
| V15 | users_roles | Multi-role support |
| V16 | users_roles | Insert cliente_role for existing users |
| V17 | user_roles | Fix audit columns |
| V18 | ordens_de_servico | Main OS table (hybrid ID, audit, soft delete) |
| V19 | orcamentos | Budget/approval table |
| V20 | nf_entradas | Add chave_acesso |
| V23 | nf_entradas | Fix columns |
| V24 | pecas, requisicoes | Fix columns |
| V25 | ordens_de_servico | Final OS table (recreated) |
| V26 | os_audit_logs | OS audit trail |
| V27 | ordens_de_servico | Add payment fields |
| V29 | ordens_de_servico | Add pagamento/entrega fields |
| V30 | processed_events | Idempotency control |
| V31 | ordens_de_servico | Fix pagamento/entrega column types |
| V32 | users | Seed admin user |
| V33 | ordens_de_servico | Fix status_pagamento/entrega column size |
| V34 | pecas | Add saldo_reservado |
| V35 | os_itens | Junction table for OS items (peças/serviços) |

- V6-V11, V35 use `BIGINT GENERATED BY DEFAULT AS IDENTITY` (H2-compatible)
- All follow H2 compatibility rules (no BIGSERIAL, no multi-column ADD)
- V35 `os_itens`: junction table with UNIQUE constraint `(os_uuid, referencia_uuid, tipo)`

## Known Issues
1. **Empty mapper stubs**: `PecaEntityMapper`, `RequisicaoCompraEntityMapper`, `NfEntradaEntityMapper` are dead code
2. **NfEntradaRepositoryImpl bug**: `entity.pecaId` and `entity.requisicaoCompraId` both set to `nfEntrada.getId()` (copy-paste error)
3. **Inconsistent FT/cache**: Cliente, Peca, RequisicaoCompra, NfEntrada repos lack fault tolerance and caching
4. **Entity style inconsistency**: Peca/RequisicaoCompra/NfEntrada use `@Data` (public fields) — others use `@Getter/@Setter` (private)
5. **Panache interface inconsistency**: Some repos use `PanacheRepositoryBase<E, Long>`, others use `PanacheRepository<E>`
6. **No `RefreshTokenEntity`, `RefreshTokenRepository`, `TokenBucketRateLimiter`, `RefreshTokenService`** — these were documented but deleted

## Dependencies (compile)
mekano-domain, quarkus-hibernate-orm-panache, quarkus-arc, quarkus-jdbc-postgresql, quarkus-flyway, quarkus-smallrye-fault-tolerance, quarkus-elytron-security-common, quarkus-smallrye-jwt-build, quarkus-cache, quarkus-config-yaml, mapstruct, lombok (provided)

## Testing
- `@QuarkusTest` + `@TestTransaction` (DevServices PostgreSQL)
- AssertJ fluent assertions
- Test files: `UserRepositoryImplTest`, `VeiculoRepositoryImplTest`, `AuditoriaOrigemTest` (unit), `AuditoriaListenerTest` (unit — SISTEMA fora do Quarkus), `AuditoriaListenerIntegrationTest` (@QuarkusTest — PUBLICO sem usuário), `OrdemDeServicoRepositoryImplTest`, `OrdemDeServicoEntityMapperImplTest`, `WhatsAppPagamentoObserverTest`, `WhatsAppOrcamentoObserverTest`, `WhatsAppCancelamentoObserverTest`, `SlaExpiryJobTest`
