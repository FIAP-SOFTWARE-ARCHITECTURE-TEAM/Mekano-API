<!-- refreshed: 2026-06-20 -->
# Architecture

**Analysis Date:** 2026-06-20

## System Overview

```text
┌──────────────────────────────────────────────────────────────────────┐
│                    mekano-rest  (Quarkus adapter layer)               │
│  JAX-RS Resources | DTOs | REST Mappers | Exception Mapper | Health  │
│  `mekano-rest/src/main/java/com/fiap/mekano/rest/api/`               │
├──────────────────────┬──────────────────────┬───────────────────────┤
│     CreateUserRequest│    UserDtoMapper     │     UserResource      │
│  `.../dto/`          │  `.../mapper/`       │   `.../UserResource`  │
│     UserResponse     │                      │                       │
│     UserPageResponse │                      │                       │
└──────────┬───────────┴──────────┬───────────┴───────────┬───────────┘
           │                      │                       │
           ▼                      ▼                       ▼
┌──────────────────────────────────────────────────────────────────────┐
│                mekano-application  (Service layer)                   │
│          `mekano-application/src/main/java/.../application/`          │
│           UserService — @Transactional orchestration            │
│           ├─ PasswordHasher.hash()                                    │
│           ├─ User.create()                                           │
│           ├─ userRepository.save()                                   │
│           └─ eventPublisher.publish()                                │
└───────────────────────────────────┬──────────────────────────────────┘
                                    │
                                    ▼
┌──────────────────────────────────────────────────────────────────────┐
│              mekano-domain  (Pure business logic — zero deps)          │
│  `mekano-domain/src/main/java/com/fiap/mekano/domain/`                │
│  model/User  |  valueobject/Email  |  port/{in,out}/*  |  exception/  │
│  Interfaces: UserServicePort, PasswordHasher,                     │
│              UserRepositoryPort, EventPublisher                       │
└──────────┬──────────────────────────────┬────────────────────────────┘
           │                              │
           ▼                              ▼
┌──────────────────────┐  ┌──────────────────────────────────────────┐
│ mekano-infrastructure│  │ mekano-infrastructure                     │
│ (persistence)         │  │ (security/events)                        │
│                       │  │                                          │
│ UserPanacheRepository │  │ BcryptPasswordHasher                    │
│ UserRepositoryImpl    │  │ CdiEventPublisher                       │
│ UserEntity (JPA)      │  │                                          │
│ UserEntityMapper      │  │                                          │
│ (MapStruct)           │  │                                          │
│ Flyway migrations     │  │                                          │
└───────────────────────┘  └──────────────────────────────────────────┘
```

## Component Responsibilities

