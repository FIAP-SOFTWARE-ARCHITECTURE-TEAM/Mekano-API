# Architecture Patterns: Clean Architecture in Quarkus 3.x + Java 17

**Domain:** Multi-module Maven REST API — Clean Architecture / Hexagonal Architecture
**Researched:** 2025-07-15
**Overall Confidence:** HIGH — Clean Architecture is a well-established pattern (Robert C. Martin, 2012); Quarkus CDI/JAX-RS patterns are stable across 3.x versions.

---

## Recommended Architecture

```
mekano/ (root, packaging=pom)
├── mekano-domain/          (jar — pure domain, zero framework deps)
├── mekano-application/     (jar — use cases, depends on domain)
├── mekano-infrastructure/  (jar — JPA/Flyway/MapStruct, depends on domain)
└── mekano-adapter/         (quarkus — REST/DTOs/ExceptionMappers, depends on domain+application)
```

**Dependency rule (strictly enforced in pom.xml):**
```
adapter  → domain, application   (NO direct infra import)
infra    → domain                 (NO application import)
app      → domain                 (NO infra, NO adapter)
domain   → (nothing)
```

The infrastructure implementation is injected at runtime via CDI — the adapter only knows about port interfaces defined in domain.

---

## Module Breakdown

### 1. `mekano-domain` — The Protected Core

**packaging:** `jar`
**dependencies:** `lombok` (optional, compile-only)
**No Quarkus, No JPA, No Spring, No Jackson annotations**

**Package structure:**
```
com.fiap.domain/
├── model/                   # Domain entities (pure POJOs)
│   └── User.java
├── valueobject/             # Immutable value objects
│   ├── UserId.java
│   └── Email.java
├── port/
│   ├── in/                  # Input ports = use case contracts
│   │   ├── CreateUserInputPort.java
│   │   ├── FindUserInputPort.java
│   │   └── DeleteUserInputPort.java
│   └── out/                 # Output ports = repository/external service contracts
│       └── UserRepositoryPort.java
└── exception/               # Domain exceptions only
    ├── DomainException.java              (abstract base)
    ├── UserNotFoundException.java
    ├── UserAlreadyExistsException.java
    └── InvalidEmailException.java
```

**Domain entity — zero annotations:**
```java
// com.fiap.domain.model.User
public class User {
    private final UserId id;
    private String name;
    private Email email;
    private LocalDateTime createdAt;

    // constructor, getters, domain methods (no setters for invariants)
    public static User newUser(String name, String email) {
        return new User(null, name, new Email(email), LocalDateTime.now());
    }
}
```

**Value object — enforces invariants:**
```java
// com.fiap.domain.valueobject.Email
public final class Email {
    private final String value;

    public Email(String value) {
        if (value == null || !value.matches("^[\\w.-]+@[\\w.-]+\\.[a-z]{2,}$")) {
            throw new InvalidEmailException("Invalid email: " + value);
        }
        this.value = value.toLowerCase();
    }

    public String value() { return value; }
}
```

**Input port (use case interface):**
```java
// com.fiap.domain.port.in.CreateUserInputPort
public interface CreateUserInputPort {
    User execute(CreateUserCommand command);
}
```

**Output port (repository interface):**
```java
// com.fiap.domain.port.out.UserRepositoryPort
public interface UserRepositoryPort {
    User save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(String email);
    List<User> findAll();
    void delete(UserId id);
    boolean existsByEmail(String email);
}
```

---

### 2. `mekano-application` — Use Case Orchestration

**packaging:** `jar`
**dependencies:** `domain`, `quarkus-arc` (for `@ApplicationScoped`)

**Package structure:**
```
com.fiap.application/
└── usecase/
    └── user/
        ├── CreateUserUseCase.java      # implements CreateUserInputPort
        ├── CreateUserCommand.java      # input data carrier (record)
        ├── FindUserUseCase.java        # implements FindUserInputPort
        └── DeleteUserUseCase.java      # implements DeleteUserInputPort
```

**Command object — simple record:**
```java
// com.fiap.application.usecase.user.CreateUserCommand
public record CreateUserCommand(
    String name,
    String email
) {}
```

**Use case implementation:**
```java
// com.fiap.application.usecase.user.CreateUserUseCase
@ApplicationScoped
public class CreateUserUseCase implements CreateUserInputPort {

    private final UserRepositoryPort userRepository;

    @Inject
    public CreateUserUseCase(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public User execute(CreateUserCommand command) {
        if (userRepository.existsByEmail(command.email())) {
            throw new UserAlreadyExistsException(
                "Email already registered: " + command.email()
            );
        }
        User user = User.newUser(command.name(), command.email());
        return userRepository.save(user);
    }
}
```

