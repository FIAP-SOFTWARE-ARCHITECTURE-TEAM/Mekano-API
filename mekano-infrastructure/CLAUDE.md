# mekano-infrastructure — Implementações Concretas

## Overview

Camada de infraestrutura — implementa as interfaces (ports) do domínio usando tecnologia real: JPA/Hibernate Panache, Flyway, BCrypt, JWT, Cache Caffeine, eventos CDI.

**Regra fundamental**: classes daqui conhecem o domínio (implementam suas interfaces), mas o domínio NÃO conhece a infraestrutura.

## Package Structure

```
com.fiap.mekano.infrastructure
├── cache/                     # Cache names centralizados
│   └── CacheNames.java                # Constantes para anotações @CacheResult
├── entity/                    # Entidades JPA
│   ├── BaseEntity.java                # @MappedSuperclass — campos de auditoria
│   ├── UserEntity.java                # Estende BaseEntity
│   └── RefreshTokenEntity.java        # Estende PanacheEntityBase diretamente
├── repository/                # Implementações concretas dos ports de saída
│   ├── UserPanacheRepository.java     # Herda PanacheRepositoryBase (evita conflito assinatura)
│   ├── UserRepositoryImpl.java        # Implementa UserRepositoryPort
│   ├── RefreshTokenPanacheRepository.java
│   └── RefreshTokenRepositoryImpl.java
├── mapper/                    # MapStruct — entidade JPA ↔ domínio
│   ├── UserEntityMapper.java
│   └── EmailMapper.java
├── service/                   # Serviços técnicos
│   ├── RefreshTokenService.java
│   └── TokenPair.java
├── security/                  # Implementações de segurança
│   ├── BcryptPasswordHasher.java      # Implementa PasswordHasher
│   └── TokenBucketRateLimiter.java
└── event/                     # Eventos CDI
    └── CdiEventPublisher.java         # Implementa EventPublisher
```

## Key Conventions

### JPA Entities

- `BaseEntity` (`@MappedSuperclass`) — classe base com PK e campos de auditoria:
  - `Long id` com `@Id @GeneratedValue(IDENTITY)` — PK sequencial (joins, FK performance)
  - `LocalDateTime createdAt` (não-nulo), `LocalDateTime updatedAt`, `UUID createdBy`, `UUID updatedBy`
  - `@PreUpdate` em `preUpdate()` — define `updatedAt` automaticamente
  - Estende `PanacheEntityBase` — entidades concretas herdam acesso a métodos Panache
- Entidades que precisam de auditoria estendem `BaseEntity` (ex: `UserEntity`)
- Entidades sem auditoria estendem `PanacheEntityBase` com sua própria `@Id Long id` (ex: `RefreshTokenEntity`)
- **Hybrid ID**: PK sequencial (`Long id`) para o banco + coluna `UUID uuid` (unique) exposta em APIs — evita enumeração sequencial via endpoints
- Mapeamento explícito: `@Table(name = "users")`, `@Column(name = "...")`
- Soft delete: `Boolean isActive`, `LocalDateTime deletedAt`

### Repository Pattern (Two-Class)

A PK interna é `Long` (auto-increment). Como `UserRepositoryPort.findById(UUID)` recebe UUID (público), usamos HQL customizado (`"uuid = ?1"`) em vez de `findByIdOptional()` — que opera sobre a PK interna.

Usamos **duas classes** para clareza:

1. `UserPanacheRepository` — herda `PanacheRepositoryBase<UserEntity, Long>`, bean separado
2. `UserRepositoryImpl` — implementa `UserRepositoryPort`, delega para `UserPanacheRepository` via injeção

### MapStruct Patterns

- `@Mapper(componentModel = "cdi")` — NUNCA `"spring"` (G9)
- `@Mapper(uses = {EmailMapper.class})` para reutilizar conversões de VO
- `default` methods para lógica de mapeamento complexa (ex: `toDomain()` que chama `User.reconstitute()`)
- `EmailMapper` é `@ApplicationScoped @Named("EmailMapper")` — métodos nomeados para Mapping
- `unmappedTargetPolicy = ReportingPolicy.IGNORE` quando a entidade domain não possui campos de infraestrutura (audit, soft delete) — evita warnings de campos não mapeados

### Fault Tolerance

- `@Retry(maxRetries = 3)` em leituras (`findById`, `findByEmail`)
- `@Timeout(value = 5, unit = ChronoUnit.SECONDS)` em escrita (`save`)
- SEM `@CircuitBreaker` (D-03 — PostgreSQL local não justifica)
- SEM `@Retry` em `save` (escrita não-idempotente)

### Cache Caffeine

- Cache names centralizados em `CacheNames.java` (`CacheNames.USERS`)
- `@CacheResult(cacheName = CacheNames.USERS)` em `findById`, `findByEmail`
- `@CacheInvalidate(cacheName = CacheNames.USERS)` em `save`, `markAsDeleted`
- Config: `cache-config.yml` com `expire-after-write=60s`, `maximum-size=100`

### Flyway Migrations

Local: `src/main/resources/db/migration/`

| Migration | Descrição |
|-----------|-----------|
| `V1__create_users_table.sql` | Tabela `users` com id UUID, name, email UNIQUE, password_hash, created_at |
| `V2__create_refresh_tokens_table.sql` | Tabela `refresh_tokens` com jti UNIQUE, token_hash, user_id FK, expires_at, rotated_at |
| `V3__add_soft_delete_to_users.sql` | Adiciona `deleted_at`, `is_active` com default true, índice |
| `V4__add_audit_columns_to_users.sql` | Adiciona `created_by`, `updated_by`, `updated_at` |
| `V5__add_sequential_id.sql` | Hybrid ID: renomeia `id`→`uuid`, adiciona `BIGSERIAL id` como PK, reconstrói FK |

## Dependencies

- **compile**: `mekano-domain`, `quarkus-hibernate-orm-panache`, `quarkus-arc`, `quarkus-jdbc-postgresql`, `quarkus-flyway`, `quarkus-smallrye-fault-tolerance`, `quarkus-elytron-security-common`, `quarkus-smallrye-jwt-build`, `quarkus-cache`, `mapstruct`, `lombok` (provided)
- **test**: `quarkus-junit5`, `assertj-core`
- **annotationProcessorPaths**: Lombok → lombok-mapstruct-binding → mapstruct-processor (G3)

## How to Add New Repository

1. Criar `entity/NovaEntity.java` — `@Entity @Table`
   - Se precisar de auditoria: `extends BaseEntity` (herda `Long id` PK + audit fields)
   - Se não precisar: `extends PanacheEntityBase` com `@Id @GeneratedValue(IDENTITY) Long id` próprio
   - Adicionar campo `UUID uuid` (unique, not null) para exposição segura em APIs
2. Criar `repository/NovoPanacheRepository.java` — estende `PanacheRepositoryBase<NovaEntity, Long>` (PK é Long)
3. Criar `mapper/NovoEntityMapper.java` — `@Mapper(componentModel = "cdi")`
4. Criar `repository/NovoRepositoryImpl.java` — implementa interface do port, delega para PanacheRepository
5. Criar migration `V5__create_nova_tabela.sql`
6. Adicionar port de saída no domain se não existir

## Testing

- `@QuarkusTest` — inicializa container Quarkus para testes de integração
- `@TestTransaction` — rollback automático por teste
- DevServices PostgreSQL — sem `jdbc.url` no perfil test
- AssertJ fluent para asserções
- Test profiles para configurações específicas

Exemplos: `UserRepositoryImplTest.java`, `RefreshTokenServiceTest.java`, `TokenBucketRateLimiterTest.java`
