# Mekano — Roadmap

**Milestone:** v1 — Clean Architecture Quarkus API  
**Granularidade:** Standard (8 fases)  
**Cobertura:** 48/48 requirements mapeados ✓  
**Idioma:** Português (pt-BR)  
**Atualizado:** 2025-07-15

---

## Fases

- [ ] **Phase 1: Esqueleto Maven Multi-Módulo** — Converte scaffold Quarkus em projeto multi-módulo com 4 sub-módulos, BOM, Jandex, docker-compose e application.properties base
- [ ] **Phase 2: Módulo Domain** — Entidades puras, Value Objects, interfaces de Port e exceções de domínio; zero dependências de framework
- [ ] **Phase 3: Módulo Application** — Caso de uso `CreateUserUseCase` orquestrando via ports; testável sem container
- [ ] **Phase 4: Módulo Infrastructure** — Entidade JPA Panache, `UserRepositoryImpl`, MapStruct CDI, migrations Flyway e datasource PostgreSQL configurado
- [ ] **Phase 5: Módulo Adapter** — REST Resource, DTOs Request/Response, MapStruct, ExceptionMappers HTTP, testes REST Assured — fluxo end-to-end completo
- [ ] **Phase 6: Observabilidade** — Health checks liveness/readiness, métricas Prometheus e OpenAPI completamente documentado
- [ ] **Phase 7: Tolerância a Falhas** — Anotações SmallRye Fault Tolerance (`@Retry`, `@Timeout`, `@CircuitBreaker`) na camada de infraestrutura
- [ ] **Phase 8: Fundação JWT** — SmallRye JWT configurado com `mp.jwt.*`, chave PKCS#8 e `@RolesAllowed` placeholder no `UserResource`

---

## Detalhes das Fases

### Phase 1: Esqueleto Maven Multi-Módulo

**Goal**: Estrutura multi-módulo Maven compila sem erros, todas as extensões Quarkus necessárias declaradas no módulo `adapter`, docker-compose disponível e `./mvnw quarkus:dev -pl mekano-adapter -am` sobe a aplicação.

**Motivo**: Nenhuma outra fase pode começar sem a estrutura de módulos correta. Erros de configuração no `pom.xml` — packaging errado, `quarkus-maven-plugin` no lugar errado, BOM ausente — silenciosamente quebram CDI, Jandex e o build nativo. Esta fase é a fundação inegociável de tudo.

**Depende de**: Nada (primeira fase)

**Requirements**: MOD-01, MOD-02, MOD-03, MOD-04, MOD-05, MOD-06, MOD-07, MOD-08, DEV-01, DEV-02, DEV-03, EXT-01, EXT-02, EXT-03, EXT-04, EXT-05, EXT-10

**Planos**:

1. **[CRÍTICO]** Refatorar `pom.xml` raiz:
   - Alterar `<packaging>quarkus</packaging>` → `<packaging>pom</packaging>`
   - Remover bloco `<build><plugins>` contendo `quarkus-maven-plugin`
   - Adicionar `<modules>`: `mekano-domain`, `mekano-application`, `mekano-infrastructure`, `mekano-adapter`
   - Adicionar `<dependencyManagement>` com `quarkus-bom:3.36.0` (import), `mapstruct:1.6.3`, `lombok:1.18.36`, `lombok-mapstruct-binding:0.2.0`
   - Adicionar `<pluginManagement>` com `jandex-maven-plugin:3.5.3` (io.smallrye) e `maven-compiler-plugin:3.15.0`
   - Mover `GreetingResource.java` para `mekano-adapter` (ou deletar — é apenas scaffold)

2. Criar `mekano-domain/pom.xml`:
   - `<packaging>jar</packaging>`
   - `<parent>` apontando para root
   - Dependência `lombok` com `<scope>provided</scope>` e `<optional>true</optional>`
   - Nenhum outro `<dependency>` de runtime
   - Criar diretórios: `src/main/java/com/fiap/mekano/domain/`

3. Criar `mekano-application/pom.xml`:
   - `<packaging>jar</packaging>`
   - Dependência: `mekano-domain` (compile)
   - `jandex-maven-plugin` configurado com `<goal>jandex</goal>` em `generate-sources`
   - Criar diretórios: `src/main/java/com/fiap/mekano/application/`

4. Criar `mekano-infrastructure/pom.xml`:
   - `<packaging>jar</packaging>`
   - Dependência: `mekano-domain` (compile)
   - `jandex-maven-plugin` configurado (igual ao application)
   - Criar diretórios: `src/main/java/com/fiap/mekano/infrastructure/`

5. Criar `mekano-adapter/pom.xml`:
   - `<packaging>quarkus</packaging>`
   - `quarkus-maven-plugin` com `<extensions>true</extensions>` — **ÚNICO módulo com este plugin**
   - Dependências: `mekano-domain` (compile), `mekano-application` (compile), `mekano-infrastructure` (compile)
   - Todas as extensões declaradas: `quarkus-rest-jackson` (EXT-01), `quarkus-hibernate-orm-panache` (EXT-02), `quarkus-flyway` (EXT-03), `quarkus-smallrye-openapi` (EXT-04), `quarkus-hibernate-validator` (EXT-05), `quarkus-jdbc-postgresql` (EXT-10)
   - *Nota: EXT-06, EXT-07 adicionados na Fase 6; EXT-08 na Fase 7; EXT-09 na Fase 8*
   - Criar diretório: `src/main/java/com/fiap/mekano/adapter/`

