---
phase: 02-code-review
reviewed: 2026-06-20T10:00:00Z
depth: deep
files_reviewed: 17
files_reviewed_list:
  - mekano-domain/src/main/java/com/fiap/mekano/domain/exception/AppException.java
  - mekano-domain/src/main/java/com/fiap/mekano/domain/exception/Messages.java
  - mekano-domain/src/main/resources/com/fiap/mekano/domain/exception/messages.properties
  - mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/Email.java
  - mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/UserServicePort.java
  - mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/UserRepositoryPort.java
  - mekano-domain/src/test/java/com/fiap/mekano/domain/model/UserTest.java
  - mekano-domain/src/test/java/com/fiap/mekano/domain/valueobject/EmailTest.java
  - mekano-application/src/main/java/com/fiap/mekano/application/service/user/UserService.java
  - mekano-application/src/test/java/com/fiap/mekano/application/service/user/UserServiceTest.java
  - mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/UserRepositoryImpl.java
  - mekano-rest/src/main/java/com/fiap/mekano/rest/api/exception/ApiExceptionMapper.java
  - mekano-rest/src/main/java/com/fiap/mekano/rest/api/exception/ProblemDetail.java
  - mekano-rest/src/main/java/com/fiap/mekano/rest/api/UserResource.java
  - mekano-rest/src/test/java/com/fiap/mekano/rest/api/UserResourceTest.java
  - mekano-rest/src/test/java/com/fiap/mekano/rest/api/UserSoftDeleteTest.java
  - mekano-rest/src/test/java/com/fiap/mekano/rest/observability/ObservabilityEndpointsTest.java
findings:
  critical: 1
  warning: 4
  info: 2
  total: 7
status: issues_found
---

# Phase 02: Code Review Report

**Reviewed:** 2026-06-20T10:00:00Z
**Depth:** deep
**Files Reviewed:** 17
**Status:** issues_found

## Summary

Reviewed 17 files across all four layers (domain, application, infrastructure, rest) after a refactoring session that centralized exception messages, removed factory methods from `AppException`, replaced former `ErrorResponse` with RFC 7807 `ProblemDetail`, and cleaned up port interface signatures.

The changes are structurally sound — centralized message handling via `ResourceBundle` + `MessageFormat` works correctly, the `AppException` simplification to constructor-only is clean, and the RFC 7807 response format is a good direction. However, **one critical security issue** was found: the sole REST resource has **zero authentication/authorization annotations** despite documentation claiming otherwise. Several other medium-severity issues are present including a race-condition-to-500 bug in user creation, HQL injection surface in pagination, and missing input size limits.

---

## Critical Issues

### CR-01: Missing authentication annotations on UserResource — all endpoints publicly accessible

**File:** `mekano-rest/src/main/java/com/fiap/mekano/rest/api/UserResource.java:57-59`
**Issue:** `UserResource` has `@Path("/users")` and `@RequestScoped` but is **missing both `@Authenticated` and `@RolesAllowed("user")` annotations**. The class Javadoc at lines 47-48 explicitly documents that "Exige autenticação JWT em todos os endpoints" and "POST /users requer role `user`", but these annotations were never applied to the class.

With `quarkus.http.auth.proactive=false` (the required mode for proper exception mapper behavior — see project conventions), the absence of `@Authenticated` means all four endpoints are completely unauthenticated:

| Endpoint | Method | Exposure |
|---|---|---|
| `POST /api/v1/users` | Create user | Anyone can create accounts |
| `GET /api/v1/users` | List all users | Anyone can enumerate users |
| `GET /api/v1/users/{id}` | Get user by ID | Anyone can read any user |
| `DELETE /api/v1/users/{id}` | Soft-delete user | Anyone can delete any user |

**Severity:** BLOCKER

**Root cause:** The `@Authenticated` and `@RolesAllowed("user")` annotations that should be on the class (or at minimum on each method) were never applied. Javadoc claims they exist but the code doesn't back it up.

**Fix:**
```java
@Path("/users")
@RequestScoped
@Authenticated  // ← ADD THIS
@Tag(name = "Users", description = "User management")
public class UserResource {
```

And add `@RolesAllowed("user")` to each method that requires authorization, e.g.:
```java
@POST
@RolesAllowed("user")  // ← ADD THIS
public Response create(@Valid CreateUserRequest request, @Context UriInfo uriInfo) {
```

Also: the `application.properties` file has no `mp.jwt.*` configuration at all. Add the minimum required JWT configuration:
```properties
mp.jwt.verify.publickey.location=publicKey.pem
mp.jwt.verify.issuer=${MP_JWT_ISSUER:https://mekano.fiap.com.br/auth}
mp.jwt.verify.algorithm=EdDSA
quarkus.http.auth.proactive=false
```

**Downstream impact:** If deployed as-is, user data (names, emails, UUIDs) is fully open to the internet and anyone can create or delete users.

---

## Warnings

### WR-01: Race condition in user creation produces 500 error instead of proper 409

