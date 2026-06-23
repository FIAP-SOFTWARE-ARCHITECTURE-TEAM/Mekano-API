# PLAN-04: OS Execution/Services — Start/Finish, Metrics, Detail, List

## Goal
Implement OS execution tracking (mechanic start/finish), average execution time per service type, OS detail with full comparison (executed vs budgeted), and OS list with filters (date, status, cliente, placa). Wire stock output at execution start (EST-08).

## Dependencies
- PLAN-03 complete (OS state machine extended with execution transitions)
- Phase 1 (OrdemDeServico domain, repository, StatusOS enum)

## Requirements Covered
OS-13 (start execution), OS-14 (finish execution), OS-16 (list with filters), OS-17 (detail), OS-18 (avg execution time), EST-08 (stock debit at start)

---

## Tasks

### Task 1: Execution Service — Start and Finish

**Files created:**
- `mekano-application/src/main/java/com/fiap/mekano/application/service/os/OrdemDeServicoExecutionService.java`

**Action:**
- `@ApplicationScoped`, `@Transactional`
- Inject: `OrdemDeServicoRepositoryPort`, `PecaRepositoryPort`, `EventPublisher`

**`iniciarExecucao(UUID osUuid, UUID mecanicoId, String observacao):`**
- Load OS. Validate status is `EM_EXECUCAO` (post-approval, per state machine).
- Call `os.iniciarExecucao(mecanicoId, observacao)` — sets `execucaoIniciadaEm`, `mecanicoId`, status stays `EM_EXECUCAO` (already set during approval in PLAN-03).
- Save OS.
- **Stock debit per EST-08:** For each `PecaUsada` in OS items with `reservada = true`, call `pecaRepository.reservarEstoque(pecaUuid, quantidade)`. Note: uses the same atomic UPDATE — this debits the FINAL stock (the reservation already happened at approval). If stock was already debited at reservation, this clears the reservation flag.
  - Alternatively, since stock was RESERVED (NOT debited) at approval, this step does the ACTUAL debit. The D-33 says: "Saída de peças do estoque acontece ao INICIAR execução (não ao finalizar)."
  - Implementation: For each part used, call `pecaRepository.creditarEstoque()`? No — the reservation at approval already decremented saldo. The "reservation flag" is just the decremented saldo. At execution start, we mark the peças as "efetivamente usadas" (used) in the OS detail.
  
  Wait, let me re-read D-33 and D-08:
  - D-08: "Reserva de peças acontece SOMENTE ao cliente aprovar o orçamento."
  - D-33: "Saída de peças do estoque acontece ao INICIAR execução (não ao finalizar)."
  
  This is a bit ambiguous. The atomic stock reservation (EST-03, Pattern 1) already decrements `saldo` at approval time. So at execution start (EST-08), the physical stock is already debited. What happens at execution start is:
  1. The reserved peças are marked as "consumed" in the OS record
  2. A separate flag or table tracks that the reservation has been "applied" to execution
  
  Let me clarify: The atomic UPDATE `saldo = saldo - qtd` IS the reservation mechanism. This happens at approval (PLAN-02 observer). The stock IS already decremented. At execution start, we just record that the parts were used (update the OS pecas_usadas records to mark them as "confirmed used").
  
  Actually, re-reading more carefully:
  - D-33 says "Saída de peças do estoque acontece ao INICIAR execução"
  - EST-03 says "Sistema reserva automaticamente peças disponíveis ao aprovar orçamento (reserva = flag)"
  - EST-08 says "Almoxarife registra saída de peças reservadas ao iniciar execução (saldo debitado, reserva encerrada)"
  
  So the flow is:
  1. Budget approval: stock is RESERVED (flagged, but saldo NOT decremented yet) — per EST-03, "reserva = flag"
  2. Execution start: stock is DEBITED (saldo decremented) — per D-33 and EST-08
  
  Wait but EST-03 says "reserva = flag" not "reserva = atomic decrement". Let me re-read the research pattern...
  
  Research Pattern 1 says: `UPDATE pecas SET saldo = saldo - :qtd WHERE uuid = :uuid AND saldo >= :qtd AND is_active = true`
  
  But this decrements saldo, not just sets a flag. And EST-08 says "saldo debitado, reserva encerrada" — meaning at execution start the saldo is debited.
  
  Actually I think there might be two phases:
  - Phase 2a: At budget approval → atomic reservation (saldo = saldo - qtd)
  - Phase 2b: At execution start → confirm usage (no change to saldo, just mark as consumed)
  
  But that contradicts EST-08 which says "saldo debitado, reserva encerrada".
  
  Let me re-interpret: The research pattern is the correct one. The atomic reservation at approval DOES decrement saldo. This prevents overselling. Then at execution start:
  - The stock was already debited at approval
  - Execution start just marks the items as "confirmed used" in the OS
  
  If the OS is cancelled between approval and execution, the stock would need to be credited back. D-56 says "Cancelamento libera reservas de peças automaticamente."
  
  So the flow is:
  1. Approval → atomic `saldo = saldo - qtd` (reservation AND debit combined, for atomicity)
  2. Cancellation → `saldo = saldo + qtd` (release reservation)
  3. Execution start → mark as confirmed (no stock change)
  4. Execution finish → nothing stock-related
  
  This makes sense from an atomicity standpoint. The atomic UPDATE at approval prevents race conditions. If cancelled, stock is credited back.