6. **[PARALELO com Plano 5]** Criar `docker-compose.yml` na raiz:
   - Serviço `postgres` — imagem `postgres:16-alpine`, porta `5432:5432`
   - Variáveis: `POSTGRES_DB=mekano`, `POSTGRES_USER`, `POSTGRES_PASSWORD` via env
   - Health check: `pg_isready -U ${POSTGRES_USER}`
   - Volume nomeado para persistência local

7. **[PARALELO com Plano 5]** Criar `mekano-adapter/src/main/resources/application.properties`:
   - Perfil `%dev`: `quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/mekano`, credenciais hardcoded para dev
   - Perfil `%prod`: datasource via `${DB_URL}`, `${DB_USER}`, `${DB_PASSWORD}`
   - Perfil `%test`: deixar sem URL (ativa DevServices automaticamente)
   - `quarkus.swagger-ui.always-include=true`

**Critérios UAT (o que deve ser VERDADE ao fim desta fase)**:

1. `./mvnw clean install -DskipTests` na raiz compila todos os 4 módulos sem erros — output final mostra `BUILD SUCCESS` para cada sub-módulo
2. `./mvnw quarkus:dev -pl mekano-adapter -am` inicia sem `ClassNotFoundException`, `UnsatisfiedResolutionException` ou erros de dependência circular
3. `docker-compose up -d` sobe PostgreSQL na porta 5432 e `docker-compose ps` reporta status `healthy`
4. `curl http://localhost:8080/q/dev-ui/` retorna página HTML da Dev UI do Quarkus (modo dev ativo)

**Plans**: 3 planos

- [ ] 01-01-PLAN.md — Refatoração do root pom.xml (packaging=pom, modules, BOM, pluginManagement) + deleção do scaffold
- [ ] 01-02-PLAN.md — POMs dos 4 sub-módulos (domain/application/infrastructure/adapter) + estrutura de diretórios com .gitkeep
- [ ] 01-03-PLAN.md — docker-compose.yml (PostgreSQL 16-alpine) + application.properties (perfis dev/prod/test)

---

### Phase 2: Módulo Domain

**Goal**: Camada de domínio pura compila com zero dependências de framework; Value Objects rejeitam valores inválidos; todos os testes unitários do módulo passam em menos de 3 segundos.

**Motivo**: Domain é o núcleo imutável do Clean Architecture. Sem ele, `application`, `infrastructure` e `adapter` não têm contratos para implementar. Deve ser 100% puro antes de qualquer outra camada ser construída — qualquer vazamento de framework aqui contamina todas as camadas superiores.

**Depende de**: Fase 1

**Requirements**: DOM-01, DOM-02, DOM-03, DOM-04, DOM-05, DOM-06

**Planos**:

1. Criar hierarquia de exceções em `domain/exception/`:
   - `DomainException extends RuntimeException` (abstract, sem imports HTTP)
   - `UserAlreadyExistsException extends DomainException`
   - `UserNotFoundException extends DomainException`
   - `InvalidEmailException extends DomainException`

2. **[PARALELO com Plano 1]** Criar Value Object `Email` em `domain/valueobject/Email.java`:
   - Construtor `public Email(String value)` valida formato via regex; lança `InvalidEmailException` se inválido
   - Campo `private final String value`; getter `getValue()`; `equals`/`hashCode` por valor
   - Nenhuma anotação Lombok em campos públicos de API (imutabilidade garantida por construtor)

3. **[PARALELO com Plano 1]** Criar entidade `User` em `domain/model/User.java`:
   - Campos: `UUID id`, `String name`, `Email email`, `String passwordHash`, `LocalDateTime createdAt`
   - Factory method estático `User.create(String name, String email, String rawPassword)` — instancia `Email` VO, atribui `UUID.randomUUID()`, define `createdAt = LocalDateTime.now()`
   - Sem anotações `@Entity`, `@Column`, `@Id` — POJO puro
   - Lombok `@Getter`, `@Builder` permitidos como `provided`

4. **[PARALELO com Planos 1+2+3]** Criar interfaces de Port em `domain/port/`:
   - `port/out/UserRepositoryPort.java` — métodos: `save(User)`, `findById(UUID): Optional<User>`, `findByEmail(String): Optional<User>`, `existsByEmail(String): boolean`
   - `port/in/CreateUserInputPort.java` — método: `execute(CreateUserCommand): User` *(CreateUserCommand definido na Fase 3, mas interface pode referenciar o pacote antecipadamente ou usar um tipo simples agora)*
   - *Alternativa limpa: `CreateUserInputPort` usa parâmetros primitivos e é refinado na Fase 3*

