# Codebase Concerns

**Analysis Date:** 2026-06-20

## Tech Debt

### Auth subsystem completely unimplemented

- **Issue:** The CLAUDE.md architecture describes `AuthResource`, `AuthController`, `LoginRequest`, `LoginResponse`, `AuthenticateUserUseCase`, and JWT token generation — but **none** of these classes exist in the codebase. Only user CRUD is implemented.
- **Files:** References expected but missing:
  - `mekano-rest/src/main/java/com/fiap/mekano/rest/api/AuthResource.java`
  - `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/LoginRequest.java`
  - `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/LoginResponse.java`
  - `mekano-application/src/main/java/com/fiap/mekano/application/service/user/AuthenticateUserUseCase.java`
  - `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/AuthenticateUserInputPort.java`
  - `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/AuthenticateUserCommand.java`
- **Impact:** The only existing resource (`UserResource`) uses `@RolesAllowed("user")` but there is no way to obtain a JWT token. The entire authentication flow is broken/unimplemented. The API cannot be used in practice — there is no login endpoint, no token generation, no token refresh.
- **Fix approach:** Implement `AuthResource` with `POST /auth/login` and `POST /auth/refresh`, plus all supporting domain/application/infrastructure classes. The JWT signing infrastructure (`privateKey.pem`, `mp.jwt.*` config) is already set up.

### RefreshToken entities and repositories missing

- **Issue:** Migration `V2__create_refresh_tokens_table.sql` creates the `refresh_tokens` table, and `V5__add_sequential_id.sql` modifies it — but no corresponding JPA entity, repository, service, or mapper exists in Java code.
- **Files:** Missing:
  - `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/RefreshTokenEntity.java`
  - `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/RefreshTokenPanacheRepository.java`
  - `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/RefreshTokenRepositoryImpl.java`
  - `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/service/RefreshTokenService.java`
  - `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/RefreshTokenRepositoryPort.java`
  - `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/RefreshTokenData.java`
- **Impact:** The database has a `refresh_tokens` table (created by Flyway migrations) but no Java code interacts with it. Any attempt to implement auth will need to create all these classes from scratch. Running Flyway against a fresh database will create the table but it will never be used.
- **Fix approach:** Create the full RefreshToken infrastructure stack. The table schema is already defined in the migration files.

### ApplicationLivenessCheck always returns UP

- **Issue:** `ApplicationLivenessCheck.call()` hardcodes `HealthCheckResponse.up("mekano-application")` with zero actual health-checking logic. This is acknowledged in the Javadoc as "purely pedagogical."
- **Files:**
  - `mekano-rest/src/main/java/com/fiap/mekano/rest/observability/ApplicationLivenessCheck.java:29`
- **Impact:** The liveness check will report UP even when the application is malfunctioning (e.g., database connection pool exhausted, deadlocked threads). Kubernetes/container orchestrators relying on liveness probes would not detect degraded states.
- **Fix approach:** Add actual health checks: database connectivity (`PanacheEntityBase.getEntityManager()` ping), available connections, thread state. Or remove the custom check and rely on the auto-registered `DataSourceHealthCheck`.

### CdiEventPublisher uses raw Event\<Object\>

- **Issue:** `CdiEventPublisher` uses `@Inject Event<Object> eventBus` which is type-unsafe. Any CDI bean observing any event type will receive all published events, requiring runtime filtering.
- **Files:**
  - `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/event/CdiEventPublisher.java:16`
- **Impact:** Type safety is lost at compile time. If an observer is registered for the wrong type, the error appears only at runtime. For a small codebase this is acceptable, but it will become a problem as more event types are added.
- **Fix approach:** Use a typed event bus or a dedicated event infrastructure (e.g., Axon, Guava EventBus) rather than raw `Event<Object>`.

### V5 migration references refresh_tokens FK constraint by name that may not exist on clean DB

- **Issue:** `V5__add_sequential_id.sql` line 7 tries to `DROP CONSTRAINT IF EXISTS fk_refresh_tokens_user` before renaming columns. On a clean database that has run all migrations sequentially, V2 creates this constraint. But if V5 is applied against a database where V2 did not execute (e.g., partial migration), this could produce unexpected state.
- **Files:**
  - `mekano-infrastructure/src/main/resources/db/migration/V5__add_sequential_id.sql`