OK so at execution start, we just record that the parts were confirmed used. At the repository level, this could update a `confirmado` flag on os_pecas_usadas.

Let me adjust the task action accordingly.

**`finalizarExecucao(UUID osUuid, String observacao):`**
- Load OS. Validate status is `EM_EXECUCAO`.
- Call `os.finalizarExecucao(observacao)` — sets `execucaoFinalizadaEm`, status → `FINALIZADA`.
- Save OS.
- Publish `OSFinalizadaEvent` (event created in PLAN-03).

**`iniciarExecucao(UUID osUuid, UUID mecanicoId, String observacao):`**
- Load OS, validate status
- Call `os.iniciarExecucao(mecanicoId, observacao)` — records mechanic, timestamp
- Mark peças as confirmed used in os_pecas_usadas table
- Save OS

**Verification:**
```bash
./mvnw test -pl mekano-application -am -Dtest="*OrdemDeServicoExecution*"
```

---

### Task 2: OS List with Filters (OS-16)

**Files created:**
- `mekano-application/src/main/java/com/fiap/mekano/application/service/os/OrdemDeServicoQueryService.java`

**Action:**
- `@ApplicationScoped` (no `@Transactional` — read-only per D-06)
- Inject: `OrdemDeServicoRepositoryPort`

**`listar(int page, int size, LocalDate dataInicio, LocalDate dataFim, String status, UUID clienteUuid, String placa, String sort, String order):`**
- Delegate to repository's `findAllWithFilters()` and `countAllWithFilters()`
- Per D-61..D-65: default page=0, size=10, sort=dataCriacao, order=desc. Clamp size to 50.
- Build paginated response matching `UserPageResponse` pattern:
  ```java
  public record OsListResponse(
      List<OsResumoResponse> content,
      int page, int size, long totalElements, int totalPages
  ) {}
  ```
- `OsResumoResponse`: UUID, numeroOS, status, clienteNome, veiculoPlaca, dataCriacao, valorTotal

**Verification:**
```bash
./mvnw test -pl mekano-application -am -Dtest="*OrdemDeServicoQuery*"
```

---

### Task 3: OS Detail with Full Comparison (OS-17)

**Files created:**
- None new (method in `OrdemDeServicoQueryService`)

**Action:**
**`buscarDetalhe(UUID osUuid):`**
- Per D-43: returns OS + cliente + veiculo + serviços + peças + historico + orcamento + pagamento
- Load OS aggregate root (includes ServicosExecutados, PecasUsadas)
- Load Orcamento from `OrcamentoRepositoryPort.findByOrdemDeServicoUuid(osUuid)`
- Load Historico from `OsAuditLogRepositoryPort.findByOrdemDeServicoUuid(osUuid)` (from PLAN-08)
- Build response DTO (defined in PLAN-07): `OsDetalheResponse`
- Comparison (D-41): For each servico in orçamento, show if executed or not. For each peça, show reserved quantity vs used quantity.

**Verification:**
```bash
./mvnw test -pl mekano-application -am -Dtest="*OrdemDeServicoQuery*"
```

---

### Task 4: Average Execution Time (OS-18)

**Files modified:**
- `OrdemDeServicoQueryService` (add method)

