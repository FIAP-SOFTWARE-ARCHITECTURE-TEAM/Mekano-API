# Phase 01: Auth & OS Foundation - Pattern Map

**Mapped:** 2026-06-22
**Files analyzed:** ~105 new files across 5 aggregates
**Analogs found:** 50 / 56 (89%)

## File Classification

| New/Modified File | Role | Data Flow | Closest Analog | Match Quality |
|---|---|---|---|---|
| **Domain — Model** | | | | |
| `Cliente.java` | model | CRUD | `User.java` | exact |
| `Veiculo.java` | model | CRUD | `User.java` | exact |
| `Servico.java` | model | CRUD | `User.java` | exact |
| `OrdemDeServico.java` | model | CRUD + state-machine | `User.java` + custom | role-match |
| **Domain — Value Objects** | | | | |
| `Cpf.java` | valueobject | validation | `Email.java` | exact |
| `PlacaVeiculo.java` | valueobject | validation | `Email.java` | exact |
| `Endereco.java` | valueobject | validation | `Email.java` | role-match |
| `Telefone.java` | valueobject | validation | `Email.java` | exact |
| `StatusOS.java` | valueobject | enum + state-machine | — | no-analog |
| `PoliticaSLA.java` | valueobject | validation | `Email.java` | role-match |
| **Domain — Ports In** | | | | |
| `ClienteServicePort.java` | port-in | CRUD | `UserServicePort.java` | exact |
| `VeiculoServicePort.java` | port-in | CRUD | `UserServicePort.java` | exact |
| `ServicoServicePort.java` | port-in | CRUD | `UserServicePort.java` | exact |
| `OrdemDeServicoServicePort.java` | port-in | CRUD+state | `UserServicePort.java` | role-match |
| `AuthServicePort.java` | port-in | request-response | — | no-analog |
| `*Command.java` (6 files) | port-in | CRUD | `CreateUserCommand.java` | exact |
| **Domain — Ports Out** | | | | |
| `ClienteRepositoryPort.java` | port-out | CRUD | `UserRepositoryPort.java` | exact |
| `VeiculoRepositoryPort.java` | port-out | CRUD | `UserRepositoryPort.java` | exact |
| `ServicoRepositoryPort.java` | port-out | CRUD | `UserRepositoryPort.java` | exact |
| `OrdemDeServicoRepositoryPort.java` | port-out | CRUD+state | `UserRepositoryPort.java` | role-match |
| `RefreshTokenRepositoryPort.java` | port-out | CRUD | `UserRepositoryPort.java` | role-match |
| `UserRoleRepositoryPort.java` | port-out | CRUD | `UserRepositoryPort.java` | role-match |
| **Domain — Events** | | | | |
| `OrdemDeServicoCriadaEvent.java` | event | event-driven | `UserCreatedEvent.java` | exact |
| **Domain — Exceptions** | | | | |
| `ClienteNotFoundException.java` | exception | — | `AppException` usage | role-match |
| `VeiculoNotFoundException.java` | exception | — | `AppException` usage | role-match |
| `ServicoNotFoundException.java` | exception | — | `AppException` usage | role-match |
| `OrdemDeServicoNotFoundException.java` | exception | — | `AppException` usage | role-match |
| **Application — Services** | | | | |
| `ClienteService.java` | service | CRUD | `UserService.java` | exact |
| `VeiculoService.java` | service | CRUD | `UserService.java` | exact |
| `ServicoService.java` | service | CRUD | `UserService.java` | exact |
| `OrdemDeServicoService.java` | service | CRUD+state | `UserService.java` | role-match |
| `AuthService.java` | service | request-response | `UserService.java` | role-match |
| `RefreshTokenService.java` | service | CRUD | `UserService.java` | role-match |
| `*Response.java` (5 files) | service | CRUD | `CreateUserResponse.java` | exact |
| **Infrastructure — Entities** | | | | |
| `ClienteEntity.java` | entity | CRUD | `UserEntity.java` | exact |
| `VeiculoEntity.java` | entity | CRUD | `UserEntity.java` | exact |
| `ServicoEntity.java` | entity | CRUD | `UserEntity.java` | exact |
| `OrdemDeServicoEntity.java` | entity | CRUD+state | `UserEntity.java` | role-match |
| `ServicoExecutadoEntity.java` | entity | CRUD | `UserEntity.java` | role-match |
| `PecaUsadaEntity.java` | entity | CRUD | `UserEntity.java` | role-match |
| `RefreshTokenEntity.java` | entity | CRUD | `BaseEntity` (no audit) | role-match |
| **Infrastructure — Repositories** | | | | |
| `*PanacheRepository.java` (5 files) | repository | CRUD | `UserPanacheRepository.java` | exact |
| `*RepositoryImpl.java` (5 files) | repository | CRUD | `UserRepositoryImpl.java` | exact |
| **Infrastructure — Mappers** | | | | |
| `*EntityMapper.java` (5 files) | mapper | CRUD | `UserEntityMapper.java` | exact |
| `CpfMapper.java`, `PlacaVeiculoMapper.java`, `TelefoneMapper.java` | mapper | CRUD | `EmailMapper.java` | exact |
| **REST — Resources** | | | | |
| `AuthResource.java` | controller | request-response | `UserResource.java` | role-match |
| `ClienteResource.java` | controller | CRUD | `UserResource.java` | exact |
| `VeiculoResource.java` | controller | CRUD | `UserResource.java` | exact |
| `ServicoResource.java` | controller | CRUD | `UserResource.java` | exact |
| `OrdemDeServicoResource.java` | controller | CRUD+public | `UserResource.java` | role-match |
| **REST — DTOs** | | | | |
| `*Request.java` (6 files) | dto | CRUD | `CreateUserRequest.java` | exact |
| `*Response.java` (6 files) | dto | CRUD | `UserResponse.java` | exact |
| `*PageResponse.java` (4 files) | dto | CRUD | `UserPageResponse.java` | exact |
| `LoginRequest.java` | dto | request-response | — | no-analog |
| `LoginResponse.java` | dto | request-response | — | no-analog |
| **REST — DTO Mappers** | | | | |
| `*DtoMapper.java` (4 files) | mapper | CRUD | `UserDtoMapper.java` | exact |
| **Config / Build** | | | | |
| Flyway V6..V10 | migration | file-I/O | `V1..V5` | exact |
| `cache-config.yml` update | config | — | existing | exact |
| `CacheNames.java` update | config | — | existing | exact |
| `mekano-rest/pom.xml` update | config | — | existing | exact |
| Key generation script | utility | — | — | no-analog |

