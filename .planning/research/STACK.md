# Stack Research

**Domain:** Mechanical workshop management system (Ordem de Serviço, Estoque, Pagamento)
**Researched:** 2026-06-20
**Confidence:** HIGH (verified with multiple sources: Quarkus docs, GitHub discussions, community patterns)

> **Context:** This builds on the existing stack (Java 17, Quarkus 3.36.0, PostgreSQL 16, Maven multi-module, Clean Architecture). See `.planning/codebase/STACK.md` for the existing foundation. This document adds the libraries and patterns needed for the 3 new bounded contexts.

---

## Recommended Stack

### Core Domain Libraries (New Additions)

| Library | Version | Purpose | Why Recommended |
|---------|---------|---------|-----------------|
| **cpf-cnpj-utils** | 1.0.0-alpha | Brazilian CPF/CNPJ validation, generation, formatting | Zero dependencies, Java 17+, supports **alphanumeric CNPJ** (RFB NT 2025.001, mandatory from July 2026), clean API with `CpfUtils`/`CnpjUtils` static methods. Only library with active 2025-2026 maintenance covering both CPF and CNPJ. |
| **ArchUnit** | 1.3.0 | Architecture boundary enforcement in tests | Only reliable way to **enforce bounded context isolation** in a modular monolith. Catches `UnsatisfiedResolutionException`-type coupling problems at compile/test time. JUnit 5 native. CI-fails builds on violations. **Non-negotiable for multi-context monoliths.** |
| **cnpj-alfanumerico** | 1.1.0 | CNPJ Bean Validation + Jackson support | If using `@CNPJ` annotation on DTOs (Bean Validation) or Jackson ser/deser for CNPJ types. Zero runtime deps, Apache 2.0 license. Complements cpf-cnpj-utils for the REST layer. |
| **AssertJ** | 3.27.3 | Fluent test assertions | Already in project. Keep using — its `extracting()`, `filteredOn()`, and `tuple()` API is essential for multi-entity integration tests. |

**Confidence: HIGH** — all libraries verified on Maven Central and GitHub with 2025-2026 release dates.

### Event Bus (In-Process)

| Technology | Version | Purpose | Why |
|------------|---------|---------|-----|
| **CDI Events** (`jakarta.enterprise.event.Event`) | Built into Quarkus 3.36 | Integration events between bounded contexts | Quarkus discussion #51183 confirms: "Each module = one bounded context. Modules communicate via CDI Events or Messaging (Kafka). CDI Domain Events recommended for monolith." Zero infrastructure, type-safe, same-transaction by default. This is the industry-standard pattern for modular monoliths (equivalent to Spring Modulith's ApplicationEvents). |
| **CDI `@ObservesAsync`** | Built into Quarkus 3.36 | Non-blocking cross-context reactions | `fireAsync()` + `@ObservesAsync` for inter-context events that don't need to block the caller. Example: `OrdemServicoFinalizada` fires async → Estoque context updates stock in background. |

**Confidence: HIGH** — verified against Quarkus docs, GitHub discussions, and community patterns.

### Supporting Libraries

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| **MapStruct** | 1.6.3 | DTO ↔ Domain mapping (already in project) | Already configured. Keep annotation processor order: Lombok → binding → MapStruct. |
| **Lombok** | 1.18.36 | Boilerplate reduction (already in project) | Already configured. Use `@Builder` on command objects for bounded contexts. |
| **SmallRye Fault Tolerance** | (managed by Quarkus BOM) | `@Retry`, `@Timeout` | Already in project for repository resilience. Add `@CircuitBreaker` if external bank API integration in Pagamento context. |
| **Quarkus Cache (Caffeine)** | (managed by Quarkus BOM) | In-memory cache | Already configured for user queries. Extend to product/stock queries. |
| **Hibernate Validator** | (managed by Quarkus BOM) | Bean Validation on DTOs | Already in project. Add custom `@CPF`/`@CNPJ`/`@Placa` annotations. |

### Development Tools

