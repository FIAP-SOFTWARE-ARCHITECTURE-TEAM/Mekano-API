# PLAN-03: Orcamento Domain/Services — Budget Generation, Approval, OS State Machine

## Goal
Implement Orcamento aggregate (separate AR) with auto-generation at diagnosis finalization, client approval/rejection, OS state machine extensions for budget flow, and SLA expiry scheduled job.

## Dependencies
- PLAN-01 complete (OrcamentoEntity, ItemOrcamentoEntity, OrcamentoPanacheRepository, ports, ALL domain events)
- Phase 1 OS domain model (StatusOS enum, transition matrix, OrdemDeServico domain class)
- Phase 1 ServicoTipo domain (for valor unitario in budget calculation)

## Requirements Covered
OS-09 (auto budget generation), OS-10 (client approval), OS-11 (client rejection), OS-12 (SLA expiry — foundation)

---

## Tasks

### Task 1: OS Domain Model Extensions — New State Transitions

> 🚨 **Merge Note (INC-06):** Antes de implementar este plano, o `OrdemDeServico.java` da Phase 1 precisa ser atualizado com: (a) método `reprovarOrcamento(String motivo)`, (b) campo `motivoCancelamento`, (c) modificação de `aprovarOrcamento()` para transitar para `EM_EXECUCAO` (não `APROVADA`). As alterações estão documentadas em `01-auth-os-foundation/01-PATTERNS.md` (linha 218) e `01-auth-os-foundation/01-05-PLAN.md`.
>
> Domain events (OrcamentoGeradoEvent, OrcamentoAprovadoEvent, OrcamentoReprovadoEvent, OSFinalizadaEvent) are created in PLAN-01 Task 3.5. This plan consumes them.

**Files modified:**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/OrdemDeServico.java` (if from Phase 1)
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/StatusOS.java` (if from Phase 1)

**Action:**
Add state transitions to the transition matrix for the new Phase 2 lifecycle:

> Transitions `EM_DIAGNOSTICO → AGUARDANDO_APROVACAO`, `AGUARDANDO_APROVACAO → EM_EXECUCAO`, `AGUARDANDO_APROVACAO → CANCELADA`, `EM_EXECUCAO → FINALIZADA` JÁ EXISTEM na matriz de Phase 1 (StatusOS enum). Nenhuma transição nova necessária.

> Transition methods `finalizarDiagnostico()`, `aprovarOrcamento()`, `iniciarExecucao()`, `finalizar()` JÁ EXISTEM em Phase 1. Phase 2 expande os existentes e adiciona os novos abaixo.

**Métodos NOVOS de Phase 2:**
- `void reprovarOrcamento(String motivo)` → CANCELADA. Armazena motivo.
- `void cancelarPorSLA()` → CANCELADA. Internal para job agendado.

**Métodos EXISTENTES (Phase 1) que Phase 2 expande:**
- `iniciarExecucao()` → modificar para receber `UUID mecanicoId, String observacao` e armazenar `execucaoIniciadaEm`, `mecanicoId`, `observacaoExecucao`
- `finalizar()` → modificar para receber `String observacao` e armazenar `execucaoFinalizadaEm`
- Não modificar: `finalizarDiagnostico()`, `aprovarOrcamento()`, `cancelar()`, `entregar()`

Add fields to `OrdemDeServico`:
- `LocalDateTime dataAprovacao`
- `UUID orcamentoUuid` (link to the budget)
- `UUID mecanicoId` (who executes)
- `LocalDateTime execucaoIniciadaEm`
- `LocalDateTime execucaoFinalizadaEm`
- `String observacaoExecucao`

**Verification:**
```bash
./mvnw test -pl mekano-domain -Dtest="*OrdemDeServico*"
```

---

### Task 2: OrcamentoRepositoryImpl

