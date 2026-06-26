# PLAN-05: Admin Users — Admin User CRUD, Role 'cliente' Auth

## Goal
Implement AUTH-04: admin CRUD of all system users via dedicated `/admin/users` endpoint. Reuse existing `User` domain and auth infrastructure. Create role `cliente` for client budget approval access. Generate random passwords on user creation (shown once to admin).

## Dependencies
- Phase 1 complete (User domain, UserService, UserRepository, auth with roles, JWT Ed25519)
- Phase 1 roles: `admin`, `atendente`, `mecanico`, `almoxarife`, `financeiro` already exist

## Requirements Covered
AUTH-04 (admin user CRUD), D-09..D-17 (client auth integration)

---

## Tasks

### Task 1: AdminUserService — Application Layer

**Files created:**
- `mekano-application/src/main/java/com/fiap/mekano/application/service/admin/AdminUserService.java`

**Action:**
- `@ApplicationScoped`, `@Transactional` on mutating methods
- Inject: `UserRepositoryPort` (existing), `PasswordHasher` (existing `BcryptPasswordHasher`), `EventPublisher`

**`criarUsuario(String name, String email, String role, String password):`**
- Per D-44..D-47: Admin creates users with name, email, role. If password is null/empty, generate one via `PasswordGenerator.generate()`.
- Validate email uniqueness via `UserRepositoryPort.existsByEmail()`.
- Hash password via `PasswordHasher.hash()`.
- Delegate to existing `User.create()` factory method.
- Save via `UserRepositoryPort.save()`.
- Return `CreateUserResponse` with generated password (if new) — `@JsonInclude(NON_NULL)` per OQ-02.

**`listarUsuarios(int page, int size, boolean apenasAtivos):`**
- Passthrough to `UserRepositoryPort.findAll()` and `countAll()`.
- Per D-61..D-65 pagination contract.

**`editarUsuario(UUID uuid, String name, String email, String role):`**
- Find existing user, update mutable fields (nome, email, role per D-47).
- Save via repository.

**`resetarSenha(UUID uuid):`**
- Generate new random password, hash it, update user's `passwordHash`.
- Return new plaintext password (shown once to admin).

**`deletarUsuario(UUID uuid):`**
- Soft-delete via `UserRepositoryPort.markAsDeleted()`.

**`restaurarUsuario(UUID uuid):`**
- Per D-51: `PATCH /admin/users/{uuid}/restore` — reactivate soft-deleted user.

**Verification:**
```bash
./mvnw test -pl mekano-application -am -Dtest="AdminUserServiceTest"
```

---

### Task 2: PasswordGenerator Utility

**Files created:**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/util/PasswordGenerator.java` (or in `application` package)

**Action:**
Per Pattern 4: `SecureRandom` + Base64 encoding.
```java
public final class PasswordGenerator {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int PASSWORD_LENGTH = 12;
    
