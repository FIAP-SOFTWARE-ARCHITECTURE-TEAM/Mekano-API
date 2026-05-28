# Technology Stack Research

**Project:** Mekano — Clean Architecture Quarkus API  
**Researched:** 2026-05-27  
**Overall confidence:** HIGH (all key findings verified against official Quarkus docs and MapStruct reference)

---

## 1. Multi-module Maven + Quarkus Project Structure

### Parent POM Layout

The root `pom.xml` must have `<packaging>pom</packaging>` and list all child modules. The Quarkus BOM belongs in the parent's `<dependencyManagement>` so all modules can declare Quarkus dependencies without repeating versions.

```xml
<!-- Parent pom.xml (root) -->
<packaging>pom</packaging>

<modules>
  <module>domain</module>
  <module>application</module>
  <module>infrastructure</module>
  <module>adapter</module>
</modules>

<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>io.quarkus.platform</groupId>
      <artifactId>quarkus-bom</artifactId>
      <version>3.36.0</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

### Module Packaging Rules

| Module | `<packaging>` | `quarkus-maven-plugin` | Notes |
|--------|--------------|------------------------|-------|
| `domain` | `jar` | ❌ | Pure Java, no Quarkus deps |
| `application` | `jar` | ❌ | CDI allowed; needs jandex |
| `infrastructure` | `jar` | ❌ | JPA, Flyway; needs jandex |
| `adapter` | **`quarkus`** | ✅ (with `<extensions>true</extensions>`) | Runner module |

**Critical rule:** Only ONE module gets `<packaging>quarkus</packaging>` and the `quarkus-maven-plugin`. In this architecture, that is the `adapter` module (the JAX-RS layer that is the application entry point).

The current root `pom.xml` uses `<packaging>quarkus</packaging>` — this will need to move to the `adapter` module when the multi-module structure is created.

### CDI Bean Discovery in Non-Runner Modules

> **Official Quarkus docs:** "By default, Quarkus will not discover CDI beans inside another module."

Every non-runner module that contains CDI beans (`@ApplicationScoped`, `@Inject`, etc.) **must** include the `jandex-maven-plugin`. The runner module (`adapter`) is indexed automatically by `quarkus-maven-plugin`.

```xml
<!-- Required in: application/pom.xml, infrastructure/pom.xml -->
<build>
  <plugins>
    <plugin>
      <groupId>io.smallrye</groupId>
      <artifactId>jandex-maven-plugin</artifactId>
      <version>3.5.3</version>
      <executions>
        <execution>
          <id>make-index</id>
          <goals>
            <goal>jandex</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

The `domain` module has no CDI beans by design (pure domain objects) so it **does not** need jandex.

### Dependency Rules in `pom.xml`

Reflects Clean Architecture rules:
```
adapter     → domain + application + infrastructure
infrastructure → domain
application → domain
domain      → (nothing)
```

Each module declares its dependencies explicitly. The BOM version is inherited from parent so no `<version>` tags needed for Quarkus artifacts.

**Source:** https://quarkus.io/guides/maven-tooling — "Working with multi-module projects" section  
**Confidence:** HIGH

---

## 2. Quarkus REST (quarkus-rest-jackson)

The current `pom.xml` uses `quarkus-rest` (RESTEasy Reactive without Jackson). For JSON serialization with Jackson, use `quarkus-rest-jackson` instead.

```xml
<!-- Replace quarkus-rest with quarkus-rest-jackson in adapter/pom.xml -->
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-rest-jackson</artifactId>
</dependency>
```

For `@Valid` / `@NotNull` bean validation on request bodies:
```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-hibernate-validator</artifactId>
</dependency>
```

---

## 3. Hibernate ORM Panache + Flyway + PostgreSQL

### Dependencies (infrastructure module)

```xml
<!-- infrastructure/pom.xml -->
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-hibernate-orm-panache</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-flyway</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>
```

### application.properties Configuration