- **Impact:** Low — `IF EXISTS` makes it safe. But the migration is complex and touches both `users` and `refresh_tokens` tables, making it sensitive to migration ordering.

### UserResourceTest and UserSoftDeleteTest share same Quarkus context with separate state

- **Issue:** Both test classes use `@QuarkusTest` and `@TestMethodOrder`, and both rely on `%test.quarkus.flyway.clean-at-start=true`. Since QuarkusTest classes often share the same application context, Flyway clean happens only once at startup. Data created by one test class may leak into the other.
- **Files:**
  - `mekano-rest/src/test/java/com/fiap/mekano/rest/api/UserResourceTest.java`
  - `mekano-rest/src/test/java/com/fiap/mekano/rest/api/UserSoftDeleteTest.java`
- **Impact:** Test flakiness if test classes run in the same JVM. UserResourceTest creates a user with email "ana@fiap.br" and UserSoftDeleteTest creates "softdelete@fiap.br" so collision is unlikely, but the pattern is fragile. Adding more test classes would increase collision risk.
- **Fix approach:** Use `@TestTransaction` for rollback per method, or use unique test data per class, or isolate test classes with separate QuarkusTest instances.

### No `@ParameterizedTest` for password validation in domain layer

- **Issue:** The domain layer (`User.create()`) accepts any non-null password hash. There is no password strength validation anywhere — no minimum length check, no complexity rules. The only validation is `@Size(min=6, max=128)` at the REST DTO level (`CreateUserRequest.java:40`).
- **Files:**
  - `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/CreateUserRequest.java:40`
  - `mekano-domain/src/main/java/com/fiap/mekano/domain/model/User.java`
- **Impact:** Password validation is entirely in the adapter layer. If a different adapter is used (e.g., CLI, batch import), weak passwords can be created. The domain should enforce password strength invariants.
- **Fix approach:** Add a `Password` value object in the domain layer that validates password strength (minimum length, complexity rules). Move the validation from the adapter into the domain.

## Known Bugs

### Email regex rejects valid RFC 5322 addresses

- **Symptoms:** The regex `^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$` rejects valid email addresses such as:
  - `"quoted string"@example.com` (quoted local parts)
  - `user+tag@example.com` (plus addressing — common for filtering)
  - `user@subdomain.example.com` (if subdomain starts with hyphen)
  - Internationalized email addresses (UTF-8 domains)
  - `a@b.cd` would work but `a@b.c` fails (TLD < 2 chars)
- **Files:**
  - `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/Email.java:31-32`
- **Trigger:** Any email address using valid RFC 5322 features outside the simplified pattern will be rejected with a 400 error.
- **Workaround:** Use simple email addresses matching the regex `^alphanumeric@domain.tld`.
- **Priority:** Medium — impacts real-world users whose valid email addresses are rejected.

### Infrastructure module tests require local PostgreSQL (not DevServices)

- **Symptoms:** Running `./mvnw test -pl mekano-infrastructure -am` fails unless a PostgreSQL instance is running on `localhost:5432` with username/password `mekano/mekano`.
- **Files:**
  - `mekano-infrastructure/src/test/resources/application.properties:12-14`
- **Trigger:** The properties file sets `quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mekano` **unscoped** (not under `%test`). This disables DevServices/Testcontainers. The `%test.quarkus.datasource.devservices.image-name` on line 27 is dead code — it has no effect because the JDBC URL is already set.
- **Contradiction:** The CLAUDE.md for infrastructure states: "DevServices PostgreSQL — sem `jdbc.url` no perfil test" — this is incorrect. The jdbc.url IS set, and DevServices is NOT used.
- **Workaround:** Start PostgreSQL locally (e.g., `docker-compose up -d` before running infrastructure tests).
- **Priority:** Medium — breaks CI without Docker/PostgreSQL setup.

### MapStruct `Unmapped target property` warnings suppressed globally

- **Symptoms:** `UserEntityMapper` uses `unmappedTargetPolicy = ReportingPolicy.IGNORE` in the infrastructure CLAUDE.md, and `UserDtoMapper` has no explicit policy. If JPA entities gain new fields without mapper updates, the mismatch is silently ignored at compile time.
- **Files:**
  - `mekano-rest/src/main/java/com/fiap/mekano/rest/api/mapper/UserDtoMapper.java`
  - `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/UserEntityMapper.java`
