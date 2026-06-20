# Research Summary — Mekano API

**Project:** Mekano — Clean Architecture REST API  
**Stack:** Quarkus 3.36.0 · Java 17 · PostgreSQL · Maven Multi-Module  
**Synthesized:** 2025-07-15  
**Source files:** STACK.md · ARCHITECTURE.md · TESTING.md · PITFALLS.md  
**Overall confidence:** HIGH (all findings verified against official Quarkus 3.x docs, MapStruct reference, and SmallRye guides)

---

## Executive Summary

Mekano is a production-grade Quarkus 3.x REST API built on Clean Architecture (Ports & Adapters / Hexagonal). The project splits into four Maven modules — `domain`, `application`, `infrastructure`, `adapter` — with strict inward-only dependency rules enforced at the `pom.xml` level. The domain module is a pure-Java core with zero framework dependencies; use cases in the application module orchestrate through port interfaces; infrastructure owns JPA/Flyway/MapStruct; and the adapter module is the sole Quarkus runner that exposes HTTP endpoints via JAX-RS. This structure maximises testability: domain and application tests are plain JUnit with no container startup, while infrastructure and adapter tests use `@QuarkusTest` backed by DevServices.

The biggest risk in this project is **Quarkus-specific plumbing that the framework cannot detect at compile time**. Four problems will silently break a working-looking codebase if missed: (1) CDI beans in non-runner modules are invisible unless each module publishes a Jandex index; (2) MapStruct generates null-filled mappers unless `lombok-mapstruct-binding` mediates annotation processor ordering; (3) the `quarkus-maven-plugin` must live only in the adapter module — placing it in the parent causes multi-module build failures; and (4) JWT verification silently returns 401 when the wrong property namespace (`quarkus.smallrye-jwt.*` instead of `mp.jwt.*`) or a non-PKCS#8 key format is used. All four are well-documented once known, and the fix in each case is a one-time configuration change.

The recommended build order follows natural layer dependency: scaffold the parent POM and multi-module skeleton first, then domain (pure Java), then application (use cases), then infrastructure (JPA/Flyway/MapStruct), then adapter (REST/JWT/OpenAPI), with cross-cutting concerns (observability, security) added to the adapter layer last. Testing follows the same order — fast unit tests are written alongside each layer, integration tests are added in the adapter phase when the full CDI context is first available.

---

## Key Findings

### From STACK.md — Confirmed Technology Decisions

| Technology | Version | Decision | Rationale |
|------------|---------|----------|-----------|
| Quarkus BOM | 3.36.0 | ✅ Use in parent `<dependencyManagement>` | Single version alignment across all modules |
| Java | 17 | ✅ Set via `<maven.compiler.release>17</maven.compiler.release>` | Records, sealed classes, text blocks |
| quarkus-rest-jackson | BOM-managed | ✅ **Replace `quarkus-rest`** | `quarkus-rest` has no JSON; Jackson must be explicit |
| Hibernate ORM Panache | BOM-managed | ✅ Infra module only | Active Record pattern; keeps JPA out of domain |
| quarkus-flyway | BOM-managed | ✅ Infra module | Owns all DDL; Hibernate set to `validate` mode only |
| quarkus-jdbc-postgresql | BOM-managed | ✅ Infra module | Activates DevServices automatically in dev/test |
| MapStruct | 1.6.3 | ✅ Infra + Adapter | `componentModel = "cdi"` required; NOT `"spring"` |
| Lombok | 1.18.36 | ✅ All modules | `<scope>provided</scope>`; does NOT go on runtime classpath |
| lombok-mapstruct-binding | 0.2.0 | ✅ Required | Bridge for Lombok ≥ 1.18.16; without it mapper fields are null |
| jandex-maven-plugin | 3.5.3 | ✅ application + infrastructure modules | Publishes `META-INF/jandex.idx`; domain doesn't need it |
| quarkus-smallrye-jwt | BOM-managed | ✅ Adapter module | MicroProfile JWT; use `mp.jwt.*` namespace, not `quarkus.smallrye-jwt.*` |
| quarkus-smallrye-health | BOM-managed | ✅ Adapter module | Auto-adds datasource check; exposes `/q/health` |
| quarkus-micrometer-registry-prometheus | BOM-managed | ✅ Adapter module | Auto-exposes `/q/metrics`; custom counters/timers via `MeterRegistry` |
| quarkus-smallrye-openapi | BOM-managed | ✅ Adapter module | Generates OpenAPI spec; Swagger UI at `/q/swagger-ui` |
| quarkus-smallrye-fault-tolerance | BOM-managed | ✅ Infrastructure module | `@Retry`, `@CircuitBreaker`, `@Timeout` on external adapters |
| maven-compiler-plugin | 3.15.0 | ✅ With annotationProcessorPaths | Required for MapStruct+Lombok ordering |
| maven-surefire-plugin | 3.5.4 | ✅ (already in root pom) | JUnit 5 test discovery |