```properties
# --- Datasource (JDBC, PostgreSQL) ---
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.max-size=16

# Production credentials (injected via env or docker-compose)
%prod.quarkus.datasource.username=${DB_USERNAME:mekano}
%prod.quarkus.datasource.password=${DB_PASSWORD:mekano}
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mekano

# --- Hibernate ORM ---
# Dialect auto-detected from db-kind=postgresql — no explicit dialect needed
# schema-management.strategy ONLY for dev convenience; Flyway owns schema in prod
%dev.quarkus.hibernate-orm.schema-management.strategy=validate
%prod.quarkus.hibernate-orm.schema-management.strategy=validate
# Log SQL in dev, not prod
%dev.quarkus.hibernate-orm.log.sql=true
%prod.quarkus.hibernate-orm.log.sql=false

# --- Flyway ---
quarkus.flyway.migrate-at-start=true
quarkus.flyway.locations=classpath:db/migration
# Safety: never drop schema in production
%prod.quarkus.flyway.clean-disabled=true
# Dev convenience only — NEVER in prod
%dev.quarkus.flyway.clean-at-start=false
# Retry connecting to DB on startup (important for docker-compose race condition)
quarkus.flyway.connect-retries=5

# --- Dev Services (auto-provisioned PostgreSQL in dev/test) ---
# Pin image and port for reproducibility
quarkus.datasource.devservices.image-name=postgres:16-alpine
quarkus.datasource.devservices.port=5432
quarkus.datasource.devservices.db-name=mekano
```

### Flyway Migration Naming

Files go in `infrastructure/src/main/resources/db/migration/`:
```
V1__create_users_table.sql
V2__add_email_index.sql
```

Standard naming: `V{version}__{description}.sql` (double underscore).

### Panache Usage Pattern

In a Clean Architecture setup, the `infrastructure` module owns JPA entities and repository implementations:

```java
// infrastructure module — JPA entity (NOT the domain entity)
@Entity
@Table(name = "users")
public class UserJpaEntity extends PanacheEntity {  // or PanacheEntityBase with custom ID
    public String name;
    public String email;
}

// infrastructure module — repository implementation
@ApplicationScoped
public class UserRepositoryImpl implements UserRepositoryPort {

    @Inject
    EntityManager em;  // or: extend PanacheRepository<UserJpaEntity>

    @Override
    @Transactional
    public User save(User user) {
        UserJpaEntity entity = UserMapper.INSTANCE.toJpa(user);
        entity.persist();
        return UserMapper.INSTANCE.toDomain(entity);
    }

    @Override
    public Optional<User> findById(Long id) {
        return UserJpaEntity.<UserJpaEntity>findByIdOptional(id)
                .map(UserMapper.INSTANCE::toDomain);
    }
}
```

**Important:** `@Transactional` must be on methods that modify the database. Recommended placement: service/use-case layer OR repository layer — do NOT place at the JAX-RS resource level in Clean Architecture.

**Official note:** Do NOT mix `persistence.xml` and `quarkus.hibernate-orm.*` in `application.properties` — Quarkus throws an exception.

**Source:** https://quarkus.io/guides/hibernate-orm, https://quarkus.io/guides/flyway, https://quarkus.io/guides/datasource  
**Confidence:** HIGH

---

## 4. MapStruct + Lombok — Annotation Processor Ordering

### The Problem

MapStruct generates mapper implementations from interfaces at compile time by reading getters/setters. Lombok also generates getters/setters at compile time. If MapStruct runs first, Lombok's generated methods don't exist yet, causing "no property named X" compilation errors.

Additionally, Lombok 1.18.16+ introduced a breaking change requiring `lombok-mapstruct-binding` as a mediator.

### Correct annotationProcessorPaths Order

The order in `<annotationProcessorPaths>` is **critical**:

```xml
<!-- infrastructure/pom.xml and adapter/pom.xml (wherever MapStruct is used) -->
<plugin>
  <artifactId>maven-compiler-plugin</artifactId>
  <version>3.15.0</version>
  <configuration>
    <parameters>true</parameters>
    <annotationProcessorPaths>
      <!-- 1. Lombok FIRST — generates getters/setters -->
      <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
      </path>
      <!-- 2. Binding bridge — required for Lombok >= 1.18.16 -->
      <path>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok-mapstruct-binding</artifactId>
        <version>0.2.0</version>
      </path>
      <!-- 3. MapStruct LAST — reads Lombok-generated methods -->
      <path>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct-processor</artifactId>
        <version>${mapstruct.version}</version>
      </path>
    </annotationProcessorPaths>
  </configuration>
</plugin>
```