| Tool | Purpose | Notes |
|------|---------|-------|
| **ArchUnit** | Enforce Clean Architecture + bounded context isolation | Must run as `@AnalyzeClasses` in a dedicated test class per bounded context. Configure to fail the build in CI. |
| **Quarkus Component Test** | Fast CDI-only tests (since Quarkus 3.22) | Use `@QuarkusComponentTest` instead of full `@QuarkusTest` for service-level logic without HTTP/database. Cuts test execution time ~80%. |
| **DevServices** | Auto-managed PostgreSQL in tests | Already configured. One PostgreSQL container shared across all bounded context tests — no per-context database needed in a monolith. |
| **Flyway** | Schema migrations per bounded context | Already configured. Name migrations by context: `V1__os_schema.sql`, `V2__estoque_schema.sql`, `V3__pagamento_schema.sql`. No separate migration modules needed since it's the same database. |

---

## Dependencies: What to Add to `pom.xml`

### mekano-domain (zero deps — NO changes)

Domain layer stays pure Java. Value objects handle validation inline or through static helpers. CPF/CNPJ validation uses pure Java algorithm (Mod 11) in the value object itself.

### mekano-infrastructure

```xml
<!-- Brazilian Document Validation (infrastructure/rest layer only) -->
<dependency>
    <groupId>io.github.felseje</groupId>
    <artifactId>cpf-cnpj-utils</artifactId>
    <version>1.0.0-alpha</version>
</dependency>

<!-- CNPJ Bean Validation annotation support (optional, for REST DTOs) -->
<dependency>
    <groupId>io.github.johnpitter</groupId>
    <artifactId>cnpj-alfanumerico</artifactId>
    <version>1.1.0</version>
</dependency>
```

### mekano-rest

```xml
<!-- No additional runtime deps needed -->
```

### Test Dependencies (add to mekano-rest or dedicated test module)