**Files created:**
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/OrcamentoRepositoryImpl.java`

**Action:**
- `@ApplicationScoped`, inject `OrcamentoPanacheRepository` + `OrcamentoEntityMapper`
- **`save(Orcamento)`:** `@Timeout(5s)`, `@CacheInvalidate(cacheName = "orcamentos")`. Persist cascade = ALL (items). Flush. Return mapped domain.
- **`findByUuid(UUID)`:** `@Retry(maxRetries = 3)`, `@CacheResult(cacheName = "orcamentos")`. HQL: `"uuid = ?1 AND isActive = ?2"`. Join fetch `itens`.
- **`findByOrdemDeServicoUuid(UUID osUuid)`:** HQL: `"ordemDeServicoUuid = ?1 AND isActive = ?2"`. Returns latest active budget for an OS.

**Verification:**
```bash
./mvnw test -pl mekano-infrastructure -am -Dtest="OrcamentoRepositoryImplTest"
```

---

### Task 3: OrcamentoService — Application Layer

**Files created:**
- `mekano-application/src/main/java/com/fiap/mekano/application/service/orcamento/OrcamentoService.java`

**Action:**
- `@ApplicationScoped`, `@Transactional` on all mutating methods
- Inject: `OrcamentoRepositoryPort`, `OrdemDeServicoRepositoryPort` (from Phase 1), `ServicoTipoRepositoryPort`, `PecaRepositoryPort`, `EventPublisher`

**`gerarOrcamento(UUID osUuid, List<ItemOrcamento> itens):`**
- Load `OrdemDeServico` from Phase 1 repository, validate status is `EM_DIAGNOSTICO`
- Call `os.finalizarDiagnostico()` (transition to AGUARDANDO_APROVACAO)
- Calculate `valorTotal` = sum of (servico.qtd × servico.valorUnitario) + sum of (peca.qtd × peca.valorUnitario) per D-01, D-05
- ServicoTipo.valor is fixed per tipo (D-05). Peca.valor from `pecaRepository.findById()`.
- Create `Orcamento` with calculated total, SLA expiry time (default 72h from D-18, configurable)
- Save OS + Orcamento in same TX
- Publish `OrcamentoGeradoEvent`
- Return Orcamento

**`aprovar(UUID orcamentoUuid, UUID clienteId):`**
- Load Orcamento + associated OS
- Validate OS status is `AGUARDANDO_APROVACAO`
- Validate client owns this OS (cliente UUID match per D-13)
- Validate budget not expired (if expired, throw AppException and suggest reprovar)
- Call `orcamento.aprovar()` and `os.aprovarOrcamento()`
- Save both aggregates
- Publish `OrcamentoAprovadoEvent` (with item list per D-59)
- Note: stock reservation happens asynchronously via CDI observer (PecaOrcamentoObserver in PLAN-02)

**`reprovar(UUID orcamentoUuid, UUID clienteId, String motivo):`**
- Load Orcamento + associated OS
- Validate client owns this OS
- Call `orcamento.reprovar()` and `os.reprovarOrcamento(motivo)`
- Save both aggregates
- Publish `OrcamentoReprovadoEvent`
- Note: stock reservations released via cancellation (handled later)

**`buscarPorId(UUID orcamentoUuid):`**
- Read-only. No @Transactional. Find by UUID or throw 404.

**Verification:**
```bash
./mvnw test -pl mekano-application -am -Dtest="OrcamentoServiceTest"
```

---

### Task 4: SLA Expiry Scheduled Job

**Files created:**
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/service/SlaExpiryJob.java`

**Action:**
Per Pattern 2: `@ApplicationScoped` with `@Scheduled`.

```java
@ApplicationScoped
public class SlaExpiryJob {
    
    @Inject
    OrdemDeServicoRepositoryPort osRepository;  // from Phase 1
    
    @Inject
    OrcamentoRepositoryPort orcamentoRepository;
    
    @Inject
    EventPublisher eventPublisher;
    
    @Scheduled(
        cron = "${sla.expiry.cron:0 0 */12 * * ?}",
        identity = "sla-expiry-job",
        concurrentExecution = ConcurrentExecution.SKIP
    )
    @Transactional
    void verificarExpiracaoSLA() {
        // Find all OS in AGUARDANDO_APROVACAO where budget has expired
        List<OrdemDeServico> expiradas = osRepository
            .findExpiradasAguardandoAprovacao(LocalDateTime.now());
        
        for (OrdemDeServico os : expiradas) {
            os.cancelar("SLA expirado", "sistema");
            osRepository.save(os);
            // Also mark orcamento as reprovado
            Orcamento orcamento = orcamentoRepository.findByOrdemDeServicoUuid(os.getUuid());
            if (orcamento != null) {
                orcamento.reprovar();
                orcamentoRepository.save(orcamento);
            }
        }
    }
}
```