### Recommended Versions (parent POM properties)

```xml
<properties>
  <mapstruct.version>1.6.3</mapstruct.version>
  <lombok.version>1.18.36</lombok.version>
</properties>
```

### Dependencies

```xml
<!-- Lombok: compile-only (annotation processor generates code, runtime not needed) -->
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <version>${lombok.version}</version>
  <scope>provided</scope>
</dependency>

<!-- MapStruct: compile + runtime (generated mappers referenced at runtime) -->
<dependency>
  <groupId>org.mapstruct</groupId>
  <artifactId>mapstruct</artifactId>
  <version>${mapstruct.version}</version>
</dependency>
```

### Mapper Example (Clean Architecture)

```java
// infrastructure module: JPA entity ↔ domain object
@Mapper(componentModel = "cdi")  // CDI injection via @Inject
public interface UserEntityMapper {
    User toDomain(UserJpaEntity entity);
    UserJpaEntity toJpa(User domain);
}

// adapter module: domain object ↔ DTO
@Mapper(componentModel = "cdi")
public interface UserDtoMapper {
    UserResponse toResponse(User domain);
    User toDomain(CreateUserRequest request);
}
```

`componentModel = "cdi"` makes the generated mapper a CDI bean injectable via `@Inject`. This is the required approach for Quarkus (not `Mappers.getMapper()`).

**Source:** https://mapstruct.org/documentation/stable/reference/html/#lombok  
**Confidence:** HIGH

---

## 5. SmallRye JWT Setup

### Dependency (adapter module)

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-smallrye-jwt</artifactId>
</dependency>
<!-- Optional: for building tokens in tests -->
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-smallrye-jwt-build</artifactId>
  <scope>test</scope>
</dependency>
```

### application.properties

```properties
# Public key for JWT verification (place publicKey.pem in src/main/resources)
mp.jwt.verify.publickey.location=publicKey.pem
# Issuer must match the `iss` claim in incoming JWTs
mp.jwt.verify.issuer=https://mekano.fiap.com.br/auth

# For signing in tests (private key in src/test/resources)
# smallrye.jwt.sign.key.location=privateKey.pem
```

### Resource Annotation

```java
// CRITICAL: @RequestScoped is required for JWT-protected resources
// @ApplicationScoped will cause CDI + JWT injection issues
@Path("/users")
@RequestScoped
@Authenticated   // or @RolesAllowed on individual methods
public class UserResource {

    @Inject
    JsonWebToken jwt;           // full token

    @Inject
    @Claim(standard = Claims.upn)
    String upn;                 // individual claim injection

    @GET
    @Path("/me")
    @RolesAllowed({"User", "Admin"})
    public UserResponse me() {
        return userService.findByUpn(upn);
    }
}
```

### Key Generation (for dev/test)

```bash
# Generate RSA key pair
openssl genrsa -out privateKey.pem 2048
openssl rsa -pubout -in privateKey.pem -out publicKey.pem
```

Place `publicKey.pem` in `adapter/src/main/resources/`.

**Source:** https://quarkus.io/guides/security-jwt  
**Confidence:** HIGH

---

## 6. SmallRye Fault Tolerance

### Dependency (infrastructure or application module, wherever resilience is needed)

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-smallrye-fault-tolerance</artifactId>
</dependency>
```

### Annotation Reference