```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

---

## Alternatives Considered

| Recommended | Alternative | Why Not |
|-------------|-------------|---------|
| **cpf-cnpj-utils** (felseje) | `com.danielfariati:cpf-cnpj-validator:1.1` | Last updated **2012**. Uses `javax.validation` (not Jakarta). No alphanumeric CNPJ support. Dead project. |
| **cpf-cnpj-utils** (felseje) | `com.jereztech:validation-br-api:1.3` | **GPLv3 license** — incompatible with proprietary/commercial use. Also Spring Boot-specific auto-configuration. |
| **cpf-cnpj-utils** (felseje) | `io.github.andrelamego:br-validator:1.4.0` | **Spring Boot starter** — not usable with Quarkus CDI. Depends on Spring's `Validator` interface. |
| **cpf-cnpj-utils** (felseje) | `io.github.robsonkades:cnpj:1.0.0` | CNPJ only — doesn't validate CPF. Need both for customer domain. |
| **cpf-cnpj-utils** (felseje) | Write own algorithm from scratch | Re-inventing the wheel. The Mod 11 check-digit algorithm is well-known but edge cases (blocked sequences, formatting, alphanumeric CNPJ base-36 conversion) are error-prone. Use a library. |
| **CDI Events** | Direct method call between contexts | Breaks bounded context isolation. Creates compile-time coupling. Two contexts that depend on each other's classes cannot be extracted as separate services later. |
| **CDI Events** | Kafka for in-process events | Overengineering for a monolith. Kafka adds operational complexity (broker, topics, consumer groups, schema registry) with zero benefit when both producer and consumer run in the same JVM. |
| **ArchUnit** | Manual code review | Error-prone at 5-person team velocity. ArchUnit automates the check and fails the build — no review can catch every wrong `import`. |
| **ArchUnit** | JPMS (Java modules) | JPMS `module-info.java` is compile-time only and poorly supported by frameworks (Quarkus reflection, Hibernate proxy). ArchUnit works at bytecode level and catches actual violations, not just module-declared ones. |

---

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| **`double`/`float` for monetary values** | Floating point precision errors accumulate. A service costing R$ 149.90 becomes R$ 149.899999. | `BigDecimal` with `MathContext.DECIMAL64` for domain values. `Long` (cents) for stored values if space-optimizing. |
| **`@ApplicationScoped` on JAX-RS resources** | Stale JWT claims injection (`@Context UriInfo` issue). Documented in project as G8 pitfall. | `@RequestScoped` on all resources (already enforced in existing code). |
| **JPA entities in `api/` package or cross-context** | Leaks persistence details into API contract. Entities have lazy loading, proxies, and lifecycle callbacks that break when serialized. | DTOs/records in API layer. Domain entities in domain layer. JPA entities stay inside infrastructure layer. |
| **Direct repository cross-context access** | Context A reading Context B's tables via repository creates implicit coupling. Prevents future extraction to microservice. | Only via events or explicit API/port interfaces. |
| **Synchronous HTTP calls between contexts** | Defeats purpose of modular monolith. Creates temporal coupling and cascading failures. | CDI events for in-process. If external service needed, use `@RestClient` with `@Retry` and `@CircuitBreaker`. |
| **Transactional outbox pattern inside monolith** | Unnecessary complexity when producer and consumer share same JVM + transaction. Outbox is only needed for Kafka or cross-service events. | CDI events naturally execute in the same transaction. `@Observes(during = AFTER_SUCCESS)` for transactional guarantees. |
| **Lombok `@Data` on JPA entities** | `@Data` = `@Getter` + `@Setter` + `@EqualsAndHashCode` + `@ToString`. `@EqualsAndHashCode` on entities causes issues with proxy IDs and lazy loading. | Use `@Getter` + `@Setter` explicitly. Override `equals()`/`hashCode()` using business key (UUID) if needed. |
| **Kafka/RabbitMQ for monolith** | Adds operational complexity (broker, schema registry, consumer groups) with no benefit within same JVM. | CDI events. Only add message broker if integrating with external systems or if bounded contexts are extracted as separate services. |

---

## Stack Patterns by Variant

### If bounded context needs synchronous query of another context's data:
- **Use:** A read-only `*RepositoryPort` interface in the consuming context's domain, implemented by a gateway in infrastructure that calls the other context's **public API** (not its internal repository).
- **Because:** Bounded context A should never import B's repository or entities. The gateway in infrastructure can access B's public API without violating domain purity.
- **Example:** Pagamento context needs order total for invoice — define `ConsultarOrdemPort` in Pagamento domain, implement via `OrdemServiceGateway` in infrastructure that calls `OrdemDeServicoResource` REST endpoint or shared query service.

### If event-driven eventual consistency is acceptable:
- **Use:** CDI `Event.fireAsync()` with `@ObservesAsync` listener in the consuming context.
- **Because:** Decouples producer from consumer. Producer doesn't wait for consumer to process. Error in consumer doesn't affect producer transaction.
- **Trade-off:** Consumer sees event after producer transaction commits. Not suitable for: "reserve stock before confirming order" (needs immediate consistency).

### If immediate consistency is required across contexts:
- **Use:** Same-transaction CDI `Event.fire()` + `@Observes` — or (simpler) direct service call through a **port interface**.
- **Because:** Both operations commit or rollback together. No eventual consistency window.
- **Trade-off:** Tighter coupling. Use sparingly — only for flows where inconsistency causes business harm (e.g., reserving same part for two orders).

### When to split CDI event → Kafka:
- Bounded context extracted as separate microservice
- Need durable event storage (CDI events are lost on restart)
- Need event replayability for analytics
- Need different scaling policies per context

---

## Version Compatibility

| Package | Compatible With | Notes |
|---------|-----------------|-------|
| `cpf-cnpj-utils:1.0.0-alpha` | Java 17+ | Zero deps. Safe for production — alpha tag reflects API stabilization, not stability. GitHub issues: 0 open. |
| `cnpj-alfanumerico:1.1.0` | Java 8+, Jakarta Validation 3.x | Provides `@CNPJ` annotation compatible with Quarkus Hibernate Validator. |
| `archunit-junit5:1.3.0` | JUnit 5.10+, Java 17 | Released Feb 2026. Full records, pattern matching, and sealed class support in bytecode analysis. |
| Quarkus 3.36.0 | All above | CDI `Event` API stable since Jakarta EE 8. No conflicts. |
| MapStruct 1.6.3 + Lombok 1.18.36 | Confirmed working | Annotation processor order is **critical**: Lombok → lombok-mapstruct-binding → MapStruct. Wrong order produces mappers with null fields. |

---

## Domain Value Object Patterns

### CPF/CNPJ Value Object

```java
// mekano-domain/src/main/java/.../domain/valueobject/Documento.java
public sealed abstract class Documento permits Documento.CPF, Documento.CNPJ {
    private final String valor;  // raw digits only