5. Criar testes JUnit 5 em `mekano-domain/src/test/java/`:
   - `EmailTest`: `new Email("user@fiap.br")` → válido; `new Email("invalido")` → lança `InvalidEmailException`; `new Email("")` → lança `InvalidEmailException`; `new Email(null)` → lança
   - `UserTest`: `User.create(...)` com email válido → retorna `User` com `id` e `createdAt` populados; campos imutáveis após criação

**Critérios UAT (o que deve ser VERDADE ao fim desta fase)**:

1. `./mvnw test -pl mekano-domain` executa e passa em menos de 3 segundos — **sem inicializar container Quarkus** (sem linhas `Starting application` no log)
2. `./mvnw dependency:tree -pl mekano-domain` lista **zero** dependências de runtime além de Lombok (provided)
3. `new Email("invalido")` lança `InvalidEmailException` com mensagem `"Invalid email format: invalido"`
4. `./mvnw compile -pl mekano-domain` — `grep -r "jakarta.persistence\|jakarta.ws.rs\|io.quarkus" mekano-domain/src/main/java` retorna **zero resultados**

**Plans**: TBD

---

### Phase 3: Módulo Application

**Goal**: `CreateUserUseCase` orquestra as regras de negócio (verificação de duplicidade, criação de entidade, persistência) invocando apenas ports; testável com Mockito sem nenhum container ativo.

**Motivo**: A camada application é onde a política de negócio vive. Validar que a lógica funciona de forma completamente isolada — sem JPA, sem HTTP, sem Quarkus runtime — confirma que o design de Ports & Adapters está correto e que o use case pode ser testado em milissegundos.

**Depende de**: Fase 2

**Requirements**: APP-01, APP-02, APP-03, APP-04

**Planos**:

1. Criar `CreateUserCommand` em `application/usecase/user/CreateUserCommand.java`:
   - Record Java: `record CreateUserCommand(String name, String email, String password)`
   - Sem anotações Jakarta (`@NotBlank` etc. — validação é responsabilidade do adapter)
   - Refinar `CreateUserInputPort` na Fase 2 (se necessário) para referenciar este tipo

2. Criar `CreateUserUseCase` em `application/usecase/user/CreateUserUseCase.java`:
   - Implementa `CreateUserInputPort`
   - Anotado com `@ApplicationScoped` (único CDI permitido nesta camada)
   - Injeta `UserRepositoryPort` via construtor (ou `@Inject`)
   - Lógica: `if (repo.existsByEmail(cmd.email())) throw UserAlreadyExistsException` → `User user = User.create(...)` → `repo.save(user)` → `return user`
   - `@Transactional` **PROIBIDO** nesta camada

3. Criar testes JUnit 5 + Mockito em `mekano-application/src/test/java/`:
   - `CreateUserUseCaseTest` com `@ExtendWith(MockitoExtension.class)`
   - `@Mock UserRepositoryPort repositoryPort`; `@InjectMocks CreateUserUseCase useCase`
   - Cenário **sucesso**: `existsByEmail` retorna `false`, `save` captura `User` retornado; verificar nome, email e que `passwordHash` foi gerado
   - Cenário **duplicidade**: `existsByEmail` retorna `true`; assert que `UserAlreadyExistsException` é lançada e `save` nunca é chamado (`verify(repo, never()).save(any())`)

4. **[PARALELO com Plano 3]** Auditoria de imports proibidos:
   - `grep -r "jakarta.persistence\|jakarta.ws.rs\|org.hibernate\|io.quarkus" mekano-application/src/main/java` deve retornar zero resultados
   - `grep -r "@Transactional" mekano-application/src/main/java` deve retornar zero resultados
   - Documentar resultado como comentário no PR/commit

**Critérios UAT (o que deve ser VERDADE ao fim desta fase)**:

1. `./mvnw test -pl mekano-application` passa em menos de 5 segundos — sem linhas `Quarkus` no log de teste
2. Teste de duplicidade confirma que `UserRepositoryPort.save(...)` é chamado **zero vezes** quando email já existe
3. `./mvnw dependency:tree -pl mekano-application` não contém `quarkus-hibernate-orm`, `quarkus-rest-jackson` nem qualquer artefato `jakarta.persistence`
4. Código fonte de `CreateUserUseCase` não contém a string `@Transactional` (verificável por grep)

**Plans**: TBD

---

### Phase 4: Módulo Infrastructure

**Goal**: `UserRepositoryImpl` persiste e recupera `User` corretamente contra PostgreSQL real via DevServices; migrations Flyway executam automaticamente no startup; mappers MapStruct geram objetos sem campos `null`.

**Motivo**: Infrastructure é a fase com mais armadilhas silenciosas do projeto: Jandex ausente quebra CDI, a ordem dos `annotationProcessorPaths` quebra MapStruct, e o naming convention do Flyway silenciosamente pula migrations sem erro. Isolar e validar aqui evita misturar bugs de persistência com bugs de HTTP mais tarde.

**Depende de**: Fase 2 (contratos de domain), Fase 1 (Quarkus runtime + DevServices PostgreSQL)

**Requirements**: INF-01, INF-02, INF-03, INF-04, INF-05, INF-06, INF-07

**Planos**:

1. **[CRÍTICO]** Configurar `maven-compiler-plugin` em `mekano-infrastructure/pom.xml` com `annotationProcessorPaths` na **ordem exata** (desvio causa campos `null` no mapper sem erro de compilação):
   ```xml
   <annotationProcessorPaths>
     <path><!-- lombok --></path>
     <path><!-- lombok-mapstruct-binding:0.2.0 --></path>
     <path><!-- mapstruct-processor --></path>
   </annotationProcessorPaths>
   ```
   Adicionar dependências de runtime: `mapstruct` (compile), `lombok` (provided)

2. Criar `UserEntity` em `infrastructure/entity/UserEntity.java`:
   - `@Entity @Table(name = "users")`
   - Estende `PanacheEntityBase`
   - `@Id @GeneratedValue(strategy = UUID)` ou `@Id UUID id`
   - `@Column(nullable=false) String name`; `@Column(unique=true, nullable=false) String email`; `@Column(name="password_hash") String passwordHash`; `@Column(name="created_at") LocalDateTime createdAt`
   - Nenhuma referência a classes do módulo `domain`

3. **[PARALELO com Plano 2]** Criar migration `V1__create_users_table.sql` em `mekano-infrastructure/src/main/resources/db/migration/`:
   - Naming: **capital V + double underscore** (`V1__` não `v1_` nem `V1_`)
   - DDL: `CREATE TABLE users (id UUID PRIMARY KEY, name VARCHAR(255) NOT NULL, email VARCHAR(255) NOT NULL UNIQUE, password_hash VARCHAR(255) NOT NULL, created_at TIMESTAMP NOT NULL)`

4. **[PARALELO com Plano 2]** Criar `UserEntityMapper` em `infrastructure/mapper/UserEntityMapper.java`:
   - `@Mapper(componentModel = "cdi")` — **NÃO** `"spring"`
   - Interface com métodos: `UserEntity toEntity(User user)` e `User toDomain(UserEntity entity)`
   - Campo `email` em `User` é VO `Email` → mapper precisa de `@Mapping(expression = "java(...)")` ou método default de conversão

5. Criar `UserRepositoryImpl` em `infrastructure/repository/UserRepositoryImpl.java`:
   - `@ApplicationScoped`
   - Implementa `UserRepositoryPort`
   - `PanacheRepositoryBase<UserEntity, UUID>`
   - Injeta `UserEntityMapper`
   - Métodos de escrita (`save`) anotados com `@Transactional` — **ÚNICO local válido para esta anotação**
   - `findByEmail`: usa `find("email", email).firstResultOptional()`

6. Atualizar `mekano-adapter/src/main/resources/application.properties`:
   - `quarkus.flyway.migrate-at-start=true`
   - `quarkus.hibernate-orm.schema-management.strategy=validate`
   - `%test.quarkus.flyway.clean-at-start=true`
   - DevServices: `quarkus.datasource.devservices.image-name=docker.io/library/postgres:16-alpine`

7. **[PARALELO com Planos 5+6]** Criar `@QuarkusTest` para `UserRepositoryImpl` em `mekano-infrastructure/src/test/java/`:
   - Sem URL de datasource no perfil test (ativa DevServices automaticamente)
   - `@TestTransaction` para rollback por teste
   - Cenário: `save(user)` → `findByEmail(email)` retorna `Optional` presente com mesmo email
   - Cenário: `existsByEmail` retorna `false` para email inexistente

**Critérios UAT (o que deve ser VERDADE ao fim desta fase)**:

1. `./mvnw test -pl mekano-infrastructure -am` executa migrations e testes passam com PostgreSQL via DevServices (Docker precisa estar ativo)
2. Log de startup de testes contém a linha `"Successfully applied 1 migration to schema"` do Flyway
3. `UserEntityMapper` converte `User → UserEntity → User` sem nenhum campo `null` (verificado em teste dedicado de mapeamento)
4. `./mvnw dependency:tree -pl mekano-infrastructure` **não** contém artefatos `quarkus-rest-jackson` nem `jakarta.ws.rs`
5. `grep -r "@Transactional" mekano-application/src/main/java` retorna zero resultados (confirmar que a anotação não vazou para application)

**Plans**: TBD

---

### Phase 5: Módulo Adapter

**Goal**: `POST /users` retorna `201 Created` com JSON de `UserResponse` (sem `passwordHash`); email duplicado retorna `409 Conflict`; payload inválido retorna `400 Bad Request`; Swagger UI acessível em `/q/swagger-ui`; todos os testes REST Assured passam.

**Motivo**: Esta é a entrega central do v1 — o fluxo completo `HTTP Request → Adapter → Application → Infrastructure → PostgreSQL → HTTP Response` funcionando end-to-end. Somente nesta fase o sistema pode ser exercitado como produto.

**Depende de**: Fases 1, 2, 3, 4

**Requirements**: ADP-01, ADP-02, ADP-03, ADP-04, ADP-05, ADP-06, ADP-07, ADP-08

**Planos**:

1. **[CRÍTICO]** Configurar `maven-compiler-plugin` em `mekano-adapter/pom.xml` com mesmos `annotationProcessorPaths` (lombok → binding → mapstruct-processor) — necessário pois adapter também usa MapStruct

