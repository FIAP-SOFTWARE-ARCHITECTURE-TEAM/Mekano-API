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
│   ├── PecaResource.java                 — @Path("/pecas") @RequestScoped @RolesAllowed("admin")
│   ├── RequisicaoCompraResource.java     — @Path("/requisicoes-compra") @RequestScoped @RolesAllowed({"admin","financeiro"})
│   ├── NfEntradaResource.java            — @Path("/nf-entrada") @RequestScoped @RolesAllowed("admin")
│   ├── AlertaResource.java               — @Path("/alertas") @RequestScoped @RolesAllowed({"admin","atendente"})
│   ├── OrdemDeServicoResource.java       — @Path("/os") @RequestScoped @RolesAllowed({"admin","atendente","mecanico"})
│   ├── PagamentoResource.java            — @Path("/pagamentos") @RequestScoped @RolesAllowed({"admin","financeiro"})
│   ├── OrcamentoResource.java            — @Path("/orcamentos") @RequestScoped @RolesAllowed({"admin","cliente"})
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
│   │   ├── ClientePageResponse.java      — Output record
│   │   ├── CreatePecaRequest.java        — Input Lombok: @NotBlank codigo, descricao, unidadeMedida, @DecimalMin valorUnitario
│   │   ├── PecaResponse.java             — Output record: id, codigo, descricao, unidadeMedida, valorUnitario, saldoAtual, estoqueMinimo, createdAt
│   │   ├── PecaPageResponse.java         — Output record (paginated)
│   │   ├── CreateRequisicaoCompraRequest.java — Input Lombok: @NotNull pecaId, quantidade, @NotBlank motivo
│   │   ├── RequisicaoCompraResponse.java  — Output record: id, pecaId, quantidade, status, motivo, createdAt
│   │   ├── RequisicaoCompraPageResponse.java — Output record (paginated)
│   │   ├── CreateNfEntradaRequest.java   — Input Lombok: @NotBlank numero, serie, cnpjFornecedor, nomeFornecedor, chaveAcesso + @DecimalMin campos NF-e + @NotNull pecaId, requisicaoCompraId, quantidade
│   │   ├── NfEntradaResponse.java        — Output record: full NF-e fields + id, createdAt
│   │   ├── NfEntradaPageResponse.java    — Output record (paginated)
│   │   ├── AlertaResponse.java           — Output record: pecaId, codigo, descricao, saldoAtual, estoqueMinimo
│   │   ├── CreateOrdemDeServicoRequest.java — Input Lombok: @NotNull clienteId, veiculoId, @NotBlank descricaoProblema, List<CreateItemOsRequest> itens
│   │   ├── CreateItemOsRequest.java      — Input Lombok: @NotNull referenciaUuid, @NotBlank tipo ("PECA"|"SERVICO"), quantidade
│   │   ├── OrdemDeServicoResponse.java   — Output record: id, clienteId, veiculoId, descricaoProblema, status, ..., List<ItemOsResponse> itens
│   │   ├── ItemOsResponse.java           — Output record: id, referenciaUuid, tipo, descricao, quantidade
│   │   ├── OrdemDeServicoDetailResponse.java — Output record: full OS + itens + itensOrcados
│   │   ├── OrdemDeServicoPageResponse.java — Output record (paginated)
│   │   └── ... (other DTOs)
│   ├── mapper/
│   │   ├── UserDtoMapper.java            — @Mapper(componentModel = "cdi")
│   │   ├── VeiculoDtoMapper.java         — @Mapper(componentModel = "cdi")
│   │   ├── ServicoDtoMapper.java         — @Mapper(componentModel = "cdi")
│   │   ├── ClienteDtoMapper.java         — @Mapper(componentModel = "cdi") (7 @Mapping for address flattening)
│   │   ├── PecaDtoMapper.java            — @Mapper(componentModel = "cdi")
│   │   ├── RequisicaoCompraDtoMapper.java — @Mapper(componentModel = "cdi")
│   │   └── NfEntradaDtoMapper.java       — @Mapper(componentModel = "cdi")
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
| PecaResource | ✓ EXISTS — DTOs, mapper, resource, tests |
| RequisicaoCompraResource | ✓ EXISTS — DTOs, mapper, resource, tests |
| NfEntradaResource | ✓ EXISTS — DTOs, mapper, resource, tests |
| AlertaResource | ✓ EXISTS — DTO, resource, tests |
| OrdemDeServicoResource | ✓ EXISTS — full CRUD + lifecycle transitions (diagnostico, execucao, pagamento, entrega) |
| PagamentoResource | ✓ EXISTS — confirmacao de pagamento |
| OrcamentoResource | ✓ EXISTS — aprovacao/reprovacao |
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
| PecaResource | admin | POST, GET (list+byId) — Peca immutable, no PUT/DELETE |
| RequisicaoCompraResource | admin | POST, GET (list+byId), PUT enviar/cancelar/receber |
| NfEntradaResource | admin | POST, GET (list+byId) |
| AlertaResource | admin, atendente | GET (list) |
| OrdemDeServicoResource | admin, atendente, mecanico | POST, PUT, GET (list+byId+detalhamento), POST iniciar/finalizar diagnostico, POST iniciar/finalizar execucao, POST cancelar, POST entregar, POST confirmar pagamento |
| PagamentoResource | admin, financeiro | POST confirmar |
| OrcamentoResource | admin, cliente | POST aprovar/reprovar |
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
  - ⚠ `user`/`roles` values MUST be compile-time constants (e.g. literal strings, NOT `UUID.randomUUID().toString()`)
  - ⚠ `@TestSecurity` populates `SecurityIdentity` (principal name = `user`), NOT the `JsonWebToken` bean — resolve the principal via `SecurityIdentity` (see `AuditoriaContext`)
- `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` + `@Order(N)` for sequential tests
- `@InjectMock` for mocking repository deps (Quarkus Mockito)
- `@TestTransaction` for automatic rollback
- 20 test files: AdminUserResourceSecurityTest, AdminUserResourceTest, AlertaResourceTest, AuditoriaAuditFieldsTest, ClasspathDiagnosticTest, ClienteResourceTest, FaultToleranceTest, NfEntradaResourceTest, OrcamentoResourceTest, OrdemDeServicoResourceTest, PagamentoResourceTest, PecaResourceTest, RequisicaoCompraResourceTest, ServicoResourceTest, VeiculoFaultToleranceTest, VeiculoGetResourceTest, VeiculoResourceTest, WebhookEvolutionResourceTest, ObservabilityEndpointsTest (observability package), OsAuditResourceTest
- `AuditoriaAuditFieldsTest` (E2E do auto-fill) usa beans REAIS (service + repo, SEM `@InjectMock`) + `@TestTransaction`, com POST/PUT de `/pecas` e leitura do `createdBy`/`updatedBy` persistido