    private Documento(String valor) {
        this.valor = valor;
    }

    public abstract String formatado();

    public static final class CPF extends Documento {
        public CPF(String valor) {
            super(validarCPF(somenteDigitos(valor)));
        }

        @Override
        public String formatado() {
            return String.format("%s.%s.%s-%s",
                valor.substring(0, 3), valor.substring(3, 6),
                valor.substring(6, 9), valor.substring(9, 11));
        }
    }

    public static final class CNPJ extends Documento {
        public CNPJ(String valor) {
            super(validarCNPJ(somenteDigitos(valor)));
        }

        @Override
        public String formatado() {
            return String.format("%s.%s.%s/%s-%s",
                valor.substring(0, 2), valor.substring(2, 5),
                valor.substring(5, 8), valor.substring(8, 12),
                valor.substring(12, 14));
        }
    }

    public String valor() { return valor; }
    public boolean isCPF() { return this instanceof CPF; }
    public boolean isCNPJ() { return this instanceof CNPJ; }

    private static String validarCPF(String digitos) { /* Mod 11 */ }
    private static String validarCNPJ(String digitos) { /* Mod 11 */ }
    private static String somenteDigitos(String raw) { /* strip non-digits */ }
}
```

> **Rationale:** Sealed class ensures type safety at compile time. The domain knows whether it's dealing with CPF or CNPJ without string parsing. `CpfUtils`/`CnpjUtils` from `cpf-cnpj-utils` library provides the actual Mod 11 algorithm if you don't want to reimplement it in domain.

### Placa (License Plate) Value Object

```java
// mekano-domain/src/main/java/.../domain/valueobject/Placa.java
public record Placa(String valor) {
    // Regex: 3 letters (no I/O/Q) + 1 digit + (3 digits OR 1 letter + 2 digits)
    private static final Pattern PADRAO = 
        Pattern.compile("^[A-HJ-NP-Z]{3}[0-9]([0-9]{3}|[A-HJ-NP-Z][0-9]{2})$");

    public Placa {
        var raw = valor.toUpperCase().replace("-", "");
        if (!PADRAO.matcher(raw).matches()) {
            throw new IllegalArgumentException("Placa inválida: " + valor);
        }
    }
}
```

> **Rationale:** Covers both old format (ABC1234) and Mercosul format (ABC1D23). Excludes confusing letters I, O, Q per DENATRAN rules. No external dependencies needed.

---

## Event-Driven Architecture: Integration Events Between Bounded Contexts

### Event Taxonomy

```
┌──────────────────────────────────────────────────────────────────────┐
│                        CDI Event Bus (in-JVM)                        │
│  Event.fire() = synchronous (same transaction, immediate consistency) │
│  Event.fireAsync() = async (separate transaction, eventual consistency)│
├──────────────────────────────────────────────────────────────────────┤
│                                                                       │
│  DOMAIN EVENTS (stay inside context)       INTEGRATION EVENTS         │
│  ──────────────────────────────            (between contexts)         │
│  OrcamentoAprovado                        OrcamentoAprovadoEvent      │
│  DiagnosticoIniciado                     └→ Estoque: reserva peças    │
│  ServicoExecutado                         OrdemServicoFinalizadaEvent  │
│  PagamentoRecebido                       └→ Pagamento: emitir cobrança│
│                                                                       │
│  Rule: Domain events are                    Rule: Integration events   │
│  dispatched to handlers within              contain only primitive     │
│  the same aggregate boundary.               types (serializable).      │
│  Never become integration events            Never reference domain     │
│  without explicit mapping.                  entities or VOs.           │
└──────────────────────────────────────────────────────────────────────┘
```

### Choreography: Orçamento Aprovado Flow

```java
// === CONTEXTO OS (Application Layer) ===