> **Key rule:** Use cases ONLY depend on port interfaces (domain). They never import `UserEntity`, `UserRepositoryImpl`, or any JAX-RS/HTTP class.

---

### 3. `mekano-infrastructure` — Technical Adapters (Driven Side)

**packaging:** `jar`
**dependencies:** `domain`, `quarkus-hibernate-orm-panache`, `quarkus-flyway`, `quarkus-jdbc-postgresql`, `mapstruct`, `lombok`

**Package structure:**
```
com.fiap.infrastructure/
├── entity/               # JPA entities (Panache) — infra-only, never cross boundary
│   └── UserEntity.java
├── repository/           # Port implementations
│   └── UserRepositoryImpl.java
├── mapper/               # MapStruct Entity↔Domain
│   └── UserEntityMapper.java
└── config/               # Technical config beans (if any)
    └── DatabaseConfig.java
```

**JPA entity with Panache (only in infrastructure):**
```java
// com.fiap.infrastructure.entity.UserEntity
@Entity
@Table(name = "users")
public class UserEntity extends PanacheEntity {

    @Column(nullable = false, length = 100)
    public String name;

    @Column(nullable = false, unique = true, length = 254)
    public String email;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    // Panache active record: static finders
    public static Optional<UserEntity> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public static boolean existsByEmail(String email) {
        return count("email", email) > 0;
    }
}
```

> **Why `PanacheEntity` over `PanacheEntityBase<Long>`?** `PanacheEntity` provides auto-generated `Long id` — sufficient for most use cases. Use `PanacheEntityBase<T>` only when you need a custom ID type or UUID.

**MapStruct mapper — Entity↔Domain (infrastructure layer):**
```java
// com.fiap.infrastructure.mapper.UserEntityMapper
@Mapper(
    componentModel = "cdi",          // Quarkus CDI injection
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserEntityMapper {

    @Mapping(target = "id", expression = "java(com.fiap.domain.valueobject.UserId.of(entity.id))")
    @Mapping(target = "email", expression = "java(new com.fiap.domain.valueobject.Email(entity.email))")
    User toDomain(UserEntity entity);

    @Mapping(target = "id", expression = "java(domain.getId() != null ? domain.getId().value() : null)")
    @Mapping(target = "email", expression = "java(domain.getEmail().value())")
    UserEntity toEntity(User domain);
}
```

> **MapStruct + Quarkus CDI:** Use `componentModel = "cdi"` (not `"spring"`). Quarkus generates an `@ApplicationScoped` implementation at build time. The mapper is injectable via `@Inject`.

**Repository implementation — implements domain port:**
```java
// com.fiap.infrastructure.repository.UserRepositoryImpl
@ApplicationScoped
public class UserRepositoryImpl implements UserRepositoryPort {

    @Inject
    UserEntityMapper mapper;

    @Override
    @Transactional
    public User save(User user) {
        UserEntity entity = mapper.toEntity(user);
        entity.persist();
        return mapper.toDomain(entity);
    }

    @Override
    public Optional<User> findById(UserId id) {
        return UserEntity.<UserEntity>findByIdOptional(id.value())
                         .map(mapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return UserEntity.findByEmail(email)
                         .map(mapper::toDomain);
    }

    @Override
    public List<User> findAll() {
        return UserEntity.<UserEntity>listAll()
                         .stream()
                         .map(mapper::toDomain)
                         .toList();
    }

    @Override
    @Transactional
    public void delete(UserId id) {
        UserEntity.findByIdOptional(id.value())
                  .ifPresentOrElse(
                      UserEntity::delete,
                      () -> { throw new UserNotFoundException("User not found: " + id.value()); }
                  );
    }

    @Override
    public boolean existsByEmail(String email) {
        return UserEntity.existsByEmail(email);
    }
}
```

> **`@Transactional` placement:** Belongs on the repository implementation (infrastructure), NOT on the use case. Domain and application layers should be transaction-agnostic.

**Flyway migration location:**
```
infrastructure/src/main/resources/db/migration/
├── V1__create_users_table.sql
└── V2__add_users_indexes.sql
```