```java
@ApplicationScoped
public class ExternalServiceClient {

    // Retry: retry up to 4 times on failure
    @Retry(maxRetries = 4, delay = 200, delayUnit = ChronoUnit.MILLIS)
    public String callExternalService() { ... }

    // Timeout: fail if call takes > 500ms
    @Timeout(500)
    public Data fetchData() { ... }

    // CircuitBreaker: open after 2 of 4 consecutive failures; stay open 5s
    @CircuitBreaker(
        requestVolumeThreshold = 4,   // rolling window size
        failureRatio = 0.5,           // 50% failures = open (default)
        delay = 5000,                 // stay open 5s before half-open (default)
        delayUnit = ChronoUnit.MILLIS
    )
    public Status checkStatus() { ... }

    // Fallback: called when annotated method fails (after retries/timeout/CB)
    @Fallback(fallbackMethod = "fallbackStatus")
    @CircuitBreaker(requestVolumeThreshold = 4)
    public Status getStatus() { ... }

    private Status fallbackStatus() {
        return Status.DEGRADED;
    }
}
```

### Per-Method Config Override (application.properties)

```properties
# Override annotation values without recompiling:
quarkus.fault-tolerance."com.fiap.ExternalServiceClient/callExternalService".retry.max-retries=6

# Disable a specific strategy:
quarkus.fault-tolerance."com.fiap.ExternalServiceClient/checkStatus".circuit-breaker.enabled=false

# Disable all strategies (useful for testing):
quarkus.fault-tolerance.enabled=false
```

### Placement in Clean Architecture

Best applied in `infrastructure` module on external adapters (HTTP clients, third-party integrations). Do **not** annotate domain or use-case code with fault tolerance annotations — those should stay infrastructure concerns.

**Source:** https://quarkus.io/guides/smallrye-fault-tolerance  
**Confidence:** HIGH

---

## 7. Quarkus Health (SmallRye Health)

### Dependency (adapter module or infrastructure module)

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-smallrye-health</artifactId>
</dependency>
```

### Built-in Endpoints

| Endpoint | Purpose |
|----------|---------|
| `GET /q/health/live` | Liveness — app is running |
| `GET /q/health/ready` | Readiness — app can serve requests |
| `GET /q/health/started` | Startup — app finished starting |
| `GET /q/health` | All health checks combined |

The datasource health check is automatically added when `quarkus-jdbc-postgresql` is present.

### Custom Health Check

```java
@Liveness          // or @Readiness, @Startup
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {

    @Inject
    DataSource dataSource;

    @Override
    public HealthCheckResponse call() {
        try (var conn = dataSource.getConnection()) {
            return HealthCheckResponse.up("database");
        } catch (Exception e) {
            return HealthCheckResponse
                .named("database")
                .down()
                .withData("error", e.getMessage())
                .build();
        }
    }
}
```

**Source:** https://quarkus.io/guides/smallrye-health  
**Confidence:** HIGH

---

## 8. Micrometer + Prometheus

### Dependencies

```xml
<!-- quarkus-micrometer-registry-prometheus pulls in micrometer-core automatically -->
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-micrometer-registry-prometheus</artifactId>
</dependency>
```

### Metrics Endpoint

Default: `GET /q/metrics` (Prometheus `application/openmetrics-text` format by default)

```properties
# Optional: Expose on management interface (separate port)
quarkus.management.enabled=true
# Default management port: 9000 → metrics at http://0.0.0.0:9000/q/metrics

# Customize Prometheus path
quarkus.micrometer.export.prometheus.path=metrics/prometheus
```

### Custom Metrics

```java
@ApplicationScoped
public class UserMetrics {

    private final Counter userCreationCounter;
    private final Timer userLookupTimer;

    @Inject
    public UserMetrics(MeterRegistry registry) {
        userCreationCounter = registry.counter("mekano.users.created",
            "module", "user");
        userLookupTimer = registry.timer("mekano.users.lookup.duration",
            "module", "user");
    }

    public void recordCreation() {
        userCreationCounter.increment();
    }