// Integration event record in OS module
public record OrcamentoAprovadoEvent(
    UUID ordemServicoId,
    UUID orcamentoId,
    UUID clienteId,
    UUID veiculoId,
    List<ItemOSDTO> servicos,
    List<ItemOSDTO> pecas,
    BigDecimal valorTotal
) {}

// Use case fires event after approving budget
@ApplicationScoped
public class AprovarOrcamentoUseCase {

    @Inject
    Event<OrcamentoAprovadoEvent> eventBus;

    @Transactional
    public void execute(AprovarOrcamentoCommand cmd) {
        OrdemDeServico os = repository.buscar(cmd.ordemServicoId());
        os.aprovarOrcamento();
        repository.save(os);

        // Fire integration event — Estoque context will listen
        eventBus.fireAsync(new OrcamentoAprovadoEvent(
            os.getId(), os.getOrcamento().getId(),
            os.getClienteId(), os.getVeiculoId(),
            os.getServicos(), os.getPecas(), os.getValorTotal()
        ));
    }
}

// === CONTEXTO ESTOQUE (Listener in infrastructure/events package) ===

@ApplicationScoped
public class OrcamentoAprovadoListener {

    @Inject
    ReservarPecasUseCase reservarPecas;

    @Inject
    GerarRequisicaoCompraUseCase gerarRequisicao;

    void on(@ObservesAsync OrcamentoAprovadoEvent event) {
        // Verify stock availability
        var resultado = reservarPecas.execute(
            new ReservarPecasCommand(event.ordemServicoId(), event.pecas()));

        // If stock insufficient, generate purchase requests
        resultado.pecasIndisponiveis().forEach(p -> 
            gerarRequisicao.execute(
                new GerarRequisicaoCommand(event.ordemServicoId(), p)));
    }
}
```

### When to Use Sync vs Async Events

| Scenario | Event Type | Why |
|----------|-----------|-----|
| OS created → Estoque reserves parts | `Event.fire()` sync | Must succeed or entire operation fails. Immediate consistency required. |
| OS finalized → Pagamento issues invoice | `Event.fireAsync()` async | Pagamento failure shouldn't block OS finalization. Pagamento can be retried. |
| OS delivered → OS context updates metrics | `Event.fireAsync()` async | Reporting is side-effect. No consistency requirement. |

### Saga: OS → Estoque → Pagamento (Choreography)

```
OrcamentoAprovado ──CDI──→ [Estoque: reserva peças]
                                      │
                                      ├── Sucesso: peças reservadas ──→ (OS: aguarda execução)
                                      └── Falha: req. compra gerada ──→ (OS: notifica adm)

OS Finalizada ──CDI──→ [Pagamento: emite cobrança]
                               │
                               ├── Pagamento Confirmado (via banco) ──→ [OS: ENTREGUE]
                               └── Pagamento Falhou ──→ [OS: notifica adm]
```

> **No Orchestrator needed.** Choreography-based saga works because the flow is linear and each step is independently retryable. Only add saga orchestrator (e.g., with `@Compensating` in MicroProfile LRA) if the flow has conditional branching or complex compensation logic.

---

## Testing Strategy for Multi-Context Quarkus

### Test Pyramid

```
         ╱╲
        ╱  ╲          ArchUnit Tests (1 per context)
       ╱    ╲         ─────────
      ╱ ALL  ╲        3-5 tests per bounded context
     ╱ CONTRACTS╲     
    ╱ (public API)╲    ─────────
   ╱              ╲    5-10 tests per context
  ╱ INTEGRATION    ╲   @QuarkusTest + REST Assured
 ╱  (HTTP + DB)    ╲  ─────────
╱                  ╲  10-20 tests per use case
╱ COMPONENT + UNIT  ╲  @QuarkusComponentTest / pure JUnit 5
╱ (CDI beans, VOs)  ╲ ─────────
╱                   ╲  30-50 tests per value object
╱  DOMAIN TESTS     ╲  Pure JUnit 5 (no framework)
╱  (VOs, entities)  ╲
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Layer-Specific Testing Recommendations