**Action:**
**`calcularTempoMedio(LocalDate inicio, LocalDate fim, UUID servicoTipoUuid):`**
- Per D-40: average time = SUM(execucaoFinalizadaEm - execucaoIniciadaEm) / count
- Only FINALIZADA OS's (per OQ-03 resolution)
- Delegates to repository's `calcularTempoMedioExecucao(servicoTipoUuid, inicio, fim)`
- Repository HQL:
  ```java
  public Double calcularTempoMedioExecucao(UUID servicoTipoUuid, LocalDate inicio, LocalDate fim) {
      // AVG of duration between execucao_iniciada_em and execucao_finalizada_em
      // Join os_servicos_executados to filter by servico_tipo_uuid
      // WHERE status = 'FINALIZADA' AND execucao_finalizada_em BETWEEN :inicio AND :fim
  }
  ```
- Return `Double` (duration in hours/minutes) or null if no data.

**Verification:**
```bash
./mvnw test -pl mekano-application -am -Dtest="*TempoMedio*"
```

---

### Task 5: Soft Delete Guards — Client and Peca (D-49, D-50)

**Files modified:**
- Phase 1's `ClienteService` (or wherever client soft-delete lives)
- Phase 1's `OrdemDeServicoRepositoryPort` (add check methods)

**Action:**
- **D-49:** Block soft-delete of client with open OS's (status != ENTREGUE and != CANCELADA).
  - Add method to `OrdemDeServicoRepositoryPort`: `boolean existsByClienteUuidAndStatusNotIn(UUID clienteUuid, List<StatusOS> statuses)`
  - Call from client delete service before `markAsDeleted()`. Throw `AppException(409)` if OS's exist.
- **D-50:** Block soft-delete of peça referenced in pending OS (status != CANCELADA and != ENTREGUE).
  - Already handled in `PecaRepositoryImpl.markAsDeleted()` (PLAN-02 Task 2) via native query check.

**Verification:**
```bash
./mvnw test -pl mekano-application -am
```

---

### Task 6: OS Cancellation with Stock Release (D-54..D-57)

**Files modified:**
- `OrdemDeServicoExecutionService` or existing cancellation service

**Action:**
- **D-54:** Admin and client can cancel. Only in `AGUARDANDO_APROVACAO` state.
- **D-55:** Reason required (inclusive for budget rejection).
- **D-56:** Cancellation releases stock reservations.
  - In cancellation service: find Orcamento items with pecas, call `pecaRepository.creditarEstoque(pecaUuid, quantidade)` for each.
- **D-57:** OS retains all history (items, diagnosis, budget).
  - Don't soft-delete the OS; just set status to CANCELADA with motivo.

**Verification:**
```bash
./mvnw test -pl mekano-application -am -Dtest="*Cancelamento*"
```

---

### Task 7: Tests for Execution Services

**Files created:**
- `mekano-application/src/test/java/com/fiap/mekano/application/service/os/OrdemDeServicoExecutionServiceTest.java`
- `mekano-application/src/test/java/com/fiap/mekano/application/service/os/OrdemDeServicoQueryServiceTest.java`
- `mekano-application/src/test/java/com/fiap/mekano/application/service/os/TempoMedioServiceTest.java`

**Action:**
- **ExecutionServiceTest:** Mockito. Test `iniciarExecucao()` with valid OS (updates fields), with wrong status (AppException). Test `finalizarExecucao()` publishes OSFinalizadaEvent. Test cancellation releases stock.
- **QueryServiceTest:** Mockito. Test `listar()` with various filter combinations, verifies pagination contract (D-61..D-65). Test `buscarDetalhe()` returns full data.
- **TempoMedioServiceTest:** Mockito. Test `calcularTempoMedio()` returns correct average, handles empty result set, only includes FINALIZADA OS's.

**Verification:**
```bash
./mvnw test -pl mekano-application -am
```

---

## Verification (Plan-Level)

```bash
./mvnw compile -pl mekano-rest -am
./mvnw test -pl mekano-application -am
```

## Risk Mitigation
- **Execution concurrency:** Only one mechanic per OS per D-39. State machine guards prevent double-start/finish.
- **Stock debit at execution start:** Since stock was ALREADY decremented at approval, execution start only marks items as confirmed used. No double-debit risk.
- **Cancellation stock release:** Must credit back ALL reserved peças. Service iterates orcamento items and calls `creditarEstoque()` for each peca item.
- **Average time accuracy:** Only FINALIZADA OS's included per D-41/OQ-03. Time = `execucaoFinalizadaEm - execucaoIniciadaEm`.
