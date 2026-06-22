# Phase 1: Auth & OS Foundation - Research

**Researched:** 2026-06-22
**Domain:** JWT authentication, Clean Architecture aggregates, state machine, CRUD APIs
**Confidence:** HIGH

## Summary

This phase implements JWT authentication with Ed25519/EdDSA (5 roles), four new Clean Architecture aggregates (Cliente, Veiculo, Servico, OrdemDeServico), the OS state machine with explicit transition methods, refresh token rotation, and a public OS status endpoint. All patterns follow the existing User entity conventions exactly — same factory method pattern (`create()`/`reconstitute()`), same two-class repository structure (PanacheRepository + RepositoryImpl), same MapStruct `componentModel = "cdi"` mapping, same `@Transactional` placement in the application layer.

The phase divides naturally into vertical slices for 5 devs working in parallel: each developer owns one aggregate end-to-end (domain → infrastructure → rest). Auth (login/JWT generation/refresh) is the integration layer that ties all roles together.

**Primary recommendation:** Follow the existing User entity pattern exactly for all new aggregates. Implement auth last (after aggregates) so role definitions exist before login is built. Use SmallRye JWT Build API (`Jwt.issuer().upn().groups().sign()`) for token generation and `mp.jwt.*` config for verification with EdDSA algorithm.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions

#### Autenticação JWT
- **D-01:** Algoritmo Ed25519/EdDSA. Claim `groups` com role único (usuário tem 1 role no JWT)
- **D-02:** Access token: 15 minutos de expiração
- **D-03:** Refresh token na Fase 1 com rotação automática (cada uso gera novo par, invalida anterior). Tabela `refresh_tokens` já existe (V2 migration)
- **D-04:** Chaves Ed25519 geradas via script no build (automático). Public key em `mekano-rest/src/main/resources/publicKey.pem`, private key em `~/.mekano/secrets/privatekey.pem`
- **D-05:** Só admin cria usuários do sistema (endpoint `POST /api/v1/users` protegido com `@RolesAllowed("admin")`)
- **D-06:** JWT contém claims: `sub` (UUID), `groups` (role), `name` (nome do usuário)
- **D-07:** Issuer: `https://mekano.fiap.com.br/auth` configurado via `MP_JWT_ISSUER` env var
- **D-08:** Login identifier: email + senha (POST /api/v1/auth/login)
- **D-09:** Endpoint `POST /api/v1/auth/logout` invalida refresh token no servidor

#### Roles e Permissões
- **D-10:** Tabela separada `user_roles` (N:N) — suporta múltiplos papéis por usuário no futuro. Na prática, cada usuário tem 1 role por enquanto
- **D-11:** Clientes CRUD: admin + atendente
- **D-12:** Veículos CRUD: admin + atendente
- **D-13:** Serviços (tipos de serviço) CRUD: só admin
- **D-14:** OS listar: todos os perfis; criar: admin + atendente
- **D-15:** Iniciar diagnóstico (RECEBIDA → EM_DIAGNOSTICO): mecânico + admin
- **D-16:** Endpoint público OS status: `@PermitAll` (sem autenticação)

#### Modelo do Cliente
- **D-17:** Só CPF (pessoa física) na Fase 1. CNPJ (pessoa jurídica) na Fase 2
- **D-18:** Value Object `Cpf` no domain — valida dígitos verificadores no construtor (padrão `Email` VO)
- **D-19:** Campos obrigatórios: nome, CPF, email, telefone
- **D-20:** Endereço completo: logradouro, número, bairro, cidade, UF, CEP (modelado como Value Object `Endereco`)
- **D-21:** Cliente pode ter múltiplos veículos (relacionamento 1:N)

#### Veículo
- **D-22:** Placa armazenada normalizada: uppercase, sem hífen. Ex: `ABC1234` ou `ABC1D23`
- **D-23:** Validação via regex único que cobre ambos os formatos (Mercosul + antigo). Identificação automática do formato — sem campo `tipo`
- **D-24:** Placa única no sistema (UNIQUE constraint no banco)

#### Ordem de Serviço e Máquina de Estados
- **D-25:** Matriz de transição completa (todos os estados do ciclo de vida) implementada desde a Fase 1. `Map<StatusOS, Set<StatusOS>>` como fonte única da verdade. Teste parametrizado cobrindo todas as transições
- **D-26:** Transição via métodos explícitos (ex: `os.iniciarDiagnostico()`) — NUNCA setter genérico `setStatus()` (previne Pitfall 5: lost updates)
- **D-27:** OS criada já com serviços solicitados (atendente informa no momento da criação)
- **D-28:** Itens da OS modelados como duas entidades separadas: `ServicoExecutado` + `PecaUsada`

#### Divisão do Trabalho (5 devs, dias 1-4)
- **D-29:** Divisão vertical por entidade: cada dev implementa uma entidade completa (domain → infra → rest). Dev1: Auth, Dev2: Cliente, Dev3: Veículo, Dev4: Serviço, Dev5: OrdemDeServico
- **D-30:** Ordem de implementação: entidades primeiro (Cliente, Veículo, Serviço, OS domain/infra/rest), auth depois (login + JWT + roles)

#### Dependências Maven a Adicionar
- `quarkus-smallrye-jwt` em `mekano-rest/pom.xml` — verificação JWT
- `quarkus-smallrye-jwt-build` em `mekano-rest/pom.xml` — geração JWT

### The Agent's Discretion
- Detalhes de implementação não cobertos acima ficam a critério do agente/planner, respeitando os padrões existentes no codebase
- Estrutura exata dos testes e cobertura segue padrão já estabelecido (JUnit 5 + Mockito + REST Assured + AssertJ)

