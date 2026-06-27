# mekano-rest — REST Adapter / Application Entrypoint

## Constraint
Only module with `quarkus-maven-plugin`. Packaging type: `quarkus`. Depends on all other modules.

## Package Map (Verified Against Source)

```
com.fiap.mekano.rest
├── api/
│   ├── MekanoApiApplication.java         — @OpenAPIDefinition, extends Application (bootstrap)
│   ├── UserResource.java                 — @Path("/users") @RequestScoped @RolesAllowed("user")
│   ├── VeiculoResource.java              — @Path("/veiculos") @RequestScoped @RolesAllowed({"admin","atendente"})
│   ├── ServicoResource.java              — @Path("/servicos") @RequestScoped @RolesAllowed("admin")
│   ├── dto/
│   │   ├── CreateUserRequest.java        — Input Lombok: @NotBlank name, @Email email, @Size(min=6) password
│   │   ├── UserResponse.java             — Output record
│   │   ├── UserPageResponse.java         — Output record (content, page, size, totalElements, totalPages)
│   │   ├── CreateVeiculoRequest.java     — Input Lombok: @NotNull clienteUuid, @NotBlank placa, marca, modelo, ano
│   │   ├── UpdateVeiculoRequest.java     — Input Lombok (no clienteUuid)
│   │   ├── VeiculoResponse.java          — Output record
│   │   ├── VeiculoPageResponse.java      — Output record
│   │   ├── CreateServicoRequest.java     — Input Lombok: @NotBlank nome, @DecimalMin("0.01") valor
│   │   ├── UpdateServicoRequest.java     — Input Lombok
│   │   ├── ServicoResponse.java          — Output record
│   │   ├── ServicoPageResponse.java      — Output record
│   │   ├── CreateClienteRequest.java     — Input Lombok: @Pattern("\d{11}") cpf, @Size(min=2,max=2) uf
│   │   ├── UpdateClienteRequest.java     — Input Lombok (no CPF)
│   │   ├── ClienteResponse.java          — Output record (flattened address fields)
│   │   └── ClientePageResponse.java      — Output record
│   ├── mapper/
│   │   ├── UserDtoMapper.java            — @Mapper(componentModel = "cdi")
│   │   ├── VeiculoDtoMapper.java         — @Mapper(componentModel = "cdi")
│   │   ├── ServicoDtoMapper.java         — @Mapper(componentModel = "cdi")
│   │   └── ClienteDtoMapper.java         — @Mapper(componentModel = "cdi") (7 @Mapping for address flattening)
│   └── exception/
│       ├── ProblemDetail.java            — record: type, title, status, detail, instance
│       └── ApiExceptionMapper.java       — @Provider @ApplicationScoped
│                                           Handles: AppException → status, WebApplicationException, fallback 500
│                                           Format: application/problem+json (RFC 7807)
└── observability/
    └── ApplicationLivenessCheck.java     — @Liveness (always returns UP)
```

## What EXISTS in Code vs What's Documented in Old CLAUDE.md

| Item | Status |
|------|--------|
| UserResource | ✓ EXISTS — fully implemented |
| VeiculoResource | ✓ EXISTS — fully implemented |
| ServicoResource | ✓ EXISTS — fully implemented |
| Cliente DTOs + Mapper | ✓ EXISTS — but NO ClienteResource (no controller yet) |
| ApiExceptionMapper | ✓ EXISTS |
| ProblemDetail | ✓ EXISTS |
| ApplicationLivenessCheck | ✓ EXISTS |
| AuthResource | ✗ DOES NOT EXIST |
| LoginRequest / LoginResponse DTOs | ✗ DO NOT EXIST |
| LoginRateLimiterFilter | ✗ DOES NOT EXIST |
| filter/ package | ✗ DOES NOT EXIST |
| *ExceptionMapper (10 deprecated) | ✗ DO NOT EXIST |
| JwtTestProfile | ✗ DOES NOT EXIST |
| AuthResourceTest | ✗ DOES NOT EXIST |
| publicKey.pem | ✗ DOES NOT EXIST in src/main/resources/ |

