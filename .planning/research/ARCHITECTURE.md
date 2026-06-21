# Architecture Research

**Domain:** Mechanical Workshop Management System (Ordem de Serviço, Estoque, Pagamento)
**Researched:** 2026-06-20
**Confidence:** HIGH (patterns verified against existing codebase + EventStorming docs)

## Standard Architecture

### System Overview

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         mekano-rest  (Quarkus Adapter)                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐        │
│  │User/     │ │OS        │ │Estoque   │ │Pagamento │ │Auth      │        │
│  │Resources │ │Resources │ │Resources │ │Resources │ │Resources │        │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘ └────┬─────┘        │
│       │            │            │            │            │              │
├───────┴────────────┴────────────┴────────────┴────────────┴──────────────┤
│                     mekano-application  (Use Case Layer)                   │
│  ┌───────────┐ ┌───────────┐ ┌───────────┐ ┌───────────┐                │
│  │User       │ │OS Use     │ │Estoque    │ │Pagamento  │                │
│  │Use Cases  │ │Cases      │ │Use Cases  │ │Use Cases  │                │
│  └─────┬─────┘ └─────┬─────┘ └─────┬─────┘ └─────┬─────┘                │
│        │             │             │             │                      │
│        │     OS → Estoque         │             │                      │
│        │     OS → Pagamento       │             │                      │
│        │     Pagamento → OS       │             │                      │
│        └─────────┬────────────────┴─────────────┴──────────┘              │
│                  │ Domain Events (CDI Event Bus)                          │
├──────────────────┴───────────────────────────────────────────────────────┤
│                         mekano-infrastructure                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │User JPA  │ │OS JPA    │ │Estoque   │ │Pagamento  │ │CDI Event │      │
│  │Entities  │ │Entities  │ │JPA       │ │JPA        │ │Publisher │      │
│  │          │ │          │ │Entities  │ │Entities   │ │          │      │
│  ├──────────┤ ├──────────┤ ├──────────┤ ├──────────┤ ├──────────┤      │
│  │User Repos│ │OS Repos  │ │Estoque   │ │Pagamento  │ │BCrypt    │      │
│  │(Panache) │ │(Panache) │ │Repos     │ │Repos      │ │Hasher    │      │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘      │
├──────────────────────────────────────────────────────────────────────────┤
│                          mekano-domain  (Pure Business Logic)              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐      │
│  │User      │ │OS        │ │Estoque   │ │Pagamento  │ │Domain    │      │
│  │Model     │ │Model     │ │Model     │ │Model      │ │Events    │      │
│  │  + VOs   │ │  + VOs   │ │  + VOs   │ │  + VOs    │ │          │      │
│  ├──────────┤ ├──────────┤ ├──────────┤ ├──────────┤ ├──────────┤      │
│  │Input/    │ │Input/    │ │Input/    │ │Input/    │ │Ports     │      │
│  │Output    │ │Output    │ │Output    │ │Output    │ │(in/out)  │      │
│  │Ports     │ │Ports     │ │Ports     │ │Ports     │ │          │      │
│  └──────────┘ └──────────┘ └──────────┘ └──────────┘ └──────────┘      │
└──────────────────────────────────────────────────────────────────────────┘
                            │
                            ▼
┌──────────────────────────────────────────────────────────────────────────┐
│                      PostgreSQL 16 (Single Database)                       │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐   │
│  │users,        │ │clientes,     │ │itens_estoque,│ │ordens_       │   │
│  │refresh_tokens│ │veiculos,     │ │reservas_     │ │pagamento,    │   │
│  │              │ │ordens_servico,│ │estoque,      │ │cobrancas,    │   │
│  │              │ │itens_os,     │ │requisicoes_  │ │pagamentos,   │   │
│  │              │ │orcamentos    │ │compra,       │ │entregas      │   │
│  │              │ │              │ │notas_fiscais │ │              │   │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘   │
└──────────────────────────────────────────────────────────────────────────┘
```

### Bounded Context Map

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Monolith Boundary                               │
│                                                                      │
│  ┌─────────────────────┐    Domain Events (CDI in-process)          │
│  │                     │────────────────────────────────┐            │
│  │  Ordem de Serviço   │                                │            │
│  │                     │  OrcamentoAprovadoEvent ────────┤            │
│  │  AR: OrdemDeServico │  OSFinalizadaEvent ────────────┤            │
│  │  AR: Cliente        │  PagamentoConfirmadoEvent ◄────┤            │
│  │  AR: Veiculo        │                                │            │
│  │  AR: Servico (cat.) │◄───────────────────────────────┘            │
│  └──────────┬──────────┘                                            │
│             │                        ┌─────────────────────┐        │
│             │────────────┐           │  Gestão de Estoque   │        │
│             │            │           │                     │        │
│             │            └──────────►│  AR: Estoque        │        │
│             │                        │  AR: RequisicaoDe-  │        │
│             │                        │      Compra         │        │
│             │   EstoqueReservadoEvent│                     │        │
│             │◄───────────────────────│  AR: NotaFiscal     │        │
│             │                        └──────────┬──────────┘        │
│             │                                   │                   │
│             │                                   │                   │
│             │            ┌─────────────────────┐│                   │
│             │            │  Ordem de Pagamento  ││                   │
│             │───────────►│                     ││                   │
│             │            │  AR: OrdemDePagamento││                   │
│             │◄───────────│                     ││                   │
│             │            │  AR: EntregaVeiculo  ││                   │
│             │            └─────────────────────┘│                   │
│  ┌─────────────────────┐                        │                   │
│  │  User & Auth (exist.)│                       │                   │
│  │                     │                       │                   │
│  │  AR: User           │                       │                   │
│  └─────────────────────┘                       │                   │
│                                                │                   │
│  ┌─────────────────────┐                       │                   │
│  │  Shared Kernel      │                       │                   │
│  │  (Hybrid ID,         │                       │                   │
│  │   BaseEntity,       │                       │                   │
│  │   EventPublisher,   │                       │                   │
│  │   ApiException)     │◄──────────────────────┘                   │
│  └─────────────────────┘                                           │
└─────────────────────────────────────────────────────────────────────┘
```

