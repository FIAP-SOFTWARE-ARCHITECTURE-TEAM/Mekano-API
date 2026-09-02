# mekano-domain — Core Business Layer

## Constraint
Zero framework dependencies. No `jakarta.*`, `io.quarkus.*`, `org.hibernate.*` imports. Only Java SE + Lombok (`scope=provided`).

## Package Map (Verified Against Source)

```
com.fiap.mekano.domain
├── model/                 # Entities (POJO, @Builder(access=PRIVATE))
│   ├── User.java               — create() + reconstitute() factories
│   ├── Cliente.java            — CPF, Email, Telefone (optional), Endereco VOs
│   ├── Veiculo.java            — linked to cliente via clienteUuid
│   ├── Servico.java            — mutable atualizar() (only entity with mutation outside factories)
│   ├── Peca.java               — debitarSaldo(), creditarSaldo(), isEstoqueMinimoAtingido()
│   ├── Orcamento.java          — auto-calc valorTotal from ItemOrcamento list
│   ├── ItemOrcamento.java      — VALUE OBJECT misplaced in model/ (has @EqualsAndHashCode, no UUID)
│   ├── RequisicaoCompra.java   — estados: ABERTA/ENVIADA/COMPRADA/RECEBIDA/CANCELADA
│   ├── NfEntrada.java          — validates CNPJ (14 digits), chave acesso (44 digits)
│   ├── OrdemDeServico.java     — create(clienteId, veiculoId, descricaoProblema), 25-param reconstitute()
│   ├── ItemOs.java             — create(osUuid, referenciaUuid, tipo, descricao, quantidade); isPeca()/isServico()
│   ├── Role.java               — enum: ADMIN, ATENDENTE, MECANICO, FINANCEIRO, CLIENTE
│   ├── StatusOS.java           — enum: RECEBIDA, EM_DIAGNOSTICO, etc.
│   ├── StatusOrcamento.java    — enum: PENDENTE, APROVADO, REPROVADO
│   ├── StatusRequisicao.java   — enum: ABERTA, ENVIADA, COMPRADA, RECEBIDA, CANCELADA
│   ├── MotivoRequisicao.java   — enum: ESTOQUE_MINIMO, SOLICITACAO_CLIENTE, etc.
│   └── UnidadeMedida.java      — enum: UNIDADE, LITRO, METRO, QUILO, CAIXA
├── os/                      # OS-specific value objects
│   ├── StatusPagamento.java    — enum: PENDENTE, CONFIRMADO, CANCELADO
│   ├── StatusEntrega.java      — enum: PENDENTE, ENTREGUE, NAO_ENTREGUE
│   └── OsAuditAction.java      — enum: CRIADA, INICIADA, etc.
├── valueobject/            # Immutable, validate in constructor, @EqualsAndHashCode
│   ├── Email.java              — normalizes trimmed lowercase, RFC 5322 regex
│   ├── Cpf.java                — 11 digits, check digits (mod 11), rejects repeated
│   ├── Endereco.java           — UF uppercase, CEP 8 digits
│   ├── Telefone.java           — 10-11 digits, validates DDD against ANATEL set (67 DDDs)
│   ├── PlacaVeiculo.java       — plate VO (LLLNNNN + LLLNLNN)
│   └── ItemOrcamento.java      — VALUE OBJECT (precio, quantidade, descricao, tipo)
├── port/
│   ├── in/                    # Input contracts (called by application layer)
│   │   ├── UserServicePort.java
│   │   ├── ClienteServicePort.java
│   │   ├── VeiculoServicePort.java
│   │   ├── ServicoServicePort.java
│   │   ├── OrdemDeServicoServicePort.java
│   │   ├── OrcamentoServicePort.java
│   │   ├── PasswordHasher.java
│   │   ├── CreateOrdemDeServicoCommand.java  — (clienteId, veiculoId, descricaoProblema, List<CreateItemOsCommand>)
│   │   ├── CreateItemOsCommand.java          — (referenciaUuid, tipo, quantidade)
│   │   ├── CreateOrcamentoCommand.java
│   │   ├── AprovarOrcamentoCommand.java
│   │   ├── ReprovarOrcamentoCommand.java
│   │   ├── FinalizarDiagnosticoCommand.java  — (descricao)
│   │   ├── IniciarExecucaoCommand.java       — (observacao)
│   │   ├── FinalizarExecucaoCommand.java     — (observacao)
│   │   ├── CancelarOSCommand.java
│   │   ├── CreateUserCommand.java            (record)
│   │   ├── CreateClienteCommand.java         (record)
│   │   ├── UpdateClienteCommand.java         (record — no CPF, immutable)
│   │   ├── CreateVeiculoCommand.java         (record)
│   │   ├── UpdateVeiculoCommand.java         (record)
│   │   ├── CreateServicoCommand.java         (record)
│   │   ├── UpdateServicoCommand.java         (record)
│   │   ├── CreatePecaCommand.java            (record)
│   │   ├── CreateNfEntradaCommand.java       (record)
│   │   └── CreateRequisicaoCompraCommand.java (record)
│   └── out/                   # Output contracts (implemented by infrastructure)
│       ├── UserRepositoryPort.java
│       ├── ClienteRepositoryPort.java
│       ├── VeiculoRepositoryPort.java
│       ├── ServicoRepositoryPort.java
│       ├── PecaRepositoryPort.java         — uses EN method names (save, findById)
│       ├── NfEntradaRepositoryPort.java    — uses EN method names (save, findById)
│       ├── RequisicaoCompraRepositoryPort.java — uses EN method names (save, findById)
│       ├── OrdemDeServicoRepositoryPort.java
│       ├── OrcamentoRepositoryPort.java
│       ├── ItemOsRepositoryPort.java       — save, findByOsUuid, deleteByOsUuid
│       ├── OsAuditLogRepositoryPort.java
│       ├── ProcessedEventsRepositoryPort.java
│       ├── WhatsAppNotifierPort.java
│       ├── AccessTokenIssuerPort.java
│       ├── PasswordHasherPort.java
│       └── EventPublisher.java            — <T> void publish(T event)
├── exception/
│   ├── AppException.java      — SINGLE exception for ALL domain errors (unchecked RuntimeException)
│   │                             carries int status (HTTP code) — leaks transport concern
│   └── Messages.java          — ResourceBundle("messages") + MessageFormat i18n (keys)
└── event/                    # Immutable records (15 events)
    ├── UserCreatedEvent.java
    ├── ClienteCriadoEvent.java
    ├── VeiculoCriadoEvent.java
    ├── OrdemDeServicoCriadaEvent.java
    ├── OsTransitionedEvent.java
    ├── OSCanceladaEvent.java
    ├── OSEntregueEvent.java
    ├── OSFinalizadaEvent.java
    ├── DiagnosticoFinalizadoEvent.java
    ├── OrcamentoAprovadoEvent.java
    ├── CobrancaGeradaEvent.java
    ├── CobrancaEmitidaEvent.java
    ├── PagamentoConfirmadoEvent.java
    ├── EntregaConfirmadaEvent.java
    └── EstoqueMinimoAtingidoEvent.java
```