**Files:**
- `mekano-application/src/main/java/com/fiap/mekano/application/service/user/UserService.java:58-60` (existsByEmail check)
- `mekano-application/src/main/java/com/fiap/mekano/application/service/user/UserService.java:69` (save)
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/exception/ApiExceptionMapper.java:33-38` (fallback 500)

**Issue:** The `UserService.execute()` method performs a `existsByEmail` check (line 58) and then call `save()` (line 69) in separate steps within a `@Transactional` method. Under concurrent requests with the same email:

1. Thread A: `existsByEmail("x@x.com")` → `false`
2. Thread B: `existsByEmail("x@x.com")` → `false` (Thread A hasn't committed yet under READ_COMMITTED)
3. Thread A: `save()` → succeeds, transaction commits
4. Thread B: `save()` → Hibernate `ConstraintViolationException` (unique constraint on `email`) → transaction marked for rollback

The `ConstraintViolationException` from Hibernate is **not** an `AppException`, so `ApiExceptionMapper` catches it as a generic exception and returns **500 Internal Server Error** instead of the correct **409 Conflict** response.

The test `UserServiceTest.deveLancarExcecaoQuandoEmailDuplicado()` (line 57-63) tests the non-concurrent path (detects duplicate before save) but does **not** test the concurrent race path (save fails despite pre-check passing).

**Fix (option A — catch and convert in use case):**
```java
@Transactional
public User execute(CreateUserCommand command) {
    // ... name validation ...
    // ... existsByEmail check ...

    String passwordHash = passwordHasher.hash(command.password());
    User user = User.create(command.name(), command.email(), passwordHash);

    try {
        User savedUser = userRepository.save(user);
        eventPublisher.publish(UserCreatedEvent.of(savedUser));
        return savedUser;
    } catch (ConstraintViolationException e) {
        // Race condition: email was inserted between existsByEmail check and save
        throw new AppException(409, Messages.get("user.already.exists", command.email()), e);
    }
}
```

**Fix (option B — handle in the mapper):**
Extend `ApiExceptionMapper` to catch `jakarta.persistence.ConstraintViolationException` and return 409 for duplicate email violations.

### WR-02: User-controlled sort parameter enables HQL/SQL manipulation via ORDER BY

**Files:**
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/UserRepositoryImpl.java:157-165`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/UserResource.java:149-152`

**Issue:** The `GET /api/v1/users` endpoint accepts a user-controlled `sort` query parameter (default `"name,asc"`). This string is passed through `UserResource.listAll()` → `UserRepositoryPort.findAll()` → `UserRepositoryImpl.findAll()`, where it's split on `,` and the first part is used directly as a field name in `Sort.by()`:

```java
String[] sortParts = sort.split(",");
String sortField = sortParts[0];  // RAW user input → Sort.by()
var direction = ascending ? Sort.Direction.Ascending : Sort.Direction.Descending;
var query = panacheRepository.find("isActive = ?1",
        Sort.by(sortField).direction(direction), true);
```

The `sortField` value is **not validated against a whitelist** of allowed entity fields. Since ORDER BY clauses cannot use parameterized queries in SQL, Hibernate concatenates the field name directly into the generated query. This opens up:

- **Information disclosure**: sorting by `passwordHash` reveals nothing about the hash content itself, but sorting by internal fields like `id` (sequential PK) allows inference of registration order and volume
- **Query errors / potential injection**: a field name containing HQL metacharacters could cause unexpected query mutation

Example attack:
```
GET /api/v1/users?sort=passwordHash,asc
```
This successfully sorts users by password hash order (BCrypt hashes are distinguishable even if not decryptable).

**Fix:** Whitelist allowed sort fields:
```java
private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "email", "createdAt", "uuid");

public List<User> findAll(int page, int size, String sort) {
    String[] sortParts = sort.split(",");
    String sortField = sortParts[0];
    
    if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
        throw new AppException(400, "Invalid sort field: " + sortField);
    }
    // ... rest of method
}
```

### WR-03: Missing @Size(max=...) constraints on all DTO fields — potential DoS vector

**Files:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/CreateUserRequest.java:28-40`

**Issue:** The `CreateUserRequest` DTO has `@NotBlank` on `name` and `email`, and `@Size(min = 6)` on `password`, but **none of the fields have a `@Size(max = ...)` constraint**. An attacker can send:

- A 100,000-character `name` — stored in database and paginated to other users in lists
- A 100,000-character `password` — hashed by BCrypt, which is **intentionally slow** (≈100ms per hash at cost 12). A 100KB password takes disproportionately longer to hash and consumes excessive memory
- A 100,000-character `email` — stored and indexed

This is a denial-of-service vector: a single request with a multi-megabyte password causes the server thread to hang in BCrypt hashing for seconds, and 10 concurrent such requests exhaust the thread pool.

**Fix:** Add reasonable `@Size(max = ...)` constraints to all fields:
```java
@NotBlank(message = "Nome é obrigatório")
@Size(max = 255, message = "Nome deve ter no máximo 255 caracteres")
private String name;

@NotBlank(message = "Email é obrigatório")
@Email(message = "Email deve ter formato válido")
@Size(max = 320, message = "Email deve ter no máximo 320 caracteres")
private String email;

@NotNull(message = "Senha é obrigatória")
@Size(min = 6, max = 128, message = "Senha deve ter entre 6 e 128 caracteres")
private String password;
```