**Key architectural decision**: Bounded contexts communicate via **domain events** only — no direct use case calls across contexts. This preserves autonomy and allows future extraction. Within a monolith, the CDI event bus (`jakarta.enterprise.event.Event.fire()`) provides in-process, synchronous, in-transaction event delivery. For eventual consistency, event listeners that fail do not roll back the publisher's transaction.

### Component Responsibilities

| Component | Responsibility | Typical Implementation |
|-----------|----------------|------------------------|
| `OrdemDeServico` (AR) | OS lifecycle state machine, item list, orçamento | Domain entity with `@Builder(PRIVATE)`, `create()`/`reconstitute()` factories |
| `Cliente` (AR) | Client registration, CPF/CNPJ validation, uniqueness | Domain entity, own aggregate |
| `Veiculo` (AR) | Vehicle registration, plate uniqueness, link to client | Domain entity, own aggregate |
| `Orcamento` (entity) | Budget generation, approval workflow, SLA expiry | Entity inside OS aggregate |
| `ItemOS` (entity) | Services/parts included in OS | Entity inside OS aggregate |
| `Estoque` (AR) | Inventory balance, reservation, minimum stock | Domain entity |
| `ItemEstoque` (entity) | Individual item with quantity, reserve flag, min level | Entity inside Estoque aggregate |
| `ReservaEstoque` (entity) | Part reservation linked to OS | Entity inside Estoque aggregate |
| `RequisicaoDeCompra` (AR) | Purchase requisition when stock insufficient | Separate aggregate root |
| `NotaFiscal` (AR) | NF entry registration, stock update | Separate aggregate root |
| `OrdemDePagamento` (AR) | Payment tracking, cobrança emission, delivery release | Domain entity |
| `Cobranca` (entity) | Invoice details, external reference | Entity inside Pagamento aggregate |
| `Pagamento` (entity) | Payment confirmation, method, timestamp | Entity inside Pagamento aggregate |
| `EntregaVeiculo` (entity) | Vehicle delivery record | Entity inside Pagamento aggregate |
| `Servico` (AR) | Service catalog (type, description, default value) | Standalone aggregate |
| `CdiEventPublisher` | Dispatches domain events via CDI Event bus | `jakarta.enterprise.event.Event<Object>.fire()` |
| `EventConsumer` | Listens for domain events, triggers cross-context reactions | CDI `@Observes` methods in use cases |

## Recommended Package Structure (Within Each Module)

The existing 4-module structure is preserved. New bounded contexts follow the same sub-package conventions as the existing User/Auth context.

```
mekano-domain/src/main/java/com/fiap/mekano/domain/
├── model/                     # Domain entities (all contexts)
│   ├── user/                  # Existing User.java
│   ├── ordem_servico/         # NEW: OrdemDeServico.java, Cliente.java, Veiculo.java, Orcamento.java, ItemOS.java, Servico.java
│   ├── estoque/               # NEW: Estoque.java, ItemEstoque.java, ReservaEstoque.java, RequisicaoDeCompra.java, NotaFiscal.java
│   └── pagamento/             # NEW: OrdemDePagamento.java, Cobranca.java, Pagamento.java, EntregaVeiculo.java
├── valueobject/               # Value objects (all contexts)
│   ├── Email.java (existing)
│   ├── CpfCnpj.java           # Validated CPF/CNPJ
│   ├── Placa.java             # Validated license plate
│   └── ... per context
├── port/
│   ├── in/                    # Input ports grouped by context
│   │   ├── user/ (existing)
│   │   ├── ordem_servico/     # NEW
│   │   ├── estoque/           # NEW
│   │   └── pagamento/         # NEW
│   └── out/                   # Output ports grouped by context
│       ├── user/ (existing)
│       ├── ordem_servico/     # NEW
│       ├── estoque/           # NEW
│       └── pagamento/         # NEW
├── event/                     # Domain events (all contexts)
│   ├── user/ (existing)
│   ├── ordem_servico/         # NEW
│   ├── estoque/               # NEW
│   └── pagamento/             # NEW
└── exception/                 # Exceptions (all contexts)

mekano-application/src/main/java/com/fiap/mekano/application/
└── usecase/
    ├── user/ (existing)
    ├── ordem_servico/          # NEW
    ├── estoque/                # NEW
    └── pagamento/              # NEW

mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/
├── entity/
│   ├── user/ (existing)
│   ├── ordem_servico/         # NEW
│   ├── estoque/               # NEW
│   └── pagamento/             # NEW
├── repository/
│   ├── user/ (existing)
│   ├── ordem_servico/         # NEW
│   ├── estoque/               # NEW
│   └── pagamento/             # NEW
├── mapper/
│   ├── user/ (existing)
│   ├── ordem_servico/         # NEW: JPA ↔ Domain entity mappers
│   ├── estoque/               # NEW
│   └── pagamento/             # NEW
├── event/ (existing — CdiEventPublisher shared)
├── security/ (existing — shared)
└── service/ (existing — shared)

mekano-rest/src/main/java/com/fiap/mekano/rest/
└── api/
    ├── user/ (existing)
    ├── ordem_servico/          # NEW: Resources, DTOs, mappers
    ├── estoque/                # NEW
    └── pagamento/              # NEW
```

### Structure Rationale

- **Group by bounded context within each layer**: Keeps code organized by domain concept, not by technical role. Prevents cross-contamination between contexts. A developer working on OS only touches `order_servico/` packages across all layers.
- **Shared infrastructure layer**: `CdiEventPublisher`, `BaseEntity`, `ApiExceptionMapper`, security utils remain shared — they are technical concerns, not domain concerns.
- **Separate domain event packages per context**: Events are namespaced by context to prevent naming collisions and make event flow explicit.

## Architectural Patterns

### Pattern 1: State Machine for OrdemDeServico Lifecycle