## Resource Patterns
- `@Path("/...")`, `@RequestScoped` (mandatory for `@Context UriInfo` — G8)
- `@RolesAllowed("role")` — NEVER `@Authenticated` (doesn't exist in code)
- NEVER `@Transactional` — transactions are in the use case (D-01)
- Inject service port + DTO mapper via constructor
- Full OpenAPI annotations: `@Tag`, `@Operation`, `@APIResponse` on every method
- Pagination: `@QueryParam("page") @DefaultValue("0")`, `@QueryParam("size") @DefaultValue("20")`

## DTO Conventions
- **Input**: Lombok class with `@Getter @Setter @NoArgsConstructor` (MapStruct needs setters + no-arg)
- **Output**: Java record (immutable, no passwordHash exposed)
- **PageResponse**: record with `List<*> content, int page, int size, long totalElements, int totalPages`

## Resource Roles
| Resource | Roles | Methods |
|----------|-------|---------|
| UserResource | user | POST, GET (list+byId), DELETE |
| VeiculoResource | admin, atendente | POST, PUT, GET (list+byId), DELETE |
| ServicoResource | admin | POST, PUT, GET (list+byId), DELETE |
| ClienteResource | NOT YET IMPLEMENTED | DTOs + mapper exist, no controller |

## Exception Handling
- `ApiExceptionMapper` catches `AppException` (uses its `int status`), `WebApplicationException`, and fallback 500
- Response format: `application/problem+json` with `type`, `title`, `status`, `detail`, `instance`
- `ConstraintViolationException` handled natively by Quarkus (format: `violations` array)

## Configuration Files (`src/main/resources/`)
| File | Purpose |
|------|---------|
| `application.properties` | References YAML configs via `quarkus.config.locations`, CORS, json pretty-print |
| `datasource-config.yml` | PostgreSQL (dev/prod), H2 in-memory (test), Flyway migrate-at-start |
| `api-config.yml` | REST prefix `/api/v1`, CORS `*`, Jackson timezone `America/Sao_Paulo` |
| `openapi-config.yml` | Swagger UI, OpenAPI info |
| `logging-config.yml` | JSON console logging (DEBUG dev, INFO prod, WARN test) |
| `cache-config.yml` | (in infrastructure module) Caffeine caches |

## JWT / Auth Config (`application.properties` in `datasource-config.yml`)
```
mp.jwt.verify.publickey.location=publicKey.pem
mp.jwt.verify.issuer=${MP_JWT_ISSUER:https://mekano.fiap.com.br/auth}
mp.jwt.verify.algorithm=EdDSA
%dev.smallrye.jwt.sign.key.location=${user.home}/.mekano/secrets/privatekey.pem
quarkus.http.auth.proactive=false
```

## Dependencies (compile)
mekano-domain, mekano-application, mekano-infrastructure, quarkus-rest-jackson, quarkus-hibernate-orm-panache, quarkus-flyway, quarkus-smallrye-openapi, quarkus-hibernate-validator, quarkus-logging-json, quarkus-jackson, quarkus-jdbc-postgresql, quarkus-smallrye-health, quarkus-micrometer-registry-prometheus, quarkus-config-yaml, mapstruct, lombok (provided)

## Testing
- `@QuarkusTest` + REST Assured
- `@TestSecurity(user = "X", roles = {"Y"})` for JWT bypass
- `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` + `@Order(N)` for sequential tests
- `@InjectMock` for mocking repository deps (Quarkus Mockito)
- `@TestTransaction` for automatic rollback
- 7 test files: UserResourceTest, UserSoftDeleteTest, VeiculoResourceTest, VeiculoFaultToleranceTest, ServicoResourceTest, FaultToleranceTest, ObservabilityEndpointsTest