    public static String generate() {
        byte[] bytes = new byte[PASSWORD_LENGTH];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
```
- No constructor (utility class).
- The output is ~16 characters (12 bytes → 16 Base64 chars).
- Javadoc explaining: used for initial passwords on user creation (admin view) and client account creation.

**Verification:**
```bash
./mvnw test -pl mekano-domain -Dtest="PasswordGeneratorTest"
```

---

### Task 3: Client Auth Integration — Role 'cliente' Auto-Provisioning

**Files created or modified:**
- `mekano-application/src/main/java/com/fiap/mekano/application/service/admin/ClienteUserProvisioningService.java` (new)

**Action:**
Per D-09..D-17: Client auth integration.

**`criarContaCliente(UUID clienteUuid, String cpf, String nome):`**
Called when a new `Cliente` is registered (Phase 1 ClienteService).
- Create a `User` with:
  - `name = nome` (cliente's name)
  - `email = cpf` (CPF as login identifier per D-10)
  - `role = "cliente"`
  - `password = PasswordGenerator.generate()` (system-generated per D-12)
- Hash password via `PasswordHasher.hash()`
- Save via `UserRepositoryPort.save()`
- Return the generated password (shown to admin at client registration)

**DTO response:**
```java
public record CreateClienteComContaResponse(
    UUID clienteUuid,
    String cpf,
    String generatedPassword   // null on subsequent reads
) {}
```

**Verification:**
```bash
./mvnw test -pl mekano-application -am -Dtest="*ClienteUserProvisioning*"
```

---

### Task 4: AdminUserResource — REST Endpoint

**Files created:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/AdminUserResource.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/admin/CreateAdminUserRequest.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/admin/AdminUserResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/admin/AdminUserPageResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/mapper/AdminUserDtoMapper.java`

**Action:**
- `@Path("/admin/users")` per D-44
- `@RequestScoped` (G8 compliance for JWT)
- `@RolesAllowed("admin")` — only admin can access
- `@Tag(name = "Admin Usuários", description = "Gestão de usuários do sistema")`

**Endpoints:**
- **`POST /`** — Create user. `@Operation(summary = "Criar usuário", description = "Admin cria usuário. Senha gerada se não fornecida.")`. Receive `CreateAdminUserRequest` (name, email, role, password optional). Return `AdminUserResponse` with generatedPassword (if new).
- **`GET /`** — List users. Paginated (page, size, sort, order). Filter `apenasAtivos` param.
- **`GET /{uuid}`** — Get user by UUID.
- **`PUT /{uuid}`** — Update user (name, email, role).
- **`POST /{uuid}/reset-password`** — Reset password. Returns new password.
- **`DELETE /{uuid}`** — Soft-delete user.
- **`PATCH /{uuid}/restore`** — Restore soft-deleted user per D-51.

**DTO pattern:**
- Input: Lombok class with `@NotBlank`, `@Email` validation (reuse existing `CreateUserRequest` pattern)
- Output: Java record with `@Schema` annotations (reuse existing `UserResponse` pattern)

**Mapper:**
- `@Mapper(componentModel = "cdi")` — MapStruct CDI
- Maps between DTOs and existing `User` domain

**Verification:**
```bash
./mvnw test -pl mekano-rest -am -Dtest="AdminUserResourceTest"
```

---

### Task 5: Role 'cliente' Permissions Integration

**Files modified:**
- None new — add `@RolesAllowed("cliente")` to Orcamento approval/rejection endpoints (PLAN-07)
- Ensure `@TestSecurity` tests cover role 'cliente'

**Action:**
- Verify that Phase 1 auth infrastructure accepts `"cliente"` as a valid role in JWT `groups` claim.
- The role `cliente` must be seeded in the `user_roles` table (if role-permission model used) or simply set as the user's role in their `User` entity.
- Phase 1 D-10 says `user_roles` table supports N:N — `cliente` should be a valid entry.
- No code changes needed here — just verify compatibility.

**Verification:**
```bash
# Auth integration test with role "cliente"
./mvnw test -pl mekano-rest -am -Dtest="*ClienteAuth*"
```

---

### Task 6: Tests for Admin User Operations

**Files created:**
- `mekano-application/src/test/java/com/fiap/mekano/application/service/admin/AdminUserServiceTest.java`
- `mekano-rest/src/test/java/com/fiap/mekano/rest/api/AdminUserResourceTest.java`

**Action:**
- **AdminUserServiceTest:** Mockito. Test `criarUsuario()` with null password (generates one), with provided password (uses it). Test `listarUsuarios()` pagination. Test `editarUsuario()` updates role. Test `resetarSenha()` returns new password. Test `deletarUsuario()` soft-deletes. Test `restaurarUsuario()` reactivates.
- **AdminUserResourceTest:** `@QuarkusTest` + REST Assured + `@TestSecurity(user = "admin", roles = {"admin"})`. Test each endpoint:
  - `POST /admin/users` returns 201 with AdminUserResponse
  - `GET /admin/users` returns 200 with paginated response
  - `GET /admin/users/{uuid}` returns 200 with user data
  - `PUT /admin/users/{uuid}` returns 200 with updated data
  - `POST /admin/users/{uuid}/reset-password` returns 200 with new password
  - `DELETE /admin/users/{uuid}` returns 204
  - `PATCH /admin/users/{uuid}/restore` returns 200
  - Test non-admin role (`@TestSecurity(user = "user", roles = {"user"})`) returns 403

**Verification:**
```bash
./mvnw test -pl mekano-application -am -Dtest="AdminUserServiceTest"
./mvnw test -pl mekano-rest -am -Dtest="AdminUserResourceTest"
```

---

## Verification (Plan-Level)

```bash
./mvnw compile -pl mekano-rest -am
./mvnw test -pl mekano-application -am -Dtest="AdminUserServiceTest"
./mvnw test -pl mekano-rest -am -Dtest="AdminUserResourceTest"
curl -u admin:... http://localhost:8080/api/v1/admin/users | jq .
```

## Risk Mitigation
- **Password generation:** `SecureRandom` + Base64 produces cryptographically random passwords. BCrypt hashed before storage. Plaintext shown once in API response, never stored.
- **Role 'cliente':** Must be a valid role in Phase 1's `user_roles` table or role model. If Phase 1 uses a fixed role set, add `cliente` to that set.
- **CPF-as-login:** D-10 specifies CPF is used as login identifier. Stored in `email` field of `User` entity. FIX: Add a custom `@CpfOrEmail` validator that accepts both CPF (11 digits) and email formats. Apply to User entity's `email` field instead of `@Email`. Create in `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/CpfOrEmailValidator.java`.
- **Client ownership filter (D-13):** Implemented at the REST layer in PLAN-07. AdminUserResource and admin endpoints are admin-only and see all users.