2. Criar DTOs em `adapter/dto/`:
   - `dto/request/CreateUserRequest.java` — campos com `@NotBlank String name`, `@Email @NotBlank String email`, `@NotNull @Size(min=6) String password`; anotação `@Schema` para OpenAPI
   - `dto/response/UserResponse.java` — record Java: `record UserResponse(UUID id, String name, String email, LocalDateTime createdAt)` — **sem passwordHash**

3. **[PARALELO com Plano 2]** Criar `UserDtoMapper` em `adapter/mapper/UserDtoMapper.java`:
   - `@Mapper(componentModel = "cdi")`
   - `CreateUserCommand toCommand(CreateUserRequest request)`
   - `UserResponse toResponse(User user)` — campo `email` do VO `Email` requer `@Mapping(expression = "java(user.getEmail().getValue())")`

4. **[PARALELO com Plano 2]** Criar ExceptionMappers em `adapter/exception/`:
   - `UserAlreadyExistsExceptionMapper implements ExceptionMapper<UserAlreadyExistsException>`:
     - Anotado com `@Provider` **e** `@ApplicationScoped`
     - Retorna `Response.status(409).entity(new ErrorResponse("User already exists")).build()`
   - `UserNotFoundExceptionMapper implements ExceptionMapper<UserNotFoundException>`:
     - Mesmas anotações; retorna `Response.status(404)...`
   - `ErrorResponse` record: `record ErrorResponse(String message)`

5. Criar `UserResource` em `adapter/rest/UserResource.java`:
   - `@Path("/users") @RequestScoped @Tag(name = "Users")`
   - Injeta `CreateUserInputPort` (nunca `CreateUserUseCase` diretamente)
   - Injeta `UserDtoMapper`
   - Método: `@POST @Consumes(JSON) @Produces(JSON) public Response create(@Valid CreateUserRequest request)` → mapeia para command → executa use case → mapeia para response → `Response.created(uri).entity(response).build()`

6. **[PARALELO com Plano 5]** Configurar OpenAPI em `application.properties`:
   - `mp.openapi.info.title=Mekano API`
   - `mp.openapi.info.version=1.0.0`
   - `mp.openapi.info.description=Clean Architecture REST API — FIAP Software Architecture`
   - Adicionar `@Operation`, `@APIResponse(responseCode="201")`, `@APIResponse(responseCode="400")`, `@APIResponse(responseCode="409")` em `UserResource`

7. Criar `@QuarkusTest` em `mekano-adapter/src/test/java/`:
   - `UserResourceTest` — usa REST Assured:
     - `POST /users` com body JSON válido → assert `HTTP 201`, body contém `"id"` e `"email"`, **não** contém `"passwordHash"`
     - Mesmo request repetido → assert `HTTP 409`, body contém `"message"`
     - `POST /users` com `email: "invalido"` → assert `HTTP 400`
     - `POST /users` com body `{}` (campos obrigatórios ausentes) → assert `HTTP 400`

**Critérios UAT (o que deve ser VERDADE ao fim desta fase)**:

1. `POST http://localhost:8080/users` com body `{"name":"Ana","email":"ana@fiap.br","password":"abc123"}` retorna `HTTP 201 Created` com JSON contendo `id`, `name`, `email`, `createdAt` — campo `passwordHash` **ausente**
2. Mesmo request repetido imediatamente retorna `HTTP 409 Conflict` com body JSON `{"message": "..."}`
3. `POST /users` com `"email": "naoemail"` retorna `HTTP 400 Bad Request` com lista de erros de validação
4. `GET http://localhost:8080/q/swagger-ui` retorna HTML completo com documentação do endpoint `POST /users`, incluindo schemas de request e responses 201/400/409
5. `./mvnw test -pl mekano-adapter -am` executa todos os testes REST Assured com `BUILD SUCCESS`

**Plans**: TBD

---

### Phase 6: Observabilidade

**Goal**: `/q/health` reporta `UP` com checks de liveness e readiness (incluindo datasource); `/q/metrics` expõe métricas no formato Prometheus; OpenAPI completamente documentado com tags e descrições.

**Motivo**: Health checks e métricas são pré-requisitos para qualquer ambiente de produção ou demonstração acadêmica funcional. Como são extensões completamente additive (não modificam código existente), fazem sentido como fase independente após o core end-to-end estar validado.

**Depende de**: Fase 5

**Requirements**: DEV-04, DEV-05, EXT-06, EXT-07

**Planos**:

1. Adicionar extensões ao `mekano-adapter/pom.xml`:
   - `quarkus-smallrye-health` (EXT-06)
   - `quarkus-micrometer-registry-prometheus` (EXT-07)

2. **[PARALELO com Plano 1]** Verificar e configurar health checks:
   - Quarkus adiciona datasource readiness automaticamente via `quarkus-smallrye-health` — sem código adicional necessário
   - Validar `/q/health/live` e `/q/health/ready` após startup
   - Opcional: criar `ApplicationLivenessCheck implements HealthCheck` com `@Liveness` se check customizado for desejado

