# Testing Patterns: Quarkus 3 + Clean Architecture Multi-Module Maven

**Project:** Mekano — Clean Architecture API  
**Stack:** Quarkus 3.36.0 + Java 17 + PostgreSQL + Maven multi-module  
**Researched:** 2025  
**Overall confidence:** HIGH (all findings verified against official Quarkus 3.x docs)

---

## 1. `@QuarkusTest` vs `@QuarkusIntegrationTest`

### `@QuarkusTest` — Unit/Integration Tests (in-process)

- Starts the **full Quarkus application inside the same JVM** as the tests
- Runs as part of `maven-surefire-plugin` (`mvn test`) — `*Test.java` naming convention
- Has access to CDI context: you can `@Inject` beans, use `@InjectMock`, `@TestTransaction`, etc.
- REST Assured is pre-configured to point at `localhost:{quarkus.http.test-port}` automatically — no URL setup needed
- **Use for:** testing REST endpoints, service integration, repository layer with real DB (DevServices/Testcontainers), use case orchestration in the Quarkus context
- **Confidence:** HIGH — [Quarkus Getting Started Testing Guide](https://quarkus.io/guides/getting-started-testing)

```java
@QuarkusTest
class UserResourceTest {
    @Test
    void createUser_shouldReturn201() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"name":"Alice","email":"alice@example.com"}""")
        .when()
            .post("/users")
        .then()
            .statusCode(201)
            .body("name", equalTo("Alice"));
    }
}
```

### `@QuarkusIntegrationTest` — Packaged Artifact Tests (out-of-process)

- Tests the **packaged artifact** (JAR or native binary) — the app runs as an **external process**
- Runs via `maven-failsafe-plugin` (`mvn verify`) — `*IT.java` naming convention
- **No access to CDI context** — no `@Inject`, no `@InjectMock`, no `@TestTransaction`
- REST Assured still works (connects to the external process HTTP port)
- Controlled by `<skipITs>true</skipITs>` property (already in the root `pom.xml` — set to `false` to enable)
- **Use for:** smoke tests against the real packaged artifact, native image validation
- **Confidence:** HIGH — official Quarkus docs

```java
// The IT class typically just extends the @QuarkusTest to reuse test methods
@QuarkusIntegrationTest
class UserResourceIT extends UserResourceTest {
    // Executes the same HTTP tests, but against the packaged JAR/native binary
}
```

### Decision Rule for This Project

| Test Type | Annotation | Maven Phase | Use When |
|-----------|-----------|-------------|----------|
| In-process (most tests) | `@QuarkusTest` | `test` (surefire) | Testing endpoints, use cases wired via CDI, repository with DB |
| Post-package smoke | `@QuarkusIntegrationTest` | `verify` (failsafe) | Validating packaged JAR; native image verification |

**Recommendation:** Keep `<skipITs>true</skipITs>` (current default). Enable only if you add native build stage. All meaningful tests live in `@QuarkusTest`.

---

## 2. Testing Use Cases in the `application` Module (Plain JUnit — No Quarkus)

The `application` module has **no Quarkus dependency** by design — use cases depend only on port interfaces (domain). This makes them trivially testable with plain JUnit 5 + Mockito.

**Why this is the best test:** fast (no app startup), deterministic, no infrastructure noise, pure business logic.

```xml
<!-- application/pom.xml — test deps only -->
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
```

```java
// No @QuarkusTest — plain JUnit 5
class CreateUserUseCaseTest {

    @Mock
    UserRepositoryPort userRepository;

    @Mock
    UserOutputPort userOutputPort;

    @InjectMocks
    CreateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldCreateUser_whenEmailIsUnique() {
        var command = new CreateUserCommand("Alice", "alice@example.com");
        when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = useCase.execute(command);

        assertThat(result.name()).isEqualTo("Alice");
        verify(userRepository).save(any(User.class));
    }

    @Test
    void shouldThrow_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("dupe@example.com")).thenReturn(true);
        assertThatThrownBy(() -> useCase.execute(new CreateUserCommand("Bob", "dupe@example.com")))
            .isInstanceOf(DuplicateEmailException.class);
    }
}
```

**No `@QuarkusTest` needed.** No application startup. Test runs in milliseconds.

---

## 3. Testing Infrastructure with Testcontainers + PostgreSQL

### Option A: Quarkus DevServices (Recommended for Development)

Quarkus 3.x has **built-in DevServices** that automatically spins up a PostgreSQL container (via Testcontainers under the hood) when:
1. `quarkus-jdbc-postgresql` extension is present
2. No explicit `quarkus.datasource.jdbc.url` is configured in the active profile

**Zero-config for tests:** DevServices activates automatically in test mode when no DB URL is set.

```properties
# application.properties — configure prod connection, DevServices auto-activates in test/dev
%prod.quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mekano
%prod.quarkus.datasource.username=mekano
%prod.quarkus.datasource.password=mekano

# DevServices config (optional, to pin image version)
quarkus.datasource.devservices.image-name=docker.io/library/postgres:16
```

- **Confidence:** HIGH — [Quarkus Dev Services Guide](https://quarkus.io/guides/dev-services), [Datasource Guide](https://quarkus.io/guides/datasource)

### Option B: Explicit `@QuarkusTestResource` with Testcontainers

Use when you need precise control over the container lifecycle, shared state, or when DevServices conflicts arise:

```xml
<!-- adapter/pom.xml or infrastructure/pom.xml test deps -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-test-common</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
```

```java
public class PostgreSQLTestResource implements QuarkusTestResourceLifecycleManager {

    private static final PostgreSQLContainer<?> POSTGRES =
        new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("mekano_test")
            .withUsername("test")
            .withPassword("test");

    @Override
    public Map<String, String> start() {
        POSTGRES.start();
        return Map.of(
            "quarkus.datasource.jdbc.url", POSTGRES.getJdbcUrl(),
            "quarkus.datasource.username",  POSTGRES.getUsername(),
            "quarkus.datasource.password",  POSTGRES.getPassword()
        );
    }

    @Override
    public void stop() {
        POSTGRES.stop();
    }
}
```

```java
@QuarkusTest
@QuarkusTestResource(PostgreSQLTestResource.class)
class UserRepositoryImplTest {
    @Inject
    UserRepositoryPort userRepository;

    @Test
    @TestTransaction  // rolls back after each test
    void shouldPersistAndRetrieveUser() {
        var user = new User(null, "Alice", "alice@example.com");
        var saved = userRepository.save(user);
        assertThat(saved.id()).isNotNull();
        assertThat(userRepository.findById(saved.id())).isPresent();
    }
}
```

### `@TestTransaction` — Automatic Rollback

- Annotate test methods (or the whole class) with `@io.quarkus.test.TestTransaction`
- Runs each test in a transaction and **rolls back** at the end — no leftover data between tests
- **Confidence:** HIGH — official Quarkus testing guide

---

## 4. REST Assured Patterns for the `adapter` Layer

### Setup (Already Present in Root `pom.xml`)

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>io.rest-assured</groupId>
    <artifactId>rest-assured</artifactId>
    <scope>test</scope>
</dependency>
```

Quarkus auto-configures REST Assured's `baseURI` and `port` — no setup code needed.

### Recommended Patterns

```java
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class UserResourceTest {

    // --- Create ---
    @Test
    void POST_users_returns201_withLocation() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"name":"Alice","email":"alice@example.com"}""")
        .when()
            .post("/users")
        .then()
            .statusCode(201)
            .header("Location", containsString("/users/"))
            .body("id",    notNullValue())
            .body("name",  equalTo("Alice"))
            .body("email", equalTo("alice@example.com"));
    }

    // --- Validation error ---
    @Test
    void POST_users_returns400_whenNameIsBlank() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"name":"","email":"bad@example.com"}""")
        .when()
            .post("/users")
        .then()
            .statusCode(400);
    }

    // --- Not found ---
    @Test
    void GET_users_returns404_forUnknownId() {
        given()
        .when()
            .get("/users/99999")
        .then()
            .statusCode(404);
    }

    // --- Typed extraction ---
    @Test
    void GET_users_returnsUser() {
        // First create
        var id = given()
            .contentType(ContentType.JSON)
            .body("""{"name":"Bob","email":"bob@example.com"}""")
            .post("/users")
            .then().statusCode(201)
            .extract().path("id");

        // Then fetch
        given()
        .when()
            .get("/users/" + id)
        .then()
            .statusCode(200)
            .body("name", equalTo("Bob"));
    }
}
```

### Testing the ExceptionMapper

```java
@QuarkusTest
class ExceptionMapperTest {