**What:** A strict state machine controlling the lifecycle of `OrdemDeServico`. Transitions are explicit methods on the aggregate root — not setters. Each method validates current state before transitioning.

**When to use:** Any entity with a well-defined lifecycle and business rules per transition. Essential for OS workflow where invalid transitions (e.g., jumping directly to ENTREGUE from RECEBIDA) must be impossible at the domain level.

**Trade-offs:**
- ++ Compile-time safety for transitions — impossible to reach invalid states
- ++ Business rules are co-located with state (each transition method has its own validation)
- -- Requires more code than a simple setStatus() approach
- -- Transitions get complex when external validation is needed (e.g., "can't finalize without orçamento aprovado")

**State Diagram:**

```
                      ┌──────────┐
                      │ RECEBIDA │
                      └────┬─────┘
                           │ iniciarDiagnostico()
                           ▼
                   ┌───────────────┐
               ┌──│ EM_DIAGNOSTICO │
               │  └───────┬───────┘
               │          │ finalizarDiagnostico()
               │          ▼
               │  ┌──────────────────────┐
               │  │ AGUARDANDO_APROVACAO │
               │  └──┬───────────────┬───┘
               │     │               │
               │     │ aprovar()     │ reprovar() / expirar SLA()
               │     ▼               ▼
               │  ┌─────────────┐ ┌───────────┐
               │  │ EM_EXECUCAO │ │ CANCELADA │◄──── (any state can cancel)
               │  └──────┬──────┘ └───────────┘
               │         │ finalizarExecucao()
               │         ▼
               │  ┌─────────────┐
               │  │ FINALIZADA  │
               │  └──────┬──────┘
               │         │ entregar()
               │         ▼
               │  ┌───────────┐
               └──│ ENTREGUE  │
                  └───────────┘
```

**Example:**

```java
// Domain entity — transitions are EXPLICIT methods, NOT setters
public class OrdemDeServico {
    private StatusOS status;
    private Orcamento orcamento;
    private List<ItemOS> itens;

    public void iniciarDiagnostico() {
        if (status != StatusOS.RECEBIDA) {
            throw new DomainException("Só é possível iniciar diagnóstico de OS Recebida");
        }
        this.status = StatusOS.EM_DIAGNOSTICO;
    }

    public void incluirServicosInsumos(List<ItemOS> novosItens) {
        if (status != StatusOS.EM_DIAGNOSTICO) {
            throw new DomainException("Só é possível incluir itens durante o diagnóstico");
        }
        this.itens.addAll(novosItens);
    }

    public Orcamento finalizarDiagnostico(PoliticaSLA sla) {
        if (status != StatusOS.EM_DIAGNOSTICO) {
            throw new DomainException("Só é possível finalizar diagnóstico em andamento");
        }
        this.orcamento = Orcamento.gerar(this.itens, sla);
        this.status = StatusOS.AGUARDANDO_APROVACAO;
        return this.orcamento;
    }

    public void aprovarOrcamento() {
        if (status != StatusOS.AGUARDANDO_APROVACAO) {
            throw new DomainException("Só é possível aprovar orçamento pendente");
        }
        if (orcamento.estaExpirado()) {
            this.status = StatusOS.CANCELADA;
            throw new BusinessException("Orçamento expirado — OS cancelada");
        }
        this.orcamento.aprovar();
        this.status = StatusOS.EM_EXECUCAO;
    }

    public void reprovarOrcamento() {
        if (status != StatusOS.AGUARDANDO_APROVACAO) {
            throw new DomainException("Só é possível reprovar orçamento pendente");
        }
        this.orcamento.reprovar();
        this.status = StatusOS.CANCELADA;
    }

    public void iniciarExecucao() {
        if (status != StatusOS.EM_EXECUCAO) {
            throw new DomainException("Só é possível iniciar execução de OS aprovada");
        }
        // Estoque já deve ter sido reservado — verificação está no use case
    }

    public void finalizarExecucao() {
        if (status != StatusOS.EM_EXECUCAO) {
            throw new DomainException("Só é possível finalizar execução em andamento");
        }
        this.status = StatusOS.FINALIZADA;
    }

    public void entregar() {
        if (status != StatusOS.FINALIZADA) {
            throw new DomainException("Só é possível entregar OS finalizada");
        }
        this.status = StatusOS.ENTREGUE;
    }

    public void cancelar() {
        if (status == StatusOS.CANCELADA || status == StatusOS.ENTREGUE) {
            throw new DomainException("OS já cancelada ou entregue não pode ser cancelada");
        }
        this.status = StatusOS.CANCELADA;
    }

    public void expirarSLA() {
        if (status == StatusOS.AGUARDANDO_APROVACAO && orcamento.estaExpirado()) {
            this.status = StatusOS.CANCELADA;
        }
    }
}
```

**Key validation rules per transition:**

| Transition | From | To | Guard Condition |
|------------|------|----|-----------------|
| `iniciarDiagnostico()` | RECEBIDA | EM_DIAGNOSTICO | Status must be RECEBIDA |
| `finalizarDiagnostico()` | EM_DIAGNOSTICO | AGUARDANDO_APROVACAO | At least 1 item must exist |
| `aprovarOrcamento()` | AGUARDANDO_APROVACAO | EM_EXECUCAO | Orçamento not expired |
| `reprovarOrcamento()` | AGUARDANDO_APROVACAO | CANCELADA | None |
| `expirarSLA()` | AGUARDANDO_APROVACAO | CANCELADA | Orçamento expired |
| `iniciarExecucao()` | EM_EXECUCAO | EM_EXECUCAO | (approval already verified) |
| `finalizarExecucao()` | EM_EXECUCAO | FINALIZADA | None |
| `entregar()` | FINALIZADA | ENTREGUE | Pagamento confirmado (checked in use case) |
| `cancelar()` | Any except CANCELADA/ENTREGUE | CANCELADA | Not already cancelled/delivered |

### Pattern 2: Domain Events for Inter-Context Communication

