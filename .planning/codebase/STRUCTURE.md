# Codebase Structure

**Analysis Date:** 2026-06-20

## Directory Layout

```
mekano/
├── pom.xml                            # Parent POM — multi-module Maven project
├── mvnw / mvnw.cmd                    # Maven wrapper 3.9.15
├── docker-compose.yml                 # PostgreSQL 16-alpine for dev
├── CLAUDE.md                          # Project knowledge base (skills config, conventions, gotchas)
├── README.md                          # Project overview
├── REVIEW.md                          # Code review log
├── .mvn/                              # Maven wrapper JAR + config
├── .quarkus/                          # Quarkus CLI plugins
├── .github/                           # CI/CD workflows (empty — not yet configured)
├── .vscode/                           # IDE settings
├── docs/                              # Documentation (if any)
├── src/                               # Top-level source (if any — currently unused)
│   └── main/docker/                   # Docker configs
│
├── mekano-domain/                     # ⬅ Core domain — zero framework deps
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fiap/mekano/domain/
│       │   ├── model/                 # Domain entities (POJOs)
│       │   ├── valueobject/           # Value objects (immutable)
│       │   ├── port/in/               # Driving port interfaces
│       │   ├── port/out/              # Driven port interfaces
│       │   ├── exception/             # Domain exceptions + messages
│       │   └── event/                 # Domain events
│       ├── main/resources/com/fiap/mekano/domain/exception/
│       │   └── messages.properties    # i18n error messages
│       └── test/java/com/fiap/mekano/domain/
│           ├── model/
│           └── valueobject/
│
├── mekano-application/                # ⬅ Service orchestration
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fiap/mekano/application/
│       │   └── usecase/
│       │       └── user/              # User Services
│       └── test/java/com/fiap/mekano/application/
│           └── service/user/          # Service tests (Mockito)
│
├── mekano-infrastructure/             # ⬅ Concrete implementations
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/fiap/mekano/infrastructure/
│       │   ├── entity/                # JPA entities
│       │   ├── repository/            # Panache repositories + impls
│       │   ├── mapper/                # MapStruct entity <-> domain mappers
│       │   ├── security/              # BCrypt password hasher, rate limiter
│       │   ├── service/               # Technical services (refresh token)
│       │   └── event/                 # CDI event publisher
│       ├── main/resources/
│       │   └── db/migration/          # Flyway SQL migrations (V1–V5)
│       └── test/
│           ├── java/com/fiap/mekano/infrastructure/repository/
│           └── resources/
│               └── application.properties  # Test config (independent of rest module)
│
└── mekano-rest/                       # ⬅ Quarkus packaging — application entrypoint
    ├── pom.xml
    └── src/
        ├── main/java/com/fiap/mekano/rest/
        │   ├── api/                   # JAX-RS resources
        │   │   ├── MekanoApiApplication.java   # Bootstrap
        │   │   ├── UserResource.java           # CRUD endpoints
        │   │   ├── AuthResource.java           # Auth endpoints
        │   │   ├── dto/                        # Input/Output DTOs
        │   │   ├── mapper/                     # DTO <-> domain mappers
        │   │   ├── exception/                  # Exception mappers + ProblemDetail
        │   │   └── filter/                     # JAX-RS filters (rate limiting)
        │   └── observability/           # Health checks
        ├── main/resources/
        │   ├── application.properties   # All config (DB, JWT, CORS, cache, logging)
        │   └── publicKey.pem            # Ed25519 public key (committed)
        └── test/java/com/fiap/mekano/rest/
            ├── api/                     # REST Assured integration tests
            └── observability/           # Health endpoint tests
```

## Directory Purposes

**`mekano-domain/`:**
- Purpose: Business logic core with zero framework dependencies
- Contains: Domain entities (`model/`), value objects (`valueobject/`), port interfaces (`port/in/`, `port/out/`), domain exceptions (`exception/`), domain events (`event/`)
- Key files: `model/User.java`, `valueobject/Email.java`, `port/in/UserServicePort.java`, `port/out/UserRepositoryPort.java`, `port/in/PasswordHasher.java`, `port/out/EventPublisher.java`

**`mekano-application/`:**
- Purpose: Service orchestration layer — drives domain entities via ports
- Contains: Service classes implementing `port/in` interfaces, response records
- Key files: `service/user/UserService.java`, `service/user/CreateUserResponse.java`