    @InjectMock
    CreateUserUseCase createUserUseCase;

    @Test
    void whenDuplicateEmail_returns409() {
        when(createUserUseCase.execute(any()))
            .thenThrow(new DuplicateEmailException("alice@example.com"));

        given()
            .contentType(ContentType.JSON)
            .body("""{"name":"Alice","email":"alice@example.com"}""")
        .when()
            .post("/users")
        .then()
            .statusCode(409)
            .body("message", containsString("alice@example.com"));
    }
}
```

---

## 5. Mocking Ports in Tests — `@InjectMock` and `QuarkusMock`

### `@InjectMock` (Recommended — Requires `quarkus-junit-mockito`)

```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-junit-mockito</artifactId>
    <scope>test</scope>
</dependency>
```

```java
@QuarkusTest
class UserResourceTest {

    @InjectMock
    CreateUserUseCase createUserUseCase;   // mocks the port/use case

    @BeforeEach
    void setup() {
        when(createUserUseCase.execute(any()))
            .thenReturn(new UserResponse(1L, "Alice", "alice@example.com"));
    }

    @Test
    void POST_users_delegatesToUseCase() {
        given()
            .contentType(ContentType.JSON)
            .body("""{"name":"Alice","email":"alice@example.com"}""")
        .when()
            .post("/users")
        .then()
            .statusCode(201);

        verify(createUserUseCase).execute(any());
    }
}
```

- `@InjectMock` replaces the CDI bean with a Mockito mock **for all tests in the class**
- Configure per-test behavior with `@BeforeEach` or inside each `@Test`
- **Works only in `@QuarkusTest`** — not in plain JUnit tests (use regular `@Mock` there)
- **Confidence:** HIGH — official Quarkus testing guide: artifact is `quarkus-junit-mockito` (not `quarkus-junit5-mockito`)

### `QuarkusMock` (Lower-level, no Mockito dependency)

```java
@BeforeAll
static void setup() {
    var mock = Mockito.mock(UserRepositoryPort.class);
    when(mock.findById(1L)).thenReturn(Optional.of(new User(1L, "Alice", "alice@example.com")));
    QuarkusMock.installMockForType(mock, UserRepositoryPort.class);
}
```

Use `QuarkusMock.installMockForType` in `@BeforeAll` (affects all tests) or `installMockForInstance` in `@Test` (affects one test method).

### What CAN and CANNOT be mocked

| Bean Type | Mockable with `@InjectMock`? | Notes |
|-----------|------------------------------|-------|
| `@ApplicationScoped` | ✅ Yes | Standard case — use cases, services |
| `@Singleton` | ✅ Yes | |
| `@RequestScoped` | ✅ Yes | |
| `@Dependent` | ⚠️ Limited | Works but lifecycle implications |
| CDI producer beans | ⚠️ Complex | May need `@Produces` override |

**Limitation:** `@InjectMock` replaces the bean for the whole `@QuarkusTest` JVM session shared per profile. Changing behavior between tests uses `Mockito.reset()` + re-stub or `QuarkusMock.installMockForInstance`.

---

## 6. Flyway Test Migrations Setup

### DevServices + Flyway Auto-Migration

When using DevServices (auto PostgreSQL), Flyway runs automatically at startup in test if `quarkus.flyway.migrate-at-start=true`.

```properties
# application.properties
quarkus.flyway.migrate-at-start=true
quarkus.flyway.locations=classpath:db/migration

