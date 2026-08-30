# Mekano — Clean Architecture Quarkus API

## Overview
API REST em Java 17 com Quarkus 3.36.0 e Maven multi-módulo seguindo Clean Architecture. Domínio de oficina mecânica: gestão de ordens de serviço, clientes, veículos, estoque e faturamento.

**⚠ CRITICAL: This file reflects VERIFIED source code. The old CLAUDE.md files contain outdated information — use AGENTS.md files instead.**

## Module Structure & Dependency Graph (VERIFIED)

```
mekano-rest (quarkus packaging — app entrypoint)
  ├── mekano-application (compile)
  ├── mekano-infrastructure (compile)
  └── mekano-domain (compile, transitiva)

mekano-application (jar) — 7 sub-packages (user, cliente, vehicle, servico, peca, nfentrada, requisicao, ordemdeservico, orcamento, pagamento)
  └── mekano-domain (compile)

mekano-infrastructure (jar) — 15 entities, 15 repos, 18 mappers, 35 migrations, audit auto-fill
  └── mekano-domain (compile)

mekano-domain (jar) — 16 models/enums, 6 VOs, 52 ports, 15 events
  — zero deps de framework (só Lombok provided)
```

## Tech Stack (VERIFIED)

| Tech | Version |
|------|---------|
| Java | 17 |
| Quarkus | 3.36.0 |
| Maven | 3.9.15 (wrapper) |
| MapStruct | 1.6.3 |
| Lombok | 1.18.36 |
| Jandex | 3.5.3 |
| AssertJ | 3.27.3 |
| PostgreSQL | 16-alpine (dev/prod), H2 (test) |

## Module AGENTS.md Files (AUTHORITATIVE)

For detailed per-module conventions, read these instead of old CLAUDE.md:
- `mekano-domain/AGENTS.md` — entities, VOs, ports, exceptions, events
- `mekano-application/AGENTS.md` — services (implemented vs stub), injection style, known bugs
- `mekano-infrastructure/AGENTS.md` — JPA entities, repositories, mappers, migrations, audit auto-fill, FT/cache coverage
- `mekano-rest/AGENTS.md` — resources, DTOs, config files, test patterns, missing items

## Cross-Cutting Conventions (VERIFIED vs Old CLAUDE.md)

### What IS True
| Convention | Detail |
|------------|--------|
| Entities | POJO in domain, `@Builder(access=PRIVATE)`, factory `create()`+`reconstitute()` |
| Hybrid ID | PK `Long id` (auto-increment) + `UUID uuid` (unique, exposed in APIs) |
| Value Objects | Immutable, validate in constructor, `@EqualsAndHashCode` by value |
| Ports | Pure interfaces in domain/ — no framework annotations |
| Services | `@ApplicationScoped`, constructor injection, `@Transactional` on write methods |
| Resources | `@RequestScoped` (mandatory for JWT+UriInfo), `@RolesAllowed`, NEVER `@Transactional` |
| DTOs | Input = Lombok class, Output = record |
| Exception Handling | `ApiExceptionMapper` — RFC 7807 Problem Details (`application/problem+json`) |
| `@Transactional` | In use case (NOT in resource, NOT in repository) |
| Soft delete | `isActive` + `deletedAt` |  |  |
| Audit auto-fill | `createdBy`/`updatedBy` filled by `AuditoriaListener` (`@EntityListeners` in `BaseEntity`); user → subject JWT, anônimo → `PUBLICO`, sem request → `SISTEMA` |

### What is FALSE (Old CLAUDE.md was WRONG)
| Old Doc Claim | Reality |
|---------------|---------|
| `DomainException`/`BusinessException` hierarchy | SINGLE `AppException(RuntimeException)` with `int status` |
| 9 specific exception classes (InvalidEmailException, etc.) | Only `AppException` + `Messages` exist |
| `AuthenticateUserInputPort`, `AuthenticateUserCommand` | DO NOT EXIST |
| `RefreshTokenRepositoryPort`, `RefreshTokenData` | DO NOT EXIST |
| `RefreshTokenEntity`, `TokenBucketRateLimiter`, `RefreshTokenService` | DO NOT EXIST |
| `AuthResource`, `LoginRequest/Response`, `LoginRateLimiterFilter` | DO NOT EXIST |
| `@Authenticated` annotation on UserResource | NOT PRESENT — uses `@RolesAllowed("user")` |
| `JwtTestProfile` | DOES NOT EXIST |
| 10 deprecated `*ExceptionMapper` files | DO NOT EXIST |
| `publicKey.pem` in rest resources | NOT FOUND |

## Key Inconsistencies (Tech Debt — Avoid Repeating)
1. **Naming**: ~~`PecaRepositoryPort`/`NfEntradaRepositoryPort`/`RequisicaoCompraRepositoryPort` use PT-BR~~ **RESOLVED**: all 3 ports now use EN (`save`, `findById`) — remaining PT-BR names (`buscarPorDescricao`, `buscarPorChaveAcesso`, `remover`, `reativar`, `atualizar`, `listarAbaixoEstoqueMinimo`, `debitarSaldo`, `creditarSaldo`, `reservarSaldo`, `debitarSaldoReservado`, `liberarReserva`) are intentional (business operations or deferred)
2. **Injection**: 3 stub services use field injection (`@Inject`) — real services use constructor injection
3. **Entity style**: Newer entities use `@Data` (public fields) — older ones use `@Getter/@Setter` (private)
4. **FT/Cache**: Only User/Veiculo/Servico repos have `@Retry`+`@Timeout`+`@CacheResult` — Cliente/Peca/RequisicaoCompra/NfEntrada do NOT
5. **Duplicate VOs**: `Placa.java` and `PlacaVeiculo.java` overlap with different regex patterns
6. **Misplaced VO**: `ItemOrcamento` is a Value Object but lives in `model/` not `valueobject/`
7. **Empty mappers**: `PecaEntityMapper`, `RequisicaoCompraEntityMapper`, `NfEntradaEntityMapper` are dead code (no methods)
8. **Bug in NfEntradaRepositoryImpl**: `pecaId` and `requisicaoCompraId` both set to `nfEntrada.getId()` (copy-paste error)
9. **Bug in ClienteService.updateCliente**: finds entity but does NOT apply updates

