# Technology Stack

**Analysis Date:** 2026-06-20

## Languages

**Primary:**
- Java 17 (OpenJDK) — All application code (`mekano-domain`, `mekano-application`, `mekano-infrastructure`, `mekano-rest`)
- SQL — Flyway migrations (`mekano-infrastructure/src/main/resources/db/migration/`)

**Secondary:**
- YAML — CI pipeline (`.github/workflows/ci.yml`), Docker Compose (`docker-compose.yml`)
- Properties/XML — Maven config (`pom.xml`), Quarkus config (`application.properties`)

## Runtime

**Environment:**
- JVM (Ubuntu 9 / UBI 9 `openjdk-17-runtime` in Docker) — JVM mode via `Dockerfile.jvm`
- Quarkus Native binary (GraalVM) — Native mode via `Dockerfile.native` / `Dockerfile.native-micro`
- Dev mode: `./mvnw quarkus:dev` with hot reload

**Package Manager:**
- Maven 3.9.15 (wrapper at `mvnw` / `mvnw.cmd`)
- Lockfile: Not applicable (Maven uses `pom.xml` dependency management)
- Compiler: `maven-compiler-plugin` 3.15.0, release target 17

## Frameworks

**Core:**
- Quarkus 3.36.0 (`io.quarkus.platform:quarkus-bom`) — Full-stack framework, CDI container, RESTEasy Reactive
- Hibernate ORM + Panache (`quarkus-hibernate-orm-panache`) — JPA persistence
- Hibernate Validator (`quarkus-hibernate-validator`) — Bean Validation (`@Valid`, `@NotBlank`)

**Testing:**
- JUnit 5 (`junit-jupiter`) — Unit tests in domain and application layers
- Quarkus JUnit5 (`quarkus-junit5`) — `@QuarkusTest` in infrastructure and REST layers
- Mockito JUnit5 (`mockito-junit-jupiter`) — Application layer unit tests (`@ExtendWith(MockitoExtension.class)`)
- REST Assured — REST endpoint integration tests (`mekano-rest`)
- AssertJ 3.27.3 (`assertj-core`) — Fluent assertions
- `@TestSecurity` (`quarkus-test-security`) — JWT bypass for Quarkus tests

**Build/Dev:**
- Maven Compiler Plugin 3.15.0
- Surefire Plugin 3.5.4 — Unit tests
- Failsafe Plugin 3.5.4 — Integration tests
- Jandex Maven Plugin 3.5.3 — CDI index generation (required in `application`, `infrastructure`, `rest`)
- Quarkus Maven Plugin — Only active in `mekano-rest` (all other modules set `<skip>true</skip>`)
- MapStruct processor — Annotation processor for DTO↔domain mapping

## Key Dependencies

### Critical Infrastructure

| Dependency | Version | Purpose | File |
|-----------|---------|---------|------|
| Quarkus | 3.36.0 | Application framework, CDI, REST | `pom.xml:25` |
| PostgreSQL JDBC | (managed by Quarkus BOM) | Database driver | `mekano-rest/pom.xml:73` |
| Hibernate ORM Panache | (managed by Quarkus BOM) | JPA ORM | `mekano-rest/pom.xml:43` |
| Flyway | (managed by Quarkus BOM) | Database migrations | `mekano-rest/pom.xml:48` |
| MapStruct | 1.6.3 | DTO JPA Entity ↔ Domain mapping | `pom.xml:28` |
| Hibernate Validator | (managed by Quarkus BOM) | Bean validation on input DTOs | `mekano-rest/pom.xml:57` |

### Observability

| Dependency | Purpose | File |
|-----------|---------|------|
| `quarkus-smallrye-health` | Health endpoints (`/q/health`, `/q/health/live`, `/q/health/ready`) | `mekano-rest/pom.xml:77` |
| `quarkus-micrometer-registry-prometheus` | Prometheus metrics (`/q/metrics`) | `mekano-rest/pom.xml:84` |
| `quarkus-logging-json` | Structured JSON logging | `mekano-rest/pom.xml:62` |

### Security & Auth

| Dependency | Purpose | File |
|-----------|---------|------|
| `quarkus-elytron-security-common` | BCrypt password hashing (`BcryptUtil`) | `mekano-infrastructure/pom.xml:62`, `mekano-application/pom.xml:38` |

### Caching & Resilience

| Dependency | Purpose | File |
|-----------|---------|------|
| `quarkus-cache` | Caffeine in-memory cache (users cache) | `mekano-infrastructure/pom.xml:67` |
| `quarkus-smallrye-fault-tolerance` | `@Retry`, `@Timeout` annotations | `mekano-infrastructure/pom.xml:54` |

### Code Generation

| Dependency | Version | Purpose | File |
|-----------|---------|---------|------|
| Lombok | 1.18.36 | Boilerplate reduction (`@Getter`, `@Setter`, `@Builder`, `@EqualsAndHashCode`) | `pom.xml:29` |
| Lombok-MapStruct binding | 0.2.0 | Ensures MapStruct sees Lombok-generated accessors | `pom.xml:31` |
| Jandex | 3.5.3 | CDI bean indexing | `pom.xml:30` |

## Configuration

**Environment:**
- Quarkus `application.properties` at `mekano-rest/src/main/resources/application.properties`
- Three Maven build profiles: default (dev), `%prod`, `%test` (Quarkus config profiles)
- Per-profile datasource config (dev→local docker-compose, prod→env vars, test→DevServices/rancher)
- `%prod` uses environment variables: `DB_USER`, `DB_PASSWORD`, `DB_URL`

**Key configs required:**
- PostgreSQL database (credentials: `mekano`/`mekano` locally)
- Public key for JWT verification at `mekano-rest/src/main/resources/publicKey.pem` (planned)
- Private key for JWT signing at `~/.mekano/secrets/privatekey.pem` (planned)

**Build config:**
- `quarkus.rest.path=/api/v1` — All API endpoints under `/api/v1`
- `quarkus.http.cors=true` — Global CORS enabled with permissive origins
- `quarkus.jackson.timezone=America/Sao_Paulo`
- `quarkus.flyway.migrate-at-start=true`

**Annotation Processor Order (critical Gotcha):**
1. Lombok → Lombok-MapStruct binding → MapStruct processor (must be exact order in `annotationProcessorPaths`)

## Platform Requirements

**Development:**
- JDK 17 (Eclipse Temurin recommended via CI config)
- Docker Desktop / Rancher Desktop (for PostgreSQL via `docker-compose.yml`)
- Maven 3.9+ (wrapper provided at `mvnw`)
- Private key at `~/.mekano/secrets/privatekey.pem` for JWT signing

**Production:**
- Deployment target: Docker container (UBI 9 base images)
- JVM mode: `registry.access.redhat.com/ubi9/openjdk-17-runtime:1.24` (`Dockerfile.jvm`)
- Native mode: `registry.access.redhat.com/ubi9/ubi-minimal:9.7` (`Dockerfile.native`)
- Micro native: `quay.io/quarkus/ubi9-quarkus-micro-image:2.0` (`Dockerfile.native-micro`)
- Environment variables: `DB_USER`, `DB_PASSWORD`, `DB_URL`, `MP_JWT_ISSUER`
- Port: 8080 (EXPOSEd in all Dockerfiles)

---

*Stack analysis: 2026-06-20*