**What:** Bounded contexts communicate exclusively via domain events. When an aggregate executes a state transition that has cross-context effects, it publishes a domain event. Other contexts listen via CDI `@Observes` and react. The event publisher (`CdiEventPublisher`) uses `jakarta.enterprise.event.Event<Object>.fire()` — synchronous, in-process, transactional.

**When to use:** Any cross-context action within the monolith. Preserves bounded context autonomy — the OS context never directly calls Estoque use cases.

**Trade-offs:**
- ++ Contexts remain decoupled — could extract to microservices later
- ++ Events are recorded in domain layer (pure Java records) — no framework leak
- ++ Transactional by default — publisher and listener share the same transaction
- -- Synchronous — listener failure rolls back publisher's transaction (mitigated by error handling)
- -- In-process only — events lost if JVM crashes before listener processes (acceptable for monolith MVP)

**Event Flow Map:**

```
┌──────────────────────────────────────────────────────────────────────┐
│                       Domain Event Flow                              │
│                                                                      │
│  OS Context:                         Estoque Context:                │
│  ┌─────────────────────┐            ┌─────────────────────┐         │
│  │ OrcamentoAprovado   │───────────►│ @Observes +Reservar │         │
│  │ (aprovacao OS)      │            │ peças p/ OS         │         │
│  └─────────────────────┘            └─────────┬───────────┘         │
│                                               │                      │
│                        ┌──────────────────────┐                     │
│                        │ EstoqueReservadoEvent │──── (info only)     │
│                        │ (ou)                 │                     │
│                        │ ReqCompraNecessaria   │                     │
│                        └──────────────────────┘                     │
│                                                                      │
│  OS Context:                          Pagamento Context:            │
│  ┌─────────────────────┐            ┌─────────────────────┐         │
│  │ OSFinalizadaEvent    │───────────►│ @Observes +Emitir    │        │
│  │ (finalizacao exec)   │            │ Cobranca            │         │
│  └─────────────────────┘            └─────────┬───────────┘         │
│                                               │                      │
│                        ┌──────────────────────┐                     │
│                        │ CobrancaEmitidaEvent  │──── (info only)    │
│                        └──────────────────────┘                     │
│                                                                      │
│  Pagamento Context:                        OS Context:              │
│  ┌─────────────────────┐            ┌─────────────────────┐         │
│  │ PagamentoConfirmado │───────────►│ @Observes +Liberar   │        │
│  │ (confirmacao pagto) │            │ Entrega (entregar())│         │
│  └─────────────────────┘            └─────────────────────┘         │
│                                                                      │
│  Pagamento Context:                        OS Context:              │
│  ┌─────────────────────┐            ┌─────────────────────┐         │
│  │ EntregaRealizadaEvent│──────────►│ @Observes +Atualizar │        │
│  │ (veiculo entregue)   │           │ OS para ENTREGUE     │        │
│  └─────────────────────┘            └─────────────────────┘         │
└──────────────────────────────────────────────────────────────────────┘
```

**Example:**

```java
// === DOMAIN LAYER ===

// Event record in mekano-domain/src/main/java/.../domain/event/ordem_servico/
public record OrcamentoAprovadoEvent(
    UUID osId,
    UUID clienteId,
    UUID veiculoId,
    List<ItemOS> itensNecessarios,
    LocalDateTime ocorreuEm
) {
    public static OrcamentoAprovadoEvent of(OrdemDeServico os) {
        return new OrcamentoAprovadoEvent(
            os.getId(), os.getClienteId(), os.getVeiculoId(),
            os.getItens(), LocalDateTime.now()
        );
    }
}

// Event record in mekano-domain/src/main/java/.../domain/event/ordem_servico/
public record OSFinalizadaEvent(
    UUID osId,
    BigDecimal valorTotal,
    LocalDateTime ocorreuEm
) {
    public static OSFinalizadaEvent of(OrdemDeServico os) {
        return new OSFinalizadaEvent(
            os.getId(), os.getOrcamento().getValorTotal(), LocalDateTime.now()
        );
    }
}

// Event record in mekano-domain/src/main/java/.../domain/event/pagamento/
public record PagamentoConfirmadoEvent(
    UUID ordemPagamentoId,
    UUID osId,
    LocalDateTime ocorreuEm
) {
    public static PagamentoConfirmadoEvent of(OrdemDePagamento op) {
        return new PagamentoConfirmadoEvent(
            op.getId(), op.getOsId(), LocalDateTime.now()
        );
    }
}

// === APPLICATION LAYER ===

// OS use case — publishes event after state transition
@ApplicationScoped
public class AprovarOrcamentoUseCase implements AprovarOrcamentoInputPort {

    private final OrdemDeServicoRepositoryPort osRepository;
    private final EventPublisher eventPublisher;

    @Override
    @Transactional
    public OrdemDeServico execute(AprovarOrcamentoCommand command) {
        OrdemDeServico os = osRepository.findById(command.osId())
                .orElseThrow(() -> new AppException(404, "OS não encontrada"));

        os.aprovarOrcamento(); // domain state machine — may throw BusinessException
        osRepository.save(os);

        // Publish event — Estoque context listens to reserve parts
        eventPublisher.publish(OrcamentoAprovadoEvent.of(os));

        return os;
    }
}

// Estoque use case — listens for OrcamentoAprovadoEvent
@ApplicationScoped
public class ReservarEstoqueUseCase {

    private final EstoqueRepositoryPort estoqueRepository;
    private final RequisicaoCompraRepositoryPort requisicaoRepository;
    private final EventPublisher eventPublisher;

    public void onOrcamentoAprovado(@Observes OrcamentoAprovadoEvent event) {
        // This runs in the same transaction as AprovarOrcamentoUseCase
        // If it throws, the approval rolls back — this is DESIRED behavior
        // because you can't approve an OS without parts available.

        for (ItemOS item : event.itensNecessarios()) {
            if (item.getTipo() == TipoItem.PECA_INSUMO) {
                Estoque estoque = estoqueRepository.findByItemId(item.getItemId());
                if (estoque.temDisponibilidade(item.getQuantidade())) {
                    estoque.reservar(event.osId(), item.getItemId(), item.getQuantidade());
                } else {
                    // Create purchase requisition for insufficient stock
                    requisicaoRepository.save(
                        RequisicaoDeCompra.criar(event.osId(), item, MotivoRequisicao.VINCULADO_OS)
                    );
                }
            }
        }
        estoqueRepository.flush();
    }
}
```