| Component | Responsibility | File |
|-----------|----------------|------|
| `UserResource` | HTTP entry point, DTO conversion, response building | `mekano-rest/src/main/java/com/fiap/mekano/rest/api/UserResource.java` |
| `MekanoApiApplication` | JAX-RS + OpenAPI bootstrap | `mekano-rest/src/main/java/com/fiap/mekano/rest/api/MekanoApiApplication.java` |
| `UserService` | Orchestrates user creation: validate → hash → create → persist → event | `mekano-application/src/main/java/com/fiap/mekano/application/service/user/UserService.java` |
| `User` (domain) | Pure domain entity, factory methods `create()` and `reconstitute()` | `mekano-domain/src/main/java/com/fiap/mekano/domain/model/User.java` |
| `Email` (value object) | Encapsulates email validation and normalization | `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/Email.java` |
| `UserServicePort` | Input port interface for user creation Service | `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/UserServicePort.java` |
| `UserRepositoryPort` | Output port interface for user persistence | `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/UserRepositoryPort.java` |
| `PasswordHasher` | Input port interface for password hashing abstraction | `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/PasswordHasher.java` |
| `EventPublisher` | Output port interface for domain event publishing | `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/EventPublisher.java` |
| `UserRepositoryImpl` | Implements `UserRepositoryPort` using JPA Panache | `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/UserRepositoryImpl.java` |
| `UserPanacheRepository` | Extends `PanacheRepositoryBase<UserEntity, Long>` | `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/UserPanacheRepository.java` |
| `UserEntity` | JPA entity, extends `BaseEntity` (Panache) | `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/UserEntity.java` |
| `BaseEntity` | `@MappedSuperclass` with PK, audit fields, soft delete | `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/BaseEntity.java` |
| `BcryptPasswordHasher` | Implements `PasswordHasher` using Quarkus `BcryptUtil` | `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/security/BcryptPasswordHasher.java` |
| `CdiEventPublisher` | Implements `EventPublisher` using CDI events | `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/event/CdiEventPublisher.java` |
| `UserEntityMapper` / `UserEntityMapperImpl` | MapStruct: JPA entity ↔ domain | `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/UserEntityMapper.java` |
| `UserDtoMapper` | MapStruct: REST DTO ↔ domain command/entity | `mekano-rest/src/main/java/com/fiap/mekano/rest/api/mapper/UserDtoMapper.java` |
| `ApiExceptionMapper` | Single `ExceptionMapper<Exception>` — RFC 7807 Problem Details | `mekano-rest/src/main/java/com/fiap/mekano/rest/api/exception/ApiExceptionMapper.java` |
| `ApplicationLivenessCheck` | Custom `/q/health/live` liveness check | `mekano-rest/src/main/java/com/fiap/mekano/rest/observability/ApplicationLivenessCheck.java` |

## Pattern Overview

**Overall:** Clean Architecture with strict dependency inversion

**Key Characteristics:**
- **Domain layer has zero framework dependencies** — no Jakarta, Quarkus, or Hibernate imports. Only Java SE + Lombok (`scope=provided`).
- **Dependencies flow inward**: `rest → application → domain` and `rest → infrastructure → domain`. The domain never imports from outer layers.
- **Port/Adapter pattern**: `domain/port/in/` (driving side) and `domain/port/out/` (driven side) are pure interfaces. `application` implements in-ports. `infrastructure` implements out-ports.
- **Cross-module CDI resolution** via Jandex indexing: `application`, `infrastructure`, and `rest` modules each run `jandex-maven-plugin` to generate CDI bean indexes. Without this, `@Inject` across module boundaries causes `UnsatisfiedResolutionException`.

## Layers

**Domain Layer (`mekano-domain`):**
- Purpose: Business entities, value objects, port interfaces, domain events, exceptions
- Location: `mekano-domain/src/main/java/com/fiap/mekano/domain/`
- Contains: `model/`, `valueobject/`, `port/in/`, `port/out/`, `exception/`, `event/`
- Depends on: Nothing (Java SE + Lombok provided)
- Used by: `mekano-application`, `mekano-infrastructure`, `mekano-rest`

**Application Layer (`mekano-application`):**
- Purpose: Service orchestration — validates, calls ports, coordinates business rules
- Location: `mekano-application/src/main/java/com/fiap/mekano/application/service/user/`
- Contains: Service classes (e.g., `UserService`), response records (e.g., `CreateUserResponse`)
- Depends on: `mekano-domain`, `quarkus-arc` (for `@ApplicationScoped`), `quarkus-elytron-security-common`
- Used by: `mekano-rest`

**Infrastructure Layer (`mekano-infrastructure`):**
- Purpose: Concrete implementations of domain out-ports using real technology
- Location: `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/`
- Contains: `entity/` (JPA), `repository/` (Panache), `mapper/` (MapStruct), `security/` (BCrypt), `event/` (CDI)
- Depends on: `mekano-domain`, Quarkus extensions (Panache, Flyway, BCrypt, Cache, FT)
- Used by: `mekano-rest` (compile scope)

**REST / Adapter Layer (`mekano-rest`):**
- Purpose: HTTP entry point, DTOs, exception handling, observability
- Location: `mekano-rest/src/main/java/com/fiap/mekano/rest/`
- Contains: `api/` (resources, DTOs, mappers, exception handlers), `observability/` (health checks)
- Depends on: All three inner modules + Quarkus extensions (REST Jackson, JWT, OpenAPI, Health, Micrometer, etc.)
- **Only module** with `packaging=quarkus` and `quarkus-maven-plugin` active