| Layer | Annotation | What to Test | Mock What |
|-------|-----------|-------------|-----------|
| Domain | `@Test` (JUnit 5, no Quarkus) | Value object validation (CPF, CNPJ, Placa, Email), entity state transitions, invariants | Nothing — pure logic |
| Application | `@ExtendWith(MockitoExtension.class)` | Use case orchestration, port interactions, event firing | All ports (repositories, event bus, hasher) |
| Infrastructure | `@QuarkusTest` + `@TestTransaction` | Repository CRUD, mapper conversion, event publishing | Nothing (real DB via DevServices) |
| REST | `@QuarkusTest` + REST Assured | HTTP status codes, DTO validation, security, Problem Details | `@TestSecurity` for JWT bypass |
| All layers | ArchUnit `@AnalyzeClasses` | Layer isolation, package boundaries, no cycles | Nothing — bytecode analysis |

### Cross-Context Integration Test Pattern

For testing event-driven flows end-to-end within the monolith:

```java
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class OrdemServicoFlowTest {

    @Inject
    Event<OrcamentoAprovadoEvent> eventBus;

    @Inject
    ReservaEstoqueRepository reservaRepo;

    @Inject
    OrdemDeServicoRepository osRepo;

    @Test
    @Order(1)
    void orcamentoAprovado_deveReservarPecas() {
        // Given: uma OS com serviços e peças
        var os = criarOSComPecas();

        // When: orçamento é aprovado (simula o fluxo)
        eventBus.fire(new OrcamentoAprovadoEvent(
            os.getId(), os.getOrcamento().getId(),
            os.getClienteId(), os.getVeiculoId(),
            os.getServicos(), os.getPecas(), os.getValorTotal()
        ));

        // Then: Estoque context deve ter reservado as peças
        var reservas = reservaRepo.findByOsId(os.getId());
        assertThat(reservas).isNotEmpty();
        assertThat(reservas.get(0).getStatus()).isEqualTo(StatusReserva.ATIVA);
    }

    @Test
    @Order(2)
    void orcamentoAprovado_semEstoque_deveGerarRequisicaoCompra() {
        // When: peças indisponíveis
        var os = criarOSComPecasIndisponiveis();
        eventBus.fire(new OrcamentoAprovadoEvent(/* ... */));

        // Then: requisição de compra deve ser criada
        var requisicoes = requisicaoRepo.findByOsId(os.getId());
        assertThat(requisicoes).isNotEmpty();
        assertThat(requisicoes.get(0).getMotivo())
            .isEqualTo(MotivoRequisicao.VINCULADO_OS);
    }
}
```

> **Key insight:** Since both contexts live in the same JVM, you can test the entire event-driven flow using `Event.fire()` (sync) in tests, which avoids timing issues while validating end-to-end behavior. The production code uses `fireAsync()` for async decoupling — but the test can use `fire()` with `@Observes` (not `@ObservesAsync`) to make the flow deterministic.

### ArchUnit Boundary Rules (Critical)