- **Trigger:** Adding a new column to `UserEntity` without adding the mapping in `UserEntityMapperImpl.toDomain()` or `toEntity()`.
- **Priority:** Low — no current mismatches detected.

### FaultToleranceTest does not assert TimeoutException behavior

- **Symptoms:** `FaultToleranceTest.save_comTimeout_persisteComSucesso()` only asserts that a normal save does NOT throw `TimeoutException`. There is no test that actually triggers a timeout.
- **Files:**
  - `mekano-rest/src/test/java/com/fiap/mekano/rest/api/FaultToleranceTest.java:59-70`
- **Impact:** The `@Timeout(5s)` annotation is untested for the timeout scenario — it's only tested for the happy path.
- **Priority:** Low — triggering a real timeout in a test is difficult (requires hanging DB connection).

## Security Considerations

### CORS wildcard origin in production profile

- **Risk:** `quarkus.http.cors.origins=*` allows any origin to make cross-origin requests. This is acceptable for development but dangerous in production if the API handles authenticated sessions or sensitive data.
- **Files:**
  - `mekano-rest/src/main/resources/application.properties:55`
- **Current mitigation:** None — wildcard applies to all profiles. The D-07 decision record acknowledges this as "global CORS."
- **Recommendations:** Scope CORS to specific origins per profile: `%prod.quarkus.http.cors.origins=https://app.mekano.com.br`. Never allow wildcard in production.

### No rate limiting on user creation endpoint

- **Risk:** The `POST /users` endpoint has no rate limiting. An attacker can create thousands of user accounts in seconds, potentially exhausting database resources or filling the system with spam accounts. The CLAUDE.md references a `LoginRateLimiterFilter` but this file does not exist.
- **Files:**
  - `mekano-rest/src/main/java/com/fiap/mekano/rest/api/UserResource.java:97-126`
- **Current mitigation:** None. Bean Validation only checks field formats.
- **Recommendations:** Implement rate limiting on user creation, similar to the planned login rate limiter. Consider IP-based or email-domain-based throttling.

### No audit logging for user deletion

- **Risk:** The `DELETE /users/{id}` endpoint performs soft delete (sets `isActive=false`, `deletedAt=now()`) but does not log who performed the deletion or from which IP/context. The audit fields `updated_by` and `updated_at` are available in `BaseEntity` but are never populated by `UserService.deleteUser()`.
- **Files:**
  - `mekano-application/src/main/java/com/fiap/mekano/application/service/user/UserService.java:116-118`
  - `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/BaseEntity.java:46-49`
- **Current mitigation:** `deleted_at` and `isActive` are set. But `created_by`/`updated_by` fields are never populated.
- **Recommendations:** Populate `updated_by` with the authenticated user's UUID from JWT claims during soft delete operations. Add a log entry for deletion events.

### Plaintext password travels through CreateUserCommand

- **Risk:** `CreateUserCommand.password` carries the plaintext password from the REST layer through the application layer before BCrypt hashing. While this is a valid architecture choice (PasswordHasher lives in infrastructure), the plaintext string exists in memory and could theoretically appear in logs if toString() is called on the command.
- **Files:**
  - `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/CreateUserCommand.java:15`
- **Current mitigation:** `CreateUserCommand` is a record with no custom `toString()` — the default record toString WILL include the password. Logging the command object at any point would expose the password.
- **Recommendations:** Add a custom `toString()` to `CreateUserCommand` that masks the password field: `password: "***"`. Similar to the `@ToString.Exclude` pattern already used in `User.java`.

## Performance Bottlenecks

### Caffeine cache TTL too short (60s)

- **Problem:** The user cache has an `expire-after-write=60s` with `maximum-size=100`. For frequently accessed users (e.g., authentication lookups), this means the cache expires every minute, forcing repeated database queries.
- **Files:**
  - `mekano-rest/src/main/resources/application.properties:73-75`
- **Cause:** Conservative tuning chosen during initial development.
- **Improvement path:** Increase `expire-after-write` to 5-10 minutes, or make it configurable via environment variable. Monitor cache hit ratios with Micrometer metrics.

### findAll sorts string by splitting manually

- **Problem:** `UserRepositoryImpl.findAll()` parses the sort parameter by splitting on `,` and then uses `if/else` to check allowed fields. This is a manual implementation of what Panache's `Sort.by()` can already do with type safety.
- **Files:**
  - `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/UserRepositoryImpl.java:178-188`
