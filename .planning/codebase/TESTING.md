# Testing Patterns

**Analysis Date:** 2026-06-20

## Test Framework

**Runner:**
- JUnit 5 (Jupiter) — managed by Quarkus BOM 3.36.0
- No test dependency on `io.quarkus:quarkus-junit5` in `mekano-domain` or `mekano-application` (pure JUnit)
- `io.quarkus:quarkus-junit5` in `mekano-infrastructure` and `mekano-rest` (integration tests)
- Config: `maven-surefire-plugin` 3.5.4 in root `pom.xml` (`pom.xml:98-106`)

**Assertion Library:**
- **Domain layer:** JUnit 5 built-in assertions — `org.junit.jupiter.api.Assertions.*`
- **Application layer:** JUnit 5 built-in assertions — `org.junit.jupiter.api.Assertions.*`
- **Infrastructure layer:** AssertJ 3.27.3 fluent assertions — `org.assertj.core.api.Assertions.assertThat`
- **REST layer:** REST Assured Hamcrest matchers — `org.hamcrest.Matchers.*` (primary) + AssertJ (FaultToleranceTest only)

**Run Commands:**
```bash
./mvnw verify -pl mekano-rest -am                 # Run all tests (full build)
./mvnw test -pl mekano-domain                      # Unit tests only (< 3s)
./mvnw test -pl mekano-application -am             # Application unit tests (Mockito)
./mvnw test -pl mekano-infrastructure -am          # Integration tests (DevServices)
./mvnw test -pl mekano-rest -am                    # REST Assured end-to-end
./mvnw package -Dnative -pl mekano-rest -am        # Native build (skips ITs)
```

## Test File Organization

**Location:**
- **Co-located by module** — tests live in `src/test/java/` mirroring the main source structure
- Each module (`mekano-domain`, `mekano-application`, `mekano-infrastructure`, `mekano-rest`) has its own test directory

**Naming:**
- `{ClassName}Test.java` suffix for all test classes: `UserTest.java`, `EmailTest.java`, `UserServiceTest.java`
- Domain-specific suffixes for scenario-based tests: `UserResourceTest.java`, `UserSoftDeleteTest.java`, `FaultToleranceTest.java`, `ObservabilityEndpointsTest.java`

**Structure:**
```
mekano-domain/src/test/java/com/fiap/mekano/
├── domain/
│   ├── model/UserTest.java
│   └── valueobject/EmailTest.java

mekano-application/src/test/java/com/fiap/mekano/
└── application/service/user/UserServiceTest.java

mekano-infrastructure/src/test/java/com/fiap/mekano/
└── infrastructure/repository/UserRepositoryImplTest.java

mekano-rest/src/test/java/com/fiap/mekano/
└── rest/
    ├── api/UserResourceTest.java
    ├── api/UserSoftDeleteTest.java
    ├── api/FaultToleranceTest.java
    └── observability/ObservabilityEndpointsTest.java
```

## Test Structure

**Suite Organization:**
```java
@DisplayName("User — entidade de domínio")        // Class-level description in Portuguese
class UserTest {

    private static final String NOME_VALIDO = "João Silva";     // Constants at top
    private static final String EMAIL_VALIDO = "joao@fiap.br";

    @Test
    @DisplayName("deve criar User com todos os campos populados")  // Test-level description
    void deveCriarUserComCamposPopulados() {
        // Arrange — inline setup
        User user = User.create(NOME_VALIDO, EMAIL_VALIDO, HASH_VALIDO);

        // Assert — multiple assertions
        assertNotNull(user);
        assertNotNull(user.getId(), "id deve ser não nulo");
        assertEquals(NOME_VALIDO, user.getName());
    }
}
```

**Patterns:**
- **Arrange-Act-Assert** comments present in some tests (`UserServiceTest.java:39-48`) but not all — simpler tests omit comments
- **`@DisplayName`** at both class and method level — describes intent in Portuguese for domain tests, mixed language for technical tests
- **`@Test`** annotation on every test method — no `@RepeatedTest`, `@TestFactory`, or dynamic tests used
- **`@ParameterizedTest`** used in `EmailTest.java` for boundary/value variations — `@NullAndEmptySource` for null/blank, `@ValueSource` for format patterns
- **Private static constants** at top of test class for reusable test values
- **One test class per production class** as a general rule, plus scenario-specific test classes