```java
@AnalyzeClasses(packages = "com.fiap.mekano")
class BoundedContextBoundaryTest {

    // Rule 1: Order OS context internals are NOT accessible from outside
    @ArchTest
    static final ArchRule os_internals_are_private = noClasses()
        .that().resideOutsideOfPackages("..ordemservico..")
        .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..ordemservico.domain..",
                "..ordemservico.application..",
                "..ordemservico.infrastructure.."
            )
        .because("Only OS events and API may be accessed from outside");

    // Rule 2: Estoque context internals are NOT accessible from outside
    @ArchTest
    static final ArchRule estoque_internals_are_private = noClasses()
        .that().resideOutsideOfPackages("..estoque..")
        .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..estoque.domain..",
                "..estoque.application..",
                "..estoque.infrastructure.."
            )
        .because("Only Estoque events and API may be accessed from outside");

    // Rule 3: Pagamento context internals are NOT accessible from outside
    @ArchTest
    static final ArchRule pagamento_internals_are_private = noClasses()
        .that().resideOutsideOfPackages("..pagamento..")
        .should().dependOnClassesThat()
            .resideInAnyPackage(
                "..pagamento.domain..",
                "..pagamento.application..",
                "..pagamento.infrastructure.."
            )
        .because("Only Pagamento events and API may be accessed from outside");

    // Rule 4: No JPA entities in REST/API layer
    @ArchTest
    static final ArchRule no_jpa_entities_in_api = noClasses()
        .that().resideInAPackage("..api..")
        .should().dependOnClassesThat()
            .areAnnotatedWith(jakarta.persistence.Entity.class)
        .because("DTOs must not leak JPA entities");

    // Rule 5: Cross-context access only through 'api' or 'events' packages
    @ArchTest
    static final ArchRule cross_context_only_through_api = noClasses()
        .that().resideInAPackage("com.fiap.mekano..")
        .should().accessClassesThat()
            .resideInAPackage("com.fiap.mekano.ordemservico..")
        .unless().resideInAPackage("com.fiap.mekano.ordemservico..")
        .or().accessClassesThat().resideInAnyPackage(
            "com.fiap.mekano.ordemservico.api..",
            "com.fiap.mekano.ordemservico.events.."
        )
        .because("Cross-context access must go through published API or events");

    // Rule 6: No cyclic dependencies between modules
    @ArchTest
    static final ArchRule no_cycles = slices()
        .matching("com.fiap.mekano.(*)..")
        .should().beFreeOfCycles();
}
```

---

## Sources

- [cpf-cnpj-utils GitHub](https://github.com/felseje/cpf-cnpj-utils) — Verified on Maven Central, Java 17+, zero deps, alphanumeric CNPJ support. **HIGH confidence.**
- [cnpj-alfanumerico GitHub](https://github.com/JohnPitter/cnpj-alfanumerico) — Verified, Apache 2.0 license, Bean Validation + Jackson support. **HIGH confidence.**
- [Quarkus Discussion #51183: Modular monolith with Quarkus](https://github.com/quarkusio/quarkus/discussions/51183) — Official Quarkus discussion confirming CDI events for bounded contexts. **HIGH confidence.**
- [Quarkus CDI Events Tutorial (Markus Eisele, 2025)](https://www.the-main-thread.com/p/quarkus-cdi-events-java-tutorial) — Verified CDI event patterns, sync vs async, transactional observers. **HIGH confidence.**
- [ArchUnit Quarkus BCE Tutorial (2025)](https://www.the-main-thread.com/p/quarkus-archunit-java-bce-architecture-tutorial) — ArchUnit 1.3 features, Quarkus integration patterns. **HIGH confidence.**
- [ArchUnit Part 2: Enforcing DDD Boundaries (Jitin Kayyala, 2026)](https://medium.com/javarevisited/archunit-part-2-enforcing-ddd-boundaries-across-modules-9058dfa785f0) — Specific rules for bounded context isolation. **MEDIUM confidence.**
- [Quarkus Testing: @QuarkusTest vs @QuarkusIntegrationTest (2025)](https://www.the-main-thread.com/p/quarkus-testing-quarkustest-vs-quarkusintegrationtest) — Testing strategy guidance. **HIGH confidence.**
- [Quarkus CDI reference](https://quarkus.io/guides/cdi-reference) — Official CDI events API documentation. **HIGH confidence.**
- [Denatran/RFB plate regex patterns](https://github.com/adriano-tirloni/regex-belt) — License plate format validation. **MEDIUM confidence.**
- [Modular Monolith with DDD (Krzybek)](https://github.com/kgrzybek/modular-monolith-with-ddd) — Reference architecture for integration events vs domain events pattern. **HIGH confidence.**
- [Quarkus DDD Hexagonal Architecture Blog (2026)](https://quarkus.io/blog/quarkus-insights-248-ddd-hexagonal-architecture/) — Official Quarkus blog post on DDD patterns. **HIGH confidence.**

---

*Stack research for: Mekano — Mechanical Workshop Management API*
*Researched: 2026-06-20*
