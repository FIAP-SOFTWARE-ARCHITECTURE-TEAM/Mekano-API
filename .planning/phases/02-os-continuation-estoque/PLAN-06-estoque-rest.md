# PLAN-06: Estoque REST — Peca, RequisicaoCompra, NfEntrada, Alertas

## Goal
Expose inventory management REST endpoints: CRUD for Peca, lifecycle for RequisicaoCompra, NF entry registration, and minimum stock alerts. All endpoints follow pagination contract (D-61..D-65), authentication via `@RolesAllowed`, and RFC 7807 error handling via existing `ApiExceptionMapper`.

## Dependencies
- PLAN-02 complete (PecaService, RequisicaoCompraService, NfEntradaService, CDI observers, repository implementations)
- PLAN-01 complete (entities, Flyway, Panache repositories)

## Requirements Covered
EST-01 (Peca REST), EST-02 (edit/delete REST), EST-05 (Req REST), EST-06 (NF REST), EST-09 (Alertas REST)

---

## Tasks

### Task 1: DTOs and Mappers for Estoque

**Files created:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/peca/PecaRequest.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/peca/PecaResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/peca/PecaPageResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/requisicao/RequisicaoCompraRequest.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/requisicao/RequisicaoCompraResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/requisicao/RequisicaoCompraPageResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/nf/NfEntradaRequest.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/nf/NfEntradaResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/nf/NfEntradaPageResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/alerta/AlertaResponse.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/mapper/PecaDtoMapper.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/mapper/RequisicaoCompraDtoMapper.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/mapper/NfEntradaDtoMapper.java`

**Action:**
Follow existing `UserResponse`/`UserPageResponse` pattern exactly.

**Input DTOs (Lombok classes with Bean Validation):**
- **PecaRequest:** `@NotBlank String nome`, `@NotBlank String codigo`, `@NotNull UnidadeMedida unidade`, `@Min(0) int saldoInicial`, `@Min(0) int estoqueMinimo`, `@Min(1) int loteCompra`, `@NotNull @DecimalMin("0.01") BigDecimal valor`
- **RequisicaoCompraRequest:** `@NotNull UUID pecaUuid`, `@Min(1) int quantidade`, `String motivo` (optional, for manual creation)
- **NfEntradaRequest:** `@NotNull UUID requisicaoCompraUuid`, `@NotBlank String numero`, `@NotBlank String serie`, `@NotBlank String fornecedor`, `@NotNull LocalDate dataEmissao`, `@NotBlank String cfop`, `@NotNull @DecimalMin("0.01") BigDecimal valorTotal`

**Output DTOs (Java records with `@Schema` annotations):**
- **PecaResponse:** UUID id, String nome, String codigo, UnidadeMedida unidade, int saldo, int estoqueMinimo, int loteCompra, BigDecimal valor, boolean estoqueAbaixoMinimo, LocalDateTime createdAt
- **RequisicaoCompraResponse:** UUID id, UUID pecaUuid, String pecaNome, int quantidade, StatusRequisicao status, UUID orcamentoUuid (nullable), String motivo, LocalDateTime createdAt
- **NfEntradaResponse:** UUID id, UUID requisicaoCompraUuid, String numero, String serie, String fornecedor, LocalDate dataEmissao, String cfop, BigDecimal valorTotal
- **AlertaResponse:** UUID pecaUuid, String pecaNome, String pecaCodigo, int saldoAtual, int estoqueMinimo, int loteCompra, String mensagem (e.g., "Estoque abaixo do mínimo")

**PageResponse records:** `List<XResponse> content`, `int page`, `int size`, `long totalElements`, `int totalPages` — exact same pattern as `UserPageResponse`.

**Mappers (`@Mapper(componentModel = "cdi")`):**
- `PecaDtoMapper`: `PecaRequest.toCommand()` (or `Peca toDomain`), `PecaResponse toResponse(Peca)`, `PecaPageResponse toPageResponse(List<Peca>, long total, int page, int size)`
- Similar pattern for RequisicaoCompra and NfEntrada mappers.

**Verification:**
```bash
./mvnw compile -pl mekano-rest -am
```

---

### Task 2: PecaResource — CRUD + Soft Delete

**Files created:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/PecaResource.java`

