# Phase 1: Auth & OS Foundation - Context

**Gathered:** 2026-06-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Auth JWT com 5 perfis + CRUD Cliente/Veículo/Serviço + OS criação/diagnóstico + endpoint público de status + diagramas de sequência.

Auth é construído do zero como parte desta fase (não reaproveitamento de código existente — não havia implementação JWT).

Requisitos: AUTH-01, AUTH-02, AUTH-03, OS-01 a OS-08, OS-15, DOC-01 (11 requisitos).
</domain>

<decisions>
## Implementation Decisions

### Autenticação JWT
- **D-01:** Algoritmo Ed25519/EdDSA. Claim `groups` com role único (usuário tem 1 role no JWT)
- **D-02:** Access token: 15 minutos de expiração
- **D-03:** Refresh token na Fase 1 com rotação automática (cada uso gera novo par, invalida anterior). Tabela `refresh_tokens` já existe (V2 migration)
- **D-04:** Chaves Ed25519 geradas via script no build (automático). Public key em `mekano-rest/src/main/resources/publicKey.pem`, private key em `~/.mekano/secrets/privatekey.pem`
- **D-05:** Só admin cria usuários do sistema (endpoint `POST /api/v1/users` protegido com `@RolesAllowed("admin")`)
- **D-06:** JWT contém claims: `sub` (UUID), `groups` (role), `name` (nome do usuário)
- **D-07:** Issuer: `https://mekano.fiap.com.br/auth` configurado via `MP_JWT_ISSUER` env var
- **D-08:** Login identifier: email + senha (POST /api/v1/auth/login)
- **D-09:** Endpoint `POST /api/v1/auth/logout` invalida refresh token no servidor

### Roles e Permissões
- **D-10:** Tabela separada `user_roles` (N:N) — suporta múltiplos papéis por usuário no futuro. Na prática, cada usuário tem 1 role por enquanto
- **D-11:** Clientes CRUD: admin + atendente
- **D-12:** Veículos CRUD: admin + atendente
- **D-13:** Serviços (tipos de serviço) CRUD: só admin
- **D-14:** OS listar: todos os perfis; criar: admin + atendente
- **D-15:** Iniciar diagnóstico (RECEBIDA → EM_DIAGNOSTICO): mecânico + admin
- **D-16:** Endpoint público OS status: `@PermitAll` (sem autenticação)

### Modelo do Cliente
- **D-17:** Só CPF (pessoa física). CNPJ (pessoa jurídica) não está no escopo atual do projeto
- **D-18:** Value Object `Cpf` no domain — valida dígitos verificadores no construtor (padrão `Email` VO)
- **D-19:** Campos obrigatórios: nome, CPF, email, telefone
- **D-20:** Endereço completo: logradouro, número, bairro, cidade, UF, CEP (modelado como Value Object `Endereco`)
- **D-21:** Cliente pode ter múltiplos veículos (relacionamento 1:N)

### Veículo
- **D-22:** Placa armazenada normalizada: uppercase, sem hífen. Ex: `ABC1234` ou `ABC1D23`
- **D-23:** Validação via regex único que cobre ambos os formatos (Mercosul + antigo). Identificação automática do formato — sem campo `tipo`
- **D-24:** Placa única no sistema (UNIQUE constraint no banco)

### Ordem de Serviço e Máquina de Estados
- **D-25:** Matriz de transição completa (todos os estados do ciclo de vida) implementada desde a Fase 1. `Map<StatusOS, Set<StatusOS>>` como fonte única da verdade. Teste parametrizado cobrindo todas as transições
- **D-26:** Transição via métodos explícitos (ex: `os.iniciarDiagnostico()`) — NUNCA setter genérico `setStatus()` (previne Pitfall 5: lost updates)
- **D-27:** OS criada já com serviços solicitados (atendente informa no momento da criação)
- **D-28:** Itens da OS modelados como duas entidades separadas: `ServicoExecutado` + `PecaUsada`

### Divisão do Trabalho (5 devs, dias 1-4)
- **D-29:** Divisão vertical por entidade: cada dev implementa uma entidade completa (domain → infra → rest). Dev1: Auth, Dev2: Cliente, Dev3: Veículo, Dev4: Serviço, Dev5: OrdemDeServico
- **D-30:** Ordem de implementação: entidades primeiro (Cliente, Veículo, Serviço, OS domain/infra/rest), auth depois (login + JWT + roles)

### Dependências Maven a Adicionar
- `quarkus-smallrye-jwt` em `mekano-rest/pom.xml` — verificação JWT
- `quarkus-smallrye-jwt-build` em `mekano-rest/pom.xml` — geração JWT