## Module Dependency Graph

```
mekano-rest (quarkus packaging)
  ├── mekano-application (compile)
  │    └── mekano-domain (compile)
  ├── mekano-infrastructure (compile)
  │    └── mekano-domain (compile)
  └── mekano-domain (compile, transitive)

mekano-application (jar)
  └── mekano-domain (compile)

mekano-infrastructure (jar)
  └── mekano-domain (compile)

mekano-domain (jar) — zero framework deps
```

## Data Flow

### Primary Request Path: POST /api/v1/users (Create User)

1. **HTTP entry** — `UserResource.create()` (`mekano-rest/.../UserResource.java:113-126`)
   - Receives JSON body → Bean Validation validates `CreateUserRequest`
   - `@RequestScoped` resource with `@RolesAllowed("user")`
   - `@Authenticated` JWT required

2. **DTO → Command** — `UserDtoMapper.toCommand()` (`mekano-rest/.../mapper/UserDtoMapper.java:37`)
   - `CreateUserRequest` (Lombok class) → `CreateUserCommand` (domain record)

3. **Service execution** — `UserService.execute()` (`mekano-application/.../service/user/UserService.java:51-75`)
   - `@Transactional` — transaction opened at this boundary
   - Validates name is not null/blank (`AppException(400)`)
   - Checks email uniqueness via `UserRepositoryPort.existsByEmail()` (`AppException(409)` if exists)
   - Hashes password via `PasswordHasher.hash()` (BCrypt)
   - Creates `User` entity via `User.create()` (validates Email VO)
   - Persists via `UserRepositoryPort.save()`
   - Publishes `UserCreatedEvent` via `EventPublisher.publish()`
   - Returns saved `User` entity

4. **User → Response** — `UserDtoMapper.toResponse()` (`mekano-rest/.../mapper/UserDtoMapper.java:48-49`)
   - `User` domain entity → `UserResponse` record (no passwordHash)

5. **HTTP response** — `201 Created` with `Location` header and `UserResponse` body

### Secondary Flow: GET /api/v1/users (List Users — direct repository access)

1. `UserResource.listAll()` (`mekano-rest/.../UserResource.java:146-158`)
2. Bypasses Service layer (D-06: reads are pure queries)
3. Calls `UserRepositoryPort.findAll(page, size, sort)` + `countAll()` directly
4. Returns `UserPageResponse` containing `List<UserResponse>` and pagination metadata

### Secondary Flow: DELETE /api/v1/users/{id} (Soft Delete)

1. `UserResource.delete()` (`mekano-rest/.../UserResource.java:205-208`)
2. `UserServicePort.deleteUser(id)` → `UserRepositoryPort.markAsDeleted(id)`
3. Sets `isActive = false` and `deletedAt = now()` on the JPA entity
4. Returns `204 No Content`

### Persistence Flow (Repository Layer)

1. `UserRepositoryImpl` (`mekano-infrastructure/.../repository/UserRepositoryImpl.java`)
2. Delegates to `UserPanacheRepository` (extends `PanacheRepositoryBase<UserEntity, Long>`)
3. `UserEntityMapperImpl.toEntity()` / `toDomain()` converts between JPA entity and domain entity
4. Uses HQL with soft-delete filter: `"uuid = ?1 AND isActive = ?2"`
5. `@CacheResult` / `@CacheInvalidate` on read/write operations (Caffeine, 60s TTL, max 100 entries)
6. `@Retry(maxRetries=3)` on reads (`findById`, `findByEmail`)
7. `@Timeout(5s)` on `save()`

## Key Abstractions

**UserServicePort (`domain/port/in/UserServicePort.java`):**
- Purpose: Driving port for user management Services
- Methods: `execute(CreateUserCommand)`, `findUserById(UUID)`, `deleteUser(UUID)`
- Implementation: `UserService` (`mekano-application/.../service/user/UserService.java`)