**`mekano-infrastructure/`:**
- Purpose: All technology-specific implementations of domain ports
- Contains: JPA entities (`entity/`), Panache repositories (`repository/`), MapStruct mappers (`mapper/`), security impls (`security/`), event publisher (`event/`), Flyway migrations (`db/migration/`)
- Key files: `entity/BaseEntity.java`, `entity/UserEntity.java`, `repository/UserPanacheRepository.java`, `repository/UserRepositoryImpl.java`, `mapper/UserEntityMapper.java`, `mapper/UserEntityMapperImpl.java`, `mapper/EmailMapper.java`, `security/BcryptPasswordHasher.java`, `event/CdiEventPublisher.java`

**`mekano-rest/`:**
- Purpose: HTTP adapter layer — Quarkus application packaging, entry point, DTOs, exception handling, observability
- Contains: JAX-RS resources (`api/`), input/output DTOs (`api/dto/`), REST mappers (`api/mapper/`), exception mappers (`api/exception/`), JAX-RS filters (`api/filter/`), health checks (`observability/`)
- Key files: `api/MekanoApiApplication.java`, `api/UserResource.java`, `api/dto/CreateUserRequest.java`, `api/dto/UserResponse.java`, `api/dto/UserPageResponse.java`, `api/mapper/UserDtoMapper.java`, `api/exception/ApiExceptionMapper.java`, `api/exception/ProblemDetail.java`, `observability/ApplicationLivenessCheck.java`

## Key File Locations

**Entry Points:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/MekanoApiApplication.java`: JAX-RS + OpenAPI bootstrap
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/UserResource.java`: User CRUD HTTP endpoints
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/AuthResource.java`: Authentication HTTP endpoints

**Configuration:**
- `mekano-rest/src/main/resources/application.properties`: All application configuration (datasource, JWT, CORS, cache, logging, Flyway, timezone)
- `mekano-infrastructure/src/test/resources/application.properties`: Test configuration for infrastructure module (independent from rest module)
- `pom.xml`: Parent POM — dependency management, plugin management, module declarations
- `docker-compose.yml`: PostgreSQL 16-alpine for local development

**Core Logic:**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/User.java`: Domain entity with factory methods
- `mekano-application/src/main/java/com/fiap/mekano/application/service/user/UserService.java`: Create user orchestration
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/UserRepositoryImpl.java`: Persistence implementation
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/UserEntityMapperImpl.java`: JPA ↔ domain mapping

**Exception Handling:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/exception/ApiExceptionMapper.java`: Single exception mapper (RFC 7807)
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/exception/ProblemDetail.java`: RFC 7807 Problem Details record
- `mekano-domain/src/main/java/com/fiap/mekano/domain/exception/AppException.java`: Base app exception with HTTP status

**Database:**
- `mekano-infrastructure/src/main/resources/db/migration/V1__create_users_table.sql`: Initial users table
- `mekano-infrastructure/src/main/resources/db/migration/V2__create_refresh_tokens_table.sql`: Refresh tokens
- `mekano-infrastructure/src/main/resources/db/migration/V3__add_soft_delete_to_users.sql`: Soft delete columns
- `mekano-infrastructure/src/main/resources/db/migration/V4__add_audit_columns_to_users.sql`: Audit fields
- `mekano-infrastructure/src/main/resources/db/migration/V5__add_sequential_id.sql`: Hybrid ID (UUID + BIGSERIAL PK)

**Testing:**
- `mekano-domain/src/test/java/com/fiap/mekano/domain/model/UserTest.java`: Domain unit tests (JUnit 5 pure)
- `mekano-domain/src/test/java/com/fiap/mekano/domain/valueobject/EmailTest.java`: VO unit tests
- `mekano-application/src/test/java/com/fiap/mekano/application/service/user/UserServiceTest.java`: Service unit tests (Mockito)
- `mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/repository/UserRepositoryImplTest.java`: Repository integration tests (@QuarkusTest)
- `mekano-rest/src/test/java/com/fiap/mekano/rest/api/UserResourceTest.java`: REST endpoint integration tests (REST Assured)
- `mekano-rest/src/test/java/com/fiap/mekano/rest/api/FaultToleranceTest.java`: Fault tolerance integration tests
- `mekano-rest/src/test/java/com/fiap/mekano/rest/observability/ObservabilityEndpointsTest.java`: Health/metrics endpoint tests

## Naming Conventions

**Files:**
- Pattern: `PascalCase.java` for all Java source files — matches class names
- Pattern: `V{N}__{description}.sql` for Flyway migrations (e.g., `V1__create_users_table.sql`)
- Pattern: `kebab-case.properties` for config files (e.g., `application.properties`)
- Pattern: `lowercase-docker` for Docker files (e.g., `docker-compose.yml`, `.dockerignore`)