```sql
-- V1__create_users_table.sql
CREATE TABLE users (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100)  NOT NULL,
    email      VARCHAR(254)  NOT NULL UNIQUE,
    created_at TIMESTAMP     NOT NULL DEFAULT NOW()
);
```

---

### 4. `mekano-adapter` — REST Adapters (Driving Side)

**packaging:** `quarkus` ← ONLY this module has `<packaging>quarkus</packaging>`
**dependencies:** `domain`, `application`, `infrastructure`, `quarkus-rest-jackson`, `quarkus-hibernate-validator`, `quarkus-smallrye-openapi`, `mapstruct`, `quarkus-smallrye-health`, `quarkus-smallrye-jwt`

> **Note:** `infrastructure` is listed as a dependency of `adapter` to ensure it's on the classpath for CDI discovery. The adapter code itself does NOT import infrastructure classes directly.

**Package structure:**
```
com.fiap.adapter/
├── rest/                    # JAX-RS resources
│   └── UserResource.java
├── dto/
│   ├── request/             # Inbound DTOs with validation
│   │   └── CreateUserRequest.java
│   └── response/            # Outbound DTOs / projections
│       └── UserResponse.java
├── mapper/                  # MapStruct Domain↔DTO
│   └── UserDtoMapper.java
└── exception/               # ExceptionMapper providers
    ├── DomainExceptionMapper.java
    ├── UserNotFoundExceptionMapper.java
    ├── ConstraintViolationExceptionMapper.java
    └── ErrorResponse.java
```

**Request DTO — validation annotations only on the boundary:**
```java
// com.fiap.adapter.dto.request.CreateUserRequest
public record CreateUserRequest(

    @NotBlank(message = "Name must not be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    String name,

    @NotBlank(message = "Email must not be blank")
    @Email(message = "Must be a valid email address")
    @Size(max = 254, message = "Email must not exceed 254 characters")
    String email

) {}
```

**Response DTO — projection, hides internals:**
```java
// com.fiap.adapter.dto.response.UserResponse
public record UserResponse(
    Long id,
    String name,
    String email,
    LocalDateTime createdAt
) {}
```

**MapStruct mapper — Domain↔DTO (adapter layer):**
```java
// com.fiap.adapter.mapper.UserDtoMapper
@Mapper(
    componentModel = "cdi",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface UserDtoMapper {

    @Mapping(target = "id", expression = "java(user.getId() != null ? user.getId().value() : null)")
    @Mapping(target = "email", expression = "java(user.getEmail().value())")
    UserResponse toResponse(User user);

    // No toDomain here — adapter receives a CreateUserCommand, not a User
}
```

**JAX-RS Resource — thin, delegates to use case via port:**
```java
// com.fiap.adapter.rest.UserResource
@Path("/users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Users", description = "User management operations")
public class UserResource {

    @Inject
    CreateUserInputPort createUserUseCase;   // ← injected by type (interface)

    @Inject
    FindUserInputPort findUserUseCase;

    @Inject
    UserDtoMapper mapper;

    @POST
    @Operation(summary = "Create a new user")
    @APIResponse(responseCode = "201", description = "User created")
    @APIResponse(responseCode = "400", description = "Validation error")
    @APIResponse(responseCode = "409", description = "Email already exists")
    public Response create(@Valid @NotNull CreateUserRequest request) {
        CreateUserCommand command = new CreateUserCommand(request.name(), request.email());
        User created = createUserUseCase.execute(command);
        return Response.status(Response.Status.CREATED)
                       .entity(mapper.toResponse(created))
                       .build();
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "Find user by ID")
    public Response findById(@PathParam("id") Long id) {
        User user = findUserUseCase.findById(UserId.of(id));
        return Response.ok(mapper.toResponse(user)).build();
    }
}
```

---

## Exception Hierarchy

```
java.lang.RuntimeException
├── DomainException (abstract — in domain module)
│   ├── UserNotFoundException           → HTTP 404
│   ├── UserAlreadyExistsException      → HTTP 409
│   └── InvalidEmailException           → HTTP 400
│
└── InfrastructureException (in infrastructure module — optional)
    └── DatabaseException               → HTTP 500 (via generic mapper)
```

**Domain exception base:**
```java
// com.fiap.domain.exception.DomainException
public abstract class DomainException extends RuntimeException {
    protected DomainException(String message) {
        super(message);
    }
    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
```

**Concrete domain exception:**
```java
// com.fiap.domain.exception.UserNotFoundException
public class UserNotFoundException extends DomainException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
```

