# PLAN-07: Orcamento REST + OS Execution REST

## Goal
Expose Orcamento endpoints (client budget approval/rejection with JWT auth) and OS execution endpoints (start/finish, detail with full comparison, list with filters, average time metrics). Wire together the backend services from PLAN-03 and PLAN-04.

## Dependencies
- PLAN-03 complete (OrcamentoService, OS state machine extensions, SLA job)
- PLAN-04 complete (OS execution services, metrics, detail queries)
- PLAN-02 complete (stock reservation observer for budget approval)
- PLAN-05 complete (role 'cliente' provisioning for client auth)
- Phase 1 (OS repository, StatusOS enum, Cliente/Veiculo/Servico for detail embedding)

## Requirements Covered
OS-09 (budget generation endpoint), OS-10 (approve), OS-11 (reject), OS-13 (start execution), OS-14 (finish execution), OS-16 (list with filters), OS-17 (detail), OS-18 (average time)

---

## Tasks

### Task 1: DTOs and Mappers for Orcamento and OS Extensions

**Files created:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/orcamento/OrcamentoResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/orcamento/OrcamentoItemResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/orcamento/AprovarOrcamentoRequest.java` (reprovar)
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/os/OsDetailResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/os/OsSummaryResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/os/OsPageResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/os/OsExecucaoRequest.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/os/TempoMedioResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/mapper/OrcamentoDtoMapper.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/mapper/OsDtoMapper.java`

**Action:**
Follow existing `UserResponse`/`UserPageResponse` pattern exactly.

**OrcamentoResponse (record with `@Schema`):**
```java
public record OrcamentoResponse(
    UUID id,
    UUID ordemDeServicoUuid,
    BigDecimal valorTotal,
    LocalDateTime dataCriacao,
    LocalDateTime dataExpiracao,
    Boolean aprovado,
    List<OrcamentoItemResponse> itens
) {}
```

**OrcamentoItemResponse:**
```java
public record OrcamentoItemResponse(
    UUID id,
    String tipo,        // SERVICO or PECA
    String descricao,
    int quantidade,
    BigDecimal valorUnitario,
    BigDecimal valorTotal,
    UUID pecaUuid       // null for servicos
) {}
```

**AprovarOrcamentoRequest / ReprovarOrcamentoRequest:**
```java
// Reprovar needs motivo. Aprovar needs nothing (just POST).
public record ReprovarOrcamentoRequest(
    @NotBlank String motivo
) {}
```

**OsDetailResponse** (per D-43):
```java
public record OsDetailResponse(
    UUID id,
    String numero,
    String status,
    // Embedded cliente + veiculo
    ClienteResumo cliente,
    VeiculoResumo veiculo,
    // Services and parts (executed vs budgeted per D-41)
    List<ServicoExecutadoResponse> servicosOrcados,
    List<ServicoExecutadoResponse> servicosExecutados,
    List<PecaUsadaResponse> pecasReservadas,
    List<PecaUsadaResponse> pecasUtilizadas,
    // Budget
    OrcamentoResponse orcamento,
    // History
    List<HistoricoResponse> historico,
    // Execution
    UUID mecanicoId, LocalDateTime execucaoIniciadaEm, LocalDateTime execucaoFinalizadaEm,
    // Payment (null if Phase 3 not done)
    ...
) {}
```

**OsSummaryResponse** (for list view):
```java
public record OsSummaryResponse(
    UUID id, String numero, String status,
    String clienteNome, String veiculoPlaca,
    LocalDateTime dataCriacao, BigDecimal valorTotal
) {}
```

**OsPageResponse:** `List<OsSummaryResponse> content`, `int page`, `int size`, `long totalElements`, `int totalPages`

**OsExecucaoRequest:**
```java
public record OsExecucaoRequest(
    String observacao   // optional
) {}
```

**TempoMedioResponse:**
```java
public record TempoMedioResponse(
    UUID servicoTipoUuid,
    String servicoTipoNome,
    Double mediaHoras,
    int totalOrdens,
    LocalDate dataInicio,
    LocalDate dataFim
) {}
```

**Mappers:** `@Mapper(componentModel = "cdi")` for Orcamento and OS DTOs.

**Verification:**
```bash
./mvnw compile -pl mekano-rest -am
```

---

### Task 2: OrcamentoResource — Client Budget Approval/Rejection