**Critical version note:** `quarkus-rest-jackson` must replace `quarkus-rest` in the current root `pom.xml` — this is the first change needed before any REST endpoint will return JSON.

**Annotation processor path order (non-negotiable):**
```
1. lombok (generates getters/setters)
2. lombok-mapstruct-binding (coordinates execution order)
3. mapstruct-processor (reads Lombok-generated methods)
```

---

### From ARCHITECTURE.md — Module Structure and Dependency Rules

#### Module Layout

```
mekano/                     (parent, packaging=pom)
├── mekano-domain/           jar  — pure Java, zero framework deps
├── mekano-application/      jar  — use cases, depends on domain
├── mekano-infrastructure/   jar  — JPA/Flyway/MapStruct, depends on domain
└── mekano-adapter/          quarkus — REST/DTOs/JWT/ExceptionMappers
```

#### Dependency Rule (enforced in pom.xml)

```
adapter      → domain, application, infrastructure (runtime classpath for CDI)
infrastructure → domain
application  → domain
domain       → (nothing)
```

> The adapter imports `infrastructure` only for the runtime classpath (CDI discovers `UserRepositoryImpl`). Adapter code never imports infrastructure classes directly — it only knows the `UserRepositoryPort` interface from domain.

#### Package Structure per Module

| Module | Key Packages |
|--------|-------------|
| `domain` | `model/`, `valueobject/`, `port/in/`, `port/out/`, `exception/` |
| `application` | `usecase/{entity}/` — use case classes + command records |
| `infrastructure` | `entity/`, `repository/`, `mapper/`, `config/` |
| `adapter` | `rest/`, `dto/request/`, `dto/response/`, `mapper/`, `exception/` |

#### Data Flow (POST /users end-to-end)

```
HTTP Request
  → [adapter] UserResource: @Valid, map Request → Command
  → [application] CreateUserUseCase: check uniqueness, create domain entity
  → [infrastructure] UserRepositoryImpl: MapStruct User→Entity, persist(), Entity→User
  → [application] returns domain User
  → [adapter] MapStruct User→UserResponse
  → HTTP 201 + JSON
```

#### Key Architecture Rules

1. **No JPA annotations in domain** — `@Entity`, `@Column` live in `infrastructure.entity` only
2. **No `@Transactional` in use cases** — belongs on repository implementation methods only
3. **No HTTP types in domain exceptions** — `ExceptionMapper` in adapter translates domain → HTTP
4. **No DTOs crossing layer boundaries** — DTOs exist only at the adapter boundary
5. **MapStruct `componentModel = "cdi"`** always — `componentModel = "spring"` silently fails
6. **`@RequestScoped` on JWT-protected resources** — `@ApplicationScoped` breaks claim injection

#### Exception Hierarchy

```
RuntimeException
└── DomainException (abstract, in domain)
    ├── UserNotFoundException          → HTTP 404 (adapter ExceptionMapper)
    ├── UserAlreadyExistsException     → HTTP 409
    └── InvalidEmailException          → HTTP 400
```

---

### From TESTING.md — Testing Strategy by Layer