---

## Pattern Assignments

### DOMAIN — Model Entity Template

#### `Cliente.java`, `Veiculo.java`, `Servico.java` (model, CRUD)

**Analog:** `mekano-domain/src/main/java/com/fiap/mekano/domain/model/User.java`

**Imports pattern** (lines 1-10):
```java
package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.valueobject.Email;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;
```

**Core pattern — `@Builder(access = PRIVATE)` + factory methods `create()` and `reconstitute()`** (lines 24-86):
```java
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class User {

    private final UUID id;
    private final String name;
    private final Email email;

    @ToString.Exclude
    private final String passwordHash;

    private final LocalDateTime createdAt;

    /**
     * Factory method — único ponto de criação de uma nova entidade.
     * Gera UUID e timestamp automaticamente.
     */
    public static User create(String name, String emailValue, String passwordHash) {
        return User.builder()
                .id(UUID.randomUUID())
                .name(name)
                .email(new Email(emailValue))
                .passwordHash(passwordHash)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Factory method para reconstrução a partir de dados persistidos.
     * Preserva valores exatos vindos do banco (UUID, createdAt).
     */
    public static User reconstitute(UUID id, String name, String emailValue,
                                     String passwordHash, LocalDateTime createdAt) {
        return User.builder()
                .id(id)
                .name(name)
                .email(new Email(emailValue))
                .passwordHash(passwordHash)
                .createdAt(createdAt)
                .build();
    }
}
```

**Rules to replicate for each new entity:**
1. `@Builder(access = AccessLevel.PRIVATE)` — força uso dos factory methods
2. `@ToString.Exclude` em campos sensíveis (senha, hash)
3. `create()` — gera UUID + timestamp, recebe strings (não VOs) para validação no construtor do VO
4. `reconstitute()` — preserva UUID + timestamp existentes (usado por mappers JPA)
5. Todos os campos `final` — imutabilidade
6. Sem setters públicos

---

#### `OrdemDeServico.java` (model, CRUD + state-machine)

**Analog:** `User.java` lines 24-86 (for base entity pattern) + RESEARCH.md §OS State Machine (for state machine)

**Extended pattern — state machine with explicit transition methods:**

```java
@Getter
@Builder(access = AccessLevel.PRIVATE)
@ToString
public class OrdemDeServico {

    private final UUID id;
    private final UUID clienteUuid;
    private final UUID veiculoUuid;
    private StatusOS status;
    private final LocalDateTime dataEntrada;
    private LocalDateTime dataSaida;
    private final String observacoes;
    @Builder.Default
    private final List<ServicoSolicitado> servicosSolicitados = new ArrayList<>();
    private final Long version;  // @Version for optimistic locking

    @Builder(access = AccessLevel.PRIVATE)
    @Getter
    public static class ServicoSolicitado {
        private final UUID servicoUuid;
        private final String nome;
        private final BigDecimal valor;
    }

    public static OrdemDeServico create(UUID clienteUuid, UUID veiculoUuid,
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

    // --- State Machine: explicit transition methods (NUNCA setStatus!) ---

    public void iniciarDiagnostico() { transitarPara(StatusOS.EM_DIAGNOSTICO); }

    public void finalizarDiagnostico() { transitarPara(StatusOS.AGUARDANDO_APROVACAO); }

    public void aprovarOrcamento() { transitarPara(StatusOS.APROVADA); }

    public void cancelar() { transitarPara(StatusOS.CANCELADA); }

    public void iniciarExecucao() { transitarPara(StatusOS.EM_EXECUCAO); }

    public void finalizar() { transitarPara(StatusOS.FINALIZADA); }

    public void entregar() { transitarPara(StatusOS.ENTREGUE); }

    private void transitarPara(StatusOS destino) {
        if (!status.podeTransitarPara(destino)) {
            throw new AppException(400, "Transição inválida: " + status + " → " + destino);
        }
        this.status = destino;
    }
}
```

**Key rules:**
1. `@Version Long version` — optimistic locking (prevents Pitfall 1: lost updates)
2. NUNCA expor `setStatus()` — only explicit transition methods (Pitfall 2)
3. `ServicoSolicitado` inner class with `@Builder(access = PRIVATE)` — same pattern as entity
4. `clienteUuid`, `veiculoUuid` — references by UUID, NOT embedded entities (prevents Pitfall: mega-aggregate)

---

### DOMAIN — Value Object Template

#### `Cpf.java`, `PlacaVeiculo.java`, `Telefone.java` (valueobject, validation)

**Analog:** `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/Email.java`

**Core pattern** (lines 22-52):
```java
@Getter
@EqualsAndHashCode
@ToString
public final class Email {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z0-9._%+\\-]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$");

    private final String value;

    /**
     * Construtor com validação.
     * Lança AppException(400) se valor for null, blank ou não corresponder ao padrão.
     * Normaliza o valor (lowercase, strip non-digits, uppercase, etc.)
     */
    public Email(String value) {
        if (value == null || value.isBlank()) {
            throw new AppException(400, Messages.get("email.invalid.format", value == null ? "null" : value.strip()));
        }
        String trimmed = value.strip();
        if (!EMAIL_PATTERN.matcher(trimmed).matches()) {
            throw new AppException(400, Messages.get("email.invalid.format", trimmed));
        }
        this.value = trimmed.toLowerCase(Locale.ROOT);
    }
}
```

**Rules to replicate:**
1. `final class` — não pode ser estendida
2. `@EqualsAndHashCode` — igualdade por valor
3. Construtor valida: null → `AppException(400)`, blank → `AppException(400)`, pattern mismatch → `AppException(400)`
4. Normaliza o valor (lowercase, uppercase sem hífen, strip non-digits)
5. Pattern compilado como `static final` — thread-safe

**Specific patterns for new VOs:**