3. **[PARALELO com Plano 1]** Verificar e opcionalmente enriquecer métricas:
   - `/q/metrics` exposto automaticamente após adição da extensão Prometheus
   - Opcional: injetar `MeterRegistry` em `CreateUserUseCase` e incrementar `Counter` `user.created.total` a cada criação bem-sucedida
   - Validar que métricas JVM, HTTP e datasource aparecem no endpoint

4. **[PARALELO com Planos 1+2+3]** Completar documentação OpenAPI (revisão final):
   - Garantir `@Tag(name="Users", description="Gerenciamento de usuários")` em `UserResource`
   - Garantir `@Operation(summary=..., description=...)` em cada endpoint
   - Garantir `@Schema(description=..., example=...)` nos campos de `CreateUserRequest` e `UserResponse`
   - Validar que `/q/openapi` exporta YAML válido com todas as respostas documentadas

**Critérios UAT (o que deve ser VERDADE ao fim desta fase)**:

1. `GET http://localhost:8080/q/health` retorna `{"status":"UP","checks":[...]}` com status HTTP 200
2. `GET http://localhost:8080/q/health/ready` lista check de datasource PostgreSQL com status `UP`
3. `GET http://localhost:8080/q/metrics` retorna corpo texto com linhas `# HELP` e `# TYPE` no formato Prometheus (Content-Type: `text/plain`)
4. `GET http://localhost:8080/q/swagger-ui` exibe documentação completa com tags, summary, description e schemas de request/response para todos os endpoints

**Plans**: TBD

---

### Phase 7: Tolerância a Falhas

**Goal**: Operações de leitura em `UserRepositoryImpl` são protegidas por `@Retry`; timeout configurado para operações potencialmente lentas; build continua passando sem conflitos entre anotações de fault tolerance e `@Transactional`.

**Motivo**: SmallRye Fault Tolerance demonstra o padrão de resiliência na camada arquitetural correta (infrastructure) — não no domain nem no application. Isolar nesta fase permite adicionar as anotações sem risco de introduzir regressões nas camadas já validadas.

**Depende de**: Fase 4

**Requirements**: EXT-08

**Planos**:

1. Adicionar `quarkus-smallrye-fault-tolerance` ao `mekano-adapter/pom.xml`:
   - Confirmar que a extensão não estava declarada anteriormente; adicionar se ausente
   - Executar `./mvnw compile -pl mekano-adapter -am` para confirmar resolução sem conflitos

2. Anotar métodos de `UserRepositoryImpl` com políticas de fault tolerance:
   - `findByEmail(String email)` → `@Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)`
   - `findById(UUID id)` → `@Retry(maxRetries = 3)`
   - `save(User user)` → `@Timeout(value = 5, unit = ChronoUnit.SECONDS)` (escrita com timeout, sem retry para evitar duplicação)
   - Documentar em comentário Javadoc por que `@CircuitBreaker` não é aplicado aqui (PostgreSQL local não é serviço externo instável neste contexto)
   - **Atenção**: `@Transactional` + `@Retry` em mesmo método requer que `@Transactional` esteja na camada acima (use case) — confirmar que não há conflito, dado que use cases não têm `@Transactional`

3. **[PARALELO com Plano 2]** Verificar ausência de regressões:
   - `./mvnw test -pl mekano-adapter -am` deve continuar passando com `BUILD SUCCESS`
   - Verificar logs de startup por warnings de interceptor conflict entre `@Transactional` e fault tolerance

**Critérios UAT (o que deve ser VERDADE ao fim desta fase)**:

1. `UserRepositoryImpl.findByEmail(...)` tem `@Retry(maxRetries = 3)` presente — verificável por `grep -n "@Retry" mekano-infrastructure/src/main/java`
2. `./mvnw test -pl mekano-adapter -am` continua passando com `BUILD SUCCESS` — nenhuma regressão introduzida
3. Log de startup **não** exibe `WARN` relacionado a conflito de interceptores CDI entre `@Transactional` e anotações de fault tolerance
4. `./mvnw dependency:tree -pl mekano-adapter` confirma presença de `quarkus-smallrye-fault-tolerance` no classpath

**Plans**: TBD

---

### Phase 8: Fundação JWT