## Test Types by Layer

### Domain Tests (`mekano-domain`)
- **Scope:** Pure unit tests — no Quarkus container, no database, no mocks
- **Framework:** JUnit 5 only (no Mockito, no QuarkusTest)
- **What they test:** Value object validation, factory methods, entity creation, exception throwing
- **File examples:**
  - `mekano-domain/src/test/java/com/fiap/mekano/domain/valueobject/EmailTest.java` — 86 lines, 10 tests
  - `mekano-domain/src/test/java/com/fiap/mekano/domain/model/UserTest.java` — 79 lines, 8 tests
- **Typical patterns:**
  ```java
  @Test
  @DisplayName("deve lançar AppException(400) para email inválido")
  void deveLancarExcecaoParaEmailInvalido() {
      assertThrows(AppException.class,
              () -> User.create(NOME_VALIDO, "email-invalido", HASH_VALIDO));
  }
  ```

### Application Tests (`mekano-application`)
- **Scope:** Unit tests with Mockito — no Quarkus container, no database
- **Framework:** JUnit 5 + Mockito (`mockito-junit-jupiter`)
- **What they test:** Service orchestration logic, validation guards, port interaction, exception propagation
- **File examples:**
  - `mekano-application/src/test/java/com/fiap/mekano/application/service/user/UserServiceTest.java` — 84 lines, 4 tests
- **Typical patterns:**
  ```java
  @ExtendWith(MockitoExtension.class)
  @DisplayName("UserService")
  class UserServiceTest {
      @Mock UserRepositoryPort userRepository;
      @Mock PasswordHasher passwordHasher;
      @Mock EventPublisher eventPublisher;
      @InjectMocks UserService useCase;

      @Test
      @DisplayName("deve criar usuário com dados válidos")
      void deveCriarUsuarioComDadosValidos() {
          var command = new CreateUserCommand("João Silva", "joao@fiap.br", "senha123");
          when(userRepository.existsByEmail("joao@fiap.br")).thenReturn(false);
          when(passwordHasher.hash("senha123")).thenReturn("$2a$10$hashedpassword");
          when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

          User result = useCase.execute(command);

          assertNotNull(result);
          assertEquals("João Silva", result.getName());
          verify(userRepository, times(1)).save(any(User.class));
          verify(eventPublisher, times(1)).publish(any());
      }
  }
  ```

### Infrastructure Tests (`mekano-infrastructure`)
- **Scope:** Integration tests with QuarkusTest + DevServices PostgreSQL
- **Framework:** `@QuarkusTest`, `@TestTransaction`, AssertJ fluent assertions
- **What they test:** JPA persistence round-trip, repository queries, soft delete, constraint violations
- **File examples:**
  - `mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/repository/UserRepositoryImplTest.java` — 77 lines, 3 tests
- **Typical patterns:**
  ```java
  @QuarkusTest
  class UserRepositoryImplTest {
      @Inject UserRepositoryImpl repository;

      @Test
      @TestTransaction
      void save_devePersistirERetornarUserComEmailCorreto() {
          User user = User.create("João Silva", "joao@fiap.br", "$2a$10$hashbcrypt");
          User salvo = repository.save(user);

          Optional<User> encontrado = repository.findByEmail("joao@fiap.br");
          assertThat(encontrado).isPresent();
          assertThat(encontrado.get().getEmail().getValue()).isEqualTo("joao@fiap.br");
      }
  }
  ```

### REST Tests (`mekano-rest`)
- **Scope:** End-to-end integration tests with QuarkusTest + DevServices PostgreSQL + REST Assured
- **Framework:** `@QuarkusTest`, REST Assured, Hamcrest matchers, `@TestSecurity`
- **What they test:** HTTP endpoints, authentication, error responses, pagination, soft delete, observability
- **File examples:**
  - `mekano-rest/src/test/java/com/fiap/mekano/rest/api/UserResourceTest.java` — 106 lines, 4 tests
  - `mekano-rest/src/test/java/com/fiap/mekano/rest/api/UserSoftDeleteTest.java` — 74 lines, 3 tests
  - `mekano-rest/src/test/java/com/fiap/mekano/rest/api/FaultToleranceTest.java` — 105 lines, 5 tests
  - `mekano-rest/src/test/java/com/fiap/mekano/rest/observability/ObservabilityEndpointsTest.java` — 91 lines, 5 tests