# Override for tests — add test-specific seed data location
%test.quarkus.flyway.locations=classpath:db/migration,classpath:db/testdata
```

### Recommended Directory Layout (inside `infrastructure` module)

```
infrastructure/
└── src/
    ├── main/
    │   └── resources/
    │       └── db/
    │           └── migration/
    │               ├── V1__create_users.sql
    │               └── V2__add_email_unique.sql
    └── test/
        └── resources/
            └── db/
                └── testdata/
                    └── R__seed_test_users.sql   # Repeatable migration for test seed data
```

### `%test` Profile Overrides

```properties
# application.properties
%test.quarkus.flyway.clean-at-start=true          # Wipe & re-run all migrations on each test run
%test.quarkus.flyway.locations=classpath:db/migration,classpath:db/testdata
%test.quarkus.flyway.migrate-at-start=true
```

> **Warning:** `clean-at-start=true` in tests is fine — but `clean-disabled` defaults to `false` so it must remain explicitly allowed. Never set `clean-at-start=true` in prod.

### `@TestTransaction` vs Flyway

- `@TestTransaction` handles per-test rollback at the **data level** (DML)
- Flyway handles **schema level** (DDL) — migrations run once per test app startup
- Combine: Flyway creates schema on startup, `@TestTransaction` cleans DML between tests

---

## 7. Critical Pitfall: Bean Discovery in Multi-Module Maven

### The Problem (Most Important Pitfall)

> **"By default, Quarkus will not discover CDI beans inside another module."**  
> — [Quarkus Maven Tooling Guide](https://quarkus.io/guides/maven-tooling)

When the project is split into `domain`, `application`, `infrastructure`, `adapter` modules, **beans in non-main modules are invisible to Quarkus** unless explicitly indexed. This causes:
- `@ApplicationScoped` use cases in `application` module → not found → `UnsatisfiedResolutionException`
- `@Entity` and `@ApplicationScoped` repository impls in `infrastructure` → not found
- Silent failures: app starts but injections are `null` or throw at runtime

### The Fix: Add `jandex-maven-plugin` to Every Sub-Module

Add to **every module that contains CDI beans** (i.e., `application`, `infrastructure`, `adapter`):

```xml
<!-- In application/pom.xml, infrastructure/pom.xml, adapter/pom.xml -->
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

