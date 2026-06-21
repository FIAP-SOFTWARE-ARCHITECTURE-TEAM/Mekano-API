# Coding Conventions

**Analysis Date:** 2026-06-20

## Naming Patterns

**Files:**
- PascalCase for Java class files — matches the public class name exactly: `User.java`, `EmailTest.java`, `ApiExceptionMapper.java`
- Hyphenated lowercase for SQL migration files: `V1__create_users_table.sql`
- All lowercase with hyphens for properties files: `application.properties`, `messages.properties`
- Directory names are singular and lowercase: `domain/model/`, `domain/valueobject/`, `rest/api/dto/`, `infrastructure/repository/`, `domain/port/in/`

**Classes:**
- PascalCase, descriptive noun or noun phrase: `UserService`, `UserRepositoryImpl`, `UserPanacheRepository`, `UserDtoMapper`, `ProblemDetail`
- Interface names describe capabilities: `UserServicePort`, `UserRepositoryPort`, `PasswordHasher`, `EventPublisher`
- Abstract classes are prefixed with `Abstract` when applicable; `BaseEntity` used as base class
- Exceptions describe the error: `AppException`, `DomainException`, `BusinessException`
- Test classes end in `Test` (not `Spec`): `UserTest`, `EmailTest`, `UserServiceTest`
- Mapper interfaces follow `{Source}To{Target}Mapper` or `{Entity}Mapper` pattern: `UserDtoMapper`, `UserEntityMapper`, `EmailMapper`

**Methods:**
- `camelCase` for all methods
- Service methods named `execute()` and `executeResponse()`: `UserService.execute(CreateUserCommand)`
- Factory methods named `create()` and `reconstitute()`: `User.create()`, `User.reconstitute()` — clear distinction between "new" and "restored from persistence"
- Repository methods follow CRUD conventions: `save()`, `findById()`, `findByEmail()`, `existsByEmail()`, `findAll()`, `countAll()`, `markAsDeleted()`
- Domain event factory method named `of()`: `UserCreatedEvent.of(User)`
- Exception mapper named `toResponse()` implementing `ExceptionMapper.toResponse()`
- Port interface methods are imperative verbs: `execute()`, `findUserById()`, `deleteUser()`
- Test methods use Portuguese naming convention with `deve` (should) prefix: `deveCriarUserComCamposPopulados()`, `deveLancarExcecaoParaEmailInvalido()`
- Test method names in `FaultToleranceTest` mix Portuguese and English: `findById_comRetry_optionalEmpty()`, `save_comTimeout_persisteComSucesso()`

**Variables:**
- `camelCase` for local variables
- Constants in `UPPER_SNAKE_CASE`: `NOME_VALIDO`, `EMAIL_VALIDO`, `HASH_VALIDO`, `EXAMPLE_PASSWORD`
- `static final` Pattern constants compiled once: `EMAIL_PATTERN`, `ALLOWED_SORT_FIELDS`
- `var` for local type inference is used extensively in concise method bodies: `var command = ...`, `var user = ...`, `var response = ...`

**Types:**
- PascalCase for all types (classes, interfaces, records, enums)
- Records used for DTOs and commands when all fields are data carriers: `CreateUserCommand`, `UserResponse`, `ProblemDetail`, `UserPageResponse`
- Records used for domain events: `UserCreatedEvent`

## Code Style

**Formatting:**
- No ESLint/Prettier/Checkstyle config files detected — project relies on IDE defaults and Maven compiler conventions
- No explicit formatting tool configured in `pom.xml` — no `spotless-maven-plugin`, `checkstyle`, or `formatter-maven-plugin`
- Braces follow standard Java convention: opening brace on same line
- 4-space indentation (Java standard)
- Blank lines separate logical sections within methods
- `@Override` annotation used on all interface implementation methods

**Linting:**
- No static analysis tools configured (`checkstyle`, `spotbugs`, `pmd`, `errorprone` are absent from all `pom.xml` files)
- No `.editorconfig` file detected at project root
- Compiler warnings are not enforced as errors
- The project relies on Maven compiler's `-parameters` flag for method parameter name retention (`<parameters>true</parameters>` in all `pom.xml`)