| Layer | Framework | What Gets Tested | Speed |
|-------|-----------|-----------------|-------|
| `domain` | Plain JUnit 5 | Value object invariants, domain entity factory methods | ⚡ Milliseconds |
| `application` | Plain JUnit 5 + Mockito `@Mock` | Use case logic, port interaction, business rule enforcement | ⚡ Milliseconds |
| `infrastructure` | `@QuarkusTest` + DevServices + `@TestTransaction` | Repository implementations against real PostgreSQL | 🐢 Seconds (container) |
| `adapter` | `@QuarkusTest` + REST Assured + `@InjectMock` | HTTP contract, validation errors, ExceptionMappers | 🐢 Seconds (app startup) |

#### Test Annotations Decision Tree

```
Testing business logic with no DB? → Plain JUnit + Mockito (@Mock, @InjectMocks)
Testing HTTP endpoints?            → @QuarkusTest + REST Assured
Need to mock a CDI bean?           → @InjectMock (requires quarkus-junit-mockito artifact)
Need DB rollback per test?         → @TestTransaction
Testing packaged JAR (smoke)?      → @QuarkusIntegrationTest (extends @QuarkusTest class)
```

#### DevServices Configuration (Zero-Config for Tests)

```properties
# DevServices activates automatically when NO explicit JDBC URL is set for the active profile
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mekano
quarkus.datasource.devservices.image-name=docker.io/library/postgres:16-alpine
quarkus.flyway.migrate-at-start=true
```

#### Flyway in Tests

```properties
# Recommended test profile overrides
%test.quarkus.flyway.clean-at-start=true          # wipe & re-apply on each test run
%test.quarkus.flyway.locations=classpath:db/migration,classpath:db/testdata
```

Use `R__seed_test_users.sql` (repeatable migration) for test seed data in `infrastructure/src/test/resources/db/testdata/`.

#### Test Profile Warning

Each distinct `@TestProfile` causes a full Quarkus application restart. Keep profiles ≤ 3 (default, mock-infra, security). Prefer `@InjectMock` over new profiles when possible.

---

### From PITFALLS.md — Critical and Moderate Pitfalls

See dedicated section below.

---

## Critical Gotchas — Top 5 Things That WILL Break the Project

These are **silent or misleading failures** that cost hours to debug if missed.

### 🔴 Gotcha 1: `quarkus-maven-plugin` in the Wrong POM

**Symptom:** `mvn quarkus:dev` fails or runs from wrong module; native profile applies everywhere.

**Rule:** `<packaging>quarkus</packaging>` and `quarkus-maven-plugin` (with `<extensions>true</extensions>`) belong **only in `mekano-adapter/pom.xml`**. The parent POM must use `<packaging>pom</packaging>` with no plugin definition.

```xml
<!-- ✅ parent pom.xml -->
<packaging>pom</packaging>  ← NOT "quarkus"

<!-- ✅ mekano-adapter/pom.xml ONLY -->
<packaging>quarkus</packaging>
<build><plugins>
  <plugin>
    <groupId>io.quarkus.platform</groupId>
    <artifactId>quarkus-maven-plugin</artifactId>
    <extensions>true</extensions>
  </plugin>
</plugins></build>
```

---

### 🔴 Gotcha 2: CDI Beans in Sub-Modules Not Discovered (Jandex)

**Symptom:** `UnsatisfiedResolutionException` at startup, or injected fields are `null` at runtime. The class looks correct — Quarkus just never sees it.

**Official statement:** *"By default, Quarkus will not discover CDI beans inside another module."*

**Rule:** Add `jandex-maven-plugin` (v3.5.3) to every module containing CDI beans. That means `mekano-application` and `mekano-infrastructure`. The `mekano-adapter` module is indexed automatically by `quarkus-maven-plugin`. `mekano-domain` has no CDI beans — skip it.

```xml
<!-- Required in: mekano-application/pom.xml, mekano-infrastructure/pom.xml -->
<plugin>
  <groupId>io.smallrye</groupId>
  <artifactId>jandex-maven-plugin</artifactId>
  <version>3.5.3</version>
  <executions>
    <execution><id>make-index</id><goals><goal>jandex</goal></goals></execution>
  </executions>
</plugin>
```

---

### 🔴 Gotcha 3: MapStruct + Lombok — Missing Binding Artifact