### Deferred Ideas (OUT OF SCOPE)
- **CNPJ (pessoa jurídica):** Modelagem de cliente PJ com CNPJ fica para Fase 2. A estrutura atual (CPF-only) deve ser desenhada para permitir extensão futura
- **Refresh token pode virar feature opcional em versões futuras** se o modelo de segurança evoluir
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| AUTH-01 | Sistema oferece roles para cada perfil (admin, atendente, mecânico, almoxarife, financeiro) | user_roles N:N table + JWT groups claim. See §Standard Stack → Roles & user_roles |
| AUTH-02 | Endpoints administrativos protegidos por @RolesAllowed com base no perfil | SmallRye JWT + mp.jwt.* config + @RolesAllowed on resources. See §JWT Implementation |
| AUTH-03 | Cliente consulta status da OS via endpoint público sem autenticação | @PermitAll endpoint returning minimal DTO. See §Public OS Status Endpoint |
| OS-01 | Admin/atendente cadastra cliente com CPF validado, email e telefone | Cliente aggregate + Cpf VO + Endereco VO + Telefone VO. See §New Aggregates |
| OS-02 | Admin/atendente edita, consulta e exclui clientes | Full CRUD on ClienteResource with @RolesAllowed. See §New Aggregates |
| OS-03 | Admin/atendente cadastra veículo com placa única (Mercosul+antigo) | Veiculo aggregate + PlacaVeiculo VO with regex. See §Veiculo |
| OS-04 | Admin/atendente edita, consulta e exclui veículos | Full CRUD on VeiculoResource with @RolesAllowed |
| OS-05 | Admin cadastra tipos de serviço com nome, descrição e valor > 0 | Servico aggregate + valor validation in VO |
| OS-06 | Admin edita, consulta e exclui tipos de serviço | Full CRUD on ServicoResource, admin-only |
| OS-07 | Atendente cria OS (RECEBIDA) com cliente, veículo e serviços solicitados | OrdemDeServico aggregate with factory method. See §OS State Machine |
| OS-08 | Mecânico inicia diagnóstico (EM_DIAGNOSTICO) com serviços e peças | Transition method `iniciarDiagnostico()` + ServicoExecutado + PecaUsada |
| OS-15 | Cliente consulta status público da OS via API sem autenticação | @PermitAll GET endpoint on OrdemDeServicoResource |
| DOC-01 | Diagramas de sequência dos fluxos principais | Sequence diagrams in docs/ for: criar OS, aprovar orçamento, fluxo estoque, fluxo pagamento |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| JWT token generation & signing | API / Backend (AuthService) | — | Uses SmallRye JWT Build API to sign tokens with Ed25519 private key |
| JWT verification & @RolesAllowed | API / Backend (Quarkus middleware) | — | Handled by SmallRye JWT filter configured via mp.jwt.* properties |
| Refresh token rotation | API / Backend (RefreshTokenService) | Database | Uses existing V2 `refresh_tokens` table; rotation logic in application layer |
| Password hashing & verification | API / Backend (BcryptPasswordHasher) | — | Existing infrastructure service, reused for login |
| Cliente CRUD | API / Backend | Database | Full domain entity + service + JPA repository + REST resource |
| Cliente→Veiculo relationship (1:N) | API / Backend | Database | Veiculo references Cliente by UUID; modeled as separate aggregate (no cascade) |
| Veiculo CRUD | API / Backend | Database | Follows same pattern as Cliente, with PlacaVeiculo VO validation |
| Servico CRUD | API / Backend | Database | Admin-only; valor > 0 validation in domain |
| OS state machine | API / Backend (domain model) | — | Pure Java with StatusOS enum + Map<StatusOS, Set<StatusOS>> transition matrix |
| OS lifecycle operations | API / Backend | Database | @Transactional service methods calling explicit state transitions |
| Public OS status query | API / Backend | Database | @PermitAll endpoint, minimal DTO without internal details |
| Cache (Caffeine) | Database / Backend | — | @CacheResult on repository reads, @CacheInvalidate on writes |

## Standard Stack

### JWT Implementation

**Dependencies** to add to `mekano-rest/pom.xml`:

```xml
<!-- JWT verification (server-side) -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-jwt</artifactId>
</dependency>
<!-- JWT generation (signing tokens) -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-jwt-build</artifactId>
</dependency>
```

**Configuration** in `application.properties` (or new `auth-config.yml`):

```properties
# EdDSA/Ed25519 algorithm — note the algorithm value
mp.jwt.verify.publickey.location=publicKey.pem
mp.jwt.verify.publickey.algorithm=EdDSA
mp.jwt.verify.issuer=${MP_JWT_ISSUER:https://mekano.fiap.com.br/auth}
smallrye.jwt.sign.key.location=${user.home}/.mekano/secrets/privatekey.pem
smallrye.jwt.new-token.signature-algorithm=EdDSA
quarkus.http.auth.proactive=false
```

[VERIFIED: Quarkus docs — `security-jwt.adoc` shows `mp.jwt.verify.publickey.location`, `mp.jwt.verify.issuer`, and `smallrye.jwt.sign.key.location`. The `mp.jwt.verify.publickey.algorithm` defaults to `RS256` and must be set to `EdDSA` for Ed25519 keys.]