## Import Organization

**Order:**
1. Package declaration
2. Blank line
3. Local project imports (own classes)
4. Third-party imports (Lombok, MapStruct, Quarkus/Jakarta, JUnit/Mockito)
5. Java standard library imports
6. Static imports

**Examples from `UserService.java`:**
```java
package com.fiap.mekano.application.usecase.user;

import com.fiap.mekano.domain.event.UserCreatedEvent;          // 3. Local project
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.in.CreateUserCommand;
import com.fiap.mekano.domain.port.in.UserServicePort;
import com.fiap.mekano.domain.port.in.PasswordHasher;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;            // 4. Third-party (Jakarta)
import jakarta.transaction.Transactional;

import java.util.UUID;                                          // 6. Java stdlib
```

**Path Aliases:**
- No path aliases or module path shortcuts configured — all imports use fully qualified package names
- Maven module dependencies use `${project.version}` for inter-module references

## Error Handling

**Strategy:** Layers-based exception propagation with a single exception mapper.

**Exception Hierarchy** (`mekano-domain/src/main/java/com/fiap/mekano/domain/exception/`):

- `AppException extends RuntimeException` — unified application exception carrying `int status` and `String message`. Used across all layers (domain, application, infrastructure). Avoids framework imports in domain layer.
- Domain-layer exceptions file (`AppException.java`, `Messages.java`) plus `messages.properties` for i18n-friendly messages.

**Patterns:**

- **Domain layer** (`User.java:51-59`, `Email.java:42-51`): throws `AppException(400, ...)` for validation failures. Value objects validate in constructor.
  ```java
  public Email(String value) {
      if (value == null || value.isBlank()) {
          throw new AppException(400, Messages.get("email.invalid.format", ...));
      }
  }
  ```

- **Application layer** (`UserService.java:51-75`): validates business rules, throws `AppException(409, ...)` for duplicates, `AppException(400, ...)` for validation. Errors propagate upward — no try-catch in Services.

- **Infrastructure layer** (`UserRepositoryImpl.java:106-115`): catches `PersistenceException`, unwraps cause chain to find `ConstraintViolationException`, rethrows as `AppException(409)`. If no constraint violation found, the original exception propagates unmodified.

- **REST layer** (`ApiExceptionMapper.java:36-44`): single `@Provider @ApplicationScoped` ExceptionMapper. Dispatches via `instanceof`:
  - `AppException` → status code from `ex.getStatus()`
  - Fallback → 500 with error log
  - Response format: RFC 7807 Problem Details (`application/problem+json`)

- **Where NOT to catch:** Resources (`UserResource.java`) never catch exceptions — they propagate to `ApiExceptionMapper`. Repositories only catch `PersistenceException` for constraint mapping.

## Logging

**Framework:** `io.quarkus.logging.Log` (Quarkus built-in logging facade).

**Configuration** (`mekano-rest/src/main/resources/application.properties:65-70`):
```properties
quarkus.log.console.json=true
quarkus.log.console.json.pretty-print=false
%dev.quarkus.log.level=DEBUG
%prod.quarkus.log.level=INFO
%test.quarkus.log.level=WARN
```

**Patterns:**
- `Log.errorf(exception, "Unhandled exception: %s", exception.getMessage())` — structured error logging with exception parameter
- No `@Slf4j` or manual logger declarations — Quarkus Log is used statically
- Logging is minimal in domain layer (zero logging) and application layer (zero logging)
- Only `ApiExceptionMapper` and configuration files reference logging

## Comments

**When to Comment:**
- Every public class has a Javadoc block explaining purpose, invariants, and key design decisions
- Factory methods have detailed Javadoc explaining the difference from other factory methods (e.g., `User.create()` vs `User.reconstitute()`)
- Complex business rules are documented inline (`User.java:13-23` — explains factory method intent and builder privacy)
- Security-sensitive fields annotated with rationale for exclusion (`@ToString.Exclude` on `passwordHash`)
- Suppressed warnings are commented with the reason and decision record link

