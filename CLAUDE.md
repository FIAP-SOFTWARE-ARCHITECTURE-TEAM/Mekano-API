# Mekano — Clean Architecture Quarkus API

## Overview

API REST em Java 17 com Quarkus 3.36.0 e Maven multi-módulo seguindo Clean Architecture. Domínio de oficina mecânica (futuro), atualmente implementa apenas o subsistema de autenticação/usuários (User).

## Tech Stack

| Tech | Versão |
|------|--------|
| Java | 17 |
| Quarkus | 3.36.0 |
| Maven | 3.9.15 (wrapper) |
| MapStruct | 1.6.3 |
| Lombok | 1.18.36 |
| Jandex | 3.5.3 |
| AssertJ | 3.27.3 |
| PostgreSQL | 16-alpine |

## Module Structure & Dependency Graph

```
mekano-rest (quarkus packaging — app entrypoint)
  ├── mekano-application (compile)
  ├── mekano-infrastructure (compile)
  └── mekano-domain (compile, transitiva)

mekano-application (jar)
  └── mekano-domain (compile)

mekano-infrastructure (jar)
  └── mekano-domain (compile)

mekano-domain (jar) — zero deps de framework (só Lombok provided)
```

## Package Structure

```
com.fiap.mekano.{layer}.{subdomain}
```

| Módulo | Package Base | Pacotes |
|--------|-------------|---------|
| domain | `com.fiap.mekano.domain` | `model`, `valueobject`, `port/in`, `port/out`, `exception`, `event` |
| application | `com.fiap.mekano.application` | `service/user` |
| infrastructure | `com.fiap.mekano.infrastructure` | `entity` (inclui `BaseEntity`), `repository`, `mapper`, `service`, `security`, `event` |
| rest | `com.fiap.mekano.rest` | `api`, `api/dto`, `api/mapper`, `api/exception`, `api/filter`, `observability` |

## Commands

```bash
# Dev mode (requer PostgreSQL via docker-compose)
docker-compose up -d
./mvnw quarkus:dev

# Testes completos
./mvnw verify -pl mekano-rest -am

# Testes por camada
./mvnw test -pl mekano-domain                                 # unitários puros (< 3s)
./mvnw test -pl mekano-application -am                         # unitários com Mockito
./mvnw test -pl mekano-infrastructure -am                      # integração com DevServices
./mvnw test -pl mekano-rest -am                                # REST Assured end-to-end

# Native build
./mvnw package -Dnative -pl mekano-rest -am
./mvnw package -Dnative -Dquarkus.native.container-build=true  # em container Docker
```

## Key Conventions

### Code
- **Entidades**: POJO puro no domain (`User`), JPA Panache no infrastructure (`UserEntity` estende `BaseEntity` que estende `PanacheEntityBase`)
- **Hybrid ID**: PK sequencial (`Long id` auto-increment) no banco + `UUID uuid` (unique) exposto em APIs — previne enumeração de recursos
- **Value Objects**: Imutáveis, validam no construtor, `@EqualsAndHashCode` por valor
- **Ports**: Interfaces puras no domain/ — sem anotações de framework
- **Services**: `@ApplicationScoped`, injetam ports via construtor, `@Transactional` no método execute()
- **Resources**: `@RequestScoped` (obrigatório para JWT + `@Context UriInfo`), `@Authenticated`, `@RolesAllowed("user")`
- **DTOs**: Input = classe Lombok com Bean Validation; Output = record Java
- **Mappers**: MapStruct `componentModel = "cdi"`, ordem annotationProcessorPaths: `lombok → lombok-mapstruct-binding → mapstruct-processor`
- **Exception Handling**: `ApiExceptionMapper` único — RFC 7807 Problem Details (`application/problem+json`)

### Build
- `quarkus-maven-plugin` APENAS no `mekano-rest` — nos demais módulos está em `<pluginManagement>` com `<skip>true</skip>`
- `jandex-maven-plugin` em `application`, `infrastructure` e `rest` — obrigatório para descoberta CDI
- Annotation Processor Paths ORDEM EXATA: Lombok → binding → MapStruct (ordem errada produz mappers com campos null)