**Error response DTO (in adapter):**
```java
// com.fiap.adapter.exception.ErrorResponse
public record ErrorResponse(
    String message,
    String code,
    LocalDateTime timestamp
) {
    public static ErrorResponse of(String message, String code) {
        return new ErrorResponse(message, code, LocalDateTime.now());
    }
}
```

---

## ExceptionMapper Patterns in Quarkus JAX-RS

**Per-exception mapper (preferred — most specific):**
```java
// com.fiap.adapter.exception.UserNotFoundExceptionMapper
@Provider
public class UserNotFoundExceptionMapper
        implements ExceptionMapper<UserNotFoundException> {

    @Override
    public Response toResponse(UserNotFoundException exception) {
        return Response
            .status(Response.Status.NOT_FOUND)
            .entity(ErrorResponse.of(exception.getMessage(), "USER_NOT_FOUND"))
            .build();
    }
}
```

**Conflict mapper (409):**
```java
@Provider
public class UserAlreadyExistsExceptionMapper
        implements ExceptionMapper<UserAlreadyExistsException> {

    @Override
    public Response toResponse(UserAlreadyExistsException exception) {
        return Response
            .status(Response.Status.CONFLICT)
            .entity(ErrorResponse.of(exception.getMessage(), "USER_ALREADY_EXISTS"))
            .build();
    }
}
```

**Bean Validation constraint violations (400):**
```java
// com.fiap.adapter.exception.ConstraintViolationExceptionMapper
@Provider
public class ConstraintViolationExceptionMapper
        implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        List<String> messages = exception.getConstraintViolations()
            .stream()
            .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
            .sorted()
            .toList();
        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(new ValidationErrorResponse(messages, LocalDateTime.now()))
            .build();
    }
}
```

**Generic fallback mapper (catches unhandled exceptions):**
```java
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        LOG.error("Unhandled exception", exception);
        return Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(ErrorResponse.of("Internal server error", "INTERNAL_ERROR"))
            .build();
    }
}
```

> **Quarkus specifics:** `@Provider` is sufficient — `@ApplicationScoped` is optional but harmless. Quarkus discovers `ExceptionMapper` implementations at build time via CDI. No XML registration needed. More specific exception types take priority over base types.

---

## Maven Multi-Module `pom.xml` Structure

**Root pom (parent):**
```xml
<groupId>com.fiap</groupId>
<artifactId>mekano</artifactId>
<version>1.0.0-SNAPSHOT</version>
<packaging>pom</packaging>              <!-- CHANGED from "quarkus" to "pom" -->

<modules>
    <module>mekano-domain</module>
    <module>mekano-application</module>
    <module>mekano-infrastructure</module>
    <module>mekano-adapter</module>
</modules>

<dependencyManagement>
    <!-- quarkus-bom here for version alignment across all modules -->
    <!-- mapstruct version here -->
    <!-- lombok version here -->
</dependencyManagement>
```

**domain pom — truly zero Quarkus deps:**
```xml
<artifactId>mekano-domain</artifactId>
<packaging>jar</packaging>
<dependencies>
    <!-- lombok: optional for boilerplate reduction -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

**application pom:**
```xml
<artifactId>mekano-application</artifactId>
<packaging>jar</packaging>
<dependencies>
    <dependency>
        <groupId>com.fiap</groupId>
        <artifactId>mekano-domain</artifactId>
    </dependency>
    <!-- CDI annotations for @ApplicationScoped -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-arc</artifactId>
    </dependency>
</dependencies>
```

**infrastructure pom:**
```xml
<artifactId>mekano-infrastructure</artifactId>
<packaging>jar</packaging>
<dependencies>
    <dependency>
        <groupId>com.fiap</groupId>
        <artifactId>mekano-domain</artifactId>
    </dependency>
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
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
    </dependency>
</dependencies>
```

**adapter pom — the only `<packaging>quarkus</packaging>` module:**
```xml
<artifactId>mekano-adapter</artifactId>
<packaging>quarkus</packaging>           <!-- Quarkus runner module -->
<dependencies>
    <dependency>
        <groupId>com.fiap</groupId>
        <artifactId>mekano-domain</artifactId>
    </dependency>
    <dependency>
        <groupId>com.fiap</groupId>
        <artifactId>mekano-application</artifactId>
    </dependency>
    <dependency>
        <!-- Runtime classpath — CDI discovers UserRepositoryImpl -->
        <groupId>com.fiap</groupId>
        <artifactId>mekano-infrastructure</artifactId>
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
        <artifactId>quarkus-smallrye-health</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-smallrye-jwt</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-micrometer</artifactId>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-smallrye-fault-tolerance</artifactId>
    </dependency>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
    </dependency>