### Pattern 3: Aggregate Design with Identity Boundaries

**What:** Each bounded context has its own aggregate roots with clear identity boundaries. Cross-aggregate references use UUIDs, not object references. Each aggregate root is responsible for its own consistency boundary.

**When to use:** Any domain modeled with DDD. Essential to prevent anemic domain models and god aggregates.

**Trade-offs:**
- ++ Clear consistency boundaries — no cross-aggregate transaction spanning
- ++ UUID references prevent accidental navigation across contexts
- -- Requires more repository calls to fetch related data
- -- Some queries need joining across aggregates (acceptable in monolith with SQL)

**Aggregate Boundaries:**

| Aggregate Root | Owned Entities | UUID References To |
|----------------|----------------|-------------------|
| `Cliente` | None | `User.createdBy` (audit) |
| `Veiculo` | None | `Cliente.id` |
| `OrdemDeServico` | `Orcamento`, `ItemOS` | `Cliente.id`, `Veiculo.id` |
| `Servico` (catalog) | None | — |
| `Estoque` | `ItemEstoque`, `ReservaEstoque` | `OrdemDeServico.id` (in Reserva) |
| `RequisicaoDeCompra` | `ItemRequisicao` | `OrdemDeServico.id`, `ItemEstoque.id` |
| `NotaFiscal` | `ItemNotaFiscal` | `RequisicaoDeCompra.id`, `ItemEstoque.id` |
| `OrdemDePagamento` | `Cobranca`, `Pagamento`, `EntregaVeiculo` | `OrdemDeServico.id` |
| `User` | None | — (auth context) |

**Why Cliente and Veiculo are separate aggregates** (not inside OrdemDeServico):
- They have their own lifecycle independent of any single OS
- A client can have multiple OS over time
- A vehicle can return for multiple services
- They must be referenceable without loading the full OS aggregate

**Why ReservaEstoque is inside Estoque aggregate** (not standalone):
- Reservation consistency with stock balance is critical — cannot reserve more than available
- Each reservation modifies `ItemEstoque.reservado` flag
- A separate Reserva aggregate would allow inconsistent reservations

### Pattern 4: Use Case as Eventual Consistency Coordinator

**What:** Use cases in the application layer act as coordinators for eventual consistency between contexts. When a use case in context A completes its primary operation, it publishes a domain event. Use cases in context B listen for that event and perform their own operations.

**When to use:** Cross-context flows where business rules span multiple aggregates/contexts.

**Trade-offs:**
- ++ Decoupled contexts — each use case only knows about its own domain
- ++ Clear transaction boundaries — each use case has its own `@Transactional`
- -- Synchronous within monolith — listener failure affects publisher (acceptable)
- -- Two-phase operations impossible without distributed tx (not needed for MVP)

**Critical Flow: Orcamento Aprovado → Estoque Reservation**

```
AprovarOrcamentoUseCase (OS context)
┌──────────────────────────────────────────────┐
│ @Transactional                               │
│ 1. Buscar OS (uuid)                          │
│ 2. Validar status AGUARDANDO_APROVACAO       │
│ 3. os.aprovarOrcamento()                     │
│ 4. osRepository.save(os)                     │
│ 5. eventPublisher.publish(                   │
│      OrcamentoAprovadoEvent(...))            │
│    ┌──────────────────────────────────────┐  │
│    │ CDI Event Bus (same thread, same tx)  │──┤──→ ReservarEstoqueUseCase.@Observes()
│    └──────────────────────────────────────┘  │       if fails → rollback
└──────────────────────────────────────────────┘
```

**Critical Flow: OS Finalizada → Emitir Cobrança**

```
FinalizarExecucaoUseCase (OS context)
┌──────────────────────────────────────────────┐
│ @Transactional                               │
│ 1. Buscar OS                                 │
│ 2. os.finalizarExecucao()                    │
│ 3. osRepository.save(os)                     │
│ 4. eventPublisher.publish(                   │
│      OSFinalizadaEvent(...))                 │
│    ┌──────────────────────────────────────┐  │
│    │ CDI Event Bus                         │──┤──→ EmitirCobrancaUseCase.@Observes()
│    └──────────────────────────────────────┘  │       creates OrdemDePagamento
└──────────────────────────────────────────────┘
```

**Critical Flow: Pagamento Confirmado → Entrega Veículo**

```
RegistrarPagamentoUseCase (Pagamento context)
┌──────────────────────────────────────────────┐
│ @Transactional                               │
│ 1. Buscar OrdemDePagamento                   │
│ 2. op.registrarPagamento(metodo, ref)        │
│ 3. opRepository.save(op)                     │
│ 4. eventPublisher.publish(                   │
│      PagamentoConfirmadoEvent(...))          │
│    ┌──────────────────────────────────────┐  │
│    │ CDI Event Bus                         │──┤──→ LiberarEntregaUseCase.@Observes()
│    └──────────────────────────────────────┘  │       calls os.entregar()
│                                              │       calls op.registrarEntrega()
└──────────────────────────────────────────────┘
```

## Data Flow

### Complete OS Lifecycle Data Flow