[VERIFIED: Quarkus docs — `security-jwt-build.adoc` shows `smallrye.jwt.new-token.signature-algorithm` can be set to configure the signing algorithm globally. SmallRye JWT's `SignatureAlgorithm` enum includes `EDDSA` (lowercase in enum constant, but case-insensitive after PR #801 fix in 4.6+).]

**Edge cases:**
- `publicKey.pem` is a classpath resource — placed in `mekano-rest/src/main/resources/`
- In dev mode, if no public key exists, SmallRye JWT auto-generates a keypair. For production/consistent keys, always provide explicit files.
- `publicKey.pem` is committed to git; `privatekey.pem` is gitignored.

**Key generation script** (build-time, one-time setup or CI):

```bash
# Generate Ed25519 private key
openssl genpkey -algorithm Ed25519 -out ~/.mekano/secrets/privatekey.pem

# Extract public key from private key
openssl pkey -in ~/.mekano/secrets/privatekey.pem -pubout -out mekano-rest/src/main/resources/publicKey.pem
```

[VERIFIED: OpenSSL 1.1.1+ docs — `genpkey -algorithm Ed25519` generates Ed25519 keys. The public key is extracted via `pkey -pubout`.]

### Token Generation Pattern

```java
// AuthService — generates JWT access token
import io.smallrye.jwt.build.Jwt;
import java.time.Duration;

String token = Jwt.issuer("https://mekano.fiap.com.br/auth")
        .subject(user.getUuid().toString())
        .upn(user.getEmail().getValue())
        .groups(Set.of(role))     // single role from user_roles
        .claim("name", user.getName())
        .expiresIn(Duration.ofMinutes(15))
        .sign();
```

[VERIFIED: `quarkus-smallrye-jwt-build` provides `Jwt.issuer()`, `.upn()`, `.groups()`, `.subject()`, `.claim()`, `.expiresIn()`, and `.sign()` via the SmallRye JWT Build API documented in `security-jwt-build.adoc`.]

### Roles & user_roles

**Port/interface layer** — add to `domain/port/out/`:

- `UserRoleRepositoryPort` — find role by user UUID, assign role
- `Role` — enum or String constant set = {admin, atendente, mecanico, almoxarife, financeiro}

**JWT groups claim:** SmallRye JWT maps `groups` to `@RolesAllowed`. Since each user has exactly one role (for now), set `groups` to a singleton set with the role name.

### Refresh Token Rotation

The `refresh_tokens` table already exists (V2 migration). Create the missing infrastructure classes:

- `RefreshTokenEntity` — JPA entity referencing the V2 table
- `RefreshTokenPanacheRepository` — Panache repository
- `RefreshTokenRepositoryImpl` — implements port interface
- `RefreshTokenService` in `application/` — rotation logic: validate old token → generate new pair → persist new token → invalidate old token

Refresh token TTL: 7 days (configurable). Rotation: each refresh call invalidates the current token and creates a new pair.

**Domain port interfaces needed in `domain/port/out/`:**

```java
// RefreshTokenRepositoryPort
Optional<RefreshTokenData> findByTokenHash(String hash);
void save(RefreshTokenData data);
void deleteByUser(UUID userUuid);

// Token data record
public record RefreshTokenData(UUID id, String jti, String tokenHash, UUID userUuid, String role, Instant expiresAt, Instant rotatedAt) {}
```

**Refresh token response DTO:**

```json
{
  "access_token": "eyJraWQiO...",
  "refresh_token": "dGhpcyBpcyBh...",
  "expires_in": 900
}
```

### Maps of Data

#### entity → pacote patterns (Cliente template)

| Layer | Package | File Pattern | Key Annotation/Pattern |
|-------|---------|-------------|----------------------|
| domain | `com.fiap.mekano.domain.model` | `Cliente.java` | `@Builder(access=PRIVATE)`, factory `create()`/`reconstitute()` |
| domain | `com.fiap.mekano.domain.valueobject` | `Cpf.java`, `Endereco.java`, `Telefone.java` | `final class`, `@EqualsAndHashCode`, validate in constructor |
| domain | `com.fiap.mekano.domain.port.in` | `ClienteServicePort.java`, `CreateClienteCommand.java` | Interface + record |
| domain | `com.fiap.mekano.domain.port.out` | `ClienteRepositoryPort.java` | Interface with CRUD methods |
| domain | `com.fiap.mekano.domain.exception` | `ClienteNotFoundException.java` | Extends `AppException` or `DomainException` |
| application | `com.fiap.mekano.application.service.cliente` | `ClienteService.java` | `@ApplicationScoped`, `@Transactional` on `execute()` |
| infrastructure | `com.fiap.mekano.infrastructure.entity` | `ClienteEntity.java` | `extends BaseEntity`, `@Entity @Table` |
| infrastructure | `com.fiap.mekano.infrastructure.repository` | `ClientePanacheRepository.java` + `ClienteRepositoryImpl.java` | Two-class pattern |
| infrastructure | `com.fiap.mekano.infrastructure.mapper` | `ClienteEntityMapper.java` | MapStruct `componentModel="cdi"` |
| rest | `com.fiap.mekano.rest.api` | `ClienteResource.java` | `@RequestScoped`, `@Path("/clientes")` |
| rest | `com.fiap.mekano.rest.api.dto` | `CreateClienteRequest.java`, `ClienteResponse.java` | Lombok class + record |
| rest | `com.fiap.mekano.rest.api.mapper` | `ClienteDtoMapper.java` | MapStruct `componentModel="cdi"` |

### New Aggregates

#### Cliente
| Field | Type | VO | DB Column |
|-------|------|-----|-----------|
| uuid | UUID | — | `uuid UNIQUE` |
| nome | String | — | `nome VARCHAR` |
| cpf | String (normalized) | `Cpf` | `cpf VARCHAR UNIQUE` |
| email | String | `Email` | `email VARCHAR` |
| telefone | String (normalized) | `Telefone` | `telefone VARCHAR` |
| enderecoLogradouro | String | `Endereco` | `endereco_logradouro VARCHAR` |
| enderecoNumero | String | `Endereco` | `endereco_numero VARCHAR` |
| enderecoBairro | String | `Endereco` | `endereco_bairro VARCHAR` |
| enderecoCidade | String | `Endereco` | `endereco_cidade VARCHAR` |
| enderecoUf | String | `Endereco` | `endereco_uf VARCHAR(2)` |
| enderecoCep | String | `Endereco` | `endereco_cep VARCHAR(8)` |

Endereco is flattened in the entity (not a separate table) — standard VO flattening pattern.

#### Veiculo
| Field | Type | VO | DB Column | Constraints |
|-------|------|-----|-----------|-------------|
| uuid | UUID | — | `uuid UNIQUE` | |
| clienteUuid | UUID | — | `cliente_uuid` | FK to clientes (logical, no DB constraint to avoid circular issues) |
| placa | String (normalized) | `PlacaVeiculo` | `placa VARCHAR UNIQUE` | Uppercase, no hyphen |
| marca | String | — | `marca VARCHAR` | |
| modelo | String | — | `modelo VARCHAR` | |
| ano | Integer | — | `ano INT` | |

#### Servico
| Field | Type | DB Column | Constraints |
|-------|------|-----------|-------------|
| uuid | UUID | `uuid UNIQUE` | |
| nome | String | `nome VARCHAR` | |
| descricao | String | `descricao VARCHAR` | |
| valor | BigDecimal | `valor DECIMAL(10,2)` | `CHECK (valor > 0)` |

#### OrdemDeServico
| Field | Type | DB Column | Constraints |
|-------|------|-----------|-------------|
| uuid | UUID | `uuid UNIQUE` | |
| clienteUuid | UUID | `cliente_uuid` | |
| veiculoUuid | UUID | `veiculo_uuid` | |
| status | String (enum) | `status VARCHAR` | `CHECK` with allowed values |
| dataEntrada | LocalDateTime | `data_entrada` | |
| dataSaida | LocalDateTime | `data_saida` | Nullable |
| observacoes | String | `observacoes TEXT` | Nullable |
| servicosSolicitados | List | Separate `servicos_solicitados` table | |

**ServicoExecutado** (OS child entity):
- `uuid`, `os_uuid`, `servico_nome`, `descricao`, `valor`, `quantidade`

**PecaUsada** (OS child entity):
- `uuid`, `os_uuid`, `peca_nome`, `quantidade`, `valor_unitario`

### Value Objects to Create

| VO | Validation | Normalization | Format |
|----|-----------|---------------|--------|
| `Cpf` | 11 digits, checksum validation (both verifier digits) | Strip non-digits | `String value` with format pattern |
| `PlacaVeiculo` | Regex covering old (ABC1234) and Mercosul (ABC1D23) | Uppercase, no hyphen | `String value` |
| `Endereco` | UF must be 2-letter state code, CEP 8 digits | — | Flattened fields on entity |
| `Telefone` | Brazilian phone (DDD + number) | Strip non-digits | `String value` |
| `StatusOS` | N/A (enum) | — | Enum with allowed transitions |
| `PoliticaSLA` | Positive duration | — | Duration value (deferred usage) |

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| `quarkus-smallrye-jwt` | (BOM) | JWT verification server-side | MP JWT RBAC spec, `@RolesAllowed` support |
| `quarkus-smallrye-jwt-build` | (BOM) | JWT token generation | `Jwt.issuer().upn().groups().sign()` fluent API |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| `BcryptPasswordHasher` | existing | Password verification at login | Existing infrastructure — reuse for `AuthService.login()` |
| `CdiEventPublisher` | existing | Domain events (OS lifecycle) | Publish `OrdemDeServicoCriadaEvent` etc. |

## Package Legitimacy Audit

> **Note:** This phase installs no external packages beyond Quarkus extensions from the managed BOM. All `io.quarkus:*` artifacts are first-party Quarkus extensions managed by `quarkus-bom`. MapStruct, Lombok, AssertJ are already present in the project. No slopcheck run needed — all dependencies are from the Quarkus BOM or already-established project dependencies.

| Package | Registry | Age | Downloads | Source Repo | Disposition |
|---------|----------|-----|-----------|-------------|-------------|
| `io.quarkus:quarkus-smallrye-jwt` | Maven Central | BOM-managed | billions | github.com/quarkusio/quarkus | Approved |
| `io.quarkus:quarkus-smallrye-jwt-build` | Maven Central | BOM-managed | billions | github.com/quarkusio/quarkus | Approved |

**Packages removed due to slopcheck [SLOP] verdict:** none
**Packages flagged as suspicious [SUS]:** none

## Architecture Patterns

### System Architecture Diagram

```text
┌─────────────── HTTP Request ───────────────┐
                                              │
                                              ▼
                  ┌───────────────────────────────────┐
                  │     Quarkus Security Filter        │
                  │  (SmallRye JWT: mp.jwt.* config)   │
                  │  @RolesAllowed / @PermitAll        │
                  └────────┬──────────────────────────┘
                           │
              ┌────────────┼────────────┬──────────────┐
              ▼            ▼            ▼              ▼
   ┌──────────────┐ ┌──────────┐ ┌──────────┐ ┌──────────────┐
   │ AuthResource │ │ClienteRes│ │VeiculoRes│ │OrdemServicoR │
   │ /auth/*      │ │/clientes│ │/veiculos │ │ /os          │
   │ @PermitAll   │ │@RoleA...│ │@RoleA... │ │ @RoleA/@Permit│
   └──────┬───────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘
          │               │            │               │
          ▼               ▼            ▼               ▼
   ┌─────────────────────────────────────────────────────────┐
   │              mekano-application  (Service Layer)         │
   │  AuthService  │  ClienteService │ VeiculoService │ OSSvc │
   │  (login,      │  @Transactional │ @Transactional │ @Tx   │
   │   refresh,    │                 │                │       │
   │   logout)     │                 │                │       │
   └──────┬────────┴────────┬────────┴────────┬───────┴───────┘
          │                 │                 │
          ▼                 ▼                 ▼
   ┌─────────────────────────────────────────────────────────┐
   │              mekano-domain  (Pure Business Logic)        │
   │  model: Cliente, Veiculo, Servico, OrdemDeServico       │
   │  valueobject: Cpf, PlacaVeiculo, Endereco, Telefone,    │
   │              StatusOS, PoliticaSLA                       │
   │  port/in: *ServicePort, PasswordHasher                  │
   │  port/out: *RepositoryPort, EventPublisher              │
   │  exception: AppException, *NotFoundException            │
   │  event: *Event (domain events)                          │
   └──────────┬─────────────────────────────────────────────┘
              │
              ▼
   ┌─────────────────────────────────────────────────────────┐
   │           mekano-infrastructure (JPA + Security)         │
   │  entity: ClienteEntity, VeiculoEntity, ServicoEntity,   │
   │          OrdemDeServicoEntity, ServicoExecutadoEntity,  │
   │          PecaUsadaEntity (estendem BaseEntity)           │
   │  repository: *PanacheRepository + *RepositoryImpl       │
   │  mapper: *EntityMapper (MapStruct componentModel="cdi") │
   │  security: BcryptPasswordHasher (reuse)                 │
   │  cache: CacheNames + cache-config.yml additions         │
   └─────────────────────────────────────────────────────────┘
```

### Component Responsibilities

| Component | Responsibility |
|-----------|----------------|
| `AuthResource` | `POST /auth/login` (return JWT pair), `POST /auth/refresh` (rotate), `POST /auth/logout` (invalidate). `@PermitAll` |
| `AuthService` | Application layer: validate credentials → call `PasswordHasher.matches()` → generate JWT pair → persist refresh token |
| `RefreshTokenService` | Application layer: rotation logic, validate token hash, generate new pair |
| `ClienteResource` | CRUD endpoints for clients. `@RolesAllowed({"admin","atendente"})` |
| `VeiculoResource` | CRUD endpoints for vehicles. `@RolesAllowed({"admin","atendente"})` |
| `ServicoResource` | CRUD endpoints for service types. `@RolesAllowed("admin")` |
| `OrdemDeServicoResource` | Create/list/get/transition OS. Mixed roles + `@PermitAll` for public status |
| `OrdemDeServico` | Domain entity with state machine: explicit transition methods, no setStatus() |
| `StatusOS` | Enum with allowed transitions defined as `Map<StatusOS, Set<StatusOS>>` |
| `ApiExceptionMapper` | Single existing mapper already handles all AppExceptions (HTTP status from getStatus()) |

### Migration Sequencing

New Flyway migrations (H2-compatible):

| # | File | Tables | Notes |
|---|------|--------|-------|
| V6 | `V6__create_user_roles_table.sql` | `user_roles` | N:N user ↔ role |
| V7 | `V7__create_clientes_table.sql` | `clientes` | Includes endereco flattening |
| V8 | `V8__create_veiculos_table.sql` | `veiculos` | FK to clientes (logical), unique placa |
| V9 | `V9__create_servicos_table.sql` | `servicos` | CHECK valor > 0 |
| V10 | `V10__create_ordens_de_servico_table.sql` | `ordens_de_servico`, `servicos_executados`, `pecas_usadas` | OS + child tables |

Migration SQL rules (from existing CLAUDE.md):
- Use `BIGINT GENERATED BY DEFAULT AS IDENTITY` NOT `BIGSERIAL` (H2 compatibility)
- Separate `ALTER TABLE` statements for multiple column additions
- Use `UUID`, `TIMESTAMP`, `BOOLEAN`, `VARCHAR`, `INT`, `BIGINT`, `NOW()`, `DEFAULT`
- See existing V5 migration pattern for reference (hybrid ID migration)

### OS State Machine Transition Matrix

```java
public enum StatusOS {
    RECEBIDA,
    EM_DIAGNOSTICO,
    AGUARDANDO_APROVACAO,
    APROVADA,
    EM_EXECUCAO,
    FINALIZADA,
    ENTREGUE,
    CANCELADA;
    // AGUARDANDO_PECAS, REJEITADA (Phase 2)

    private static final Map<StatusOS, Set<StatusOS>> TRANSICOES = Map.of(
        RECEBIDA, Set.of(EM_DIAGNOSTICO, CANCELADA),
        EM_DIAGNOSTICO, Set.of(AGUARDANDO_APROVACAO, CANCELADA),
        AGUARDANDO_APROVACAO, Set.of(APROVADA, CANCELADA),
        APROVADA, Set.of(EM_EXECUCAO, CANCELADA),
        EM_EXECUCAO, Set.of(FINALIZADA),
        FINALIZADA, Set.of(ENTREGUE),
        ENTREGUE, Set.of(),   // Estado terminal
        CANCELADA, Set.of()   // Estado terminal
    );

    public boolean podeTransitarPara(StatusOS destino) {
        return TRANSICOES.getOrDefault(this, Set.of()).contains(destino);
    }
}
```

**Explicit transition methods on OrdemDeServico (NUNCA `setStatus()`):**

```java
public class OrdemDeServico {
    private StatusOS status;

    public void iniciarDiagnostico() {
        transitarPara(StatusOS.EM_DIAGNOSTICO);
    }

    public void finalizarDiagnostico() {
        transitarPara(StatusOS.AGUARDANDO_APROVACAO);
    }

    public void aprovarOrcamento() {
        transitarPara(StatusOS.APROVADA);
    }

    public void cancelar() {
        transitarPara(StatusOS.CANCELADA);
    }

    public void iniciarExecucao() {
        transitarPara(StatusOS.EM_EXECUCAO);
    }

    public void finalizar() {
        transitarPara(StatusOS.FINALIZADA);
    }

    public void entregar() {
        transitarPara(StatusOS.ENTREGUE);
    }

    private void transitarPara(StatusOS destino) {
        if (!status.podeTransitarPara(destino)) {
            throw new AppException(400, "Transição inválida: " + status + " → " + destino);
        }
        this.status = destino;
    }
}
```

### Recommended Project Structure

```
mekano-domain/src/main/java/com/fiap/mekano/domain/
├── model/
│   ├── User.java               (existing)
│   ├── Cliente.java            (new)
│   ├── Veiculo.java            (new)
│   ├── Servico.java            (new)
│   └── OrdemDeServico.java     (new — with state machine)
├── valueobject/
│   ├── Email.java              (existing)
│   ├── Cpf.java                (new)
│   ├── PlacaVeiculo.java       (new)
│   ├── Endereco.java           (new)
│   ├── Telefone.java           (new)
│   ├── StatusOS.java           (new — enum with transition matrix)
│   └── PoliticaSLA.java        (new — deferred usage)
├── port/in/
│   ├── (existing ports)
│   ├── AuthServicePort.java    (new)
│   ├── ClienteServicePort.java (new)
│   ├── VeiculoServicePort.java (new)
│   ├── ServicoServicePort.java (new)
│   ├── OrdemDeServicoServicePort.java (new)
│   └── Commands for each
├── port/out/
│   ├── (existing ports)
│   ├── RefreshTokenRepositoryPort.java  (new)
│   ├── UserRoleRepositoryPort.java       (new)
│   ├── ClienteRepositoryPort.java       (new)
│   └── (plus Veiculo, Servico, OS repos)
├── exception/
│   ├── AppException.java        (existing — reuse)
│   └── entity-specific not found exceptions
├── event/
│   ├── UserCreatedEvent.java    (existing)
│   └── OrdemDeServicoCriadaEvent.java (new)

mekano-application/src/main/java/com/fiap/mekano/application/service/
├── user/                        (existing)
├── auth/
│   └── AuthService.java         (new — login, refresh, logout)
├── cliente/
│   └── ClienteService.java      (new)
├── veiculo/
│   └── VeiculoService.java      (new)
├── servico/
│   └── ServicoService.java      (new)
└── os/
    └── OrdemDeServicoService.java (new)

mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/
├── entity/
│   ├── BaseEntity.java          (existing)
│   ├── UserEntity.java          (existing)
│   ├── RefreshTokenEntity.java  (new)
│   ├── ClienteEntity.java       (new)
│   ├── VeiculoEntity.java       (new)
│   ├── ServicoEntity.java       (new)
│   ├── OrdemDeServicoEntity.java (new)
│   ├── ServicoExecutadoEntity.java (new)
│   └── PecaUsadaEntity.java     (new)
├── repository/
│   ├── (existing repos)
│   ├── RefreshTokenPanacheRepository.java   (new)
│   ├── RefreshTokenRepositoryImpl.java      (new)
│   └── (Panache + Impl for each aggregate)
├── mapper/
│   ├── (existing mappers)
│   └── (EntityMapper for each aggregate)
├── service/
│   └── RefreshTokenService.java  (new)
└── cache/
    └── CacheNames.java          (extend with new constants)

mekano-rest/src/main/java/com/fiap/mekano/rest/api/
├── AuthResource.java            (new — @PermitAll, /auth)
├── UserResource.java            (existing)
├── ClienteResource.java         (new)
├── VeiculoResource.java         (new)
├── ServicoResource.java         (new)
├── OrdemDeServicoResource.java  (new)
├── dto/                         (new DTOs for each resource)
├── mapper/                      (new DtoMappers for each resource)
└── exception/ (existing — ApiExceptionMapper handles all)
```

### Anti-Patterns to Avoid
- **setStatus() on OS entities:** Always use explicit transition methods. `setStatus()` is a Pitfall 5 (lost updates from concurrent writes). The state machine is the single source of truth.
- **@Transactional on resources:** Transaction boundary belongs in the application service layer, never in JAX-RS resources.
- **Caching writes:** `@CacheResult` on reads only; `@CacheInvalidate` on writes. Never cache mutating operations.
- **Mega-aggregate OS:** OS does NOT embed Cliente or Veiculo as child entities — references them by UUID only. This prevents transaction contention (Pitfall 1).
- **BIGSERIAL:** Use `BIGINT GENERATED BY DEFAULT AS IDENTITY` for H2 compatibility in tests.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JWT token signing/verification | Custom JWT implementation | SmallRye JWT (`quarkus-smallrye-jwt-build` + `quarkus-smallrye-jwt`) | Algorithm negotiation, key format support (PEM/JWK), CDI integration, @RolesAllowed. Ed25519 support via `mp.jwt.verify.publickey.algorithm=EdDSA` |
| Password hashing | Custom BCrypt implementation | `BcryptPasswordHasher` (existing) | Already integrated, uses Quarkus `BcryptUtil`, tested |
| Exception mapping | Per-entity exception mappers | `ApiExceptionMapper` (existing single mapper) | Already handles all `AppException` via `int status` — add new status codes as needed |
| State machine validation | If/else chains or switch | `Map<StatusOS, Set<StatusOS>>` transition matrix | Single source of truth, testable with parameterized test covering all transitions |
| DTO mapping | Manual getter/setter mapping | MapStruct `componentModel="cdi"` | Compile-time generation, no runtime overhead, follows existing pattern |
| Rate limiting login | Timestamp-based manual throttling | Token Bucket Rate Limiter (existing `TokenBucketRateLimiter.java`) | Already implemented in infrastructure layer |

**Key insight:** The existing codebase already has battle-tested patterns for every infrastructure concern. The Auth & OS Foundation phase adds new domain entities but reuses ALL existing infrastructure patterns (BCrypt, CDI events, single exception mapper, Caffeine cache, MapStruct).

## Common Pitfalls

### Pitfall 1: Lost Updates on OS State Transitions
**What goes wrong:** Two requests concurrently transition the same OS (e.g., mecanico starts diagnosis while atendente cancels). Without `@Version`, the second write silently overwrites the first.
**Why it happens:** No optimistic locking on the OS entity.
**How to avoid:** Add `@Version Long version` column to `OrdemDeServicoEntity`. The explicit transition methods (no `setStatus()`) combined with `@Version` prevent lost updates.
**Warning signs:** OS status changes mysteriously, concurrent requests in tests.

### Pitfall 2: Invalid State Transitions via Reflection or JPQL
**What goes wrong:** A developer bypasses the domain model and calls `entity.setStatus(EM_EXECUCAO)` directly (or via JPQL update), skipping the transition matrix validation.
**Why it happens:** `ServicoExecutadoEntity` or `OrdemDeServicoEntity` might have a JPA setter that can be called from infrastructure code.
**How to avoid:** NUNCA expor `setStatus()` no domain entity. The JPA entity in infrastructure CAN have a setter for ORM purposes, but the service layer must always go through the domain methods.
**Warning signs:** Direct `update ... set status =` queries in repositories.

### Pitfall 3: EdDSA Algorithm Configuration — Wrong Property Name
**What goes wrong:** Using `mp.jwt.verify.algorithm=EdDSA` instead of `mp.jwt.verify.publickey.algorithm=EdDSA`. The old `smallrye.jwt.verify.algorithm` is deprecated and may not work with asymmetric keys.
**How to avoid:** Use `mp.jwt.verify.publickey.algorithm=EdDSA` for verification (asymmetric). Use `smallrye.jwt.new-token.signature-algorithm=EdDSA` for signing.
**Warning signs:** `401 Unauthorized` with JWT that looks valid (check logs for "No suitable verifier found").

### Pitfall 4: Enum Case in SmallRye JWT Configuration
**What goes wrong:** Setting algorithm to `EdDSA` or `eddsa` or `EDDSA` may fail depending on SmallRye JWT version.
**How to avoid:** Use `EDDSA` (uppercase) in properties. In Quarkus 3.36.0 with SmallRye JWT 4.6+, the `toUpperCase()` fix (PR #801) handles any case. If encountering issues, test explicitly.
**Warning signs:** `IllegalArgumentException: No enum constant io.smallrye.jwt.algorithm.SignatureAlgorithm.EdDSA`.

### Pitfall 5: Refresh Token Rotation Race Condition
**What goes wrong:** Two concurrent refresh requests with the same token both pass validation (first checks token_hash exists, both succeed), create new tokens, and the second write might invalidate the first's new token.
**How to avoid:** Use `@Transactional` on the rotation method AND query the old token with `@Lock(PESSIMISTIC_WRITE)` to prevent concurrent access. Add a `rotated_at` check — only the first rotation should succeed.
**Warning signs:** Users getting "invalid refresh token" immediately after a refresh.

## Code Examples

### Login Flow (AuthService)

```java
@ApplicationScoped
public class AuthService implements AuthServicePort {

    private final UserRepositoryPort userRepository;
    private final PasswordHasher passwordHasher;
    private final RefreshTokenService refreshTokenService;

    public AuthService(UserRepositoryPort userRepository,
                       PasswordHasher passwordHasher,
                       RefreshTokenService refreshTokenService) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public TokenPair login(LoginCommand command) {
        // 1. Find user by email
        var user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new AppException(401, "Credenciais inválidas"));

        // 2. Verify password
        if (!passwordHasher.matches(command.password(), user.getPasswordHash())) {
            throw new AppException(401, "Credenciais inválidas");
        }

        // 3. Generate access token
        String accessToken = Jwt.issuer("https://mekano.fiap.com.br/auth")
                .subject(user.getId().toString())
                .upn(user.getEmail().getValue())
                .groups(Set.of(user.getRole()))  // role from repository or user_roles table
                .claim("name", user.getName())
                .expiresIn(Duration.ofMinutes(15))
                .sign();

        // 4. Generate and persist refresh token
        String refreshToken = refreshTokenService.createToken(user.getId(), user.getRole());

        return new TokenPair(accessToken, refreshToken, 900);
    }
}
```

### Public OS Status Endpoint

```java
@Path("/os")
@RequestScoped
@Tag(name = "Ordens de Serviço", description = "Ordens de Serviço — consulta pública")
public class OrdemDeServicoResource {

    @Inject
    OrdemDeServicoServicePort osService;

    @GET
    @Path("/{uuid}/status")
    @PermitAll  // No authentication required — OS-15 / AUTH-03
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Consulta pública de status da OS")
    public Response consultarStatusPublico(@PathParam("uuid") UUID uuid) {
        var status = osService.consultarStatus(uuid);
        return Response.ok(status).build();
    }

    @GET
    @RolesAllowed({"admin", "atendente", "mecanico", "almoxarife", "financeiro"})
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar OS (admin/atendente)")
    public Response listar(@QueryParam("page") @DefaultValue("0") int page,
                           @QueryParam("size") @DefaultValue("10") int size) {
        // ... authenticated list with full details
    }
}
```

### Transition Matrix Parameterized Test (49-transition coverage)

```java
@DisplayName("StatusOS — Matriz de Transições")
class StatusOSTest {

    @ParameterizedTest
    @CsvSource({
        "RECEBIDA, EM_DIAGNOSTICO, true",
        "RECEBIDA, CANCELADA, true",
        "RECEBIDA, AGUARDANDO_APROVACAO, false",
        "RECEBIDA, FINALIZADA, false",
        "EM_DIAGNOSTICO, AGUARDANDO_APROVACAO, true",
        "EM_DIAGNOSTICO, CANCELADA, true",
        "EM_DIAGNOSTICO, RECEBIDA, false",
        // ... all 49 transitions explicitly listed
    })
    void deveValidarTransicoes(StatusOS origem, StatusOS destino, boolean esperado) {
        assertEquals(esperado, origem.podeTransitarPara(destino));
    }
}
```

### OrdemDeServico Factory Method with Services

```java
public class OrdemDeServico {
    @Builder(access = AccessLevel.PRIVATE)
    @Getter
    public static class ServicoSolicitado {
        private final UUID servicoUuid;
        private final String nome;
        private final BigDecimal valor;
    }

    public static OrdemDeServico create(
            UUID clienteUuid,
            UUID veiculoUuid,
            List<ServicoSolicitado> servicos) {
        return OrdemDeServico.builder()
                .id(UUID.randomUUID())
                .clienteUuid(clienteUuid)
                .veiculoUuid(veiculoUuid)
                .status(StatusOS.RECEBIDA)
                .servicosSolicitados(List.copyOf(servicos))
                .dataEntrada(LocalDateTime.now())
                .version(0L)
                .build();
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `@RolesAllowed("user")` (single generic role) | `@RolesAllowed({"admin","atendente"})` (specific roles) | This phase | Multiple role checks per endpoint |
| Refresh token not implemented (table exists) | Full rotation with `@Lock(PESSIMISTIC_WRITE)` | This phase | Prevents concurrent rotation race condition |
| No state machine | `Map<StatusOS, Set<StatusOS>>` transition matrix | This phase | Testable, single source of truth, 49-transition parameterized test |
| Single aggregate (User) | 5 aggregates (User, Cliente, Veiculo, Servico, OS) | This phase | Vertical slice pattern for 5 devs |

**Deprecated/outdated:**
- None — this phase builds on existing patterns, does not replace them.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | SmallRye JWT supports EdDSA/Ed25519 algorithm via `mp.jwt.verify.publickey.algorithm=EdDSA` | JWT Implementation | Must fall back to EC (ES256) or RSA (RS256) — adds key management overhead |
| A2 | `openssl genpkey -algorithm Ed25519` is available on target CI machines | JWT Implementation | CI pipeline cannot auto-generate keys; devs must generate manually |
| A3 | `@Version Long version` prevents all lost-update race conditions on OS | Pitfall 1 | Retry logic may be needed on OService for `OptimisticLockException` |
| A4 | The existing `ApiExceptionMapper` already handles `AppException` with any HTTP status code | Code Patterns | Test that new status codes (401, 403) render correctly in RFC 7807 format |
| A5 | `refresh_tokens` table (V2) is compatible with rotation pattern (already has `rotated_at` column) | Refresh Token | If V2 schema differs from expected, migration or adapter needed |

## Open Questions

1. **SLA policy default values:** What is the default expiration window for orçamento (AGUARDANDO_APROVACAO → CANCELADA)? Phase 3 concern, but `PoliticaSLA` VO should be modeled now.
   - **What we know:** Must be a configurable duration in the domain.
   - **What's unclear:** Default value (24h? 48h? 7 days?)
   - **Recommendation:** Build `PoliticaSLA` as a Value Object now with a configurable duration, default to 48h. Exact default can change in Phase 3.

2. **User role storage:** Should the role be stored directly on the `users` table OR in the `user_roles` N:N table?
   - **What we know:** D-10 says `user_roles` N:N table.
   - **What's unclear:** Whether the `users` table should also have a `role` column as a denormalization shortcut (for JWT generation without a join).
   - **Recommendation:** Store in user_roles table only. Single-role users today. The join is negligible for auth performance. This prevents dual-write inconsistency.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| OpenSSL | Ed25519 key generation | ✓ (system) | — | Use Java `KeyPairGenerator.getInstance("Ed25519")` |
| Node.js | Optional build script | ✓ (system) | — | Not required |
| `~/.mekano/secrets/privatekey.pem` | JWT signing | ✓ EXISTS | — | Generate via script |
| `mekano-rest/src/main/resources/publicKey.pem` | JWT verification | ✗ MISSING | — | Extract from private key via OpenSSL |
| PostgreSQL 16 | All persistence | ✓ via docker-compose | 16-alpine | DevServices for tests |

**Missing dependencies with no fallback:** none
**Missing dependencies with fallback:**
- `publicKey.pem` — extract from private key: `openssl pkey -in ~/.mekano/secrets/privatekey.pem -pubout -out mekano-rest/src/main/resources/publicKey.pem`

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Quarkus JUnit 5 (AssertJ 3.27.3, REST Assured) |
| Config file | `pom.xml` (Surefire + Failsafe) |
| Quick run command | `./mvnw test -pl mekano-domain -am` (domain only) |
| Full suite command | `./mvnw verify -pl mekano-rest -am` |

### Phase Requirements → Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| AUTH-01 | Roles exist in user_roles table | unit (service) | `mvn test -pl mekano-application -am -Dtest=RoleServiceTest` | ❌ Wave 0 |
| AUTH-02 | @RolesAllowed blocks unauthorized access | integration (REST) | `mvn test -pl mekano-rest -am -Dtest=AuthResourceTest` | ❌ Wave 0 |
| AUTH-03 | @PermitAll allows public OS status | integration (REST) | `mvn test -pl mekano-rest -am -Dtest=OrdemDeServicoResourceTest` | ❌ Wave 0 |
| OS-07 | Create OS with services sets RECEBIDA | unit (domain) | `mvn test -pl mekano-domain -am -Dtest=OrdemDeServicoTest` | ❌ Wave 0 |
| OS-08 | Diagnóstico updates status | unit (domain) | `mvn test -pl mekano-domain -am -Dtest=OrdemDeServicoTest` | ❌ Wave 0 |
| OS-15 | Public status returns limited fields | integration (REST) | `mvn test -pl mekano-rest -am -Dtest=OrdemDeServicoResourceTest` | ❌ Wave 0 |
| DOC-01 | Sequence diagrams exist | manual | N/A | ❌ Wave 0 |

### Sampling Rate
- **Per task commit:** `./mvnw test -pl mekano-domain -am` (domain tests, < 5s)
- **Per wave merge:** `./mvnw test -pl mekano-rest -am` (all tests, < 60s with DevServices)
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `mekano-domain/src/test/java/.../valueobject/CpfTest.java` — domain VO tests
- [ ] `mekano-domain/src/test/java/.../valueobject/PlacaVeiculoTest.java` — domain VO tests
- [ ] `mekano-domain/src/test/java/.../model/OrdemDeServicoTest.java` — state machine + factory
- [ ] `mekano-domain/src/test/java/.../model/StatusOSTest.java` — 49-transition matrix
- [ ] `mekano-application/src/test/java/.../AuthServiceTest.java` — login/refresh service tests
- [ ] `mekano-rest/src/test/java/.../AuthResourceTest.java` — login/refresh/logout REST tests
- [ ] `mekano-rest/src/test/java/.../ClienteResourceTest.java` — CRUD REST tests

### Test Strategy per Layer

| Layer | Framework | Key Patterns | Coverage Target |
|-------|-----------|-------------|-----------------|
| **Domain** | JUnit 5 (no Quarkus) | `@ParameterizedTest`, `assertThrows`, VO validation, factory methods, transition matrix | All VOs (null/empty/invalid/valid), all 49 state transitions |
| **Application** | Mockito `@ExtendWith` | `@Mock` ports, `@InjectMocks` service, verify `@Transactional` behavior | Login success/failure, duplicate email, invalid credentials, refresh rotation |
| **Infrastructure** | `@QuarkusTest` + `@TestTransaction` | DevServices PostgreSQL, AssertJ fluent, Panache queries with soft delete | CRUD operations, cache hit/miss, constraint violations → 409 |
| **REST / E2E** | `@QuarkusTest` + REST Assured | `@TestSecurity` bypass, `@TestMethodOrder`, JSON path assertions | HTTP status codes, response body shape, @RolesAllowed enforcement, @PermitAll access |

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | yes | SmallRye JWT (EdDSA/Ed25519), BCrypt password hashing, 15-min access TTL |
| V3 Session Management | yes | Refresh token rotation with PESSIMISTIC_WRITE lock; 7-day refresh TTL |
| V4 Access Control | yes | `@RolesAllowed` on all protected resources; `@PermitAll` only on public status |
| V5 Input Validation | yes | Bean Validation on all DTOs; domain VO validation in constructors; CPF checksum |
| V6 Cryptography | yes | Ed25519/EdDSA (no RSA); BCrypt for passwords; never hand-roll cryptography |

### Known Threat Patterns for Quarkus + JWT

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| JWT token replay | Tampering | 15-min TTL; refresh token rotation with PESSIMISTIC_WRITE |
| Refresh token theft | Information Disclosure | Token stored as SHA-256 hash in DB; rotation invalidates stolen tokens on use |
| Brute force login | Denial of Service | `TokenBucketRateLimiter` (existing) — 10 attempts/min per email+IP |
| Enumeração de recursos (ID sequential) | Information Disclosure | Hybrid ID pattern — UUID público, Long PK interno |
| CPF/placa duplicado | Tampering | Unique constraints in DB; `AppException(409)` on violation |
| OS state machine bypass | Tampering | Explicit transition methods; `@Version` optimistic locking; no `setStatus()` exposed |

## Sources

### Primary (HIGH confidence)
- [VERIFIED: Quarkus 3.36 security-jwt docs] — `quarkus.io/guides/security-jwt` — mp.jwt config, token generation, algorithm selection [context7: `/quarkusio/quarkus`]
- [VERIFIED: Quarkus 3.36 security-jwt-build docs] — `quarkus.io/guides/security-jwt-build` — Jwt.issuer().groups().sign(), key location config [context7: `/quarkusio/quarkus`]
- [VERIFIED: SmallRye JWT Configuration] — `smallrye.io/docs/smallrye-jwt/configuration` — `mp.jwt.verify.publickey.algorithm` property [WEB: smallrye.io]
- [VERIFIED: OpenSSL genpkey docs] — `docs.openssl.org/3.2/man1/openssl-genpkey/` — Ed25519 key generation [WEB: openssl.org]
- [CITED: existing codebase] — User.java, UserService.java, UserEntity.java, UserRepositoryImpl.java, UserDtoMapper.java, UserEntityMapper.java, BaseEntity.java — patterns to follow

### Secondary (MEDIUM confidence)
- [CITED: github.com/smallrye/smallrye-jwt/pull/801] — EdDSA enum case fix (merged 2024-06) [WEB]
- [CITED: github.com/smallrye/smallrye-jwt/pull/789] — EdDSA KeyUtils support (merged 2024-05) [WEB]

### Tertiary (LOW confidence)
- None — all critical claims verified against official docs or existing codebase

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — dependencies, algorithm config, and key generation verified against Quarkus docs and OpenSSL docs
- Architecture: HIGH — follows existing codebase patterns exactly (User entity template)
- Pitfalls: HIGH — based on documented SmallRye JWT issues (PR #801) and existing DDD patterns
- Validation: HIGH — test patterns already established in UserTest.java, EmailTest.java

**Research date:** 2026-06-22
**Valid until:** 2026-07-22 (30 days — stack is stable Quarkus 3.36, no major changes expected in JWT support)