| VO | Validation Method | Normalization |
|---|---|---|
| `Cpf` | 11 dígitos + checksum dos 2 dígitos verificadores | `value.replaceAll("\\D", "")` |
| `PlacaVeiculo` | Regex: `^(?:[A-Z]{3}[0-9]{4}\|[A-Z]{3}[0-9][A-Z][0-9]{2})$` (Mercosul + antigo) | `value.toUpperCase().replace("-", "")` |
| `Telefone` | Brazilian phone: DDD + 8/9 dígitos | `value.replaceAll("\\D", "")` |
| `Endereco` | Flattened (not a single VO — validate UF `length==2`, CEP `length==8`) | N/A — multiple fields |

#### `StatusOS.java` (enum with transition matrix)

**Analog:** Nenhum existente — pattern do RESEARCH.md

**Core pattern** (RESEARCH.md lines 424-451):
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

---

### DOMAIN — Port In Template

#### `ClienteServicePort.java`, `VeiculoServicePort.java`, `ServicoServicePort.java` (port-in, CRUD)

**Analog:** `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/UserServicePort.java`

**Core pattern** (lines 1-24):
```java
package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.Cliente;
import java.util.List;
import java.util.UUID;

public interface ClienteServicePort {

    Cliente execute(CreateClienteCommand command);

    Cliente findById(UUID id);

    List<Cliente> findAll(int page, int size, String sort);

    long countAll();

    void delete(UUID id);
}
```

**Rules:**
1. Interface pura — sem anotações de framework
2. Métodos CRUD genéricos: `execute()`, `findById()`, `findAll()`, `countAll()`, `delete()`
3. `OrdemDeServicoServicePort` adiciona métodos específicos: `iniciarDiagnostico()`, `finalizarDiagnostico()`, `aprovarOrcamento()`, etc.

#### Command Record Template

**Analog:** `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/CreateUserCommand.java`

**Core pattern** (lines 1-14):
```java
public record CreateClienteCommand(
    String nome,
    String cpf,
    String email,
    String telefone,
    String enderecoLogradouro,
    String enderecoNumero,
    String enderecoBairro,
    String enderecoCidade,
    String enderecoUf,
    String enderecoCep
) {}
```

**Rules:**
1. Java record — imutável, conciso
2. Sem anotações de validação — validação é do adapter REST (Bean Validation no DTO) ou do domínio (VO construtor)

---

### DOMAIN — Port Out Template

#### `ClienteRepositoryPort.java`, `VeiculoRepositoryPort.java`, `ServicoRepositoryPort.java`

**Analog:** `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/UserRepositoryPort.java`

**Core pattern** (lines 1-40):
```java
package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.Cliente;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClienteRepositoryPort {

    Cliente save(Cliente cliente);

    Optional<Cliente> findById(UUID id);

    Optional<Cliente> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    List<Cliente> findAll(int page, int size, String sort);

    long countAll();

    void markAsDeleted(UUID id);
}
```

**Rules:**
1. Interface pura — sem anotações de framework
2. Métodos CRUD: `save()`, `findById()`, `findAll()`, `countAll()`, `markAsDeleted()`
3. Adicionar `findBy*` específico (ex: `findByEmail`, `findByCpf`, `findByPlaca`)

---

### DOMAIN — Event Template

#### `OrdemDeServicoCriadaEvent.java` (event, event-driven)

**Analog:** `mekano-domain/src/main/java/com/fiap/mekano/domain/event/UserCreatedEvent.java`

**Core pattern** (lines 1-16):
```java
package com.fiap.mekano.domain.event;

import com.fiap.mekano.domain.model.OrdemDeServico;
import java.time.LocalDateTime;

public record OrdemDeServicoCriadaEvent(
    OrdemDeServico ordemDeServico,
    LocalDateTime occurredAt
) {
    public static OrdemDeServicoCriadaEvent of(OrdemDeServico ordemDeServico) {
        return new OrdemDeServicoCriadaEvent(ordemDeServico, LocalDateTime.now());
    }
}
```

---

### APPLICATION — Service Template

#### `ClienteService.java`, `VeiculoService.java`, `ServicoService.java` (service, CRUD)

**Analog:** `mekano-application/src/main/java/com/fiap/mekano/application/service/user/UserService.java`

**Imports pattern** (lines 1-17):
```java
package com.fiap.mekano.application.service.cliente;

import com.fiap.mekano.domain.event.ClienteCriadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.port.in.CreateClienteCommand;
import com.fiap.mekano.domain.port.in.ClienteServicePort;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.EventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;
```

**Core pattern** (lines 18-80):
```java
@ApplicationScoped
public class ClienteService implements ClienteServicePort {

    private final ClienteRepositoryPort clienteRepository;
    private final EventPublisher eventPublisher;

    public ClienteService(ClienteRepositoryPort clienteRepository, EventPublisher eventPublisher) {
        this.clienteRepository = clienteRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public Cliente execute(CreateClienteCommand command) {
        // 1. Validate required fields
        if (command.nome() == null || command.nome().isBlank()) {
            throw new AppException(400, Messages.get("cliente.nome.required"));
        }

        // 2. Check duplicate (CPF)
        if (clienteRepository.existsByCpf(command.cpf())) {
            throw new AppException(409, Messages.get("cliente.already.exists", command.cpf()));
        }

        // 3. Create domain entity (VO constructors validate)
        Cliente cliente = Cliente.create(
            command.nome(), command.cpf(), command.email(), command.telefone(),
            command.enderecoLogradouro(), command.enderecoNumero(),
            command.enderecoBairro(), command.enderecoCidade(),
            command.enderecoUf(), command.enderecoCep()
        );

        // 4. Persist
        Cliente savedCliente = clienteRepository.save(cliente);

        // 5. Publish event
        eventPublisher.publish(ClienteCriadoEvent.of(savedCliente));

        return savedCliente;
    }

    @Override
    public Cliente findById(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new AppException(404, Messages.get("cliente.not.found", id)));
    }

    @Override
    public List<Cliente> findAll(int page, int size, String sort) {
        return clienteRepository.findAll(page, size, sort);
    }

    @Override
    public long countAll() {
        return clienteRepository.countAll();
    }

    @Override
    public void delete(UUID id) {
        clienteRepository.markAsDeleted(id);
    }
}
```