- **Typical patterns:**
  ```java
  @QuarkusTest
  @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
  @TestSecurity(user = "testuser", roles = {"user"})
  class UserResourceTest {
      @Test
      @Order(1)
      void create_validUser_returns201() {
          given()
              .contentType(ContentType.JSON)
              .body("""
                  {"name": "Ana", "email": "ana@fiap.br", "password": "abc123"}
                  """)
              .when()
              .post("/api/v1/users")
              .then()
              .statusCode(201)
              .body("id", notNullValue())
              .body("name", equalTo("Ana"))
              .body("passwordHash", nullValue());  // CRÍTICO: hash never in response
      }

      @Test
      @Order(2)
      void create_duplicateEmail_returns409() {
          // depends on Order(1) having inserted the email
          ...
      }
  }
  ```

## Mocking

**Framework:** Mockito (`mockito-junit-jupiter`) — used exclusively in `mekano-application` tests.

**Patterns:**
```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock UserRepositoryPort userRepository;       // Mock ports (interfaces)
    @Mock PasswordHasher passwordHasher;
    @Mock EventPublisher eventPublisher;
    @InjectMocks UserService useCase;         // Inject mocks into SUT
}
```

**What to Mock:**
- All port interfaces (`UserRepositoryPort`, `PasswordHasher`, `EventPublisher`) — these are boundaries to infrastructure
- Repository methods: `existsByEmail()`, `save()`, `findById()`
- Stub return values with `when().thenReturn()` or `when().thenAnswer()` for complex behavior

**What NOT to Mock:**
- Domain entities (`User`, `Email`) — instantiate real objects
- Value objects — construct directly with valid/invalid input
- Command records — use real `new CreateUserCommand(...)`
- Quarkus CDI container — not started in application tests

**Verify patterns:**
```java
verify(userRepository, times(1)).save(any(User.class));
verify(userRepository, never()).save(any());
verify(eventPublisher, times(1)).publish(any());
```

## Fixtures and Factories

**Test Data:**
- Inline constants in test classes — no external fixture files, no `ObjectMother` pattern
- Shared constants defined as `private static final` at top of test class:
  ```java
  private static final String NOME_VALIDO = "João Silva";
  private static final String EMAIL_VALIDO = "joao@fiap.br";
  private static final String HASH_VALIDO = "bcrypt:$2a$12$hashdummy";
  ```
- Test data created inline in each test method — no setup factory methods
- For REST tests, request body strings use Java text blocks (Java 17 feature) for readability:
  ```java
  .body("""
      {"name": "Delete Me", "email": "softdelete@fiap.br", "password": "abc123"}
      """)
  ```

**Location:**
- All test data lives inside the test method or as class-level constants
- `User.create()` static factory is the test's factory method for domain entities
- No `test/resources/` fixture files used in domain or application
- In infrastructure tests, `User.create()` creates domain entities that get persisted

## Coverage

**Requirements:** None enforced — no JaCoCo, no coverage plugin configured in any `pom.xml`.
**View Coverage:**
```bash
# No coverage tools configured — would need to add jacoco-maven-plugin
```

**Observed Coverage:**
- **Domain module:** High — 8 tests for `User` (all public methods covered), 10 tests for `Email` (validation boundaries, equality, normalization)
- **Application module:** Moderate — 4 tests for `UserService` covering success, duplicate email, invalid email, null name. `AuthenticateUserUseCase` has test file in directory (not read) — coverage unknown
- **Infrastructure module:** Low — 3 tests for `UserRepositoryImpl`. Cache invalidation, fault tolerance annotation behavior, and entity mapper edge cases not tested
- **REST module:** Good — `UserResourceTest` covers create validation + duplication; `UserSoftDeleteTest` covers full lifecycle; `FaultToleranceTest` covers `@Retry`, `@Timeout`, `@CacheResult`; `ObservabilityEndpointsTest` covers health, metrics, OpenAPI

## Database Test Configuration

