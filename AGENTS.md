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
| PostgreSQL | 16-alpine (dev/prod), 16-alpine via Testcontainers (test) |

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

### What is TRUE (old CLAUDE.md was WRONG — re-verified 2026-10-17)
| Claim | Reality |
|---------------|---------|
| `DomainException`/`BusinessException` hierarchy | SINGLE `AppException(RuntimeException)` with `int status` |
| 9 specific exception classes (InvalidEmailException, etc.) | Only `AppException` + `Messages` exist |
| ~~`AuthenticateUserInputPort`, `AuthenticateUserCommand`~~ | N/A — auth usa `AuthServicePort` + `LoginCommand` |
| `RefreshTokenRepositoryPort`, `RefreshTokenData` | EXIST in domain (`port/out`) — auth flow implemented |
| ~~`RefreshTokenEntity`, `TokenBucketRateLimiter`, `RefreshTokenService`~~ | `RefreshTokenEntity` + `RefreshTokenRepositoryImpl` + `RefreshTokenService` EXIST |
| ~~`AuthResource`, `LoginRequest/Response`~~ | `AuthResource` EXISTS at `rest/api/infrastructure/rest/auth/` (`/auth/login`, `/auth/refresh`, `/auth/logout`) — the old path listed here was wrong |
| `@Authenticated` annotation on UserResource | NOT PRESENT — uses `@RolesAllowed(...)` |
| `JwtTestProfile` | DOES NOT EXIST |
| 10 deprecated `*ExceptionMapper` files | DO NOT EXIST |
| `publicKey.pem` in rest resources | NOT FOUND (dev key at `${user.home}/.mekano/secrets/`) |

## Key Inconsistencies (Tech Debt — re-verified 2026-10-17)
1. **Naming**: ports use EN (`save`, `findById`); remaining PT-BR names (`buscarPorDescricao`, `creditarSaldo`, etc.) are intentional business operations
2. ~~2 stub services with field injection~~ **RESOLVED** — `PecaService`/`NfEntradaService` are real implementations with constructor injection and tests
3. **Entity style**: newer entities (Peca, RequisicaoCompra, NfEntrada) use `@Data` (public fields) — older ones use `@Getter/@Setter` (private)
4. **FT/Cache**: Only User/Veiculo/Servico/OS repos have `@Retry`+`@Timeout`+`@CacheResult` — Cliente/Peca/RequisicaoCompra/NfEntrada do NOT
5. ~~Duplicate VOs `Placa`/`PlacaVeiculo`~~ **RESOLVED** — only `PlacaVeiculo` exists
6. **Misplaced VO**: `ItemOrcamento` is a Value Object but lives in `model/` not `valueobject/`
7. ~~Empty mappers~~ **RESOLVED** — deleted from repo
8. ~~NfEntradaRepositoryImpl bug~~ **RESOLVED/NA** — model é enxuto por design (chaveAcesso, valorTotal, requisicaoCompraId, dataRecebimento); itens derivados da RequisicaoCompra (sem peca_id)
9. ~~ClienteService.updateCliente não aplica updates~~ **RESOLVED** — chama `cliente.atualizar(...)` + save
10. **Dead code**: `OrdemDeServico3.java` (bloco comentado 100%) e `reconstitute()` antigo comentado em `OrdemDeServico.java`
11. **Roles**: `OrcamentoResource` class-level `@RolesAllowed` inclui `mecanico` e mutações aprovar/reprovar são `@PermitAll` — revisar (ver docs/ARCHITECTURE-REVIEW-2026-10-17.md)

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
- D-18: Requisição de Compra `criar()` valida existência da Peça via `PecaRepositoryPort.findById()` (AppException 404). `cancelar()` bloqueia quando `motivo=ORDEM_SERVICO` (AppException 409) — a requisição faz parte do fluxo de execução da OS e não pode ser cancelada livremente.

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
- Testes de integração rodam em **PostgreSQL real** via DevServices/Testcontainers
  (`postgres:16-alpine`) — requer Docker rodando na máquina
- Flyway 33 migrations in `mekano-infrastructure/src/main/resources/db/migration/`
- Flyway does NOT run in tests (Hibernate `drop-and-create`); migrations exercitadas
  pelos testes funcionais Newman no CI (docker compose)
- Migration compatibility: no `BIGSERIAL` (use `BIGINT GENERATED BY DEFAULT AS IDENTITY`), no multi-column `ADD COLUMN`

## API Prefix
`quarkus.rest.path=/api/v1` → endpoints em `/api/v1/users`, `/api/v1/veiculos`, `/api/v1/servicos`

## Missing / Planned Items (re-verified 2026-10-17)
- ~~Auth endpoints~~ **IMPLEMENTED** — `/auth/login`, `/auth/refresh`, `/auth/logout` (`AuthResource` + `AuthServicePort` + `RefreshTokenService`)
- ~~Cliente controller~~ **IMPLEMENTED** — `ClienteResource` existe com DTOs, mapper e testes
- Rate limiting filter (`/auth/*` e webhook) — **NOT IMPLEMENTED** (prioridade P1, ver docs/ARCHITECTURE-REVIEW-2026-10-17.md)
- JwtTestProfile — NOT IMPLEMENTED (testes usam `@TestSecurity`)