```
CLIENTE / ATENDENTE         SISTEMA                    MECÂNICO
       │                       │                          │
       │ 1. Cadastra Cliente   │                          │
       │─────── RF01 ─────────►│                          │
       │                       │                          │
       │ 2. Cadastra Veículo   │                          │
       │─────── RF02 ─────────►│                          │
       │                       │                          │
       │ 3. Cria OS (Recebida) │                          │
       │─────── RF04 ─────────►│                          │
       │                       │                          │
       │                       │                          │ 4. Inicia Diagnóstico
       │                       │                          │────── RF05 ────►
       │                       │                          │
       │                       │                          │ 5. Inclui serviços/peças
       │                       │                          │────── RF05 ────►
       │                       │                          │
       │                       │ 6. Gera Orçamento        │
       │                       │◄──── RF06 ──────────────│
       │                       │                          │
       │ 7. Orçamento enviado  │                          │
       │◄─────────── RF06 ─────│                          │
       │                       │                          │
       │ 8. Aprova/Reprova     │                          │
       │─────── RF07 ─────────►│                          │
       │                       │                          │
       │ [Se Aprovado]         │                          │
       │                       │ 9. OrcamentoAprovadoEvent│
       │                       │──► Estoque: reservar     │
       │                       │    peças                 │
       │                       │                          │
       │                       │                          │ 10. Inicia Execução
       │                       │                          │────── RF08 ────►
       │                       │                          │
       │                       │                          │ 11. Finaliza Execução
       │                       │                          │────── RF08 ────►
       │                       │                          │
       │                       │ 12. OSFinalizadaEvent    │
       │                       │──► Pagamento: emitir     │
       │                       │    cobrança              │
       │                       │                          │
       │ 13. Cliente paga      │                          │
       │──(serviço bancário)──►│                          │
       │                       │                          │
       │                       │ 14. PagamentoConfirmado  │
       │                       │──► OS: liberar entrega   │
       │                       │                          │
       │                       │ 15. Entrega registrada   │
       │◄───────────────────────────────────────── RF19 ──│
       │                       │                          │
```

### Database Schema Relationship

```
┌───────────────────┐     ┌──────────────────┐     ┌──────────────────────┐
│     CLIENTES      │     │  ORDENS_SERVICO  │     │   ITENS_OS           │
├───────────────────┤     ├──────────────────┤     ├──────────────────────┤
│ id (BIGSERIAL PK) │     │ id (BIGSERIAL PK)│     │ id (BIGSERIAL PK)    │
│ uuid (UNIQUE)     │◄───►│ uuid (UNIQUE)    │     │ uuid (UNIQUE)        │
│ nome              │     │ numero (UNIQUE)  │     │ ordem_servico_id FK  │
│ cpf_cnpj (UNIQUE) │     │ cliente_uuid FK  │     │ tipo (SERVICO/PECA)  │
│ email             │     │ veiculo_uuid FK  │     │ descricao            │
│ telefone          │     │ status (VARCHAR) │     │ quantidade           │
│ created_at        │     │ data_criacao     │     │ valor_unitario       │
│ updated_at        │     │ data_entrada     │     │ created_at           │
│ is_active         │     │ created_at       │     └──────────────────────┘
│ deleted_at        │     │ updated_at       │
└───────────────────┘     │ is_active        │     ┌──────────────────────┐
                          │ deleted_at       │     │   ORCAMENTOS         │
┌───────────────────┐     ├──────────────────┤     ├──────────────────────┤
│    VEICULOS       │     │ cliente_uuid     │     │ id (BIGSERIAL PK)    │
├───────────────────┤     │ veiculo_uuid     │     │ uuid (UNIQUE)        │
│ id (BIGSERIAL PK) │────►│                  │     │ ordem_servico_id FK  │
│ uuid (UNIQUE)     │     └──────────────────┘     │ valor_total          │
│ placa (UNIQUE)    │                              │ data_geracao         │
│ marca             │         │                    │ data_envio           │
│ modelo            │         │                    │ data_expiracao       │
│ ano               │         ▼                    │ status (VARCHAR)     │
│ cliente_uuid FK ──┘                              │ created_at           │
│ created_at        │     ┌──────────────────┐     │ updated_at           │
│ updated_at        │     │ ITENS_ESTOQUE    │     └──────────────────────┘
│ is_active         │     ├──────────────────┤
│ deleted_at        │     │ id (BIGSERIAL PK)│
└───────────────────┘     │ uuid (UNIQUE)    │
                          │ codigo (UNIQUE)  │
┌───────────────────┐     │ descricao        │
│   SERVICOS (cat.) │     │ unidade          │
├───────────────────┤     │ saldo_atual      │
│ id (BIGSERIAL PK) │     │ estoque_minimo   │
│ uuid (UNIQUE)     │     │ valor_unitario   │
│ nome              │     │ reservado        │
│ descricao         │     │ created_at       │
│ valor_unitario    │     │ updated_at       │
│ created_at        │     │ is_active        │
│ updated_at        │     └──────────────────┘
│ is_active         │              │
└───────────────────┘              │ 1
                                   │ N
                          ┌──────────────────┐     ┌──────────────────────┐
                          │ RESERVAS_ESTOQUE │     │  REQUISICOES_COMPRA  │
                          ├──────────────────┤     ├──────────────────────┤
                          │ id (BIGSERIAL PK)│     │ id (BIGSERIAL PK)    │
                          │ uuid (UNIQUE)    │     │ uuid (UNIQUE)        │
                          │ os_uuid FK       │     │ os_uuid FK (nullable)│
                          │ status (VARCHAR) │     │ status (VARCHAR)     │
                          │ data_reserva     │     │ data_criacao         │
                          │ created_at       │     │ created_at           │
                          └──────────────────┘     │ updated_at           │
                                   │               └──────────────────────┘
                                   │ 1                        │
                                   │ N                        │ 1
                          ┌──────────────────┐     ┌──────────────────────┐
                          │ITENS_RESERVADOS  │     │   NOTAS_FISCAIS      │
                          ├──────────────────┤     ├──────────────────────┤
                          │ reserva_id FK    │     │ id (BIGSERIAL PK)    │
                          │ item_estoque_uuid│     │ uuid (UNIQUE)        │
                          │ quantidade       │     │ numero (UNIQUE)      │
                          │ valor_unitario   │     │ fornecedor           │
                          └──────────────────┘     │ requisicao_id FK     │
                                                   │ data_cadastro        │
┌───────────────────┐                              │ created_at           │
│ ORDENS_PAGAMENTO  │                              └──────────────────────┘
├───────────────────┤
│ id (BIGSERIAL PK) │     ┌──────────────────┐
│ uuid (UNIQUE)     │     │    PAGAMENTOS    │
│ os_uuid FK        │     ├──────────────────┤
│ valor_total       │     │ id (BIGSERIAL PK)│
│ status (VARCHAR)  │     │ uuid (UNIQUE)    │
│ data_criacao      │     │ ordem_pagto_uuid │
│ created_at        │     │ valor            │
│ updated_at        │     │ data_pagamento   │
└──────┬───────────┘     │ data_confirmacao │
       │                  │ metodo (VARCHAR) │
       │ 1                │ ref_servico_banc │
       │                  │ status (VARCHAR) │
       ▼                  └──────────────────┘
┌───────────────────┐
│    COBRANCAS      │     ┌──────────────────┐
├───────────────────┤     │ ENTREGAS_VEICULO │
│ id (BIGSERIAL PK) │     ├──────────────────┤
│ uuid (UNIQUE)     │     │ id (BIGSERIAL PK)│
│ ordem_pagto_uuid  │     │ uuid (UNIQUE)    │
│ valor             │     │ ordem_pagto_uuid │
│ data_emissao      │     │ os_uuid          │
│ metodo (VARCHAR)  │     │ data_entrega     │
│ ref_externa       │     │ responsavel      │
│ created_at        │     │ cliente_recebedor│
└───────────────────┘     │ created_at       │
                          └──────────────────┘
```