### Testes
- **Domain**: JUnit 5 puro, sem Quarkus — `assertThrows`, `@ParameterizedTest`
- **Application**: Mockito `@ExtendWith(MockitoExtension.class)`, `@Mock` + `@InjectMocks`
- **Infrastructure**: `@QuarkusTest` + `@TestTransaction`, AssertJ fluent, DevServices PostgreSQL
- **REST**: `@QuarkusTest` + REST Assured, `@TestSecurity` para bypass JWT, `@TestMethodOrder` para testes sequenciais

## JWT / Auth

- Algoritmo: **Ed25519/EdDSA** (não RSA)
- Public key: `mekano-rest/src/main/resources/publicKey.pem` (committed)
- Private key: `~/.mekano/secrets/privatekey.pem` (gitignored)
- Config: namespace `mp.jwt.*` (nunca `quarkus.smallrye-jwt.*`)
- `quarkus.http.auth.proactive=false` — permite ExceptionMapper tratar 401 antes do pipeline de segurança
- Testes JWT: `JwtTestProfile` gera par Ed25519 in-memory, sobrescreve `mp.jwt.verify.publickey.location` via System properties

## Database

- PostgreSQL 16-alpine, docker-compose na raiz
- Flyway: `V1__` a `V4__` em `mekano-infrastructure/src/main/resources/db/migration/`
- DevServices em testes (sem `jdbc.url` no perfil test)
- Soft delete: campo `is_active` + `deleted_at` — queries filtram `isActive = true`
- `@Transactional` no use case (não no resource, não no repository)

## API Prefix

`quarkus.rest.path=/api/v1` → endpoints em `/api/v1/users`, `/api/v1/auth`

## Gotchas (Armadilhas Críticas)

| # | Problema | Sintoma | Fix |
|---|----------|---------|-----|
| G1 | `quarkus-maven-plugin` em módulo não-quarkus | Build quebra | Plugin só em `mekano-rest` |
| G2 | Jandex ausente | `UnsatisfiedResolutionException` | `jandex-maven-plugin` em app/infra/rest |
| G3 | Ordem annotationProcessorPaths errada | Mapper compila mas campos null | Lombok → binding → MapStruct |
| G4 | Flyway `V1` sem duplo underscore | Migrations ignoradas | `V1__desc.sql` |
| G5 | `migrate-at-start` default = false | Nenhuma migration executa | `quarkus.flyway.migrate-at-start=true` |
| G6 | Namespace JWT `quarkus.smallrye-jwt.*` | 401 silencioso | Usar `mp.jwt.*` |
| G7 | Chave RSA sem PKCS#8 | JWT rejeitado | Usar PKCS#8 ou Ed25519 |
| G8 | `@ApplicationScoped` em Resource com JWT | Injection de claims quebra | `@RequestScoped` obrigatório |
| G9 | MapStruct `componentModel = "spring"` | NPE no mapper | Sempre `"cdi"` |
| G10 | ExceptionMapper sem `@Provider` | Mapper ignorado → 500 genérico | `@Provider @ApplicationScoped` |

## Decision Records (resumo)

- D-01: `@Transactional` no use case, não no resource nem no repository
- D-02: `PasswordHasher` é interface no domain, `BcryptPasswordHasher` no infrastructure
- D-03: Sem `@CircuitBreaker` — PostgreSQL local não justifica
- D-04: `CreateUserResponse` record em vez de expor entidade `User`
- D-05: `BusinessException` checked, `DomainException` unchecked
- D-06: `GET /users` sem use case — chamada direta ao repository (leitura pura)
- D-07: CORS global via `quarkus.http.cors.*`
- D-08: Prefixo `/api/v1` via `quarkus.rest.path` (não `@ApplicationPath`)
- D-09: `ApiExceptionMapper` único — RFC 7807 Problem Details
- D-10: Jackson timezone `America/Sao_Paulo`
- D-11: CI sem Docker explícito — DevServices auto-gerencia PostgreSQL
- D-12: Logging JSON (`quarkus.log.console.json=true`)
- D-13: Cache Caffeine em `findById`/`findByEmail` com invalidate em save/delete
- D-14: `@Retry`/`@Timeout` testados via integração; `@CircuitBreaker` omitido
- D-15: `UserCreatedEvent` record no domain; `EventPublisher` interface pura
- D-16: Audit fields (`created_by`, `updated_by`, `updated_at`) exclusivos de infrastructure