**Symptom:** Mappers compile successfully but produce objects with all `null` fields. Or: `"No property named 'X' exists in source/target"` build error.

**Rule:** In any module using both Lombok and MapStruct (`infrastructure`, `adapter`), `maven-compiler-plugin` must declare **all three** annotation processor paths in this exact order:

```xml
<annotationProcessorPaths>
  <path><groupId>org.projectlombok</groupId><artifactId>lombok</artifactId><version>${lombok.version}</version></path>
  <path><groupId>org.projectlombok</groupId><artifactId>lombok-mapstruct-binding</artifactId><version>0.2.0</version></path>
  <path><groupId>org.mapstruct</groupId><artifactId>mapstruct-processor</artifactId><version>${mapstruct.version}</version></path>
</annotationProcessorPaths>
```

Lombok ≥ 1.18.16 requires `lombok-mapstruct-binding` — without it, MapStruct reads classes before Lombok has generated getters/setters.

---

### 🔴 Gotcha 4: Flyway Silent Skip — Two Independent Causes

**Symptom A:** Flyway runs but no migrations execute.  
**Cause:** `quarkus.flyway.migrate-at-start` defaults to `false`. Set it explicitly:
```properties
quarkus.flyway.migrate-at-start=true
```

**Symptom B:** Flyway skips all migration files (no error).  
**Cause:** Wrong naming convention — single underscore, lowercase `v`, or dot separator.
```
❌ v1_create_users.sql       (lowercase v)
❌ V1_create_users.sql       (single underscore)
✅ V1__create_users.sql      (capital V + double underscore)
```

Migrations live in `mekano-infrastructure/src/main/resources/db/migration/`. Flyway's default `classpath:db/migration` resolves to this path when the infrastructure JAR is on the classpath.

---

### 🔴 Gotcha 5: JWT Property Namespace and Key Format

**Symptom:** All JWT-protected endpoints return HTTP 401. No error message indicates which validation failed.

**Two distinct causes:**

**A. Wrong property namespace:**
```properties
❌ quarkus.smallrye-jwt.public-key.location=publicKey.pem   # Quarkus namespace — wrong
✅ mp.jwt.verify.publickey.location=publicKey.pem            # MicroProfile namespace — correct
✅ mp.jwt.verify.issuer=https://mekano.fiap.com.br/auth
```

**B. Wrong key format** — SmallRye JWT requires PKCS#8 PEM:
```
✅  -----BEGIN PUBLIC KEY-----    (PKCS#8 — correct)
❌  -----BEGIN RSA PUBLIC KEY-----  (legacy — rejected)
```
Generate correctly with:
```bash
openssl genrsa -out privateKey.pem 2048
openssl pkcs8 -topk8 -nocrypt -inform pem -in privateKey.pem -outform pem -out privateKey_pkcs8.pem
openssl rsa -pubout -in privateKey.pem -out publicKey.pem
```

Also: JAX-RS resources with JWT injection **must** be `@RequestScoped`, not `@ApplicationScoped`. `@ApplicationScoped` breaks per-request claim injection.

---

## Moderate Gotchas (Reference)

| Area | Gotcha | Fix |
|------|--------|-----|
| REST stack | `quarkus-rest` has no JSON serializer | Replace with `quarkus-rest-jackson` |
| REST stack | Mixing `quarkus-resteasy-jackson` with `quarkus-rest` | Use exactly one: `quarkus-rest-jackson` for reactive stack |
| REST stack | `@Context HttpServletRequest` is null at runtime | Use `@Context UriInfo`, `@Context HttpHeaders` — no servlet API |
| Panache | `@Entity` on domain entity | Keep JPA annotations in infrastructure only; use MapStruct to map |
| MapStruct | `componentModel = "spring"` | Use `componentModel = "cdi"` always |
| ExceptionMappers | Custom mappers silently ignored (500 instead of 4xx) | Add both `@Provider` and `@ApplicationScoped`; verify at `/q/dev-ui/quarkus-rest/exception-mappers` |
| Hibernate | Conflict between `persistence.xml` and `application.properties` | Never mix both; use `application.properties` only |
| Hibernate | Hibernate auto-creates/drops schema alongside Flyway | Set `quarkus.hibernate-orm.schema-management.strategy=validate` everywhere |
| Flyway | `clean-at-start=true` in production | Gate behind `%test.` profile only |
| Native build | `publicKey.pem` not embedded in binary | `quarkus.native.resources.includes=publicKey.pem` |
| Tests | `@InjectMock` in plain JUnit (not `@QuarkusTest`) | Use `@Mock` for plain JUnit; `@InjectMock` only with `@QuarkusTest` |
| Tests | Too many `@TestProfile` — each restarts the app | Keep ≤ 3 profiles; prefer `@InjectMock` for infra mocking |