**Action:**
- `@Path("/pecas")` (resolves to `/api/v1/pecas` via `quarkus.rest.path=/api/v1`)
- `@RequestScoped` (G8 compliance), `@RolesAllowed({"admin", "almoxarife"})`
- `@Tag(name = "Peças", description = "Gestão de peças e insumos do estoque")`
- Inject: `PecaServicePort`, `PecaRepositoryPort` (for read-only D-06)

**Endpoints:**
- **`POST /`** (admin/almoxarife) — Create peca. `@Operation(summary = "Cadastrar peça")`. Request: `PecaRequest`. Response: 201 + `PecaResponse` + `Location` header.
- **`GET /`** (admin/almoxarife) — List pecas. `@Operation(summary = "Listar peças")`. Query: `page`, `size`, `sort`, `order`, `codigo` (optional filter), `nome` (optional filter). Response: 200 + `PecaPageResponse`.
- **`GET /{uuid}`** (admin/almoxarife) — Get by UUID. Response: 200 + `PecaResponse`. 404 if not found.
- **`PUT /{uuid}`** (admin/almoxarife) — Update peca. `@Operation(summary = "Atualizar peça")`. Mutates nome, codigo, unidade, estoqueMinimo, loteCompra, valor. Saldo NOT mutable here. Response: 200 + `PecaResponse`.
- **`DELETE /{uuid}`** (admin/almoxarife) — Soft-delete peca. `@Operation(summary = "Excluir peça")`. 204 if ok. 409 if referenced in pending OS per D-50.
- **`PATCH /{uuid}/restore`** (admin) — Restore soft-deleted peca per D-51. 200 + `PecaResponse`.

**DTO mapping in action:**
```java
@POST
public Response criar(@Valid @RequestBody PecaRequest request, @Context UriInfo uriInfo) {
    var peca = dtoMapper.toDomain(request);  // MapStruct creates Peca domain
    var saved = pecaService.criar(peca);
    var response = dtoMapper.toResponse(saved);
    return Response.created(uriInfo.getAbsolutePathBuilder().path(saved.getUuid().toString()).build())
        .entity(response).build();
}
```

**Verification:**
```bash
./mvnw test -pl mekano-rest -am -Dtest="PecaResourceTest"
```

---

### Task 3: RequisicaoCompraResource — Lifecycle

**Files created:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/RequisicaoCompraResource.java`

**Action:**
- `@Path("/requisicoes-compra")`, `@RequestScoped`, `@RolesAllowed({"admin", "almoxarife", "financeiro"})`
- `@Tag(name = "Requisições de Compra", description = "Gestão de requisições de compra")`

**Endpoints:**
- **`GET /`** — List requisitions (paginated). Query: `page`, `size`, `sort`, `order`, `status` (optional filter).
- **`GET /{uuid}`** — Get by UUID.
- **`POST /{uuid}/cancelar`** — Cancel requisition. Admin only (D-28). Body: `{"motivo": "..."}`. Response: 200 + updated response.
- **`POST /{uuid}/comprar`** — Mark as COMPRADA. Admin/almoxarife.
- **`POST /{uuid}/receber`** — Receive goods. Requires `NfEntradaRequest` in body. Calls `NfEntradaService.registrar()` which credits stock + marks as RECEBIDA.

**Verification:**
```bash
./mvnw test -pl mekano-rest -am -Dtest="RequisicaoCompraResourceTest"
```

---

### Task 4: NfEntradaResource — NF Entry Registration

**Files created:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/NfEntradaResource.java`

**Action:**
- `@Path("/nf-entrada")`, `@RequestScoped`, `@RolesAllowed({"admin", "almoxarife", "financeiro"})`
- `@Tag(name = "NF Entrada", description = "Registro de notas fiscais de entrada")`

**Endpoints:**
- **`POST /`** — Register NF entry. `@Operation(summary = "Registrar NF de entrada", description = "Registra NF referenciando requisição de compra. Atualiza saldo do estoque.")`. Body: `NfEntradaRequest`. Response: 201 + `NfEntradaResponse`.
- **`GET /`** — List NF entries (paginated).
- **`GET /{uuid}`** — Get NF entry by UUID.

**Verification:**
```bash
./mvnw test -pl mekano-rest -am -Dtest="NfEntradaResourceTest"
```

---

### Task 5: AlertaResource — Minimum Stock Alerts