    public <T> T timeOperation(Supplier<T> operation) {
        return userLookupTimer.record(operation);
    }
}
```

**Naming convention:** Prometheus converts `.` to `_` and adds units, so `mekano.users.created` → `mekano_users_created_total`.

**Source:** https://quarkus.io/guides/telemetry-micrometer  
**Confidence:** HIGH

---

## 9. Docker Compose for PostgreSQL + Quarkus Dev Services

### Recommended Approach: Use Dev Services for Dev/Test, docker-compose for Production-Like

Quarkus Dev Services auto-starts a PostgreSQL container (via Testcontainers) in dev and test mode. This means **no docker-compose is needed for `./mvnw quarkus:dev`** if Docker is running.

For an explicit `docker-compose.yml` (production-like local environment):

```yaml
# docker-compose.yml — root of project
version: "3.9"

services:
  postgres:
    image: postgres:16-alpine
    container_name: mekano-postgres
    environment:
      POSTGRES_DB: mekano
      POSTGRES_USER: mekano
      POSTGRES_PASSWORD: mekano
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U mekano -d mekano"]
      interval: 5s
      timeout: 5s
      retries: 5
      start_period: 10s

  mekano-api:
    image: quarkus/mekano-jvm:latest  # built by: ./mvnw package -Dquarkus.container-image.build=true
    container_name: mekano-api
    environment:
      DB_USERNAME: mekano
      DB_PASSWORD: mekano
      QUARKUS_DATASOURCE_JDBC_URL: jdbc:postgresql://postgres:5432/mekano
    ports:
      - "8080:8080"
    depends_on:
      postgres:
        condition: service_healthy

volumes:
  postgres_data:
```

### application.properties for docker-compose mode

```properties
# Dev mode: Dev Services handles PostgreSQL automatically
# Disable Dev Services only when explicit URL is provided
quarkus.datasource.db-kind=postgresql

# Dev Services config (used when NO explicit URL is set)
quarkus.datasource.devservices.image-name=postgres:16-alpine
quarkus.datasource.devservices.port=5432
quarkus.datasource.devservices.db-name=mekano