### the agent's Discretion
- Detalhes de implementação não cobertos acima ficam a critério do agente/planner, respeitando os padrões existentes no codebase
- Estrutura exata dos testes e cobertura segue padrão já estabelecido (JUnit 5 + Mockito + REST Assured + AssertJ)
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Roadmap & Requirements
- `.planning/ROADMAP.md` §Phase 1 — Goal, success criteria, requirements, risk mitigation
- `.planning/REQUIREMENTS.md` — Full requirement specs (AUTH-01..03, OS-01..08, OS-15, DOC-01)
- `.planning/PROJECT.md` — Project constraints (10 days, 5 devs, clean architecture)
- `.planning/STATE.md` — Accumulated decisions, vertical slice strategy, blocking anti-patterns

### Codebase Patterns
- `.planning/codebase/ARCHITECTURE.md` — Module structure, data flow, patterns
- `.planning/codebase/CONVENTIONS.md` — Naming, error handling, MapStruct, VO conventions
- `.planning/codebase/STACK.md` — Technology stack, dependencies, configuration
- `.planning/codebase/INTEGRATIONS.md` — Database, auth, caching, resilience

### Domain Docs
- `docs/` — Event Storming documentation, Mermaid diagrams
- `CLAUDE.md` — Project conventions, gotchas (G1-G10), build commands

### Existing Auth Implementation (patterns a seguir)
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/User.java` — Entity pattern
- `mekano-application/src/main/java/com/fiap/mekano/application/service/user/UserService.java` — Service pattern
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/UserResource.java` — Resource pattern
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/BaseEntity.java` — Base entity with audit + soft delete
</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `BaseEntity` (infrastructure/entity): PK Long + UUID público + created_at + is_active + deleted_at + audit fields
- `AppException` (domain/exception): Exceção unificada com HTTP status — usar para todos os erros
- `ApiExceptionMapper` (rest/api/exception): Mapeia AppException para RFC 7807 Problem Details — não precisa de novo mapper
- `CdiEventPublisher` (infrastructure/event): Publicação de eventos CDI — usar para eventos de domínio (ex: OrdemDeServicoCriadaEvent)
- `BcryptPasswordHasher` (infrastructure/security): BCrypt — reutilizar para verificação de senha no login
- `CacheNames` + `cache-config.yml`: Adicionar novos caches para cada aggregate
- Two-class repository pattern: `PanacheRepository` (Panache herança) + `RepositoryImpl` (implementa port) — seguir para todos os novos aggregates
- Padrão MapStruct `componentModel = "cdi"` com `@Mapping` expressions para flatten de VOs

### Established Patterns
- Clean Architecture: domain puro (zero framework) → application (@Transactional) → infrastructure (JPA) → rest (JAX-RS)
- Hybrid ID: `Long id` PK + `UUID uuid` unique exposto em APIs
- Factory methods: `static create()` (novo) + `static reconstitute()` (restaurado da persistência)
- `@Transactional` APENAS no service layer (nunca no resource ou repository)
- Soft delete: `isActive = true` em todas as queries
- `@RequestScoped` em resources JAX-RS (obrigatório para JWT)
- `@Retry(maxRetries=3)` em reads, `@Timeout(5s)` em writes
- Paginação via query params `page`, `size`, `sort`

### Integration Points
- `mekano-rest/src/main/resources/application.properties` — Config principal (quarkus.rest.path, CORS, timezone)
- `mekano-rest/pom.xml` — Adicionar quarkus-smallrye-jwt + quarkus-smallrye-jwt-build
- `mekano-infrastructure/src/main/resources/db/migration/` — Migrations Flyway (V6+)
- `mekano-rest/src/main/resources/publicKey.pem` — Public key (gerar via script)
- `~/.mekano/secrets/privatekey.pem` — Private key (gerar via script)
</code_context>

<specifics>
## Specific Ideas

Nenhuma referência específica além dos padrões já estabelecidos no codebase. Seguir as convenções existentes (nomes de método em português nos testes, Javadoc bilingue, etc.)
</specifics>

<deferred>
## Deferred Ideas

- **CNPJ (pessoa jurídica)**: Fora do escopo atual. A estrutura atual (CPF-only) deve ser desenhada para permitir extensão futura
- **Refresh token pode virar feature opcional em versões futuras** se o modelo de segurança evoluir

None — discussion stayed within phase scope.
</deferred>

---

*Phase: 1-Auth & OS Foundation*
*Context gathered: 2026-06-22*