**Key rules:**
1. `@ApplicationScoped` — CDI permite injeção
2. Constructor injection — necessário para `@InjectMocks` do Mockito
3. Implementa `ClienteServicePort` (interface no domain)
4. Injeta ports: `ClienteRepositoryPort`, `EventPublisher`
5. `@Transactional` no método `execute()` — unidade de trabalho do service
6. Fluxo: valida → verifica duplicidade → cria entidade → persiste → publica evento
7. NUNCA expor passwordHash ou entidade de domínio — sempre retornar pelo response record

#### `AuthService.java` (service, request-response)

**Analog:** `UserService.java` structure + RESEARCH.md §JWT Implementation

**Core pattern** (RESEARCH.md lines 637-679):
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
        var user = userRepository.findByEmail(command.email())
                .orElseThrow(() -> new AppException(401, "Credenciais inválidas"));

        if (!passwordHasher.matches(command.password(), user.getPasswordHash())) {
            throw new AppException(401, "Credenciais inválidas");
        }

        String accessToken = Jwt.issuer("https://mekano.fiap.com.br/auth")
                .subject(user.getId().toString())
                .upn(user.getEmail().getValue())
                .groups(Set.of(user.getRole()))
                .claim("name", user.getName())
                .expiresIn(Duration.ofMinutes(15))
                .sign();

        String refreshToken = refreshTokenService.createToken(user.getId(), user.getRole());

        return new TokenPair(accessToken, refreshToken, 900);
    }
}
```

---

### INFRASTRUCTURE — Entity (JPA) Template

#### `ClienteEntity.java`, `VeiculoEntity.java`, `ServicoEntity.java`

**Analog:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/UserEntity.java`

**Core pattern** (lines 1-44):
```java
package com.fiap.mekano.infrastructure.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.UUID;

@Entity
@Table(name = "clientes")
@Getter
@Setter
@NoArgsConstructor
public class ClienteEntity extends BaseEntity {

    @Column(unique = true, nullable = false)
    UUID uuid;

    @Column(nullable = false)
    String nome;

    @Column(unique = true, nullable = false)
    String cpf;

    @Column(nullable = false)
    String email;

    @Column(nullable = false)
    String telefone;

    // --- Endereco flattening ---
    @Column(name = "endereco_logradouro")
    String enderecoLogradouro;

    @Column(name = "endereco_numero")
    String enderecoNumero;

    @Column(name = "endereco_bairro")
    String enderecoBairro;

    @Column(name = "endereco_cidade")
    String enderecoCidade;

    @Column(name = "endereco_uf", length = 2)
    String enderecoUf;

    @Column(name = "endereco_cep", length = 8)
    String enderecoCep;
}
```

**Rules:**
1. `extends BaseEntity` — herda PK seq `Long id`, audit fields, soft delete
2. `@Table(name = "clientes")` — nome explícito
3. `UUID uuid` — público, único, exposto em APIs
4. VOs flattened as individual columns (ex: `Cpf` → `cpf VARCHAR`, `Endereco` → `endereco_logradouro`, etc.)
5. `@NoArgsConstructor` — obrigatório para JPA

#### `OrdemDeServicoEntity.java`

**Extends `BaseEntity`** with additional `@Version` field:
```java
@Entity
@Table(name = "ordens_de_servico")
@Getter
@Setter
@NoArgsConstructor
public class OrdemDeServicoEntity extends BaseEntity {

    @Column(unique = true, nullable = false)
    UUID uuid;

    @Column(name = "cliente_uuid", nullable = false)
    UUID clienteUuid;

    @Column(name = "veiculo_uuid", nullable = false)
    UUID veiculoUuid;

    @Column(nullable = false)
    String status;  // stored as String (StatusOS name)

    @Column(name = "data_entrada", nullable = false)
    LocalDateTime dataEntrada;

    @Column(name = "data_saida")
    LocalDateTime dataSaida;

    @Column(columnDefinition = "TEXT")
    String observacoes;

    @Version
    Long version;  // optimistic locking
}
```

#### `RefreshTokenEntity.java`

**Does NOT extend BaseEntity** — extends `PanacheEntityBase` directly (no audit fields needed):
```java
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(unique = true, nullable = false)
    UUID uuid;

    @Column(nullable = false, length = 36)
    String jti;

    @Column(name = "token_hash", nullable = false, length = 64)
    String tokenHash;

    @Column(name = "user_uuid", nullable = false)
    UUID userUuid;

    @Column(name = "expires_at", nullable = false)
    Instant expiresAt;

    @Column(name = "rotated_at")
    Instant rotatedAt;

    @Column(name = "created_at", nullable = false)
    LocalDateTime createdAt;
}
```

---

### INFRASTRUCTURE — Repository Template (Two-Class Pattern)

#### `ClientePanacheRepository.java` (Panache layer)

**Analog:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/UserPanacheRepository.java`

**Core pattern** (lines 1-19):
```java
package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.ClienteEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClientePanacheRepository implements PanacheRepositoryBase<ClienteEntity, Long> {
    // Methods inherited from PanacheRepositoryBase via bytecode enhancement
}
```

#### `ClienteRepositoryImpl.java` (Port implementation)

**Analog:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/UserRepositoryImpl.java`

**Imports pattern** (lines 1-26):
```java
package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import com.fiap.mekano.infrastructure.entity.ClienteEntity;
import com.fiap.mekano.infrastructure.mapper.ClienteEntityMapper;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
```

**Core pattern** (lines 61-226):
```java
@ApplicationScoped
public class ClienteRepositoryImpl implements ClienteRepositoryPort {

    @Inject
    ClientePanacheRepository panacheRepository;

    @Inject
    ClienteEntityMapper mapper;

    @Override
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CacheInvalidate(cacheName = CacheNames.CLIENTES)
    public Cliente save(Cliente cliente) {
        try {
            var entity = mapper.toEntity(cliente);
            panacheRepository.persist(entity);
            panacheRepository.flush();
            return mapper.toDomain(entity);
        } catch (PersistenceException e) {
            throw handleConstraintViolation(e);
        }
    }

    @Override
    @Retry(maxRetries = 3)
    @CacheResult(cacheName = CacheNames.CLIENTES)
    public Optional<Cliente> findById(UUID id) {
        return panacheRepository.find("uuid = ?1 AND isActive = ?2", id, true)
                .firstResultOptional().map(mapper::toDomain);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return panacheRepository.count("cpf = ?1 AND isActive = ?2", cpf, true) > 0;
    }

    @Override
    public List<Cliente> findAll(int page, int size, String sort) {
        // Same sorting/pagination pattern as UserRepositoryImpl
        var query = panacheRepository.find("isActive = ?1", Sort.by("nome").ascending(), true);
        return query.page(Page.of(page, size)).list()
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.CLIENTES)
    public void markAsDeleted(UUID id) {
        ClienteEntity entity = panacheRepository.find("uuid", id).firstResultOptional()
                .orElseThrow(() -> new AppException(404, "Cliente não encontrado"));
        entity.setDeletedAt(LocalDateTime.now());
        entity.setIsActive(false);
    }
}
```