**Files created:**
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/AlertaResource.java`

**Action:**
- `@Path("/alertas")`, `@RequestScoped`, `@RolesAllowed({"admin", "almoxarife"})`
- `@Tag(name = "Alertas", description = "Alertas de estoque mínimo")`

**Endpoints:**
- **`GET /`** — List parts below minimum stock. Calls `PecaRepositoryPort.findBySaldoAbaixoMinimo()`. Returns `List<AlertaResponse>`. Simple list (no pagination needed for alerts).

**AlertaResponse:**
```java
@Schema(description = "Alerta de estoque mínimo")
public record AlertaResponse(
    @Schema(description = "UUID da peça") UUID pecaUuid,
    @Schema(description = "Nome da peça", example = "Filtro de óleo") String pecaNome,
    @Schema(description = "Código da peça", example = "PEC-001") String pecaCodigo,
    @Schema(description = "Saldo atual", example = "2") int saldoAtual,
    @Schema(description = "Estoque mínimo configurado", example = "10") int estoqueMinimo,
    @Schema(description = "Quantidade sugerida para compra", example = "20") int loteCompra,
    @Schema(description = "Mensagem do alerta", example = "Estoque abaixo do mínimo: saldo 2, mínimo 10") String mensagem
) {}
```

**Verification:**
```bash
./mvnw test -pl mekano-rest -am -Dtest="AlertaResourceTest"
```

---

### Task 6: Integration Tests for All Estoque REST Endpoints

**Files created:**
- `mekano-rest/src/test/java/com/fiap/mekano/rest/api/PecaResourceTest.java`
- `mekano-rest/src/test/java/com/fiap/mekano/rest/api/RequisicaoCompraResourceTest.java`
- `mekano-rest/src/test/java/com/fiap/mekano/rest/api/NfEntradaResourceTest.java`
- `mekano-rest/src/test/java/com/fiap/mekano/rest/api/AlertaResourceTest.java`

**Action:**
- `@QuarkusTest` + REST Assured + `@TestSecurity`
- **PecaResourceTest:** `@TestMethodOrder(MethodOrderer.OrderAnnotation.class)` for sequential tests.
  - `@Order(1)` POST peca → 201 + Location header
  - `@Order(2)` GET /pecas → 200 with list
  - `@Order(3)` GET /pecas/{uuid} → 200 with peca data
  - `@Order(4)` PUT /pecas/{uuid} → 200 updated
  - `@Order(5)` DELETE /pecas/{uuid} → 204
  - `@Order(6)` GET /pecas/{uuid} → 404 after delete
  - `@Order(7)` PATCH /pecas/{uuid}/restore → 200
  - Test role enforcement: `@TestSecurity(user = "atendente", roles = {"atendente"})` returns 403
- **RequisicaoCompraResourceTest:** POST requisition, GET list, POST cancel with motivo, POST comprar, POST receber with NF data.
- **NfEntradaResourceTest:** POST NF with valid/invalid requisition UUID (expect 404 if requisition missing).
- **AlertaResourceTest:** Create peca with saldo=0, estoqueMinimo=10 → GET /alertas returns it.

**Verification:**
```bash
./mvnw test -pl mekano-rest -am
```

---

## Verification (Plan-Level)

```bash
# Compile
./mvnw compile -pl mekano-rest -am

# Full REST integration tests
./mvnw test -pl mekano-rest -am -Dtest="*ResourceTest"

# Specific endpoint tests
./mvnw test -pl mekano-rest -am -Dtest="PecaResourceTest"
./mvnw test -pl mekano-rest -am -Dtest="NfEntradaResourceTest"
curl http://localhost:8080/api/v1/pecas -H "Authorization: Bearer $TOKEN" | jq .
```

## Risk Mitigation
- **Pagination consistency:** All list endpoints use the same page/size/sort/order pattern. `PecaPageResponse`, `RequisicaoCompraPageResponse` follow the exact same structure as `UserPageResponse`.
- **Soft delete consistency:** All entities follow the same pattern. Restore endpoint (`PATCH /{uuid}/restore`) works the same way for all entity types.
- **Error responses:** All use existing `ApiExceptionMapper` (RFC 7807). No new exception mappers.
- **OpenAPI annotations:** Added to all endpoints per DOC-02 requirements (D-70, D-71). Detailed `@Schema` examples.