</dependencies>
<build>
    <plugins>
        <!-- Quarkus maven plugin ONLY here -->
        <plugin>
            <groupId>io.quarkus.platform</groupId>
            <artifactId>quarkus-maven-plugin</artifactId>
        </plugin>
        <!-- MapStruct annotation processor -->
        <plugin>
            <artifactId>maven-compiler-plugin</artifactId>
            <configuration>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

> **MapStruct + Lombok annotation processor order matters:** `lombok` must come BEFORE `mapstruct-processor` in the annotation processor path, and `lombok-mapstruct-binding` must be present. Without this, MapStruct can't see Lombok-generated getters/setters. Confidence: HIGH (documented Lombok+MapStruct binding requirement).

---

## Data Flow: End-to-End `POST /users`

```
HTTP Request
    │
    ▼
[adapter] UserResource.create(@Valid CreateUserRequest)
    │  1. Bean validation fires (@Valid) — 400 if invalid
    │  2. Map request → CreateUserCommand
    │
    ▼
[application] CreateUserUseCase.execute(command)
    │  3. Check duplicate email via UserRepositoryPort
    │  4. Create User domain entity
    │  5. Persist via UserRepositoryPort.save()
    │
    ▼
[infrastructure] UserRepositoryImpl.save(user)
    │  6. Map User → UserEntity (MapStruct)
    │  7. entity.persist() — Panache/Hibernate
    │  8. Map UserEntity → User (MapStruct)
    │
    ▼
[application] returns User domain object
    │
    ▼
[adapter] Map User → UserResponse (MapStruct)
    │
    ▼
HTTP 201 Created + UserResponse JSON
```

**Exception path:**
```
UserAlreadyExistsException thrown in application layer
    │
    ▼
Bubbles up through adapter
    │
    ▼
[adapter] UserAlreadyExistsExceptionMapper.toResponse()
    │
    ▼
HTTP 409 Conflict + ErrorResponse JSON
```

---

## Component Boundaries

| Component | Responsibility | Can Import | Cannot Import |
|-----------|---------------|------------|---------------|
| `domain` | Entities, VOs, Port interfaces, Domain exceptions | Java stdlib, Lombok | Anything framework-specific |
| `application` | Use case orchestration | `domain`, CDI annotations | JPA, JAX-RS, HTTP, `adapter`, `infrastructure` |
| `infrastructure` | DB persistence, external services | `domain`, JPA/Panache, MapStruct | `application`, `adapter` |
| `adapter` | HTTP in/out, DTOs, validation, error mapping | `domain`, `application` (ports), MapStruct | Infrastructure classes directly |

---

## Anti-Patterns to Avoid

### Anti-Pattern 1: JPA Annotations on Domain Entities
**What:** `@Entity`, `@Column`, `@OneToMany` on `User.java` in domain module
**Why bad:** Forces domain layer to depend on JPA; breaks testability; violates Clean Architecture
**Instead:** Keep a separate `UserEntity.java` in infrastructure; use MapStruct to convert

### Anti-Pattern 2: Use Case Depending on Infrastructure
**What:** `CreateUserUseCase` imports `UserRepositoryImpl` directly
**Why bad:** Breaks dependency inversion; impossible to test without a database
**Instead:** Always depend on the interface `UserRepositoryPort` (domain port)

### Anti-Pattern 3: `@Transactional` on Use Cases
**What:** `@Transactional` in application layer use case methods
**Why bad:** Leaks infrastructure concern (transaction management) into application layer
**Instead:** `@Transactional` belongs on `UserRepositoryImpl` methods in infrastructure

### Anti-Pattern 4: HTTP Concerns in Domain Exceptions
**What:** Domain exceptions that reference `Response.Status` or `@ResponseStatus`
**Why bad:** Domain becomes aware of transport protocol (HTTP); ties domain to web layer
**Instead:** ExceptionMappers in adapter layer translate domain exceptions to HTTP status codes

### Anti-Pattern 5: Fat DTOs Crossing All Layers
**What:** Using `UserResponse` DTO all the way through application and domain layers
**Why bad:** Couples layers tightly; changes to API response shape ripple inward
**Instead:** Use domain entities internally; map to DTOs at the adapter boundary only