**Key rules to replicate from UserRepositoryImpl:**
1. `@Retry(maxRetries = 3)` em leituras (`findById`, `findByEmail`)
2. `@Timeout(value = 5, unit = ChronoUnit.SECONDS)` em escrita (`save`)
3. `@CacheResult` em queries de leitura
4. `@CacheInvalidate` em escritas (`save`, `markAsDeleted`)
5. `flush()` após `persist()` para capturar constraint violations imediatamente
6. `handleConstraintViolation()` — percorre `getCause()` chain até `ConstraintViolationException` → `AppException(409)`
7. `HQL com isActive = true` em todas as queries (soft delete filter)

---

### INFRASTRUCTURE — Entity Mapper Template

#### `ClienteEntityMapper.java`, `VeiculoEntityMapper.java`, `ServicoEntityMapper.java`

**Analog:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/UserEntityMapper.java`

**Core pattern** (lines 1-11):
```java
package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.infrastructure.entity.ClienteEntity;

public interface ClienteEntityMapper {

    ClienteEntity toEntity(Cliente cliente);

    Cliente toDomain(ClienteEntity entity);
}
```

**Impl pattern** (from `UserEntityMapperImpl.java` lines 1-41):
```java
@ApplicationScoped
public class ClienteEntityMapperImpl implements ClienteEntityMapper {

    @Inject
    CpfMapper cpfMapper;

    @Inject
    EnderecoMapper enderecoMapper;

    @Override
    public ClienteEntity toEntity(Cliente cliente) {
        if (cliente == null) return null;
        ClienteEntity entity = new ClienteEntity();
        entity.setUuid(cliente.getId());
        entity.setNome(cliente.getNome());
        entity.setCpf(cpfMapper.cpfToString(cliente.getCpf()));
        // ... map other fields, flatten VOs
        entity.setCreatedAt(cliente.getCreatedAt());
        return entity;
    }

    @Override
    public Cliente toDomain(ClienteEntity entity) {
        if (entity == null) return null;
        return Cliente.reconstitute(
                entity.getUuid(),
                entity.getNome(),
                entity.getCpf(),
                // ... map other fields, recreate VOs
                entity.getCreatedAt()
        );
    }
}
```

**Rules:**
1. Interface `ClienteEntityMapper` (MapStruct-style or manual impl as `UserEntityMapperImpl`)
2. Manual impl preferred (existing pattern: `UserEntityMapperImpl.java` is hand-written, not MapStruct-generated)
3. `@Inject` VO mappers (like `CpfMapper`, `EnderecoMapper`)
4. `toDomain()` calls `Cliente.reconstitute()` — preserving exact values from DB
5. `toEntity()` maps domain → entity, flattening VOs to primitive fields

#### `CpfMapper.java`, `PlacaVeiculoMapper.java`, `TelefoneMapper.java` (VO Mappers)

**Analog:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/EmailMapper.java`

**Core pattern** (lines 1-40):
```java
@ApplicationScoped
public class CpfMapper {

    @Named("cpfToString")
    public String cpfToString(Cpf cpf) {
        return cpf == null ? null : cpf.getValue();
    }

    @Named("stringToCpf")
    public Cpf stringToCpf(String value) {
        return value == null ? null : new Cpf(value);
    }
}
```

---

### INFRASTRUCTURE — Cache Pattern

#### Updating `CacheNames.java`

**Analog:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/cache/CacheNames.java`

**Add new constants:**
```java
public final class CacheNames {
    public static final String USERS = "users";
    public static final String CLIENTES = "clientes";
    public static final String VEICULOS = "veiculos";
    public static final String SERVICOS = "servicos";
    public static final String ORDENS_DE_SERVICO = "ordens-de-servico";
    public static final String REFRESH_TOKENS = "refresh-tokens";
}
```

#### Updating `cache-config.yml`

**Analog:** `mekano-infrastructure/src/main/resources/cache-config.yml`

**Add new caches:**
```yaml
quarkus:
  cache:
    caffeine:
      users:
        initial-capacity: 10
        maximum-size: 100
        expire-after-write: 60s
      clientes:
        initial-capacity: 10
        maximum-size: 100
        expire-after-write: 60s
      veiculos:
        initial-capacity: 10
        maximum-size: 100
        expire-after-write: 60s
      servicos:
        initial-capacity: 10
        maximum-size: 100
        expire-after-write: 60s
      ordens-de-servico:
        initial-capacity: 10
        maximum-size: 100
        expire-after-write: 60s
      refresh-tokens:
        initial-capacity: 10
        maximum-size: 100
        expire-after-write: 60s
```

---

### REST — Resource Template

#### `ClienteResource.java`, `VeiculoResource.java`, `ServicoResource.java` (controller, CRUD)

**Analog:** `mekano-rest/src/main/java/com/fiap/mekano/rest/api/UserResource.java`

**Imports pattern** (lines 1-36):
```java
package com.fiap.mekano.rest.api;

import com.fiap.mekano.rest.api.dto.CreateClienteRequest;
import com.fiap.mekano.rest.api.dto.ClientePageResponse;
import com.fiap.mekano.rest.api.dto.ClienteResponse;
import com.fiap.mekano.rest.api.exception.ProblemDetail;
import com.fiap.mekano.rest.api.mapper.ClienteDtoMapper;
import com.fiap.mekano.domain.port.in.ClienteServicePort;
import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.openapi.annotations.*;
import org.eclipse.microprofile.openapi.annotations.media.*;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.UUID;
```

**Core pattern** (lines 37-193):
```java
@Path("/clientes")
@RequestScoped
@RolesAllowed({"admin", "atendente"})
@Tag(name = "Clientes", description = "CRUD de clientes")
public class ClienteResource {

    @Inject
    ClienteServicePort clienteServicePort;