**PasswordHasher (`domain/port/in/PasswordHasher.java`):**
- Purpose: Abstraction for password hashing
- Methods: `hash(String)`, `matches(String, String)`
- Implementation: `BcryptPasswordHasher` (`mekano-infrastructure/.../security/BcryptPasswordHasher.java`)
- Note: Interface lives in `domain/port/in/` because it's used by the application layer (driving side). This is a deliberate deviation from strict hexagonal architecture.

**UserRepositoryPort (`domain/port/out/UserRepositoryPort.java`):**
- Purpose: Driven port for user persistence
- Methods: `save()`, `findById()`, `findByEmail()`, `existsByEmail()`, `findAll()`, `countAll()`, `markAsDeleted()`
- Implementation: `UserRepositoryImpl` (`mekano-infrastructure/.../repository/UserRepositoryImpl.java`)
- Uses two-class pattern: `UserPanacheRepository` (Panache inheritance) + `UserRepositoryImpl` (port implementation)

**EventPublisher (`domain/port/out/EventPublisher.java`):**
- Purpose: Driven port for domain event publishing
- Method: `<T> void publish(T event)`
- Implementation: `CdiEventPublisher` (`mekano-infrastructure/.../event/CdiEventPublisher.java`)
- Uses `jakarta.enterprise.event.Event.fire()` under the hood

**CreateUserCommand (`domain/port/in/CreateUserCommand.java`):**
- Purpose: Immutable data carrier for user creation
- Fields: `name`, `email`, `password` (plaintext — hashed by Service)

**Hybrid ID Pattern:**
- JPA PK: `Long id` with `@GeneratedValue(IDENTITY)` — efficient for joins and FKs
- Public UUID: `UUID uuid` with `UNIQUE` constraint — exposed in APIs, prevents resource enumeration
- `BaseEntity` (`mekano-infrastructure/.../entity/BaseEntity.java:30-61`) defines the PK as `Long`

## Entry Points

**HTTP API Entry Point:**
- Location: `mekano-rest/src/main/java/com/fiap/mekano/rest/api/UserResource.java`
- Base path: `/api/v1/users` (configured via `quarkus.rest.path=/api/v1` in `application.properties`)
- Authentication: JWT with `@RolesAllowed("user")`
- Transaction boundary: `UserService.execute()` (`@Transactional`)

**Application Bootstrap:**
- Location: `mekano-rest/src/main/java/com/fiap/mekano/rest/api/MekanoApiApplication.java`
- Extends `jakarta.ws.rs.core.Application` — minimal JAX-RS bootstrap
- `@OpenAPIDefinition` configures Swagger/OpenAPI metadata
- No custom `getClasses()` or `getSingletons()` — Quarkus auto-discovers all JAX-RS resources

**Observability Entry Points:**
- Health: `GET /q/health`, `/q/health/live`, `/q/health/ready` — via `quarkus-smallrye-health`
- Custom liveness: `ApplicationLivenessCheck` (`mekano-rest/.../observability/ApplicationLivenessCheck.java`)
- Metrics: `GET /q/metrics` — Prometheus format via `quarkus-micrometer-registry-prometheus`
- Swagger UI: `GET /q/swagger-ui` — via `quarkus-smallrye-openapi`

## Architectural Constraints

- **Threading:** Single-threaded event loop (Quarkus RESTEasy Reactive). All database operations are blocking — run on worker threads via `@Transactional` and Panache.
- **Global state:** `ResourceBundle` singleton in `Messages.java` — thread-safe bundle cache. No other module-level singletons with mutable state.
- **Circular imports:** None detected. Dependency graph is strictly acyclic: `rest → {application, infrastructure} → domain`.
- **Cross-module CDI:** Jandex index required in `application`, `infrastructure`, and `rest` modules. Without it, beans in non-root modules are invisible to CDI.
- **Transactional responsibility:** `@Transactional` ONLY on `UserService.execute()` — never on resources (JAX-RS) or repository methods (except `markAsDeleted` which is a standalone operation).
- **Soft delete:** All user queries filter `isActive = true`. Deleted records remain in the database with `deletedAt` timestamp.