This generates `META-INF/jandex.idx` inside each module's JAR, making Quarkus scan it.

- **Confidence:** HIGH — [Quarkus Maven Tooling Guide](https://quarkus.io/guides/maven-tooling), [CDI Reference](https://quarkus.io/guides/cdi-reference)

### Alternative: `quarkus.index-dependency` in `application.properties`

```properties
# Main module's application.properties — fallback when you can't modify sub-module pom
quarkus.index-dependency.app.group-id=com.fiap
quarkus.index-dependency.app.artifact-id=mekano-application

quarkus.index-dependency.infra.group-id=com.fiap
quarkus.index-dependency.infra.artifact-id=mekano-infrastructure
```

**Prefer jandex plugin** — it's build-time, more reliable, and doesn't pollute `application.properties`.

### The `domain` Module Exception

The `domain` module has **no CDI beans** (pure Java — entities, value objects, port interfaces). It does NOT need jandex. The `application` module uses `@ApplicationScoped` on use cases, so it DOES need jandex.

---

## 8. Test Profiles (`QuarkusTestProfile`)

### When Profiles Are Needed

Use `@TestProfile` when a test class needs different configuration than others (e.g., different DB, disabled security, mock external services).

```java
public class NoSecurityProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "quarkus.security.auth.enabled-in-dev-mode", "false",
            "mp.jwt.verify.publickey.location", "test-public-key.pem"
        );
    }

    @Override
    public String getConfigProfile() {
        return "test-no-security";
    }
}

@QuarkusTest
@TestProfile(NoSecurityProfile.class)
class SecuredEndpointTest {
    // ...
}
```

### Profile Performance Warning

**Each different `@TestProfile` causes a Quarkus application restart.** Quarkus 3.x groups tests by profile (via `QuarkusTestProfileAwareClassOrderer`) to minimize restarts, but having many profiles = slow test suite.

**Recommendation:** Keep profiles minimal. Use 2-3 max:
1. Default profile — most tests, real DB (DevServices)
2. Mock profile — tests that mock all infrastructure  
3. (Optional) Security profile — tests verifying auth behavior

---

## 9. Test Structure Recommendation for This Project

```
mekano/
├── domain/
│   └── src/test/java/...
│       └── UserTest.java                    # Plain JUnit — value object invariants
│
├── application/
│   └── src/test/java/...
│       └── CreateUserUseCaseTest.java       # Plain JUnit + Mockito — mocks ports
│       └── GetUserUseCaseTest.java          # Plain JUnit + Mockito
│
├── infrastructure/
│   └── src/test/java/...
│       └── UserRepositoryImplTest.java      # @QuarkusTest + @TestTransaction + DevServices
│       └── UserMapperTest.java              # Plain JUnit — MapStruct mapper
│
└── adapter/ (or main quarkus module)
    └── src/test/java/...
        ├── UserResourceTest.java            # @QuarkusTest + REST Assured (+ @InjectMock use cases)
        ├── UserResourceIT.java              # @QuarkusIntegrationTest extends UserResourceTest
        └── ExceptionMapperTest.java         # @QuarkusTest + @InjectMock
```

### Maven Dependency Guidance for Tests

| Module | Test Dependencies |
|--------|-------------------|
| `domain` | `junit-jupiter` only |
| `application` | `junit-jupiter` + `mockito-core` |
| `infrastructure` | `quarkus-junit` + `quarkus-test-common` + `testcontainers-postgresql` |
| `adapter` (main) | `quarkus-junit` + `rest-assured` + `quarkus-junit-mockito` |

---

## 10. Summary of Pitfalls

| Pitfall | Impact | Fix |
|---------|--------|-----|
| Missing `jandex-maven-plugin` in sub-modules | **CRITICAL** — `UnsatisfiedResolutionException`, silent null injections | Add jandex plugin to every module with CDI beans |
| Using `@InjectMock` in plain JUnit tests (not `@QuarkusTest`) | Test fails to compile/run | Use `@Mock` (Mockito) in plain JUnit; `@InjectMock` only in `@QuarkusTest` |
| Too many `@TestProfile` — each causes app restart | Slow test suite | Consolidate profiles; use `@InjectMock` instead of profiles for infrastructure mocking |
| `quarkus.flyway.clean-at-start=true` in prod config | **DATA LOSS** | Gate behind `%test.` profile prefix |
| `@QuarkusIntegrationTest` trying to use `@Inject` | Compilation/runtime error — no CDI in IT tests | Remove `@Inject` from IT tests; use only HTTP assertions |
| `@TestTransaction` on `@QuarkusIntegrationTest` | Not supported | `@TestTransaction` only works in `@QuarkusTest` |
| DevServices requiring Docker daemon not running | `ContainerLaunchException` | Ensure Docker Desktop is running in CI; or pin `%test.quarkus.datasource.devservices.enabled=false` and use `@QuarkusTestResource` |
| Forgetting `@QuarkusTest` on test class in `adapter` module | REST Assured hits no server → `ConnectionRefusedException` | Always annotate adapter tests with `@QuarkusTest` |
| `maven-surefire` version too old for JUnit 5 | Tests not discovered | Use version ≥ 3.x (current pom uses `3.5.4` ✅) |

---

## 11. Sources

| Source | URL | Confidence |
|--------|-----|------------|
| Quarkus Getting Started Testing Guide | https://quarkus.io/guides/getting-started-testing | HIGH |
| Quarkus Maven Tooling — Multi-Module | https://quarkus.io/guides/maven-tooling | HIGH |
| Quarkus CDI Reference — Bean Discovery | https://quarkus.io/guides/cdi-reference | HIGH |
| Quarkus Dev Services Guide | https://quarkus.io/guides/dev-services | HIGH |
| Quarkus Datasource Guide | https://quarkus.io/guides/datasource | HIGH |
| Quarkus Flyway Guide | https://quarkus.io/guides/flyway | HIGH |