**Files created:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/OrcamentoResource.java`

**Action:**
- `@Path("/orcamentos")` (resolves to `/api/v1/orcamentos`)
- `@RequestScoped` (G8), JWT claims injection for client UUID
- `@Tag(name = "Orçamentos", description = "Gestão de orçamentos para clientes")`

**Endpoints (client-facing, role 'cliente'):**

- **`GET /{uuid}`** — `@RolesAllowed({"cliente", "admin", "atendente"})`. Get budget details. Client sees only own OS's budget (filter: check `os.clienteUuid == loggedUser.id`). Admin/atendente see all. Returns `OrcamentoResponse`.

- **`POST /{uuid}/aprovar`** — `@RolesAllowed("cliente")`. Approve budget.
  - `@Operation(summary = "Aprovar orçamento", description = "Cliente aprova o orçamento. Reserva de peças é feita automaticamente.")`
  - Extract JWT claim `sub` (client UUID). Call `OrcamentoService.aprovar(orcamentoUuid, clienteId)`.
  - On success: 200 + `OrcamentoResponse`. On failure: 400 (expired), 403 (wrong client), 409 (stock insufficient).
  - Per D-14: client accesses via authenticated endpoint. D-15: admin cannot approve for client.

- **`POST /{uuid}/reprovar`** — `@RolesAllowed("cliente")`. Reject budget.
  - `@Operation(summary = "Reprovar orçamento", description = "Cliente reprova o orçamento. OS é cancelada.")`
  - Body: `ReprovarOrcamentoRequest` with motivo (required per D-55).
  - Call `OrcamentoService.reprovar(orcamentoUuid, clienteId, motivo)`.
  - Response: 200 + updated OS status.

**Client ownership filter:**
```java
@Inject
JsonWebToken jwt;  // or @Inject SecurityIdentity

private void verificarCliente(UUID orcamentoUuid) {
    var orcamento = orcamentoService.buscarPorId(orcamentoUuid);
    var os = osRepository.findByUuid(orcamento.ordemDeServicoUuid());
    String loggedClientId = jwt.getSubject();  // UUID as string
    if (!os.getClienteUuid().toString().equals(loggedClientId)) {
        throw new AppException(403, "Acesso negado: este orçamento não pertence ao cliente logado");
    }
}
```

**Verification:**
```bash
./mvnw test -pl mekano-rest -am -Dtest="OrcamentoResourceTest"
```

---

### Task 3: OS Execution REST Endpoints — Start/Finish

**Files created:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/os/OrdemDeServicoExecutionResource.java`

**Action:**
- `@Path("/os/{osUuid}/executar")` under existing OS path (or `/os/executar/`)
- `@RequestScoped`, `@RolesAllowed({"admin", "mecanico"})` per D-36
- `@Tag(name = "Execução OS", description = "Início e finalização da execução de ordens de serviço")`

**Endpoints:**

- **`POST /{osUuid}/executar/iniciar`** — Start execution.
  - `@Operation(summary = "Iniciar execução", description = "Mecânico inicia a execução da OS. Peças reservadas são debitadas do estoque.")`
  - Extract `mecanicoId` from JWT `sub` claim.
  - Body: `OsExecucaoRequest` (observacao optional).
  - Calls `OrdemDeServicoExecutionService.iniciarExecucao(osUuid, mecanicoId, observacao)`.
  - Response: 200 + OS summary.

- **`POST /{osUuid}/executar/finalizar`** — Finish execution.
  - `@Operation(summary = "Finalizar execução", description = "Mecânico finaliza a execução da OS.")`
  - Body: `OsExecucaoRequest` (observacao optional).
  - Calls `OrdemDeServicoExecutionService.finalizarExecucao(osUuid, observacao)`.
  - Response: 200 + OS summary. Publishes `OSFinalizadaEvent`.

**Verification:**
```bash
./mvnw test -pl mekano-rest -am -Dtest="*OrdemDeServicoExecution*"
```

---

### Task 4: OS Query REST Endpoints — Detail, List, Metrics