## Anti-Patterns

### Resource Scoping (already resolved)

**What happens:** Quarkus RESTEasy Reactive resources can use `@ApplicationScoped`. With JWT `@Context UriInfo` injection, `@ApplicationScoped` causes stale or incorrect `UriInfo` instances. This pattern exists in early iterations.

**Why it's wrong:** JWT claims injection requires `@RequestScoped` because claims are per-request. `@ApplicationScoped` with `@Context UriInfo` produces unpredictable behavior.

**Do this instead:** Use `@RequestScoped` on all JAX-RS resources that inject JWT claims or `UriInfo`, as done in `UserResource.java:57`.

### Repository-scoped @Transactional (resolved)

**What happens:** Early iterations had `@Transactional` on `UserRepositoryImpl.save()`.

**Why it's wrong:** Transaction should be the unit of work at the Service boundary. Repository-scoped transactions prevent the Service from coordinating multiple repository calls in a single transaction.

**Do this instead:** Place `@Transactional` on the Service method (`UserService.execute()`), as currently implemented. Repositories participate in the caller's transaction.

## Error Handling

**Strategy:** Single `ExceptionMapper<Exception>` mapping all exceptions to RFC 7807 Problem Details (`application/problem+json`).

**Patterns:**
- `AppException` — carries HTTP status code (`int`) and message. Mapped directly to ProblemDetail by `ApiExceptionMapper`. Used throughout domain, application, and infrastructure layers.
- `ConstraintViolationException` — handled by Quarkus Hibernate Validator natively (returns `violations` array in Problem+JSON format).
- `PersistenceException` — unwrapped in `UserRepositoryImpl` to detect constraint violations (e.g., duplicate email → `AppException(409)`).
- Fallback: Any unhandled exception → `500 Internal Server Error` with stacktrace logged.
- `ApiExceptionMapper` (`mekano-rest/.../exception/ApiExceptionMapper.java:28-44`) uses `@Provider @ApplicationScoped` (required — G10).

## Cross-Cutting Concerns

**Logging:** JSON structured logging via `quarkus-logging-json` — `quarkus.log.console.json=true` in `mekano-rest/src/main/resources/application.properties:65`. Level: INFO (prod), DEBUG (dev), WARN (test).

**Validation:** Bean Validation (`@Valid`, `@NotBlank`, `@Email`, `@Size`) on DTOs in resource layer. Domain validation in `Email` value object constructor (throws `AppException(400)`). Service validates name null/blank (`AppException(400)`).

**Authentication:** JWT with Ed25519/EdDSA algorithm. Public key: `mekano-rest/src/main/resources/publicKey.pem`. Private key: `~/.mekano/secrets/privatekey.pem` (gitignored). Config namespace: `mp.jwt.*` (never `quarkus.smallrye-jwt.*` — G6). `quarkus.http.auth.proactive=false` allows `ApiExceptionMapper` to handle 401 before security pipeline.

**Caching:** Caffeine cache `users` — `@CacheResult` on `findById` and `findByEmail`, `@CacheInvalidate` on `save` and `markAsDeleted`. Config: `expire-after-write=60s`, `maximum-size=100`, `initial-capacity=10`.

**Fault Tolerance:** `@Retry(maxRetries=3)` on reads (`findById`, `findByEmail`). `@Timeout(5s)` on `save()`. No `@CircuitBreaker` — PostgreSQL local doesn't justify it (D-03/D-14).

**CORS:** Global configuration via `quarkus.http.cors.*` properties — origins `*`, methods `GET,POST,PUT,DELETE,PATCH,OPTIONS`, max-age `24H`.

**JSON serialization:** Jackson with `America/Sao_Paulo` timezone (`quarkus.jackson.timezone`).

---

*Architecture analysis: 2026-06-20*