**Infrastructure Tests** (`mekano-infrastructure/src/test/resources/application.properties`):
```properties
quarkus.datasource.db-kind=postgresql
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mekano
quarkus.datasource.username=mekano
quarkus.datasource.password=mekano
quarkus.flyway.migrate-at-start=true
quarkus.hibernate-orm.schema-management.strategy=validate
%test.quarkus.flyway.clean-at-start=true
%test.quarkus.datasource.devservices.image-name=docker.io/library/postgres:16-alpine
```

**REST Tests** — use `mekano-rest/src/main/resources/application.properties` with `%test` profile overrides:
```properties
%test.quarkus.flyway.clean-at-start=true
%test.quarkus.hibernate-orm.schema-management.strategy=validate
%test.quarkus.datasource.devservices.reuse=true
```

- DevServices PostgreSQL used in test profile (no `jdbc.url` in `%test` → Testcontainers auto-starts PostgreSQL)
- `%test.quarkus.flyway.clean-at-start=true` guarantees idempotent test runs by cleaning schema before each run
- `@TestTransaction` on each integration test method — auto-rollback after test completes

## Test Dependencies by Module

| Module | Test Deps | Container | Purpose |
|--------|-----------|-----------|---------|
| `mekano-domain` | `junit-jupiter` (JUnit 5) | No | Pure unit tests |
| `mekano-application` | `junit-jupiter` + `mockito-junit-jupiter` | No | Mocked Service tests |
| `mekano-infrastructure` | `quarkus-junit5` + `assertj-core` | Yes (DevServices) | Integration tests |
| `mekano-rest` | `quarkus-junit5` + `rest-assured` + `quarkus-test-security` + `assertj-core` | Yes (DevServices) | E2E + API tests |

## Common Patterns

**Async Testing:**
- No async tests present in the codebase
- `@Timeout` fault tolerance is tested synchronously — `FaultToleranceTest.save_comTimeout_persisteComSucesso()` verifies `@Timeout` doesn't break happy path
- No `CompletableFuture`, `@Async`, or reactive streams tests

**Error Testing:**
```java
// Domain — assertThrows for validation
assertThrows(AppException.class, () -> new Email("email-invalido"));

// Application — assertThrows for business rules
assertThrows(AppException.class, () -> useCase.execute(command));
verify(userRepository, never()).save(any());

// REST — statusCode matcher for HTTP errors
.then()
.statusCode(409)
.contentType(containsString("application/problem+json"))
.body("detail", notNullValue())
.body("type", equalTo("about:blank"))
.body("title", equalTo("Conflict"))
.body("status", equalTo(409));
```

**REST Assured Response Chain:**
```java
given()
    .contentType(ContentType.JSON)
    .body(payload)
.when()
    .post("/api/v1/users")
.then()
    .statusCode(201)
    .body("id", notNullValue())
    .body("name", equalTo("Ana"))
    .body("passwordHash", nullValue());
```

**Sequential Test Ordering:**
- `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` + `@Order(n)` for integration tests that share database state (`UserResourceTest`, `UserSoftDeleteTest`)
- Tests depend on prior inserts — `@Order(1)` creates data, `@Order(2)` verifies constraint

**Security Bypass in Tests:**
- `@TestSecurity(user = "testuser", roles = {"user"})` on REST test classes — bypasses JWT authentication without needing actual tokens
- No `JwtTestProfile` usage in any test file (not read by grep — available for auth-testing scenarios)

**Health Endpoint Testing:**
```java
@Test
void health_returnsUp() {
    given()
        .when().get("/q/health")
        .then()
        .statusCode(200)
        .body("status", equalTo("UP"))
        .body("checks.status", hasItem("UP"));
}
```

**Parameterized Testing:**
```java
@ParameterizedTest
@NullAndEmptySource
@DisplayName("deve lançar AppException(400) para null e vazio")
void deveLancarExcecaoParaNullEVazio(String valor) {
    assertThrows(AppException.class, () -> new Email(valor));
}

@ParameterizedTest
@ValueSource(strings = {"semArroba", "@dominio.com", "usuario@", "usuario@dominio"})
@DisplayName("deve lançar AppException(400) para formatos inválidos")
void deveLancarExcecaoParaFormatoInvalido(String emailInvalido) {
    assertThrows(AppException.class, () -> new Email(emailInvalido));
}
```

---

*Testing analysis: 2026-06-20*