**Schema Design Decisions:**

- **UUIDs for cross-aggregate references**: Foreign keys use `UUID` (the public UUID of the referenced aggregate), not `BIGSERIAL`. This prevents enumeration attacks and makes aggregate boundaries explicit at the schema level.
- **Status as VARCHAR with CHECK constraint**: Not an enum type — simpler Flyway migrations, easier to evolve. Application enforces valid values.
- **No ON DELETE CASCADE across aggregates**: Aggregates are independent — client deletion should not cascade-delete OS records. Soft delete everywhere.
- **`os_uuid FK` in multiple tables**: Pagamento, Reserva, Requisição all reference OS by UUID. No single FK path — each context owns its reference.

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| **0-10 workshops** (MVP target) | Monolith with current Clean Architecture. Single PostgreSQL. DevServices for tests. No changes needed. |
| **10-100 workshops / 10K OS/mo** | Read replicas for public status queries. Caffeine cache expansion. Connection pooling tuning. |
| **100+ workshops / 100K OS/mo** | Extract Estoque context into separate service (it has the most distinct scaling profile). Introduce message broker (RabbitMQ/Kafka) for reliable event delivery between services. |

### Scaling Priorities

1. **First bottleneck — Public OS status queries (RF09):** These are read-only, unauthenticated, and likely high-frequency (clients polling). Mitigation: Cache with short TTL + dedicated read endpoint that bypasses use case layer (same as existing D-06 pattern). Consider read replica if load grows.

2. **Second bottleneck — Eventual consistency failures in monolith:** If CDI event listeners fail repeatedly, the publisher's transaction rolls back. Mitigation: `@Observes(notifyObserver = Reception.IF_EXISTS)` for non-critical events. For critical events, consider `@ObservesAsync` + retry table (outbox pattern).

3. **Third bottleneck — Estoque reservation concurrency:** Multiple OS approving simultaneously could race on stock reservation. Mitigation: `@Lock(LockType.PESSIMISTIC_WRITE)` on `ItemEstoque` rows during reservation. Acceptable because Estoque is the most write-contended aggregate.

## Anti-Patterns

### Anti-Pattern 1: God Aggregate — Putting Cliente + Veiculo inside OrdemDeServico

**What people do:** Nesting `Cliente` and `Veiculo` inside `OrdemDeServico` as embedded entities because "an OS always has a client and vehicle."

**Why it's wrong:** Creates a massive aggregate that must be loaded in full for every OS operation. Cliente and Veiculo have independent lifecycles — a client exists before the first OS and persists after the last. Embedding them clutters the OS aggregate and forces cascade persistence concerns.

**Do this instead:** Reference Cliente and Veiculo by UUID from OrdemDeServico. Load them separately when needed. Accept the extra repository call — it's negligible compared to the design clarity.

### Anti-Pattern 2: Direct Use Case Call Across Contexts

**What people do:** `AprovarOrcamentoUseCase` directly calling `estoqueUseCase.reservarPecas()` because "we're in a monolith anyway."

**Why it's wrong:** Creates an implicit dependency between bounded contexts. Every time the OS approval flow changes, you must verify Estoque's use case interface hasn't broken. Makes future extraction impossible — you'd have to untangle all the cross-context calls.

**Do this instead:** Always use domain events for cross-context communication, even within the monolith. The `CdiEventPublisher` + `@Observes` pattern adds negligible overhead and preserves future flexibility.

### Anti-Pattern 3: Business Rules in Infrastructure (JPA entity as domain entity)

**What people do:** Using `PanacheEntity` directly as the domain entity — annotating with `@Entity`, `@Column`, etc., and mixing JPA loading logic with business methods.

**Why it's wrong:** The existing project explicitly avoids this (User.java is pure POJO, UserEntity.java is JPA). Business rules in JPA entities make testing harder (need DB), couple domain logic to Hibernate, and prevent changing persistence technology.

**Do this instead:** Maintain the existing two-class pattern — domain entity (`OrdemDeServico.java`) with business methods and factory methods, JPA entity (`OrdemDeServicoEntity.java` that extends `BaseEntity`) with MapStruct mapping between them.

### Anti-Pattern 4: Transaction Across Multiple Use Cases

**What people do:** Starting a transaction in the OS use case and trying to extend it into the Estoque event listener to get "atomic" reservation.