### Anti-Pattern 6: MapStruct `componentModel = "spring"` in Quarkus
**What:** `@Mapper(componentModel = "spring")` in Quarkus project
**Why bad:** Generates Spring-specific injection code; CDI container won't find it
**Instead:** Always use `componentModel = "cdi"` in Quarkus projects

### Anti-Pattern 7: Direct CDI Bean Discovery Failure in Multi-Module
**What:** Infrastructure module beans not found by CDI at runtime
**Why bad:** `UserRepositoryPort` has no implementation; NullPointerException on injection
**Root cause:** Non-quarkus modules need either a `beans.xml` or Quarkus CDI detection by package scan
**Instead:** Quarkus scans all modules on the classpath. Ensure infrastructure is a compile/runtime dependency of the adapter module.

---

## Scalability Considerations

| Concern | Approach |
|---------|----------|
| N+1 queries | Use Panache `find("FROM UserEntity u JOIN FETCH u.roles")` or `@NamedEntityGraph` |
| Large lists | Add pagination: `Page`, `PanacheQuery.page(Page.of(page, size))` |
| Reactive upgrade path | Application layer use cases can return `Uni<User>` from Mutiny; ports become `Uni<User>` return types |
| Transaction boundaries | One use case = one transaction boundary; wrap at repository level |
| Testability | Domain unit tests: no containers; Application tests: mock the ports; Infrastructure tests: Testcontainers + `@QuarkusTest` |

---

## MapStruct Version and Configuration

**Recommended versions (aligned with Quarkus 3.36.0 BOM):**
- `mapstruct`: `1.5.5.Final` (or latest 1.5.x stable)
- `mapstruct-processor`: same version
- `lombok-mapstruct-binding`: `0.2.0`

**Root pom dependency management:**
```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
    <scope>provided</scope>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok-mapstruct-binding</artifactId>
    <version>0.2.0</version>
    <scope>provided</scope>
</dependency>
```

> **Confidence:** HIGH — MapStruct 1.5.x + `componentModel = "cdi"` is the standard Quarkus integration. Quarkus BOM may or may not manage MapStruct version; check with `mvn dependency:resolve` and override if needed.

---

## Key Build Commands (after restructuring)

```bash
# From root — build everything
./mvnw package

# Dev mode — must run from the adapter module (the quarkus module)
cd mekano-adapter && ../mvnw quarkus:dev

# OR — run from root with module flag
./mvnw quarkus:dev -pl mekano-adapter -am
# -am = "also make" → builds all dependency modules first

# Run tests
./mvnw test

# Verify specific module
./mvnw verify -pl mekano-domain
```

---

## Sources

- Robert C. Martin — *Clean Architecture* (2017): dependency rule, layer definitions
- Alistair Cockburn — Hexagonal Architecture (Ports and Adapters) pattern
- Quarkus CDI Reference Guide: https://quarkus.io/guides/cdi-reference
- Quarkus Hibernate ORM Panache Guide: https://quarkus.io/guides/hibernate-orm-panache
- Quarkus REST Guide: https://quarkus.io/guides/rest
- Quarkus Validation Guide: https://quarkus.io/guides/validation
- MapStruct + Quarkus CDI: https://mapstruct.org/documentation/stable/reference/html/#cdi-component-model
- MapStruct + Lombok: https://mapstruct.org/faq/#can-i-use-mapstruct-together-with-project-lombok
- Quarkus Maven multi-module: https://quarkus.io/guides/maven-tooling#multi-module-maven

**Confidence assessment:**
| Area | Confidence | Reason |
|------|------------|--------|
| Module structure | HIGH | Clean Architecture is a stable, well-documented pattern |
| Port/Adapter interfaces | HIGH | Hexagonal Architecture; standard Java interface pattern |
| Use Case pattern | HIGH | Standard implementation; records for commands are Java 17+ best practice |
| MapStruct CDI integration | HIGH | `componentModel = "cdi"` is the documented Quarkus way |
| ExceptionMapper | HIGH | Standard JAX-RS `@Provider` pattern; stable across JAX-RS versions |
| Bean Validation (`@Valid`) | HIGH | Jakarta Validation 3.x; stable in Quarkus 3.x with `quarkus-hibernate-validator` |
| Multi-module pom structure | HIGH | Standard Maven multi-module; Quarkus `<packaging>quarkus</packaging>` requirement is documented |
| MapStruct + Lombok ordering | HIGH | Documented requirement; missing binding causes silent mapper failures |

---

*Architecture research: 2025-07-15*