# Production profile: reads from environment variables
%prod.quarkus.datasource.username=${DB_USERNAME:mekano}
%prod.quarkus.datasource.password=${DB_PASSWORD:mekano}
%prod.quarkus.datasource.jdbc.url=${QUARKUS_DATASOURCE_JDBC_URL:jdbc:postgresql://localhost:5432/mekano}
```

**Pattern:** Dev Services in dev/test → zero-config DB. `%prod.*` profile with env vars for docker-compose or any real deployment.

**Source:** https://quarkus.io/guides/dev-services, https://quarkus.io/guides/datasource  
**Confidence:** HIGH

---

## 10. Full Dependency Summary by Module

### Parent `pom.xml` properties block

```xml
<properties>
  <quarkus.platform.version>3.36.0</quarkus.platform.version>
  <quarkus.platform.group-id>io.quarkus.platform</quarkus.platform.group-id>
  <quarkus.platform.artifact-id>quarkus-bom</quarkus.platform.artifact-id>
  <maven.compiler.release>17</maven.compiler.release>
  <mapstruct.version>1.6.3</mapstruct.version>
  <lombok.version>1.18.36</lombok.version>
  <compiler-plugin.version>3.15.0</compiler-plugin.version>
  <surefire-plugin.version>3.5.4</surefire-plugin.version>
  <jandex-plugin.version>3.5.3</jandex-plugin.version>
</properties>
```

### `domain` module

```xml
<!-- Optional: Lombok for @Value, @Builder on domain objects -->
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <version>${lombok.version}</version>
  <scope>provided</scope>
</dependency>
```

No CDI, no JPA, no Quarkus extensions. No jandex plugin needed.

### `application` module

```xml
<!-- CDI scope annotations only -->
<dependency>
  <groupId>jakarta.enterprise</groupId>
  <artifactId>jakarta.enterprise.cdi-api</artifactId>
  <scope>provided</scope>
</dependency>
<!-- Fault Tolerance annotations if use cases need resilience -->
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-smallrye-fault-tolerance</artifactId>
</dependency>
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <version>${lombok.version}</version>
  <scope>provided</scope>
</dependency>
<!-- MUST have jandex for CDI discovery -->
```

### `infrastructure` module

```xml
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-hibernate-orm-panache</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-jdbc-postgresql</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-flyway</artifactId>
</dependency>
<dependency>
  <groupId>org.mapstruct</groupId>
  <artifactId>mapstruct</artifactId>
  <version>${mapstruct.version}</version>
</dependency>
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <version>${lombok.version}</version>
  <scope>provided</scope>
</dependency>
<!-- MUST have jandex + annotationProcessorPaths with Lombok → binding → MapStruct order -->
```

### `adapter` module (runner)

```xml
<!-- Quarkus runner packaging + plugin goes here -->
<packaging>quarkus</packaging>

<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-arc</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-rest-jackson</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-hibernate-validator</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-smallrye-openapi</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-smallrye-jwt</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-smallrye-health</artifactId>
</dependency>
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-micrometer-registry-prometheus</artifactId>
</dependency>
<dependency>
  <groupId>org.mapstruct</groupId>
  <artifactId>mapstruct</artifactId>
  <version>${mapstruct.version}</version>
</dependency>
<dependency>
  <groupId>org.projectlombok</groupId>
  <artifactId>lombok</artifactId>
  <version>${lombok.version}</version>
  <scope>provided</scope>
</dependency>
<!-- Test -->
<dependency>
  <groupId>io.quarkus</groupId>
  <artifactId>quarkus-junit5</artifactId>
  <scope>test</scope>
</dependency>
<dependency>
  <groupId>io.rest-assured</groupId>
  <artifactId>rest-assured</artifactId>
  <scope>test</scope>
</dependency>
```

---

## 11. Known Gotchas & Pitfalls

| Area | Gotcha | Fix |
|------|--------|-----|
| Multi-module CDI | CDI beans not found across modules | Add `jandex-maven-plugin` to every non-runner module with beans |
| MapStruct + Lombok | "No property named X" at compile time | Order: `lombok` → `lombok-mapstruct-binding` → `mapstruct-processor` in `annotationProcessorPaths` |
| MapStruct CDI | `@Mapper` instance not injectable | Use `componentModel = "cdi"` on every `@Mapper` interface |
| JWT + CDI scope | JWT claims not injectable | Use `@RequestScoped` on JAX-RS resources, NOT `@ApplicationScoped` |
| Flyway + Dev Services | DB not ready when Flyway runs | Set `quarkus.flyway.connect-retries=5` (race condition on startup) |
| Hibernate schema | Conflict with Flyway | Set `quarkus.hibernate-orm.schema-management.strategy=validate` in all profiles; let Flyway own the schema |
| `persistence.xml` | Can't coexist with `quarkus.hibernate-orm.*` properties | Pick one approach only (use properties, not persistence.xml) |
| `quarkus-rest` vs `quarkus-rest-jackson` | JSON not serialized | Use `quarkus-rest-jackson`; `quarkus-rest` returns plain text by default |
| Fault Tolerance placement | Annotating domain/use-case code | Apply only on infrastructure adapters (external calls); domain stays clean |
| Dev Services conflict | Explicit datasource URL disables Dev Services automatically | Only use `%prod.*` prefix for explicit URLs |

---

## Sources

| Source | URL | Confidence |
|--------|-----|------------|
| Quarkus Maven Tooling | https://quarkus.io/guides/maven-tooling | HIGH |
| Quarkus Hibernate ORM | https://quarkus.io/guides/hibernate-orm | HIGH |
| Quarkus Flyway | https://quarkus.io/guides/flyway | HIGH |
| Quarkus Datasource | https://quarkus.io/guides/datasource | HIGH |
| Quarkus Security JWT | https://quarkus.io/guides/security-jwt | HIGH |
| Quarkus Fault Tolerance | https://quarkus.io/guides/smallrye-fault-tolerance | HIGH |
| Quarkus SmallRye Health | https://quarkus.io/guides/smallrye-health | HIGH |
| Quarkus Micrometer | https://quarkus.io/guides/telemetry-micrometer | HIGH |
| Quarkus Dev Services | https://quarkus.io/guides/dev-services | HIGH |
| MapStruct + Lombok | https://mapstruct.org/documentation/stable/reference/html/#lombok | HIGH |