## Entity Pattern
- `@Getter`, `@Builder(access = AccessLevel.PRIVATE)`, `@ToString`
- Factory `create(...)`: generates UUID + `LocalDateTime.now()`
- Factory `reconstitute(id, ..., createdAt)`: preserves existing values (used by JPA mappers)
- `@ToString.Exclude` on `passwordHash` (User.java)
- Immutable after creation (except Servico.atualizar())

## Value Object Pattern
- `@Getter`, `@EqualsAndHashCode`, `@ToString`
- Constructor validates: null → `AppException(400)`, invalid format → `AppException(400)`
- Normalizes input (trim, lowercase, strip mask)
- Uses `Messages.get("key", args)` for error messages

## Exception Pattern (REAL vs Documented)
- **REAL**: `AppException extends RuntimeException` — single class, carries `int status`
- **NOT `DomainException`/`BusinessException`** — those do NOT exist
- **NOT `InvalidEmailException`, `UserNotFoundException`, etc.** — those do NOT exist
- All VOs and entities throw `AppException(status, Messages.get(...))`

## Key Deviations (Tech Debt / Known Inconsistencies)
1. `ItemOrcamento` is a Value Object but lives in `model/` instead of `valueobject/`
2. ~~`PecaRepositoryPort`/`NfEntradaRepositoryPort`/`RequisicaoCompraRepositoryPort` use PT-BR method names~~ **RESOLVED** — all use EN (`save()`, `findById()`)
3. `AppException` carries HTTP `int status` — infrastructure concern leaking into domain
4. Two assertion styles coexist in tests: JUnit5 (`assertEquals`) and AssertJ (`assertThat`)

## Dependencies
- **compile**: none (Java SE only)
- **provided**: lombok
- **test**: junit-jupiter, assertj-core

## Testing
- JUnit 5 pure — no Quarkus, no Mockito, no container
- `@ParameterizedTest`, `assertThrows`/`assertThatThrownBy`
- 17 test files: 12 model tests + 5 value object tests
- All use `@DisplayName` in Brazilian Portuguese