**Goal**: `POST /users` sem header `Authorization` retorna `HTTP 401`; com token JWT válido (PKCS#8, issuer correto) retorna `HTTP 201`; `application.properties` usa namespace `mp.jwt.*` (não `quarkus.smallrye-jwt.*`); testes de fases anteriores continuam passando via `@TestSecurity`.

**Motivo**: JWT é declarado in-scope como extensão v1 mesmo que o fluxo completo de auth (login, geração de token, refresh) seja v2. Configurar a fundação agora — com o namespace correto e o formato de chave correto — evita problemas de retroatividade descobertos tardiamente e demonstra o padrão MicroProfile JWT corretamente.

**Depende de**: Fase 5

**Requirements**: EXT-09

**Planos**:

1. Adicionar `quarkus-smallrye-jwt` ao `mekano-adapter/pom.xml`:
   - Adicionar também `quarkus-smallrye-jwt-build` em `<scope>test</scope>` para geração de tokens em testes

2. Gerar par de chaves RSA e configurar:
   - `openssl genrsa -out privateKey.pem 2048`
   - `openssl pkcs8 -topk8 -nocrypt -inform pem -in privateKey.pem -outform pem -out privateKey_pkcs8.pem`
   - `openssl rsa -pubout -in privateKey.pem -out publicKey.pem`
   - Colocar `publicKey.pem` em `mekano-adapter/src/main/resources/`
   - `privateKey_pkcs8.pem` **não** vai para resources — apenas para uso local em testes/geração

3. Configurar `application.properties` com namespace correto:
   ```properties
   mp.jwt.verify.publickey.location=publicKey.pem
   mp.jwt.verify.issuer=https://mekano.fiap.com.br/auth
   ```
   **Proibido**: `quarkus.smallrye-jwt.public-key.location` (namespace errado — causa 401 silencioso)

4. Atualizar `UserResource`:
   - Alterar anotação de escopo para `@RequestScoped` (obrigatório para injeção de claims JWT por request)
   - Adicionar `@RolesAllowed("user")` no método `POST /users` como placeholder declarativo
   - Adicionar `@Authenticated` no nível da classe ou no método, conforme preferência

5. Preservar testes existentes com `@TestSecurity`:
   - Adicionar `@TestSecurity(user = "testuser", roles = {"user"})` nos testes `UserResourceTest` existentes para que continuem passando com auth ativa
   - Criar teste adicional: request sem token → assert `HTTP 401`
   - Documentar em comentário: autenticação end-to-end completa (login, geração de token) é escopo v2

**Critérios UAT (o que deve ser VERDADE ao fim desta fase)**:

1. `POST http://localhost:8080/users` **sem** header `Authorization` retorna `HTTP 401 Unauthorized`
2. `POST http://localhost:8080/users` com token JWT válido (gerado com `SmallRyeJwtBuildApi` e chave correta) retorna `HTTP 201 Created`
3. `grep "mp.jwt.verify.publickey.location" mekano-adapter/src/main/resources/application.properties` retorna resultado — e `grep "quarkus.smallrye-jwt" application.properties` retorna **zero** resultados
4. `./mvnw test -pl mekano-adapter -am` continua passando com `BUILD SUCCESS` — testes existentes usam `@TestSecurity` para bypassar auth

**Plans**: TBD

---

## Progresso

| Fase | Planos Completos | Status | Concluída em |
|------|-----------------|--------|--------------|
| 1. Esqueleto Maven Multi-Módulo | 0/7 | Não iniciada | — |
| 2. Módulo Domain | 0/5 | Não iniciada | — |
| 3. Módulo Application | 0/4 | Não iniciada | — |
| 4. Módulo Infrastructure | 0/7 | Não iniciada | — |
| 5. Módulo Adapter | 0/7 | Não iniciada | — |
| 6. Observabilidade | 0/4 | Não iniciada | — |
| 7. Tolerância a Falhas | 0/3 | Não iniciada | — |
| 8. Fundação JWT | 0/5 | Não iniciada | — |

---

## Cobertura de Requirements

| Requirement | Descrição (resumida) | Fase | Status |
|-------------|----------------------|------|--------|
| MOD-01 | Root pom: `packaging=pom`, 4 sub-módulos | Fase 1 | Pending |
| MOD-02 | `mekano-domain`: jar, sem deps internas | Fase 1 | Pending |
| MOD-03 | `mekano-application`: jar, depende de domain | Fase 1 | Pending |
| MOD-04 | `mekano-infrastructure`: jar, depende de domain | Fase 1 | Pending |
| MOD-05 | `mekano-adapter`: packaging=quarkus | Fase 1 | Pending |
| MOD-06 | `quarkus-maven-plugin` apenas no adapter | Fase 1 | Pending |
| MOD-07 | `jandex-maven-plugin` no application + infrastructure | Fase 1 | Pending |
| MOD-08 | `quarkus-bom` via dependencyManagement no root | Fase 1 | Pending |
| DOM-01 | Entidade `User` pura (sem JPA) | Fase 2 | Pending |
| DOM-02 | Value Object `Email` com validação | Fase 2 | Pending |
| DOM-03 | Interface `UserRepositoryPort` (output port) | Fase 2 | Pending |
| DOM-04 | Interface `CreateUserInputPort` (input port) | Fase 2 | Pending |
| DOM-05 | Exceções de domínio sem imports HTTP | Fase 2 | Pending |
| DOM-06 | Zero dependências externas no módulo domain | Fase 2 | Pending |
| APP-01 | `CreateUserUseCase` com `@ApplicationScoped` | Fase 3 | Pending |
| APP-02 | Use case orquestra: duplicidade → criação → persistência | Fase 3 | Pending |
| APP-03 | `CreateUserCommand` sem anotações Jakarta | Fase 3 | Pending |
| APP-04 | Sem imports `jakarta.persistence`, `ws.rs` ou Quarkus-específicos | Fase 3 | Pending |
| INF-01 | `UserEntity` JPA (`@Entity`, `@Table`, `@Id`) | Fase 4 | Pending |
| INF-02 | `UserRepositoryImpl` via Panache | Fase 4 | Pending |
| INF-03 | `UserEntityMapper` MapStruct com `componentModel="cdi"` | Fase 4 | Pending |
| INF-04 | Migration `V1__create_users_table.sql` | Fase 4 | Pending |
| INF-05 | `quarkus.flyway.migrate-at-start=true` | Fase 4 | Pending |
| INF-06 | `lombok-mapstruct-binding:0.2.0` + ordem correta annotationProcessorPaths | Fase 4 | Pending |
| INF-07 | PostgreSQL em `application.properties` (perfis `%dev`, `%prod`) | Fase 4 | Pending |
| ADP-01 | `UserResource` `@Path("/users")` com `POST /users` | Fase 5 | Pending |
| ADP-02 | `CreateUserRequest` com validações Bean Validation | Fase 5 | Pending |
| ADP-03 | `UserResponse` sem `passwordHash` | Fase 5 | Pending |
| ADP-04 | `UserDtoMapper` MapStruct Request→Command e User→Response | Fase 5 | Pending |
| ADP-05 | `UserAlreadyExistsExceptionMapper` → HTTP 409 | Fase 5 | Pending |
| ADP-06 | `UserNotFoundExceptionMapper` → HTTP 404 | Fase 5 | Pending |
| ADP-07 | OpenAPI/Swagger funcional em `/q/swagger-ui` | Fase 5 | Pending |
| ADP-08 | `@QuarkusTest` REST Assured: 201 e 409 | Fase 5 | Pending |
| DEV-01 | `docker-compose.yml` com serviço PostgreSQL | Fase 1 | Pending |
| DEV-02 | `application.properties` com perfis `%dev` → docker-compose | Fase 1 | Pending |
| DEV-03 | `./mvnw quarkus:dev -pl adapter -am` funcional | Fase 1 | Pending |
| DEV-04 | Health check funcional em `/q/health` | Fase 6 | Pending |
| DEV-05 | Métricas em `/q/metrics` (Micrometer + Prometheus) | Fase 6 | Pending |
| EXT-01 | `quarkus-rest-jackson` no adapter | Fase 1 | Pending |
| EXT-02 | `quarkus-hibernate-orm-panache` no adapter | Fase 1 | Pending |
| EXT-03 | `quarkus-flyway` no adapter | Fase 1 | Pending |
| EXT-04 | `quarkus-smallrye-openapi` no adapter | Fase 1 | Pending |
| EXT-05 | `quarkus-hibernate-validator` no adapter | Fase 1 | Pending |
| EXT-06 | `quarkus-smallrye-health` no adapter | Fase 6 | Pending |
| EXT-07 | `quarkus-micrometer` + `quarkus-micrometer-registry-prometheus` | Fase 6 | Pending |
| EXT-08 | `quarkus-smallrye-fault-tolerance` no adapter | Fase 7 | Pending |
| EXT-09 | `quarkus-smallrye-jwt` no adapter | Fase 8 | Pending |
| EXT-10 | `quarkus-jdbc-postgresql` no adapter | Fase 1 | Pending |

**Total v1:** 48 requirements — 48 mapeados — 0 orphans ✓

---

## Armadilhas Críticas (Referência Rápida)

> Resumo das armadilhas silenciosas identificadas na pesquisa. Cada fase afetada referencia a gotcha relevante.

| # | Armadilha | Sintoma | Fase Afetada | Fix |
|---|-----------|---------|--------------|-----|
| G1 | `quarkus-maven-plugin` no POM errado | `quarkus:dev` falha ou native profile vaza | Fase 1 | Plugin apenas em `mekano-adapter/pom.xml` |
| G2 | Jandex ausente em sub-módulos | `UnsatisfiedResolutionException` no startup | Fases 1, 3 | `jandex-maven-plugin` em `application` e `infrastructure` |
| G3 | Ordem errada de `annotationProcessorPaths` | Mapper compila mas produz campos `null` | Fases 4, 5 | Lombok → `lombok-mapstruct-binding:0.2.0` → mapstruct-processor |
| G4 | Flyway naming convention errada | Migrations silenciosamente ignoradas | Fase 4 | `V1__desc.sql` — V maiúsculo + duplo underscore |
| G5 | `quarkus.flyway.migrate-at-start` padrão false | Nenhuma migration executa | Fase 4 | Setar explicitamente `=true` |
| G6 | Namespace JWT errado | Todos os endpoints retornam 401 sem erro | Fase 8 | `mp.jwt.*` (não `quarkus.smallrye-jwt.*`) |
| G7 | Chave RSA não-PKCS#8 | JWT validation silently rejects | Fase 8 | `openssl pkcs8 -topk8 -nocrypt` |
| G8 | `@ApplicationScoped` em Resource com JWT | Injeção de claims quebra por request | Fase 8 | `@RequestScoped` obrigatório em resources com JWT |
| G9 | `componentModel = "spring"` no MapStruct | Mapper CDI não injetado — `NullPointerException` | Fases 4, 5 | Sempre `componentModel = "cdi"` |
| G10 | ExceptionMapper sem `@Provider` | Exception mapper ignorado, retorna 500 | Fase 5 | `@Provider @ApplicationScoped` em cada mapper |

---

*Roadmap criado: 2025-07-15*  
*Próximo passo: `/gsd-plan-phase 1`*