Add to `mekano-rest/pom.xml`:
```xml
<dependency>
    <groupId>io.quarkus</groupId>
    <artifactId>quarkus-scheduler</artifactId>
</dependency>
```

Add to `application.properties`:
```properties
sla.expiry.cron=0 0 */12 * * ?
sla.expiry.hours=72
```

**Verification:**
```bash
./mvnw compile -pl mekano-rest -am
```

---

### Task 5: OrdemDeServicoRepository Extensions

**Files modified:**
- Phase 1's `OrdemDeServicoRepositoryPort` (add new methods)
- Phase 1's `OrdemDeServicoRepositoryImpl` (implement new methods)

**Action:**
Add to `OrdemDeServicoRepositoryPort`:
- `List<OrdemDeServico> findExpiradasAguardandoAprovacao(LocalDateTime cutoff)` — HQL: `"status = AGUARDANDO_APROVACAO AND isActive = ?1 AND ..."` (time comparison based on orcamento expiration — could use join or subquery)
- `List<OrdemDeServico> findByClienteUuid(UUID clienteUuid, Page page)` — for client OS list (D-14 enforcement)
- `OrdemDeServico findByUuidComOrcamento(UUID osUuid)` — fetch with budget data
- `Page<OrdemDeServico> findAllWithFilters(...)` — support for OS-16 (date, status, cliente, placa)
- `long countAllWithFilters(...)` — matching count
- `Double calcularTempoMedioExecucao(UUID servicoTipoUuid, LocalDate inicio, LocalDate fim)` — for OS-18

**Verification:**
```bash
./mvnw test -pl mekano-infrastructure -am
```

---

### Task 6: Unit + Integration Tests for Orcamento

**Files created:**
- `mekano-domain/src/test/java/com/fiap/mekano/domain/model/OrcamentoTest.java`
- `mekano-application/src/test/java/com/fiap/mekano/application/service/orcamento/OrcamentoServiceTest.java`
- `mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/repository/OrcamentoRepositoryImplTest.java`
- `mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/service/SlaExpiryJobTest.java`

**Action:**
- **OrcamentoTest (domain):** JUnit 5 pure. Test `create()` with itens (calculates valorTotal correctly). Test `aprovar()` transitions state. Test `reprovar()` transitions state. Test `isExpirado()` with past/future expiration.
- **OrcamentoServiceTest (application):** Mockito. Test `gerarOrcamento()` validates OS status, calculates total, saves both aggregates, publishes event. Test `aprovar()` with valid client (success), with wrong client (403/AppException), with expired SLA (AppException). Test `reprovar()` publishes event.
- **OrcamentoRepositoryImplTest (infrastructure):** `@QuarkusTest` + `@TestTransaction`. Test persist + find (with items cascade). Test findByOrdemDeServicoUuid.
- **SlaExpiryJobTest (infrastructure):** `@QuarkusTest` with `quarkus.scheduler.enabled=false`. Manually invoke job. Create OS in AGUARDANDO_APROVACAO with expired budget, run job, assert OS is CANCELADA and orcamento is reprovado.

**Verification:**
```bash
./mvnw test -pl mekano-domain
./mvnw test -pl mekano-application -am
./mvnw test -pl mekano-infrastructure -am
```

---

## Verification (Plan-Level)

```bash
# Full compile
./mvnw compile -pl mekano-rest -am

# Domain tests
./mvnw test -pl mekano-domain

# Application service tests
./mvnw test -pl mekano-application -am

# Infrastructure integration tests
./mvnw test -pl mekano-infrastructure -am
```

## Risk Mitigation
- **Orcamento as separate aggregate:** Both Orcamento and OS are saved in the same `@Transactional` service method. This prevents partial commits (Pitfall 5).
- **SLA expiry job:** Uses `quarkus-scheduler` with `concurrentExecution = SKIP`. `@Transactional` supported per Quarkus docs. Test with `quarkus.scheduler.enabled=false` and manual invocation.
- **Cross-aggregate transaction:** `OrcamentoService.aprovar()` saves both OS (status change) and Orcamento (approval flag) in one TX. The CDI event for stock reservation fires within the same TX — if reservation fails, approval rolls back (correct behavior).
- **Client ownership validation:** `OrcamentoService.aprovar()` must verify `os.getClienteUuid() == loggedClientId`. This is enforced at the service layer (domain model check or service-level guard).