- **Cause:** Implementation predates full Panache API usage.
- **Improvement path:** Use `Page.of()` with `Sort.by()` directly, removing the manual `String[] sortParts` parsing for better readability and maintainability.

### findAll loads all entities via stream before mapping

- **Problem:** `findAll()` calls `.list()` which loads ALL results into memory, then maps via stream. For large datasets, this could cause memory pressure.
- **Files:**
  - `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/UserRepositoryImpl.java:187-188`
- **Cause:** Standard Panache pattern — `.list()` materializes all rows. For the current expected dataset size (<10K users) this is acceptable.
- **Improvement path:** For large datasets, consider using `.stream()` (Hibernate ScrollableResults) or implementing cursor-based pagination.

## Fragile Areas

### HandleConstraintViolation rethrows PersistenceException as RuntimeException

- **Files:**
  - `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/UserRepositoryImpl.java:106-114`
- **Why fragile:** The `catch (PersistenceException e)` in `save()` delegates to `handleConstraintViolation()` which loops through the cause chain looking for `ConstraintViolationException`. If the constraint violation has a different exception wrapping pattern (e.g., nested in a different exception type), the method silently rethrows the raw `PersistenceException` as an unchecked exception, resulting in a 500 Internal Server Error instead of a meaningful 409 Conflict.
- **Safe modification:** Add a fallback case in `handleConstraintViolation` that logs the unrecognized exception and still throws a meaningful domain exception rather than the raw JPA exception.
- **Test coverage:** No test for non-constraint `PersistenceException` scenarios (e.g., deadlock, serialization failure).

### Shared mutable state in BaseEntity fields

- **Files:**
  - `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/BaseEntity.java`
