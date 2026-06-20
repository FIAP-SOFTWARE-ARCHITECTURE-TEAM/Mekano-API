# mekano-rest — Adaptador REST / Entrypoint

## Overview

Módulo de packaging Quarkus — entrypoint da aplicação. Contém os recursos JAX-RS, DTOs de entrada/saída, mappers REST, exception handlers, filtros, health checks e configuração da aplicação.

**Único módulo com `quarkus-maven-plugin`**. Depende de todos os outros módulos.

## Package Structure

```
com.fiap.mekano.rest
├── api/
│   ├── MekanoApiApplication.java         # Classe principal (vazia — bootstrap)
│   ├── UserResource.java                 # CRUD de usuários
│   ├── AuthResource.java                 # Autenticação (login, refresh)
│   ├── dto/
│   │   ├── CreateUserRequest.java        # Input: classe Lombok + Bean Validation
│   │   ├── UserResponse.java             # Output: record (sem passwordHash)
│   │   ├── UserPageResponse.java         # Output: record paginado
│   │   ├── LoginRequest.java             # Input: classe Lombok
│   │   └── LoginResponse.java            # Output: record com @JsonProperty snake_case
│   ├── mapper/
│   │   └── UserDtoMapper.java            # MapStruct DTO ↔ domain
│   ├── filter/
│   │   └── LoginRateLimiterFilter.java   # Rate limiting via ContainerRequestFilter
│   └── exception/
│       ├── ErrorResponse.java            # Record — mensagem de erro
│       ├── ApiExceptionMapper.java       # ÚNICO mapper ativo (D-09)
│       └── *ExceptionMapper.java         # 10 mappers @Deprecated mantidos para rollback
└── observability/
    └── ApplicationLivenessCheck.java     # @Liveness HealthCheck customizado
```

## Resource Patterns

### UserResource (`api/UserResource.java:61-241`)
- `@Path("/users") @RequestScoped @Authenticated`
- **`@RequestScoped` obrigatório** (G8) — necessário para `@Context UriInfo` e injeção de JWT claims
- **NUNCA `@Transactional`** — transações são do use case (D-01)
- Injeta `CreateUserInputPort` (interface) e `UserRepositoryPort` (consulta direta D-06)
- Endpoints: `POST /users` (cria), `GET /users` (lista paginada), `GET /users/{id}` (busca), `DELETE /users/{id}` (soft delete)
- `@RolesAllowed("user")` — exige role JWT "user"
- OpenAPI: `@Tag`, `@Operation`, `@APIResponse` em cada método

### AuthResource (`api/AuthResource.java`)
- `@Path("/auth") @PermitAll` — sem autenticação (ponto de login)
- Gera JWT via `Jwt.issuer().subject().upn().groups().expiresIn().sign()`

## DTO Conventions

### Input DTOs (Classes com Lombok)
```java
@Getter @Setter @NoArgsConstructor  // MapStruct precisa de setters + no-arg
public class CreateUserRequest {
    @NotBlank String name;
    @NotBlank @Email String email;
    @NotNull @Size(min = 6) String password;
}
```

### Output DTOs (Records)
```java
public record UserResponse(UUID id, String name, String email, LocalDateTime createdAt) {}
public record UserPageResponse(List<UserResponse> content, int page, int size, long total, int totalPages) {}
```

## Exception Handling

**`ApiExceptionMapper.java`** — único mapper ativo (D-09):
- `@Provider @ApplicationScoped` — obrigatório (G10)
- Dispatch: `instanceof` → `ConstraintViolationException` (400) → `AppException` (status da exceção) → fallback 500
- Fallback: 500 com log de stacktrace
- Mappers antigos: `@Deprecated` sem `@Provider` — mantidos para rollback

## Filter Patterns

### LoginRateLimiterFilter
- `@Provider @ApplicationScoped` — ContainerRequestFilter
- Aplica-se apenas a `POST /auth/login` (path match)
- Lê e rewinds request body para extrair email
- Chave composta: IP + email
- 10 tentativas/minuto → 429 Retry-After

## JWT / Auth Config

### application.properties
```properties
mp.jwt.verify.publickey.location=publicKey.pem     # público
mp.jwt.verify.issuer=${MP_JWT_ISSUER:https://mekano.fiap.com.br/auth}
mp.jwt.verify.algorithm=EdDSA
%dev.smallrye.jwt.sign.key.location=${user.home}/.mekano/secrets/privatekey.pem
quarkus.http.auth.proactive=false
```

### Testes com JWT
- `JwtTestProfile` — gera par Ed25519 in-memory, sobrescreve System properties
- `@TestSecurity(user = "testuser", roles = {"user"})` — bypass em testes que não testam auth
- `@TestProfile(JwtTestProfile.class)` — quando testa auth real

## Dependencies (pom.xml)

- **compile**: `mekano-domain`, `mekano-application`, `mekano-infrastructure`
- **Quarkus extensions**: `quarkus-rest-jackson`, `quarkus-hibernate-orm-panache`, `quarkus-flyway`, `quarkus-smallrye-openapi`, `quarkus-hibernate-validator`, `quarkus-logging-json`, `quarkus-jackson`, `quarkus-jdbc-postgresql`, `quarkus-smallrye-health`, `quarkus-micrometer-registry-prometheus`, `quarkus-smallrye-jwt`, `quarkus-smallrye-jwt-build`
- **Mappers**: `mapstruct`, `lombok` (provided)
- **test**: `quarkus-junit5`, `rest-assured`, `quarkus-test-security`, `assertj-core`

## How to Add New Endpoint

1. Criar DTOs: `dto/NovoRequest.java` (classe Lombok) + `dto/NovoResponse.java` (record)
2. Adicionar mapeamento em `mapper/UserDtoMapper.java` (ou criar novo mapper)
3. Criar recurso em `api/NovoResource.java` — `@Path/@RequestScoped/@Authenticated`
4. Adicionar OpenAPI: `@Tag/@Operation/@APIResponse`
5. Se necessário, adicionar exceção no `ApiExceptionMapper`
6. Criar teste: `NovoResourceTest.java` — REST Assured + `@TestSecurity`

## Testing

- `@QuarkusTest` + REST Assured
- `@TestSecurity(user = "testuser", roles = {"user"})` para bypass JWT
- `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` para testes sequenciais
- `@TestProfile(JwtTestProfile.class)` para auth real
- DevServices PostgreSQL (sem `jdbc.url`)

Exemplos: `UserResourceTest.java`, `AuthResourceTest.java`, `ObservabilityEndpointsTest.java`, `FaultToleranceTest.java`