**Why it's wrong:** The CDI `@Observes` listener runs in the same transaction as the publisher by default. While this provides atomicity, it means a reservation failure rolls back the OS approval — which IS desired for the approval case. But extending this pattern to all cross-context operations creates a distributed transaction nightmare.

**Do this instead:** Accept that some cross-context operations are eventually consistent. For example, when Pagamento confirms, the OS transition to ENTREGUE should succeed even if the pagamento status update fails — these are separate concerns. Use `@Observes(during = TransactionPhase.AFTER_SUCCESS)` for operations that should only run after the publisher's transaction commits.

## Integration Points

### Internal Boundaries (Bounded Context Communication)

| Boundary | Communication Mechanism | Data | Transactional? |
|----------|------------------------|------|----------------|
| OS → Estoque | `OrcamentoAprovadoEvent` via CDI | OS UUID, item list, quantities | Same tx (desired: stock reservation must succeed or approval rolls back) |
| OS → Pagamento | `OSFinalizadaEvent` via CDI | OS UUID, valor total | Same tx (desired: cobrança must be emitted) |
| Pagamento → OS | `PagamentoConfirmadoEvent` via CDI | Pagamento UUID, OS UUID | Same tx (desired: delivery release atomic with payment confirmation) |
| Estoque → OS | `EstoqueReservadoEvent` via CDI (info) | OS UUID, reservation status | After success (non-critical — OS doesn't need to wait) |

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| Banco (serviço externo simulado) | Interface no domain (`ServicoBancarioPort`), impl in infrastructure | Simulated for MVP — returns success/failure deterministically. Real impl would use HTTP client with circuit breaker |
| Email (envio de orçamento) | Interface no domain (`NotificadorPort`), impl in infrastructure | Simulated for MVP — just logs. Real impl would use SMTP or transactional email service |

### User/Auth Context Integration

The existing User/Auth context has no domain events that the new contexts need to consume. However, the new contexts should use `BaseEntity` (audit fields: `createdBy`, `updatedBy`) to record which authenticated user performed each operation.

**Audit pattern for new contexts:**
```java
// In each use case, inject the current user's identity
@ApplicationScoped
public class CriarOSUseCase implements CriarOSInputPort {
    // inject JWT claims to get current user UUID
    @Inject @Claim("sub")
    String currentUserId;

    @Override
    @Transactional
    public OrdemDeServico execute(CriarOSCommand command) {
        // ... business logic ...
        OrdemDeServico os = OrdemDeServico.create(command);
        osRepository.save(os, UUID.fromString(currentUserId));
        return os;
    }
}
```

## Build Order (Phase Recommendations)

### Recommended Implementation Order

```
Phase 1: OS Core (Cliente, Veiculo, Servico, OS lifecycle)
  ├── Depends on: User/Auth context (existing)
  ├── Builds: Cliente CRUD, Veiculo CRUD, Servico catalog
  ├── Builds: OS creation, diagnóstico, orçamento (state machine)
  ├── Builds: OS approval/reproval, SLA expiry
  ├── Builds: OS finalization, delivery
  └── Tests: State machine transitions, complete OS lifecycle

Phase 2: Estoque (inventory management)
  ├── Depends on: Phase 1 (needs OrcamentoAprovadoEvent)
  ├── Builds: ItemEstoque CRUD, minimum stock calculation
  ├── Builds: Reservation on OS approval (listens to event)
  ├── Builds: Purchase requisition generation
  ├── Builds: NF registration, stock update
  ├── Builds: Stock withdrawal on OS execution start
  └── Tests: Reservation flow, requisition flow, NF entry

Phase 3: Pagamento (payment processing)
  ├── Depends on: Phase 1 (needs OSFinalizadaEvent)
  ├── Builds: OrdemDePagamento creation (listens to event)
  ├── Builds: Cobranca emission
  ├── Builds: Pagamento registration + bank service simulation
  ├── Builds: Delivery release (listens to event → OS.entregar())
  └── Tests: Complete payment flow, bank integration simulation
```

### Dependency Graph
```
User/Auth (exists) → Phase 1 (OS Core) → Phase 2 (Estoque)
                                       → Phase 3 (Pagamento)
```

Phase 2 and 3 are independent of each other and can be built in parallel by different team members.

### Existing Codebase Evolution

The existing `mekano-domain/src/main/java/com/fiap/mekano/domain/model/User.java` pattern (POJO with `create()`/`reconstitute()` factories, `@Builder(PRIVATE)`) must be replicated for every new entity. The `BaseEntity` + `PanacheEntityBase` pattern in infrastructure provides the JPA mapping foundation.

**What to reuse as-is:**
- `CdiEventPublisher` — no changes needed, works for any domain event
- `EventPublisher` interface — no changes needed
- `BaseEntity` — new JPA entities extend this
- `ApiExceptionMapper` — no changes needed
- `UserResource` (existing auth endpoints) — keep for admin management, but new contexts need separate resources

**What to refactor before expansion (low priority):**
- Package structure: Consider moving context-specific files into sub-packages within each module. Currently everything is flat in `model/`, `port/`, etc. Not blocking — can be done incrementally as each context is added.
- No other refactoring needed — the existing patterns are well-suited for expansion.

## Sources

- **Existing codebase**: `mekano-domain/`, `mekano-application/`, `mekano-infrastructure/`, `mekano-rest/` — patterns verified against compiled code
- **EventStorming documentation**: `docs/EventStorming_Mermaid.md` — aggregate definitions, domain events, state machine
- **Project requirements**: `docs/MEKANO_DOCUMENTATION.md` — functional requirements for all three contexts
- **Current architecture analysis**: `.planning/codebase/ARCHITECTURE.md` — layer responsibilities, data flow patterns
- **Codebase structure**: `.planning/codebase/STRUCTURE.md` — package conventions, where to add new code
- **Domain layer conventions**: `CLAUDE.md` in each module — detailed conventions for domain, application, infrastructure patterns

---

*Architecture research for: Mechanical Workshop Management System (Ordem de Serviço, Estoque, Pagamento)*
*Researched: 2026-06-20*