---

## Suggested Build Order (Phase Sequence)

These phases follow natural compilation dependency order. Later phases cannot be started until earlier ones compile cleanly.

### Phase 1 — Maven Skeleton & Parent POM

**Deliver:** Working multi-module Maven build with correct packaging, BOM, and property declarations.

**Tasks:**
- Convert root `pom.xml` from `<packaging>quarkus</packaging>` to `<packaging>pom</packaging>`
- Remove `quarkus-maven-plugin` from root
- Declare `<modules>`: `mekano-domain`, `mekano-application`, `mekano-infrastructure`, `mekano-adapter`
- Set `<dependencyManagement>` with `quarkus-bom:3.36.0`, MapStruct 1.6.3, Lombok 1.18.36, `lombok-mapstruct-binding:0.2.0`, jandex-plugin 3.5.3
- Create stub `pom.xml` for each module with correct packaging and inter-module `<dependency>` declarations

**Pitfalls to avoid:** Gotcha 1 (plugin placement), BOM version mismatches  
**Research flag:** None — standard Maven multi-module pattern, well documented ✅

---

### Phase 2 — Domain Module

**Deliver:** Compilable pure-Java domain layer with zero framework dependencies.

**Tasks:**
- Domain entities as plain POJOs (no `@Entity`, no Lombok on public API if value objects enforce invariants via constructors)
- Value objects (`UserId`, `Email`) with invariant checks in constructor
- Port interfaces (`UserRepositoryPort`, input ports per use case)
- Domain exception hierarchy (`DomainException` → `UserNotFoundException`, `UserAlreadyExistsException`, `InvalidEmailException`)
- Plain JUnit 5 tests for value object invariants

**No Jandex needed** — domain has no CDI beans  
**Pitfalls to avoid:** No JPA, no Quarkus imports anywhere in this module  
**Research flag:** None — pure Java pattern, no Quarkus-specific concerns ✅

---

### Phase 3 — Application Module (Use Cases)

**Deliver:** Business logic layer testable without any running container.

**Tasks:**
- `CreateUserUseCase`, `FindUserUseCase`, `DeleteUserUseCase` implementing input ports
- Command records (`CreateUserCommand`, etc.)
- `@ApplicationScoped` on use case classes (CDI annotation only — no Quarkus runtime dep)
- Add `jandex-maven-plugin` to `mekano-application/pom.xml`
- Plain JUnit 5 + Mockito tests: mock `UserRepositoryPort`, verify business rules

**Pitfalls to avoid:** Gotcha 2 (must have jandex), `@Transactional` must NOT appear here  
**Research flag:** None — use case pattern is stable ✅

---

### Phase 4 — Infrastructure Module (JPA + Flyway + MapStruct)

**Deliver:** Database persistence layer that can be started and tested against a real PostgreSQL instance.

**Tasks:**
- `UserEntity extends PanacheEntity` (infra only, never exposed to other modules)
- `UserRepositoryImpl implements UserRepositoryPort` with `@Transactional` on write methods
- `UserEntityMapper` with `@Mapper(componentModel = "cdi")`
- `maven-compiler-plugin` with annotationProcessorPaths: lombok → binding → mapstruct-processor
- Flyway migrations: `V1__create_users_table.sql` in `src/main/resources/db/migration/`
- `application.properties`: `quarkus.flyway.migrate-at-start=true`, `strategy=validate`, DevServices config
- Add `jandex-maven-plugin` to `mekano-infrastructure/pom.xml`
- `@QuarkusTest` + `@TestTransaction` repository tests using DevServices