### WR-04: Resource injects concrete UserService instead of using the interface — violates documented contract and Dependency Inversion

**Files:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/UserResource.java:69-73`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/UserResource.java:125`

**Issue:** `UserResource` injects **both** the interface `UserServicePort` (line 70) and the concrete implementation `UserService` (line 73). The class Javadoc at line 45 explicitly states:
> "Invocar UserServicePort (nunca UserService diretamente — INH-04)"

But `create()` at line 125 directly calls `UserService.executeResponse(command)` on the **concrete class**, bypassing the port interface entirely:

```java
// Line 124-126  — uses concrete class, not the interface
var command = userDtoMapper.toCommand(request);
var response = UserService.executeResponse(command);
```

Meanwhile, `getById()` and `delete()` at lines 185 and 209 correctly use the `UserServicePort` interface.

This violates:
1. **Dependency Inversion Principle**: the REST layer should depend on domain abstractions (ports), not concrete application classes
2. **The project's own documented convention**: "never UserService directly — INH-04"

The root cause: `executeResponse()` is not part of the `UserServicePort` interface. The REST layer needs it to get a `CreateUserResponse` instead of the raw `User` entity, but the interface only exposes `execute(CreateUserCommand)` returning `User`.

**Fix (recommended):** Move `executeResponse()` into the `UserServicePort` interface:
```java
// In UserServicePort.java
User execute(CreateUserCommand command);
```

Then remove the `UserService` injection from `UserResource` and use the interface:
```java
@Inject
UserServicePort UserServicePort;  // single injection, covers all three methods
```

And in `UserResource.create()`:
```java
User user = UserServicePort.execute(command);
UserResponse userResponse = userDtoMapper.toResponse(user);
// ...
```

This also requires either:
- Moving `CreateUserResponse` out of the use case into the domain, or
- Having the REST layer map `User` → `UserResponse` directly (which it already can via `UserDtoMapper.toResponse(User)`)

Fix approach **B** (simpler): just remove `executeResponse()` usage and use `execute()` directly:
```java
var command = userDtoMapper.toCommand(request);
User user = UserServicePort.execute(command);
UserResponse userResponse = userDtoMapper.toResponse(user);
```

---

## Info

### IN-01: Tests never verify `application/problem+json` content type on error responses

**File:** `mekano-rest/src/test/java/com/fiap/mekano/rest/api/UserSoftDeleteTest.java:57-64`

**Issue:** After the migration to RFC 7807 Problem Details, the integration tests verify HTTP status codes and body content but **never verify the `Content-Type: application/problem+json` header**. The only error-response test is `get_afterDelete_returns404()` (line 57-64), which only checks statusCode(404) and `body("detail", notNullValue()`). Similarly, `UserResourceTest` checks `body("violations", notNullValue())` for 400 errors (which come from Quarkus' built-in validation handler, not the ProblemDetail format).

The 409 duplicate email response is also never checked for correct content type.

**Suggestion:** Add content-type assertions to all error-asserting tests:
```java
.then()
.statusCode(404)
.contentType(ContentType.JSON)  // or "application/problem+json"
.body("type", equalTo("about:blank"))
.body("title", equalTo("Not Found"))
.body("detail", notNullValue());
```

### IN-02: OpenAPI example for `instance` field is misleading — it's always null

**File:** `mekano-rest/src/main/java/com/fiap/mekano/rest/api/exception/ProblemDetail.java:39`

**Issue:** The `ProblemDetail.instance` field has a `@Schema` annotation showing `example = "/api/v1/users/123"`, but the `ApiExceptionMapper` never populates this field — it's always `null` because the mapper uses `ProblemDetail.of(int, String)` which passes `instance = null`. The `ProblemDetail.of(int, String, String)` variant that accepts an `instance` parameter exists but is never called.

This is misleading for any consumer reading the OpenAPI spec at `/q/openapi` or using Swagger UI, which would suggest the field is populated with a meaningful URI.

**Suggestion:** Either:
- Remove `instance` from the record (cleanest if never used), or
- Wire `UriInfo` or request URL into `ApiExceptionMapper` to populate `instance` with the actual request URI.

---

## Findings by File

| File | Critical | Warning | Info |
|------|----------|---------|------|
| `UserResource.java` | 1 | 1 | 0 |
| `UserService.java` | 0 | 1 | 0 |
| `CreateUserRequest.java` | 0 | 1 | 0 |
| `UserRepositoryImpl.java` | 0 | 1 | 0 |
| `ProblemDetail.java` | 0 | 0 | 1 |
| `UserSoftDeleteTest.java` | 0 | 0 | 1 |
| All other files | 0 | 0 | 0 |

---

_Reviewed: 2026-06-20T10:00:00Z_
_Reviewer: gsd-code-reviewer (deep)_
_Depth: deep_