**Directories:**
- Pattern: `kebab-case` for Maven modules: `mekano-domain`, `mekano-application`, `mekano-infrastructure`, `mekano-rest`
- Pattern: `lowercase` for package directories: `model`, `valueobject`, `port`, `exception`, `event`, `usecase`, `entity`, `repository`, `mapper`, `security`, `service`, `dto`, `filter`, `observability`
- Pattern: Singular names for package directories (e.g., `model/` not `models/`, `exception/` not `exceptions/`)

**Functions/Methods:**
- Pattern: `camelCase` for method names — Java standard (e.g., `execute()`, `findById()`, `existsByEmail()`, `markAsDeleted()`)
- Pattern: `camelCase` prefixed methods in resources: `create()`, `listAll()`, `getById()`, `delete()`
- Pattern: Static factory methods named `create()` (new instance) and `reconstitute()` (restore from persistence)

**Variables:**
- Pattern: `camelCase` — standard Java (e.g., `user`, `command`, `entity`, `passwordHash`)

**Types/Classes:**
- Pattern: `PascalCase` for all classes, interfaces, records, enums
- Pattern: Interface names without `I` prefix — e.g., `UserServicePort`, `UserRepositoryPort`, `PasswordHasher`, `EventPublisher`
- Pattern: Implementation classes suffix: `Impl` for port implementations (`UserRepositoryImpl`), `UseCase` for Services (`UserService`), `Mapper` for MapStruct interfaces/mappers (`UserDtoMapper`, `UserEntityMapper`)
- Pattern: Entity classes suffix: `Entity` for JPA entities (`UserEntity`)
- Pattern: Response classes suffix: `Response` for output records (`CreateUserResponse`, `UserResponse`)

## Where to Add New Code

**New Feature (e.g., new entity/service area):**
1. Domain entity: `mekano-domain/src/main/java/com/fiap/mekano/domain/model/Novo.java`
2. Value object: `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/NovoVO.java`
3. Input port: `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/NovoInputPort.java`
4. Output port: `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/NovoRepositoryPort.java`
5. Command record: `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/NovoCommand.java`
6. Domain exception: `mekano-domain/src/main/java/com/fiap/mekano/exception/NovaExcecao.java`
7. Domain event: `mekano-domain/src/main/java/com/fiap/mekano/domain/event/NovoEvent.java`
8. Service: `mekano-application/src/main/java/com/fiap/mekano/application/service/novo/NovoUseCase.java`
9. Response record: `mekano-application/src/main/java/com/fiap/mekano/application/service/novo/NovoResponse.java`
10. JPA entity: `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/NovoEntity.java`
11. Panache repository: `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/NovoPanacheRepository.java`
12. Entity mapper: `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/NovoEntityMapper.java`
13. Repository impl: `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/NovoRepositoryImpl.java`
14. Flyway migration: `mekano-infrastructure/src/main/resources/db/migration/V6__create_nova_tabela.sql`
15. DTOs: `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/NovoRequest.java` + `NovoResponse.java`
16. REST mapper: `mekano-rest/src/main/java/com/fiap/mekano/rest/api/mapper/NovoDtoMapper.java`
17. Resource: `mekano-rest/src/main/java/com/fiap/mekano/rest/api/NovoResource.java`
18. Tests: `mekano-rest/src/test/java/com/fiap/mekano/rest/api/NovoResourceTest.java`

**New Utility/Shared Code:**
- Shared helpers: `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/` (appropriate subpackage)
- Domain utilities: `mekano-domain/src/main/java/com/fiap/mekano/domain/` (appropriate subpackage)

**New Quarkus Extension:**
- Add dependency in `mekano-rest/pom.xml` (compile scope) for rest-layer usage
- Add dependency in `mekano-infrastructure/pom.xml` if the extension is infrastructure-specific (e.g., Panache, Flyway, Cache)

## Special Directories

**`target/`:**
- Purpose: Maven build output (compiled classes, test reports, packaged artifacts)
- Generated: Yes
- Committed: No (`.gitignore`)

**`.mvn/wrapper/`:**
- Purpose: Maven wrapper JAR and configuration for reproducible builds
- Generated: No (committed)
- Committed: Yes

**`mekano-infrastructure/src/main/resources/db/migration/`:**
- Purpose: Flyway database migration scripts
- Generated: No (hand-written SQL)
- Committed: Yes

---

*Structure analysis: 2026-06-20*