**Pitfalls to avoid:** Gotcha 2 (jandex), Gotcha 3 (MapStruct+Lombok order), Gotcha 4 (Flyway naming + migrate-at-start)  
**Research flag:** ⚠️ Moderate — MapStruct CDI integration and Flyway config have known trip wires. Follow STACK.md configuration examples exactly.

---

### Phase 5 — Adapter Module (REST + Validation + ExceptionMappers)

**Deliver:** Fully wired HTTP API that returns correct JSON responses and meaningful error codes.

**Tasks:**
- Move `quarkus-maven-plugin` + `<packaging>quarkus</packaging>` into `mekano-adapter/pom.xml`
- JAX-RS resources (`UserResource`) — thin, delegates to input ports only
- Request DTOs with `@NotBlank`, `@Email`, `@Size` validation annotations
- Response DTOs as records
- `UserDtoMapper` with `@Mapper(componentModel = "cdi")` + annotationProcessorPaths
- `ExceptionMapper` providers: `@Provider @ApplicationScoped` on each mapper
- `ErrorResponse` record
- `@QuarkusTest` + REST Assured endpoint tests
- `@InjectMock` tests for ExceptionMapper verification

**Pitfalls to avoid:** Gotcha 1 (plugin placement), Gotcha 2 (adapter is auto-indexed, but ExceptionMappers still need `@Provider`), Gotcha 3 (MapStruct), replacing `quarkus-rest` with `quarkus-rest-jackson`  
**Research flag:** ⚠️ Moderate — `quarkus-rest` vs `quarkus-rest-jackson` swap must happen here.

---

### Phase 6 — Security (SmallRye JWT)

**Deliver:** JWT-protected endpoints with working token verification.

**Tasks:**
- Add `quarkus-smallrye-jwt` to adapter `pom.xml`
- Generate RSA key pair; place PKCS#8 public key in `src/main/resources/`
- Set `mp.jwt.verify.publickey.location` and `mp.jwt.verify.issuer` (NOT `quarkus.smallrye-jwt.*`)
- Annotate resources with `@Authenticated` / `@RolesAllowed`; ensure all resources are `@RequestScoped`
- Add `quarkus-smallrye-jwt-build` to test scope for token generation in tests
- Create `NoSecurityProfile` test profile for tests that don't exercise auth

**Pitfalls to avoid:** Gotcha 5 (wrong property namespace + key format + `@RequestScoped`)  
**Research flag:** ⚠️ High-risk phase — JWT config has several non-obvious failure modes. Follow STACK.md §5 exactly.

---

### Phase 7 — Observability (Health + Metrics + OpenAPI)

**Deliver:** Production-ready operational endpoints.

**Tasks:**
- Add `quarkus-smallrye-health`, `quarkus-micrometer-registry-prometheus`, `quarkus-smallrye-openapi`
- Optional custom `@Liveness`/`@Readiness` health checks
- Optional custom `MeterRegistry` counters on use cases
- OpenAPI annotations (`@Tag`, `@Operation`, `@APIResponse`) on resources
- Verify `/q/health`, `/q/metrics`, `/q/swagger-ui` in dev mode

**Research flag:** None — additive extensions, no breaking interactions with existing layers ✅

---

### Phase 8 — Docker Compose Integration (Production Profile)

**Deliver:** Runnable production-like environment via docker-compose.

**Tasks:**
- `docker-compose.yml` at project root (postgres with healthcheck, mekano-api service)
- `%prod.*` profile in `application.properties` reading from env vars
- Verify `./mvnw quarkus:dev -pl mekano-adapter -am` works from root
- Optional: container image build via `quarkus-container-image-jib`

**Research flag:** None — standard Quarkus DevServices/prod profile pattern ✅

---

## Confidence Assessment