**Javadoc/TSDoc:**
- Full Javadoc (`/** ... */`) on all classes and public methods
- `@param`, `@return`, `@throws` tags used consistently
- `@see` references for related classes
- Code examples in `<pre>{@code ... }</pre>` blocks for usage patterns
- Javadoc written in Portuguese for domain concepts, English for technical documentation
- Javadoc references decision records with `D-XX` identifiers (e.g., `// D-04`)

## Module Design

**Exports:**
- Each module `pom.xml` exports everything (no `.m2e` filter or OSGi exports)
- Domain module (`mekano-domain`) has zero framework dependencies — only Java SE + Lombok (provided)
- Application module (`mekano-application`) depends on domain + `quarkus-arc` + `quarkus-elytron-security-common`
- Infrastructure module (`mekano-infrastructure`) depends on domain + Quarkus extensions (Panache, Flyway, Cache, FT, JWT)
- REST module (`mekano-rest`) depends on all three internal modules

**Barrel Files:**
- No barrel/index files — each class is imported individually
- Package structure is the sole organization mechanism

**Package-by-Layer:**
- Each module uses `com.fiap.mekano.{layer}.{subdomain}` structure
- Layer packages: `domain`, `application`, `infrastructure`, `rest`
- Subdomain packages: `model`, `valueobject`, `port/in`, `port/out`, `exception`, `event`, `usecase`, `entity`, `repository`, `mapper`, `service`, `security`

## Function Design

**Size:**
- Service `execute()` methods are typically 15-25 lines with numbered steps in comments
- Repository methods are 5-15 lines
- Factory methods are 5-10 lines
- Resources methods are 10-30 lines including OpenAPI annotations and JavaDoc

**Parameters:**
- Service methods accept a single `Command` record (Command pattern): `execute(CreateUserCommand command)`
- Factory methods accept primitive/String parameters: `create(String name, String emailValue, String passwordHash)`
- REST endpoints accept `@Valid` DTO class + `@Context UriInfo`
- Pagination uses three `@QueryParam` parameters: `page`, `size`, `sort`
- Methods with more than 3 primitive parameters are rare — only `User.reconstitute(UUID, String, String, String, LocalDateTime)`

**Return Values:**
- Domain entity methods return the entity or a typed value
- Repository methods return `Optional<T>` when the result may not exist
- Port interface methods return domain objects (never DTOs or entities from other layers)
- Resource methods return `Response` (JAX-RS)
- Services return domain objects or response records

## Value Object Conventions

- Declared as `final class` with `@EqualsAndHashCode` (value-based equality)
- Fields are `private final` — immutable by design
- Validation occurs in constructor, throwing `AppException(400)` on invalid input
- Normalization applied during construction (e.g., `Locale.ROOT` lowercase for email)
- Pattern constants are `private static final Pattern` (thread-safe, compiled once)
- Factory method `of()` for domain events, `create()` for entities

## Record Usage

- Input commands: `CreateUserCommand` (in domain/port/in) — pure data carrier, no validation annotations
- Output DTOs: `UserResponse`, `UserPageResponse`, `ProblemDetail`, `CreateUserResponse` — immutable response data
- Domain events: `UserCreatedEvent` — immutable event data with static factory `of()`
- All records have zero business logic — just data + static factory methods

## MapStruct Conventions

- `@Mapper(componentModel = "cdi")` — NEVER `"spring"` (critical gotcha G9)
- Interface-based mappers with `default` methods for complex logic
- `@Mapping(target = "email", expression = "java(user.getEmail().getValue())")` for VO-to-String conversion
- Reusable mapping helpers via `@Mapper(uses = {EmailMapper.class})`
- `EmailMapper` is `@ApplicationScoped @Named("emailToString"/"stringToEmail")`

---

*Convention analysis: 2026-06-20*