    @Inject
    ClienteDtoMapper clienteDtoMapper;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Criar novo cliente")
    @APIResponse(responseCode = "201", description = "Cliente criado com sucesso",
            content = @Content(schema = @Schema(implementation = ClienteResponse.class)))
    @APIResponse(responseCode = "400", description = "Dados inválidos",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    @APIResponse(responseCode = "409", description = "CPF já cadastrado",
            content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    public Response create(@Valid CreateClienteRequest request, @Context UriInfo uriInfo) {
        var command = clienteDtoMapper.toCommand(request);
        var cliente = clienteServicePort.execute(command);
        ClienteResponse response = clienteDtoMapper.toResponse(cliente);
        URI location = uriInfo.getAbsolutePathBuilder().path(response.id().toString()).build();
        return Response.created(location).entity(response).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar clientes")
    public Response listAll(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("sort") @DefaultValue("nome,asc") String sort) {
        var content = clienteServicePort.findAll(page, size, sort)
                .stream().map(clienteDtoMapper::toResponse).toList();
        long total = clienteServicePort.countAll();
        var response = new ClientePageResponse(content, page, size, total,
                (int) Math.ceil((double) total / size));
        return Response.ok(response).build();
    }

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getById(@PathParam("id") UUID id) {
        var cliente = clienteServicePort.findById(id);
        return Response.ok(clienteDtoMapper.toResponse(cliente)).build();
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "Excluir cliente (soft delete)")
    public Response delete(@PathParam("id") UUID id) {
        clienteServicePort.delete(id);
        return Response.noContent().build();
    }
}
```

**Key rules for ALL resources:**
1. `@Path("/entidade")` — plural, lowercase
2. `@RequestScoped` — obrigatório (G8), necessário para `@Context UriInfo` e injeção JWT claims
3. NUNCA `@Transactional` — transações são do service layer (D-01)
4. `@RolesAllowed({"admin", "atendente"})` — perfil específico (não genérico "user")
5. Injeta `*ServicePort` (interface) + `*DtoMapper`
6. `@Valid` no request — Bean Validation
7. OpenAPI: `@Tag`, `@Operation`, `@APIResponse` em cada método

**Role matrix per resource:**

| Resource | @Path | @RolesAllowed |
|---|---|---|
| `AuthResource` | `/auth` | `@PermitAll` |
| `ClienteResource` | `/clientes` | `{"admin", "atendente"}` |
| `VeiculoResource` | `/veiculos` | `{"admin", "atendente"}` |
| `ServicoResource` | `/servicos` | `{"admin"}` |
| `OrdemDeServicoResource` | `/os` | Mixed: `@RolesAllowed` + `@PermitAll` for status |

**OrdemDeServicoResource — Special patterns:**
```java
@Path("/os")
@RequestScoped
@Tag(name = "Ordens de Serviço", description = "OS — CRUD e transições")
public class OrdemDeServicoResource {

    // Public endpoint — NO @RolesAllowed on class level, use per-method instead
    // OS-15 / AUTH-03: Consulta pública de status sem autenticação
    @GET
    @Path("/{uuid}/status")
    @PermitAll
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Consulta pública de status da OS")
    public Response consultarStatusPublico(@PathParam("uuid") UUID uuid) {
        var status = osService.consultarStatus(uuid);
        return Response.ok(status).build();
    }

    @POST
    @RolesAllowed({"admin", "atendente"})
    @Operation(summary = "Criar OS")
    public Response criar(@Valid CreateOrdemDeServicoRequest request, @Context UriInfo uriInfo) { ... }

    @PUT
    @Path("/{uuid}/iniciar-diagnostico")
    @RolesAllowed({"mecanico", "admin"})
    @Operation(summary = "Iniciar diagnóstico (RECEBIDA → EM_DIAGNOSTICO)")
    public Response iniciarDiagnostico(@PathParam("uuid") UUID uuid) { ... }
}
```

---

### REST — DTO Template

#### Input DTO (Create Request)

**Analog:** `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/CreateUserRequest.java`

**Core pattern** (lines 1-43):
```java
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request payload para criar um novo cliente")
public class CreateClienteRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(max = 100, message = "Nome deve ter no máximo 100 caracteres")
    @Schema(required = true, description = "Nome do cliente", example = "João Silva")
    private String nome;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "CPF deve ter 11 dígitos")
    @Schema(required = true, description = "CPF (11 dígitos, apenas números)", example = "12345678901")
    private String cpf;

    // ... more fields
}
```

#### Output DTO (Response)

**Analog:** `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/UserResponse.java`

**Core pattern** (lines 1-25):
```java
@Schema(description = "Dados do cliente criado")
public record ClienteResponse(
    @Schema(description = "Identificador único", example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
    @Schema(description = "Nome do cliente", example = "João Silva") String nome,
    @Schema(description = "CPF", example = "12345678901") String cpf,
    @Schema(description = "Email", example = "joao@email.com") String email,
    @Schema(description = "Telefone", example = "11999999999") String telefone,
    @Schema(description = "Data de criação", example = "2026-06-22T10:00:00") LocalDateTime createdAt
) {}
```

#### Page Response

**Analog:** `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/UserPageResponse.java`

**Core pattern** (lines 1-26):
```java
@Schema(description = "Resposta paginada de clientes")
public record ClientePageResponse(
    @Schema(description = "Lista de clientes da página atual") List<ClienteResponse> content,
    @Schema(description = "Número da página (0-based)", example = "0") int page,
    @Schema(description = "Tamanho da página", example = "10") int size,
    @Schema(description = "Total de registros", example = "42") long totalElements,
    @Schema(description = "Total de páginas", example = "5") int totalPages
) {}
```

#### Login DTOs

**No analog in codebase — from RESEARCH.md:**
```java
// LoginRequest — input
@Getter @Setter @NoArgsConstructor
public class LoginRequest {
    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
}

// LoginResponse — output (snake_case for JSON)
@Schema(description = "Resposta de autenticação")
public record LoginResponse(
    @JsonProperty("access_token") String accessToken,
    @JsonProperty("refresh_token") String refreshToken,
    @JsonProperty("expires_in") int expiresIn
) {}
```

---

### REST — DTO Mapper Template

#### `ClienteDtoMapper.java`, `VeiculoDtoMapper.java`, `ServicoDtoMapper.java`, `OrdemDeServicoDtoMapper.java`

**Analog:** `mekano-rest/src/main/java/com/fiap/mekano/rest/api/mapper/UserDtoMapper.java`

**Core pattern** (lines 1-60):
```java
@Mapper(componentModel = "cdi")
public interface ClienteDtoMapper {

    CreateClienteCommand toCommand(CreateClienteRequest request);

    @Mapping(target = "cpf", expression = "java(cliente.getCpf().getValue())")
    @Mapping(target = "email", expression = "java(cliente.getEmail().getValue())")
    @Mapping(target = "telefone", expression = "java(cliente.getTelefone().getValue())")
    ClienteResponse toResponse(Cliente cliente);

    ClienteResponse toResponse(CreateClienteResponse response);
}
```

**Rules:**
1. `@Mapper(componentModel = "cdi")` — NUNCA `"spring"` (G9)
2. `@Mapping(target = "email", expression = "java(cliente.getEmail().getValue())")` — extrai string de VO
3. VO-to-primitive mapping via expressions (not separate mapper classes, unlike entity mappers)

---

### FLYWAY Migration Template

**Analog:** Existing V1 through V5 migrations
- `V5__add_sequential_id.sql` — shows hybrid ID pattern (PK = `BIGINT GENERATED BY DEFAULT AS IDENTITY`, `UUID uuid UNIQUE`)

**Rules (from CLAUDE.md gotchas):**
1. Use `BIGINT GENERATED BY DEFAULT AS IDENTITY` NOT `BIGSERIAL` (H2 compatibility)
2. Separate `ALTER TABLE` statements for multiple column additions
3. Use `UUID`, `TIMESTAMP`, `BOOLEAN`, `VARCHAR`, `INT`, `BIGINT`, `NOW()`, `DEFAULT`

**Migration sequencing (from RESEARCH.md §Migration Sequencing):**

| Migration | Tables | Notes |
|---|---|---|
| `V6__create_user_roles_table.sql` | `user_roles` | N:N user ↔ role |
| `V7__create_clientes_table.sql` | `clientes` | Includes endereco flattening columns |
| `V8__create_veiculos_table.sql` | `veiculos` | FK to clientes (logical UUID ref), unique placa |
| `V9__create_servicos_table.sql` | `servicos` | CHECK valor > 0 |
| `V10__create_ordens_de_servico_table.sql` | `ordens_de_servico`, `servicos_executados`, `pecas_usadas` | OS + child tables with `@Version` column |

---

### TEST Patterns

#### Domain Test (VO)

**Analog:** `mekano-domain/src/test/java/com/fiap/mekano/domain/valueobject/EmailTest.java`

```java
@DisplayName("Cpf Value Object")
class CpfTest {

    @Test
    @DisplayName("deve criar Cpf com valor válido")
    void deveCriarCpfValido() {
        Cpf cpf = new Cpf("52998224725");
        assertEquals("52998224725", cpf.getValue());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("deve lançar AppException(400) para null e vazio")
    void deveLancarExcecaoParaNullEVazio(String valor) {
        assertThrows(AppException.class, () -> new Cpf(valor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"00000000000", "123", "abcdefghijk", "11111111111"})
    @DisplayName("deve lançar AppException(400) para CPF inválido")
    void deveLancarExcecaoParaCpfInvalido(String cpfInvalido) {
        assertThrows(AppException.class, () -> new Cpf(cpfInvalido));
    }
}
```

#### Domain Test (State Machine)

**From RESEARCH.md §Transition Matrix Parameterized Test:**
```java
@DisplayName("StatusOS — Matriz de Transições")
class StatusOSTest {

    @ParameterizedTest
    @CsvSource({
        "RECEBIDA, EM_DIAGNOSTICO, true",
        "RECEBIDA, CANCELADA, true",
        "RECEBIDA, AGUARDANDO_APROVACAO, false",
        "RECEBIDA, FINALIZADA, false",
        // ... all 49 transitions
    })
    void deveValidarTransicoes(StatusOS origem, StatusOS destino, boolean esperado) {
        assertEquals(esperado, origem.podeTransitarPara(destino));
    }
}
```

#### Application Test (Service)

**Analog:** `mekano-application/src/test/java/com/fiap/mekano/application/service/user/UserServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService")
class ClienteServiceTest {

    @Mock ClienteRepositoryPort clienteRepository;
    @Mock EventPublisher eventPublisher;
    @InjectMocks ClienteService clienteService;

    @Test
    @DisplayName("deve criar cliente com dados válidos")
    void deveCriarClienteComDadosValidos() {
        var command = new CreateClienteCommand("João", "52998224725", "...");
        when(clienteRepository.existsByCpf("52998224725")).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente result = clienteService.execute(command);

        assertNotNull(result);
        assertEquals("João", result.getNome());
        verify(clienteRepository, times(1)).save(any(Cliente.class));
        verify(eventPublisher, times(1)).publish(any());
    }
}
```

#### REST Test (Integration)

**Analog:** `mekano-rest/src/test/java/com/fiap/mekano/rest/api/UserResourceTest.java`

```java
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestSecurity(user = "testuser", roles = {"admin"})
class ClienteResourceTest {

    @Test @Order(1)
    void create_validCliente_returns201() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {"nome": "João", "cpf": "52998224725", "email": "joao@email.com",
                 "telefone": "11999999999", "enderecoLogradouro": "Rua A",
                 "enderecoNumero": "123", "enderecoBairro": "Centro",
                 "enderecoCidade": "São Paulo", "enderecoUf": "SP", "enderecoCep": "01001000"}
                """)
            .when().post("/api/v1/clientes")
            .then().statusCode(201)
            .body("id", notNullValue())
            .body("nome", equalTo("João"));
    }
}
```

---

## Shared Patterns

### Authentication & Authorization

**Source:** `mekano-rest/src/main/java/com/fiap/mekano/rest/api/UserResource.java` (lines 43-45)

**Apply to:** All controller files

```java
@Path("/users")
@RequestScoped
@RolesAllowed("user")      // or specific roles per resource
```

**Rules:**
- `@RequestScoped` — obrigatório para JWT injection (G8)
- NUNCA `@ApplicationScoped` em resources com JWT
- `@RolesAllowed` com perfis específicos: `{"admin", "atendente"}`, `{"admin"}`, `{"mecanico", "admin"}`
- `@PermitAll` no `AuthResource` (login) e no endpoint público de status OS
- `quarkus.http.auth.proactive=false` — permite ExceptionMapper tratar 401 (from CLAUDE.md)

### JWT Configuration

**Source:** RESEARCH.md §JWT Implementation

**Apply to:** `mekano-rest/pom.xml` + `application.properties` (or new `auth-config.yml`)

```xml
<!-- Add to mekano-rest/pom.xml -->
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-jwt</artifactId>
</dependency>
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-smallrye-jwt-build</artifactId>
</dependency>
```

```properties
# Add to application.properties or auth-config.yml
mp.jwt.verify.publickey.location=publicKey.pem
mp.jwt.verify.publickey.algorithm=EdDSA
mp.jwt.verify.issuer=${MP_JWT_ISSUER:https://mekano.fiap.com.br/auth}
smallrye.jwt.sign.key.location=${user.home}/.mekano/secrets/privatekey.pem
smallrye.jwt.new-token.signature-algorithm=EdDSA
```

### Error Handling

**Source:** `mekano-rest/src/main/java/com/fiap/mekano/rest/api/exception/ApiExceptionMapper.java` (lines 28-53)

**Apply to:** All layers (no new mappers needed)

- Single `ApiExceptionMapper` already handles all `AppException` via `getStatus()`
- Format: RFC 7807 Problem Details (`application/problem+json`)
- New HTTP status codes (401, 403) already supported — no mapper changes needed
- `AppException(status, message)` — throw from domain, application, or infrastructure layers

### Validation

**Source:** `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/CreateUserRequest.java`

**Apply to:** All REST POST/PUT handlers

- Input DTOs: Bean Validation annotations (`@NotBlank`, `@Email`, `@Size`, `@Pattern`, `@NotNull`)
- Domain VOs: validate in constructor, throw `AppException(400)`
- Service layer: validate business rules (duplicates, existence) → `AppException(409)`, `AppException(404)`

### Event Publishing

**Source:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/event/CdiEventPublisher.java` + `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/EventPublisher.java`

**Apply to:** All service `execute()` methods

```java
// Publish after successful save
eventPublisher.publish(OrdemDeServicoCriadaEvent.of(savedOs));
```

### Cache Pattern

**Source:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/UserRepositoryImpl.java`

**Apply to:** All `*RepositoryImpl.java` files

| Operation | Annotation |
|---|---|
| Read (findById, findByEmail, etc.) | `@CacheResult(cacheName = CacheNames.XXX)` + `@Retry(maxRetries = 3)` |
| Write (save) | `@CacheInvalidate(cacheName = CacheNames.XXX)` + `@Timeout(5s)` |
| Delete (markAsDeleted) | `@Transactional` + `@CacheInvalidate(cacheName = CacheNames.XXX)` |

---

## Anti-Patterns to Avoid (Gotchas from CLAUDE.md)

| # | Anti-Pattern | Symptom | Fix |
|---|---|---|---|
| G1 | `quarkus-maven-plugin` em módulo não-quarkus | Build quebra | Plugin só em `mekano-rest` |
| G2 | Jandex ausente em app/infra/rest | `UnsatisfiedResolutionException` | `jandex-maven-plugin` nos 3 módulos |
| G3 | Ordem annotationProcessorPaths errada | Mapper compila mas campos null | Lombok → binding → MapStruct |
| G4 | Flyway sem duplo underscore | Migrations ignoradas | `V6__desc.sql` |
| G5 | `migrate-at-start` default = false | Nenhuma migration executa | `quarkus.flyway.migrate-at-start=true` |
| G6 | Namespace JWT `quarkus.smallrye-jwt.*` | 401 silencioso | Usar `mp.jwt.*` |
| G7 | Chave RSA sem PKCS#8 | JWT rejeitado | Usar PKCS#8 ou Ed25519 |
| G8 | `@ApplicationScoped` em Resource com JWT | Injection de claims quebra | `@RequestScoped` obrigatório |
| G9 | MapStruct `componentModel = "spring"` | NPE no mapper | Sempre `"cdi"` |
| G10 | ExceptionMapper sem `@Provider` | Mapper ignorado | `@Provider @ApplicationScoped` |

### Phase-Specific Anti-Patterns

| # | Anti-Pattern | Symptom | Fix |
|---|---|---|---|
| P1 | `setStatus()` on OS entity | Lost updates, invalid transitions | Use explicit transition methods only |
| P2 | `@Transactional` on resource | Transação muito longa | `@Transactional` só no service layer |
| P3 | Mega-aggregate OS (embed Cliente/Veiculo) | Transaction contention | Reference by UUID only |
| P4 | `BIGSERIAL` in migrations | H2 test incompatibility | Use `BIGINT GENERATED BY DEFAULT AS IDENTITY` |
| P5 | EdDSA algorithm property wrong name | 401 with valid-looking JWT | Use `mp.jwt.verify.publickey.algorithm=EdDSA` |

---

## No Analog Found

| File | Role | Data Flow | Reason |
|---|---|---|---|
| `StatusOS.java` | valueobject | enum + state-machine | First state machine in project — use RESEARCH.md pattern |
| `AuthServicePort.java` | port-in | request-response | Auth login/refresh is new functionality — use RESEARCH.md pattern |
| `AuthResource.java` | controller | request-response | Login/logout/refresh are new — use RESEARCH.md pattern |
| `LoginRequest.java` | dto | request-response | New auth DTO — use RESEARCH.md pattern |
| `LoginResponse.java` | dto | request-response | New auth DTO — use RESEARCH.md pattern |
| Key generation script | utility | file-I/O | New build step — use RESEARCH.md OpenSSL commands |

## Key Libraries to Add

| Library | Module | Purpose |
|---|---|---|
| `quarkus-smallrye-jwt` | `mekano-rest` | JWT verification, `@RolesAllowed` |
| `quarkus-smallrye-jwt-build` | `mekano-rest` | JWT token generation (`.sign()`) |

## Metadata

**Analog search scope:** `mekano-domain/`, `mekano-application/`, `mekano-infrastructure/`, `mekano-rest/`
**Files scanned:** ~60 existing source files
**Pattern extraction date:** 2026-06-22