## Decision Records (VERIFIED)
- D-01: `@Transactional` no use case, não no resource nem no repository
- D-02: `PasswordHasher` é interface no domain, `BcryptPasswordHasher` no infrastructure
- D-03: Sem `@CircuitBreaker` — PostgreSQL local não justifica
- D-04: Response records nunca expõem `passwordHash` ou entidade `User`
- D-05: `AppException` unchecked (RuntimeException) — exceção única para todo o domínio
- D-06: `GET /users` sem use case — chamada direta ao repository (leitura pura)
- D-07: CORS global via `quarkus.http.cors.*`
- D-08: Prefixo `/api/v1` via `quarkus.rest.path` (não `@ApplicationPath`)
- D-09: `ApiExceptionMapper` único — RFC 7807 Problem Details
- D-10: Jackson timezone `America/Sao_Paulo`
- D-11: CI sem Docker explícito — DevServices auto-gerencia PostgreSQL
- D-12: Logging JSON (`quarkus.log.console.json=true`)
- D-13: Cache Caffeine inconsistente — User/Veiculo/Servico apenas
- D-14: `@Retry`/`@Timeout` testados via integração; `@CircuitBreaker` omitido
- D-15: Eventos de domínio como records; `EventPublisher` interface pura
- D-16: Audit fields exclusivos de infrastructure — `AuditoriaListener` (`@EntityListeners` em `BaseEntity`) preenche `createdBy` no `@PrePersist` e `updatedBy` no `@PreUpdate`; resolve via `SecurityIdentity` (subject JWT) com fallbacks `PUBLICO`/`SISTEMA`; sem backfill
- D-17: OS ↔ Peça/Serviço é many-to-many via tabela pivô `os_itens` (não FK columns na OS). Itens adicionados na criação (atendente) e no `finalizarDiagnostico` (mecânico). Orçamento gerado automaticamente a partir de TODOS os itens da tabela pivô.

## Commands (VERIFIED)
```bash
docker-compose up -d
./mvnw quarkus:dev

./mvnw verify -pl mekano-rest -am                                   # full test suite
./mvnw test -pl mekano-domain                                       # unit tests (<3s)
./mvnw test -pl mekano-application -am                              # Mockito tests
./mvnw test -pl mekano-infrastructure -am                           # integration tests
./mvnw test -pl mekano-rest -am                                     # REST Assured E2E

./mvnw package -Dnative -pl mekano-rest -am                         # native build
```

## Build Gotchas (CRITICAL — do not ignore)
| # | Issue | Fix |
|---|-------|-----|
| G1 | `quarkus-maven-plugin` in non-quarkus module | Plugin ONLY in mekano-rest (skip=true elsewhere) |
| G2 | Missing Jandex | `jandex-maven-plugin` in app/infra/rest |
| G3 | Wrong annotation processor order | Lombok → lombok-mapstruct-binding → mapstruct-processor |
| G4 | Flyway V1 without double underscore | `V1__desc.sql` |
| G5 | `migrate-at-start` default false | `quarkus.flyway.migrate-at-start=true` |
| G6 | Using `quarkus.smallrye-jwt.*` namespace | Use `mp.jwt.*` |
| G7 | RSA key not PKCS#8 | Use PKCS#8 or Ed25519 |
| G8 | `@ApplicationScoped` in Resource with JWT | Use `@RequestScoped` |
| G9 | MapStruct `componentModel = "spring"` | Always `"cdi"` |
| G10 | ExceptionMapper without `@Provider` | `@Provider @ApplicationScoped` |
| G11 | MapStruct impl class files deleted by test-compile recompilation | `maven-compiler-plugin` with `<useIncrementalCompilation>false</useIncrementalCompilation>` in mekano-rest |
| G12 | Flyway duplicate migration versions | Keep unique version numbers per V-file (renamed V21→V22→V23→V24) |
| G13 | Duplicate `maven-compiler-plugin` in parent POM `pluginManagement` | Removed duplicate; single declaration with Lombok annotation paths |

## Database
- PostgreSQL 16-alpine (dev/prod), docker-compose na raiz
- H2 in-memory `MODE=PostgreSQL` (test) — no Docker needed
- Flyway V1-V35 in `mekano-infrastructure/src/main/resources/db/migration/`
- Flyway does NOT run in tests (Hibernate `drop-and-create`)
- H2 compatibility: no `BIGSERIAL` (use `BIGINT GENERATED BY DEFAULT AS IDENTITY`), no multi-column `ADD COLUMN`

## API Prefix
`quarkus.rest.path=/api/v1` → endpoints em `/api/v1/users`, `/api/v1/veiculos`, `/api/v1/servicos`

## Missing / Planned Items (do NOT assume they exist)
- Auth endpoints (`/auth/login`, `/auth/refresh`) — NOT IMPLEMENTED
- Cliente controller (`/clientes`) — DTOs/mapper exist, NO resource
- Rate limiting filter — NOT IMPLEMENTED
- JwtTestProfile — NOT IMPLEMENTED