- **Why fragile:** `BaseEntity` fields (`createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `isActive`) use `@Setter` (Lombok) making them mutable. The `@PreUpdate` callback sets `updatedAt` but there is no `@PrePersist` to set `createdAt`. This means `createdAt` must be set manually in the mapper (`UserEntityMapperImpl.toEntity()` line 24). If a new entity type extends `BaseEntity` and the mapper forgets to set `createdAt`, it would be null, violating the database `NOT NULL` constraint.
- **Safe modification:** Add a `@PrePersist` method in `BaseEntity` to auto-set `createdAt` before insert, making it impossible to forget.

### Test ordering dependency across test classes

- **Files:**
  - `mekano-rest/src/test/java/com/fiap/mekano/rest/api/UserResourceTest.java` (uses `@TestMethodOrder`)
  - `mekano-rest/src/test/java/com/fiap/mekano/rest/api/UserSoftDeleteTest.java` (uses `@TestMethodOrder`)
  - `mekano-rest/src/test/java/com/fiap/mekano/rest/api/FaultToleranceTest.java` (NO ordering)
- **Why fragile:** `UserResourceTest` creates a user in `@Order(1)`, tests duplicate email in `@Order(2)`. `UserSoftDeleteTest` creates its own user in `@Order(1)`, deletes in `@Order(2)`, checks 404 in `@Order(3)`. If tests run in different order (suite configuration change, parallel execution), assertions fail. `FaultToleranceTest` does not use ordering but its methods share database state.
- **Safe modification:** Use `@TestTransaction` for each test method (auto-rollback) to eliminate shared state. Remove `@TestMethodOrder` and make each test self-contained.

### BcryptPasswordHasher has zero test coverage

- **Files:**
  - `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/security/BcryptPasswordHasher.java`
- **Why fragile:** The only implementation of `PasswordHasher` is completely untested. Any change to the BCrypt library, Quarkus version, or configuration could silently break password hashing. This is a critical security component with no test safety net.
- **Test coverage:** Zero. No test class for `BcryptPasswordHasher`.
- **Priority:** High.

## Dependencies at Risk

### Lombok-MapStruct binding version 0.2.0

- **Risk:** `lombok-mapstruct-binding` version 0.2.0 is several versions behind the latest (0.3.x). Compatibility with newer Lombok or MapStruct versions is not guaranteed. If the binding breaks, mappers will compile but produce `null` fields at runtime (Gotcha G3).
- **Files:**
  - `pom.xml:60-61`
- **Impact:** A simple `mvn versions:display-dependency-updates` could inadvertently upgrade the binding to an incompatible version, breaking all MapStruct mappers silently. No compile-time error — only runtime NPEs.
- **Migration plan:** Pin this dependency and only upgrade after verifying that all mapper tests pass. Add a CI test step that specifically validates mapper output (non-null fields after mapping).

### Jandex index required for CDI bean discovery

- **Risk:** All non-rest modules (`domain`, `application`, `infrastructure`) depend on `jandex-maven-plugin` for CDI bean discovery. If the Jandex plugin is removed or misconfigured, CDI beans in these modules will not be discovered, causing `UnsatisfiedResolutionException` at runtime (Gotcha G2).
- **Files:**
  - `mekano-application/pom.xml:62-73`
  - `mekano-infrastructure/pom.xml:96-106`
  - `mekano-rest/pom.xml:152-163`
- **Impact:** Catastrophic — entire application fails to start if any index is missing. No implicit fallback from Quarkus.
- **Migration plan:** Keep Jandex plugin in all non-rest modules. Document in POM comments why it is mandatory.

## Test Coverage Gaps

### BcryptPasswordHasher — Untested

- **What's not tested:** The BCrypt password hashing implementation. Both `hash()` and `matches()` methods.
- **Files:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/security/BcryptPasswordHasher.java`
- **Risk:** A Quarkus upgrade could change `BcryptUtil` behavior silently. Passwords could become unverifiable without any test catching it.
- **Priority:** High

### markAsDeleted — No direct test in infrastructure

- **What's not tested:** `UserRepositoryImpl.markAsDeleted()` is only tested indirectly via the REST soft delete test (`UserSoftDeleteTest`). There is no unit/integration test at the repository level.
- **Files:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/UserRepositoryImpl.java:216-224`
- **Risk:** Changes to the soft delete implementation could break without detection if REST tests are not run.
- **Priority:** Medium

### findAll / countAll — No test

- **What's not tested:** The paginated listing (`findAll`) and counting (`countAll`) methods have no test coverage at any layer.
- **Files:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/UserRepositoryImpl.java:177-201`
- **Risk:** Sort field validation logic (`ALLOWED_SORT_FIELDS`), pagination math, and sort direction parsing are untested.
- **Priority:** Medium

### Cache invalidation — No test

- **What's not tested:** The `@CacheInvalidate` annotations on `save()` and `markAsDeleted()` callbacks are not tested. After saving or deleting, the cache should be evicted so subsequent reads get fresh data.
- **Files:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/UserRepositoryImpl.java:94` and `:218`
- **Risk:** Stale data could be served after writes if cache invalidation fails silently.
- **Priority:** Low

### FaultTolerance @Timeout — Only happy path tested

- **What's not tested:** The `@Timeout(5s)` on `save()` only has a positive test (save succeeds within timeout). No test verifies that a real timeout produces a `TimeoutException`.
- **Files:** `mekano-rest/src/test/java/com/fiap/mekano/rest/api/FaultToleranceTest.java:59-70`
- **Risk:** If Quarkus changes the `@Timeout` behavior, or if the annotation stops working, no test will detect it.
- **Priority:** Low

### Event publisher — No integration test

- **What's not tested:** `CdiEventPublisher` is only tested indirectly via `UserServiceTest` which mocks the publisher. There is no test that verifies CDI events are actually fired and received by observers.
- **Files:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/event/CdiEventPublisher.java`
- **Risk:** If CDI event infrastructure changes or the publisher bean is not discovered, domain events fail silently.
- **Priority:** Low

## Architecture Drift

### Domain exception tree is simplified to single AppException

- **Files:**
  - `mekano-domain/src/main/java/com/fiap/mekano/domain/exception/AppException.java`
- **Observation:** The CLAUDE.md describes a rich exception hierarchy (`DomainException`, `BusinessException`, `InvalidEmailException`, `UserAlreadyExistsException`, `UserNotFoundException`, etc.) but all of these are unused. The entire codebase uses only `AppException` with HTTP status codes directly in the domain layer.
- **Impact:** The domain depends on HTTP semantics (status codes 400, 404, 409). This is an adapter concern leaking into the domain. Future Services in non-HTTP contexts (message queues, CLI) would inherit HTTP status codes that don't apply.
- **Recommendation:** Either remove the unused exception classes or migrate to use them. The domain should throw `UserNotFoundException` (not `AppException(404, ...)`) and the `ApiExceptionMapper` should map domain exceptions to HTTP status codes.

---

*Concerns audit: 2026-06-20*