**Files created:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/os/OrdemDeServicoQueryResource.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/os/OsMetricasResource.java`

**Action:**

**OrdemDeServicoQueryResource:**
- `@Path("/os")`, `@RequestScoped`
- `@Tag(name = "Ordens de Serviço", description = "Consulta de ordens de serviço")`

- **`GET /{uuid}`** (OS Detail, OS-17):
  - `@RolesAllowed({"admin", "atendente", "mecanico"})`
  - Returns `OsDetailResponse` with full data per D-43.
  - Combines data from OS, Cliente, Veiculo, ServicosExecutados, PecasUsadas, Orcamento, Historico.

- **`GET /`** (OS List, OS-16):
  - `@RolesAllowed({"admin", "atendente"})` per OS-16.
  - Query params: `page` (default 0), `size` (default 10, max 50), `sort` (default dataCriacao), `order` (default desc), `dataInicio`, `dataFim`, `status`, `clienteUuid`, `placa`.
  - Returns `OsPageResponse` with paginated summaries.

**OsMetricasResource:**
- `@Path("/os/metricas")`, `@RequestScoped`, `@RolesAllowed({"admin"})` per OS-18
- `@Tag(name = "Métricas OS", description = "Métricas de execução de ordens de serviço")`

- **`GET /tempo-medio`** (Average Time, OS-18):
  - `@Operation(summary = "Tempo médio de execução", description = "Retorna o tempo médio de execução por tipo de serviço em um período.")`
  - Query: `servicoTipoUuid` (optional — all types if omitted), `dataInicio`, `dataFim`.
  - Returns `TempoMedioResponse` or `List<TempoMedioResponse>`.
  - Only FINALIZADA OS's included per D-40/OQ-03.

**Verification:**
```bash
./mvnw test -pl mekano-rest -am -Dtest="*OrdemDeServicoQuery*"
./mvnw test -pl mekano-rest -am -Dtest="*OsMetricas*"
```

---

### Task 5: Integration Tests for Orcamento + OS REST Endpoints

**Files created:**
- `mekano-rest/src/test/java/com/fiap/mekano/rest/api/OrcamentoResourceTest.java`
- `mekano-rest/src/test/java/com/fiap/mekano/rest/api/os/OrdemDeServicoExecutionResourceTest.java`
- `mekano-rest/src/test/java/com/fiap/mekano/rest/api/os/OrdemDeServicoQueryResourceTest.java`
- `mekano-rest/src/test/java/com/fiap/mekano/rest/api/os/OsMetricasResourceTest.java`

**Action:**
- `@QuarkusTest` + REST Assured. `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` for flow tests.

**OrcamentoResourceTest:**
- Test with `@TestSecurity(user = "cliente", roles = {"cliente"})`:
  - GET /orcamentos/{uuid} → 200 with OrcamentoResponse
  - POST /orcamentos/{uuid}/aprovar → 200 (budget approved)
  - POST /orcamentos/{uuid}/reprovar with motivo → 200 (budget rejected)
- Test with `@TestSecurity(user = "admin", roles = {"admin"})`:
  - GET /orcamentos/{uuid} → 200 (admin sees all)
  - POST /orcamentos/{uuid}/aprovar → 403 (admin cannot approve per D-15)
- Test 404 for non-existent orcamento

**OrdemDeServicoExecutionResourceTest:**
- Start execution with `@TestSecurity(user = "mecanico", roles = {"mecanico"})`:
  - POST /os/{uuid}/executar/iniciar → 200
- Finish execution:
  - POST /os/{uuid}/executar/finalizar → 200
- Test wrong role (`@TestSecurity(user = "atendente", roles = {"atendente"})`) → 403
- Test wrong status → 400/AppException

**OrdemDeServicoQueryResourceTest:**
- GET /os → 200 with OsPageResponse. Verify pagination metadata.
- GET /os/{uuid} → 200 with full detail. Verify cliente+veiculo embedded.
- Test filter params: `?status=EM_EXECUCAO&dataInicio=2026-06-01`

**OsMetricasResourceTest:**
- GET /os/metricas/tempo-medio?dataInicio=2026-06-01&dataFim=2026-07-01 → 200
- Without data (no OS finalizada) → 200 with null media

**Verification:**
```bash
./mvnw test -pl mekano-rest -am
```

---

## Verification (Plan-Level)

```bash
# Compile
./mvnw compile -pl mekano-rest -am

# All REST integration tests
./mvnw test -pl mekano-rest -am

# Manual test (after docker-compose up)
curl -X POST http://localhost:8080/api/v1/orcamentos/$UUID/aprovar \
  -H "Authorization: Bearer $CLIENTE_TOKEN" \
  -H "Content-Type: application/json" | jq .

curl http://localhost:8080/api/v1/os/$UUID \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq .
```

## Risk Mitigation
- **Client JWT:** Role `cliente` must be accepted by Phase 1's JWT infrastructure. If Phase 1 role model is restrictive, update it to accept multiple roles.
- **Ownership filter:** Critical for D-13 (client sees only own OS). Implemented via JWT `sub` claim comparison with `os.clienteUuid`. Test with two different client tokens to verify isolation.
- **Execution permissions:** D-36 specifies only mechanic can start/finish. `@RolesAllowed({"admin", "mecanico"})` on execution endpoints.
- **OpenAPI annotations:** All endpoints get `@Operation`, `@APIResponse`, `@Schema` per DOC-02 (D-70, D-71). Security scheme auto-detected from `@RolesAllowed`.