| Area | Confidence | Basis |
|------|------------|-------|
| Multi-module Maven structure | **HIGH** | Official Quarkus maven-tooling guide; stable pattern |
| Clean Architecture module breakdown | **HIGH** | Robert C. Martin's pattern; Hexagonal Architecture (Cockburn); well-established in Java ecosystem |
| CDI bean discovery / Jandex | **HIGH** | Explicit Quarkus documentation; confirmed anti-pattern with known fix |
| MapStruct + Lombok ordering | **HIGH** | MapStruct official reference §14.2; `lombok-mapstruct-binding` is the documented solution |
| Flyway configuration | **HIGH** | Quarkus Flyway guide; confirmed `migrate-at-start=false` default |
| SmallRye JWT | **HIGH** | Official Quarkus security-jwt guide; MicroProfile namespace documented |
| Testing patterns | **HIGH** | Official Quarkus getting-started-testing guide; DevServices guide |
| Panache in Clean Architecture | **HIGH** | Pattern is well-documented; separation into infrastructure-only confirmed |
| Native build concerns | **MEDIUM** | JVM mode is the primary target; native is out of scope for this phase |

**Gaps / Items Requiring Validation During Implementation:**

1. **MapStruct version alignment with BOM** — Quarkus BOM 3.36.0 may or may not manage MapStruct. Run `./mvnw dependency:resolve` and confirm no version conflict between BOM-managed and explicitly declared MapStruct versions.
2. **DevServices Docker availability in CI** — If this project runs in a CI environment without Docker daemon access, DevServices will fail. Mitigation: add `%test.quarkus.datasource.devservices.enabled=false` + explicit `@QuarkusTestResource` Testcontainers config.
3. **`quarkus-arc` vs `jakarta.enterprise.cdi-api`** — Architecture research recommends `quarkus-arc` in the application module for `@ApplicationScoped`; stack research recommends the lighter `jakarta.enterprise.cdi-api`. Either works; `quarkus-arc` is safer as it pulls in the exact CDI implementation Quarkus uses.
4. **Issuer URL final form** — `mp.jwt.verify.issuer` must exactly match the `iss` claim. Confirm the issuer string before wiring the first JWT test.

---

## Aggregated Sources

| Source | URL | Used In |
|--------|-----|---------|
| Quarkus Maven Tooling | https://quarkus.io/guides/maven-tooling | STACK, PITFALLS, TESTING |
| Quarkus CDI Reference | https://quarkus.io/guides/cdi-reference | STACK, PITFALLS, TESTING |
| Quarkus Hibernate ORM | https://quarkus.io/guides/hibernate-orm | STACK, ARCHITECTURE |
| Quarkus Hibernate ORM Panache | https://quarkus.io/guides/hibernate-orm-panache | ARCHITECTURE, PITFALLS |
| Quarkus Flyway Guide | https://quarkus.io/guides/flyway | STACK, PITFALLS, TESTING |
| Quarkus Datasource Guide | https://quarkus.io/guides/datasource | STACK |
| Quarkus Dev Services Guide | https://quarkus.io/guides/dev-services | STACK, TESTING |
| Quarkus REST Guide | https://quarkus.io/guides/rest | ARCHITECTURE |
| Quarkus REST Migration Guide | https://quarkus.io/guides/rest-migration | PITFALLS |
| Quarkus Security JWT | https://quarkus.io/guides/security-jwt | STACK, PITFALLS |
| Quarkus SmallRye Health | https://quarkus.io/guides/smallrye-health | STACK |
| Quarkus Fault Tolerance | https://quarkus.io/guides/smallrye-fault-tolerance | STACK |
| Quarkus Micrometer | https://quarkus.io/guides/telemetry-micrometer | STACK |
| Quarkus Getting Started Testing | https://quarkus.io/guides/getting-started-testing | TESTING |
| Quarkus Native Application Tips | https://quarkus.io/guides/writing-native-applications-tips | PITFALLS |
| MapStruct Reference — Lombok | https://mapstruct.org/documentation/stable/reference/html/#lombok | STACK, PITFALLS |
| MapStruct Reference — CDI | https://mapstruct.org/documentation/stable/reference/html/#cdi-component-model | ARCHITECTURE |
| Robert C. Martin — Clean Architecture (2017) | (book) | ARCHITECTURE |
| Alistair Cockburn — Hexagonal Architecture | (original paper) | ARCHITECTURE |

---

*Research synthesized: 2025-07-15*  
*Ready for roadmap phase planning.*
