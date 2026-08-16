# Phase 7: API Improvements - Research

**Researched:** 2026-08-08
**Domain:** Quarkus REST API / Hibernate JPQL / Panache Repository
**Confidence:** HIGH

## Summary

Phase 7 has four requirements: three verification-only docs tasks (API-01, API-02, API-03) and one implementation task (API-04 — status priority ordering + terminal status exclusion). The implementation change is scoped entirely to `OrdemDeServicoRepositoryImpl.findAllWithFilters()` — no new endpoints, no service layer changes, no schema changes.

**Status is stored as VARCHAR(30)** (not ordinal) — `StatusOS.name()` in the mapper, column `status VARCHAR(30)` in migration V18. The solution uses JPQL `ORDER BY CASE status WHEN 'EM_EXECUCAO' THEN 0 ... END, createdAt ASC` embedded in Panache's query string, replacing the `Sort.by("createdAt").descending()` parameter. Terminal statuses (FINALIZADA, ENTREGUE, CANCELADA) are excluded via `AND status NOT IN :terminalStatuses`.

**Primary recommendation:** Modify `findAllWithFilters` only. Use Panache's `find(String, Map)` overload without `Sort` — embed ORDER BY CASE WHEN directly in the query string. Add a `terminalStatuses` param as `List<String>`. The existing test `findAllWithFilters_semResultados_retornaListaVazia` will break because it queries for FINALIZADA — needs to be updated.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-04:** Ordernar por prioridade de status via `ORDER BY CASE`: EM_EXECUCAO > AGUARDANDO_APROVACAO > EM_DIAGNOSTICO > RECEBIDA > AGUARDANDO_EXECUCAO
- **D-05:** Dentro de cada status, ordenar por createdAt ASC (mais antigas primeiro)
- **D-06:** Excluir da listagem OS com status FINALIZADA, ENTREGUE e CANCELADA
- **D-07:** Aplicar à query `findAllWithFilters` no OrdemDeServicoRepositoryImpl, adicionando ORDER BY + filtro de exclusão

### the agent's Discretion
- Detalhes da implementação do CASE WHEN no JPQL (sintaxe exata)
- Se a ordenação deve ser aplicada também no endpoint `/filtro` ou apenas no `GET /api/v1/os`

### Deferred Ideas (OUT OF SCOPE)
- Nenhum — fase focada em verificação e ordenação simples
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| API-01 | Verificar e esclarecer se "APIs" refere-se a endpoints ou múltiplas APIs | Task must document both scenarios. No code changes until professor esclarece. See Open Questions below. |
| API-02 | Verificar se já existe endpoint para abertura de OS | VERIFIED: `POST /api/v1/os` at OrdemDeServicoResource.java:72 — exists, works, creates OS with status RECEBIDA. Document-only task. |
| API-03 | Verificar se já existe endpoint para consulta de status da OS | VERIFIED: `GET /api/v1/os/{id}/status` at OrdemDeServicoResource.java:101-102 — exists, `@PermitAll` public endpoint. Document-only task. |
| API-04 | Modificar listagem de OS para ordenar por prioridade de status, omitindo finalizadas/entregues | Implementation task. See findings below for exact JPQL syntax, test impact, and code example. |

## Phase Requirements → Key Files

| File | Role in API-04 |
|------|----------------|
| `OrdemDeServicoRepositoryImpl.java:119-150` | **Target** — `findAllWithFilters` method to modify |
| `OrdemDeServicoRepositoryImplTest.java` | **Will break** — one test queries FINALIZADA |
| `OrdemDeServicoResourceTest.java` | **No breakage expected** — uses mocks, doesn't test ordering |
| `OrdemDeServicoEntity.java:35` | **Status type** — `String status;` mapped to VARCHAR(30) |
| `OrdemDeServicoEntityMapperImpl.java:23` | **Status mapping** — `entity.setStatus(os.getStatus().name())` |
| `StatusOS.java` | **Enum definition** — 8 values, order determined by CONTEXT.md |
| `OrdemDeServicoService.java` | **Pass-through** — delegates to repository, no business logic change needed |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| OS listing with ordering | API / Backend (Repository) | — | Ordering is a data access concern — belongs in the repository layer, not in service or resource |
| Terminal status exclusion | API / Backend (Repository) | — | Same as ordering — filter applied at query level for efficiency |
| API endpoint documentation | — | — | Pure documentation tasks (API-01, API-02, API-03) — no data tier involved |
| Status priority definition | Domain (Config) | — | Priority order is a business rule — captured in CONTEXT.md as locked decisions |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Quarkus Panache (Hibernate ORM) | 3.36.0 | Query building & execution | Existing project stack — all repositories use it |
| Hibernate JPQL | 6.x (Quarkus 3.36) | Custom ORDER BY CASE WHEN | Project already uses JPQL throughout; CASE WHEN is standard JPQL |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Panache `Sort` | 3.36.0 | Simple column sorting | Existing usage, but NOT for this phase — CASE WHEN cannot be expressed via `Sort` |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| JPQL ORDER BY CASE WHEN embedded in query string | In-memory Java sorting after fetch | Breaks pagination for large datasets — JPQL approach is correct |
| JPQL ORDER BY CASE WHEN | Native SQL (`createNativeQuery`) | Unnecessary — JPQL supports CASE WHEN in ORDER BY since JPA 2.0 |
| JPQL ORDER BY CASE WHEN | Panache `Sort.disableEscaping()` with `CASE` | Panache's `Sort` cannot embed CASE WHEN expressions — query string approach is the only way |

**Implementation approach:** Panache's `find(String query, Map<String, Object> params)` method accepts a full WHERE+ORDER BY string. Unlike `Sort`, the query string approach supports arbitrary JPQL expressions including `CASE WHEN`. This is the recommended approach.

## Package Legitimacy Audit

> No new external packages are installed for this phase. All changes use existing dependencies (Quarkus Panache, Hibernate ORM, Lombok). Legacy audit skipped.

## Architecture Patterns

### System Architecture Diagram

```
┌──────────────┐     GET /api/v1/os         ┌──────────────────────┐
│              │ ────────────────────────►   │                      │
│   Client     │     GET /api/v1/os/filtro   │ OrdemDeServicoResource│
│  (Browser/   │ ────────────────────────►   │   (RequestScoped)    │
│   API tool)  │     POST /api/v1/os         │                      │
│              │ ◄────────────────────────   │   toResponse()       │
└──────────────┘     JSON response           │   (JSON serialization)│
                                             └───────┬──────────────┘
                                                     │ delegate
                                                     ▼
                                             ┌──────────────────────┐
                                             │OrdemDeServicoService │
                                             │  (ApplicationScoped) │
                                             │                      │
                                             │  findAllWithFilters()│
                                             └───────┬──────────────┘
                                                     │ delegate
                                                     ▼
┌─────────────────────────────────────────────────────────────────────┐
│  OrdemDeServicoRepositoryImpl  (ApplicationScoped)                  │
│                                                                     │
│  findAllWithFilters(status, clienteUuid, veiculoUuid,               │
│                      dataInicio, dataFim, page, size) {             │
│                                                                     │
│    query = "isActive = :active"                                     │
│          + " AND status NOT IN :terminalStatuses"      ← NEW        │
│          + [optional AND filters ...]                               │
│          + " ORDER BY CASE status                                   │
│                 WHEN 'EM_EXECUCAO' THEN 0                            │
│                 WHEN 'AGUARDANDO_APROVACAO' THEN 1                  │
│                 WHEN 'EM_DIAGNOSTICO' THEN 2                        │
│                 WHEN 'RECEBIDA' THEN 3                              │
│                 WHEN 'AGUARDANDO_EXECUCAO' THEN 4                   │
│             END, createdAt ASC"                    ← NEW (was Sort)│
│                                                                     │
│    return panacheRepository.find(query, params)                     │
│                .page(Page.of(page, size)).list()                    │
│                .stream().map(mapper::toDomain).toList();            │
│  }                                                                  │
└─────────────────────────────────────────────────────────────────────┘
```

### Recommended Project Structure
No structural changes — all changes in `OrdemDeServicoRepositoryImpl.java` only.

### Pattern 1: JPQL ORDER BY CASE WHEN with Panache
**What:** Use a full JPQL query string (not a Sort object) to embed CASE WHEN expressions in ORDER BY.
**When to use:** Whenever sorting logic is more complex than "column ASC/DESC" — e.g., priority-based sorting, conditional ordering, computed sort keys.
**Example:**
```java
// BEFORE (current — simple createdAt desc)
String query = "isActive = :active";
return panacheRepository.find(query, Sort.by("createdAt").descending(), params)
        .page(Page.of(page, size)).list()
        .stream().map(mapper::toDomain).toList();

// AFTER (priority sort + exclude terminal + createdAt asc)
String query = "isActive = :active"
    + " AND status NOT IN :terminalStatuses"
    + " ORDER BY CASE status"
    + "   WHEN 'EM_EXECUCAO' THEN 0"
    + "   WHEN 'AGUARDANDO_APROVACAO' THEN 1"
    + "   WHEN 'EM_DIAGNOSTICO' THEN 2"
    + "   WHEN 'RECEBIDA' THEN 3"
    + "   WHEN 'AGUARDANDO_EXECUCAO' THEN 4"
    + " END, createdAt ASC";
return panacheRepository.find(query, params)
        .page(Page.of(page, size)).list()
        .stream().map(mapper::toDomain).toList();
```

### Pattern 2: Panache Query with Collection Parameter for NOT IN
**What:** Pass a `List<String>` as a named parameter for the `NOT IN` clause.
**When to use:** When filtering against a set of known values.
**Example:**
```java
params.put("terminalStatuses", List.of("FINALIZADA", "ENTREGUE", "CANCELADA"));
```

### Anti-Patterns to Avoid
- **Using `Sort` with CASE WHEN:** Panache's `Sort` object only supports simple column names and HQL functions via `disableEscaping()`, but cannot express `CASE WHEN`. Don't try to force it.
- **Sorting in Java after fetching:** For small datasets this works, but as data grows, in-memory sorting breaks pagination. Always sort at the database level.
- **Changing the service or resource layer:** The ordering and filtering logic belongs in the repository layer. Don't add ordering logic to `OrdemDeServicoService` or `OrdemDeServicoResource`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Custom enum priority sort | Write a Java comparator | Hibernate JPQL `ORDER BY CASE WHEN` | Database-level sort preserves pagination, Hibernate has optimized CASE expression support |

**Key insight:** JPQL `CASE WHEN` in ORDER BY is a standard JPA 2.0 feature. Hibernate supports it natively and generates efficient SQL. No custom sorting needed.

## Runtime State Inventory

> Skip — this phase is not a rename/refactor/migration phase. The API-04 implementation modifies only the query string in one repository method.

## Common Pitfalls

### Pitfall 1: Panache query string does NOT prepend "FROM" when it starts with "order by"
**What goes wrong:** Developer assumes Panache always prepends `FROM Entity WHERE` and adds conditions, then ORDER BY gets placed after WHERE incorrectly.
**Why it happens:** Panache is flexible — it detects whether the query starts with `order by`, `from`, or a plain condition:
- `"order by name"` → becomes `FROM Entity ORDER BY name` [VERIFIED: Quarkus docs]
- `"isActive = true"` → becomes `FROM Entity WHERE isActive = true`
- The query string is treated as a **JPQL fragment** — not strictly a WHERE clause

**How to avoid:** When embedding ORDER BY in the query string, ensure the full sequence is `conditions ORDER BY expr`. Test the generated JPQL in tests.
**Warning signs:** QueryException, HQL parsing errors at runtime.

### Pitfall 2: Status filter override ambiguity
**What goes wrong:** If a user passes `status=FINALIZADA` as a filter, the terminal status exclusion (`NOT IN ('FINALIZADA', 'ENTREGUE', 'CANCELADA')`) will return zero results.
**Why it happens:** The `status` query param filter adds `AND status = :status` to the same query that has `AND status NOT IN :terminalStatuses`.
**How to avoid:** Two options exist:
1. **Always exclude terminal statuses** — users can never see them via the filter endpoint (simplest, recommended for v2)
2. **Conditionally exclude only when no status filter is provided** — more complex, requires branching in query building

**Recommendation:** Go with option 1 (always exclude). Terminal status OS records can be viewed via a future dedicated endpoint or directly in the database.
**Warning signs:** When testing `filter?status=FINALIZADA`, empty results are expected behavior.

### Pitfall 3: Test `findAllWithFilters_semResultados_retornaListaVazia` will break
**What goes wrong:** This test queries `findAllWithFilters("FINALIZADA", ...)` — the terminal status exclusion will cause it to always return empty even when FINALIZADA records exist.
**Why it happens:** The test was written before the terminal status exclusion rule existed.
**How to avoid:** Update the test to query a non-terminal status like `"RECEBIDA"` when no results are expected, or remove/replace the `FINALIZADA` query test with a scenario that demonstrates the terminal exclusion behavior.
**Warning signs:** Failing test after implementation.

### Pitfall 4: Mismatch between entity field name and column name in CASE WHEN
**What goes wrong:** The entity field is `status` (Java), the column is `status` (VARCHAR(30)). JPQL references entity field names, not column names — but they match here. `CASE status WHEN 'RECEBIDA' THEN 0` works because both entity and column use the same name.
**How to avoid:** Always reference the **entity field name** in JPQL, not the column name. Since `@Column(name = "status")` and field name `status` match, there's no issue here.

## Code Examples

### Verified Pattern: JPQL ORDER BY CASE WHEN in Panache

Source: [VERIFIED: Hibernate ORM docs — /hibernate/hibernate-orm, CASE expressions](https://github.com/hibernate/hibernate-orm/blob/main/documentation/src/main/asciidoc/querylanguage/Expressions.adoc)

```hql
from Book
order by
    case type
    when BOOK then 1
    when MAGAZINE then 2
    when JOURNAL then 3
    else 4
    end
```

### Full Implementation for `findAllWithFilters`:

```java
@Override
public List<OrdemDeServico> findAllWithFilters(String status, UUID clienteUuid, UUID veiculoUuid,
                                                LocalDateTime dataInicio, LocalDateTime dataFim,
                                                int page, int size) {
    StringBuilder query = new StringBuilder("isActive = :active");
    Map<String, Object> params = new HashMap<>();
    params.put("active", true);

    // Always exclude terminal statuses (D-06)
    List<String> terminalStatuses = List.of("FINALIZADA", "ENTREGUE", "CANCELADA");
    params.put("terminalStatuses", terminalStatuses);
    query.append(" AND status NOT IN :terminalStatuses");

    if (status != null && !status.isBlank()) {
        query.append(" AND status = :status");
        params.put("status", status);
    }
    if (clienteUuid != null) {
        query.append(" AND clienteUuid = :clienteUuid");
        params.put("clienteUuid", clienteUuid);
    }
    if (veiculoUuid != null) {
        query.append(" AND veiculoUuid = :veiculoUuid");
        params.put("veiculoUuid", veiculoUuid);
    }
    if (dataInicio != null) {
        query.append(" AND createdAt >= :dataInicio");
        params.put("dataInicio", dataInicio);
    }
    if (dataFim != null) {
        query.append(" AND createdAt <= :dataFim");
        params.put("dataFim", dataFim);
    }

    // Priority ordering (D-04, D-05): EM_EXECUCAO > AGUARDANDO_APROVACAO > EM_DIAGNOSTICO > RECEBIDA > AGUARDANDO_EXECUCAO
    // Inside each status, oldest first (createdAt ASC)
    query.append(" ORDER BY CASE status")
         .append("   WHEN 'EM_EXECUCAO' THEN 0")
         .append("   WHEN 'AGUARDANDO_APROVACAO' THEN 1")
         .append("   WHEN 'EM_DIAGNOSTICO' THEN 2")
         .append("   WHEN 'RECEBIDA' THEN 3")
         .append("   WHEN 'AGUARDANDO_EXECUCAO' THEN 4")
         .append(" END, createdAt ASC");

    return panacheRepository.find(query.toString(), params)
            .page(Page.of(page, size)).list()
            .stream().map(mapper::toDomain).toList();
}
```

### Updated Test for `OrdemDeServicoRepositoryImplTest`:

The test `findAllWithFilters_semResultados_retornaListaVazia` at line 92 queries for FINALIZADA, which will now be excluded. Update it to test with a non-terminal status that has no records:

```java
@Test
@TestTransaction
void findAllWithFilters_semResultados_retornaListaVazia() {
    var resultado = repository.findAllWithFilters("RECEBIDA", null, null, null, null, 0, 10);
    assertThat(resultado).isEmpty();
}
```

Add a new test to verify terminal status exclusion:

```java
@Test
@TestTransaction
void findAllWithFilters_excluiStatusTerminais() {
    var osAtiva = repository.save(criarOS("Ativa"));
    var osFinalizada = repository.save(criarOS("Finalizada"));
    osFinalizada.finalizarDiagnostico();
    osFinalizada.aprovarOrcamento(UUID.randomUUID());
    osFinalizada.iniciarExecucao(UUID.randomUUID(), null);
    osFinalizada.finalizarExecucao(null);
    repository.save(osFinalizada);

    var resultado = repository.findAllWithFilters(null, null, null, null, null, 0, 100);

    assertThat(resultado).hasSize(1);
    assertThat(resultado.get(0).getId()).isEqualTo(osAtiva.getId());
}
```

### Status Enum Priority Order (from CONTEXT.md, D-04 and D-05)

| Priority | Status | Priority Value |
|----------|--------|---------------|
| 1 (highest) | EM_EXECUCAO | 0 |
| 2 | AGUARDANDO_APROVACAO | 1 |
| 3 | EM_DIAGNOSTICO | 2 |
| 4 | RECEBIDA | 3 |
| 5 (lowest) | AGUARDANDO_EXECUCAO | 4 |

All terminal statuses (FINALIZADA, ENTREGUE, CANCELADA) are excluded.

Secondary sort: `createdAt ASC` — oldest records first within each priority tier.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| `Sort.by("createdAt").descending()` | `ORDER BY CASE ... END, createdAt ASC` | Phase 7 | Sort direction changes: newest-first → oldest-first |
| No terminal status filter | `AND status NOT IN ('FINALIZADA','ENTREGUE','CANCELADA')` | Phase 7 | Active OS only in list/filter endpoints |
| `find(query, Sort, params)` | `find(query, params)` | Phase 7 | Remove `Sort` parameter, embed ORDER BY in query string |

## Assumptions Log

> No claims tagged `[ASSUMED]` — all technical findings were verified via Context7/Hibernate docs or confirmed from existing source code.

## Open Questions (RESOLVED)

1. **Should `status=FINALIZADA` filter override the terminal exclusion?**
   - What we know: Always-exclude is simpler and matches the requirement "excluir da listagem"
   - What's unclear: Whether an admin should be able to filter for FINALIZADA records via `/filtro`
   - Recommendation: Go with **always exclude** for v2. If needed, a dedicated "historical OS" endpoint can be added later

2. **API-01 open question — single API or multiple?**
   - What we know: The task should document both scenarios
   - What's unclear: Professor's intent — "APIs" in the requirement title
   - Recommendation: Create a doc-only task listing the evidence for both interpretations. No code changes until clarification

3. **Should `GET /api/v1/os` (listAll) also use the new ordering?**
   - What we know: `listAll` calls `findAll` (line 95-98 of Resource), not `findAllWithFilters`
   - What's unclear: CONTEXT.md D-07 specifies `findAllWithFilters` — but the default list endpoint might benefit from the same ordering
   - Recommendation: CONTEXT.md D-07 explicitly targets `findAllWithFilters`. Keep `findAll` unchanged. If the user wants the same ordering on the default list, that's a future change

## Environment Availability

> Skip — this phase has no external dependencies beyond the existing Quarkus/Maven toolchain (confirmed via `mvnw --version` in working dir).

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + QuarkusTest + REST Assured |
| Config file | `application.properties` via `datasource-config.yml` |
| Quick run command | `./mvnw test -pl mekano-infrastructure -am -Dtest=OrdemDeServicoRepositoryImplTest` |
| Full suite command | `./mvnw verify -pl mekano-rest -am` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| API-04 | findAllWithFilters excludes FINALIZADA/ENTREGUE/CANCELADA | integration | `mvnw test -pl mekano-infrastructure -am -Dtest=OrdemDeServicoRepositoryImplTest#findAllWithFilters_excluiStatusTerminais` | ❌ Wave 0 — need new test |
| API-04 | findAllWithFilters orders by status priority + createdAt ASC | integration | Same suite — verify sort order in assertion | ❌ Wave 0 — need new or updated test |
| API-02 | POST /api/v1/os exists and creates OS with RECEBIDA | E2E | Existing test `create_asAdmin_returns201` | ✅ Already passing |
| API-03 | GET /api/v1/os/{id}/status exists and is public | E2E | Existing test `getStatus_anonimo_retorna200` | ✅ Already passing |

### Sampling Rate
- **Per task commit:** `./mvnw test -pl mekano-infrastructure -am -Dtest=OrdemDeServicoRepositoryImplTest -DfailIfNoTests=false`
- **Per wave merge:** `./mvnw verify -pl mekano-rest -am`
- **Phase gate:** Full suite green before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] `OrdemDeServicoRepositoryImplTest#findAllWithFilters_excluiStatusTerminais` — new test for terminal status exclusion
- [ ] `OrdemDeServicoRepositoryImplTest#findAllWithFilters_ordemPrioridade` — new test verifying sort order (hard with H2 because createdAt is `now()`, but can verify first/last status value)
- [ ] Update existing `findAllWithFilters_semResultados_retornaListaVazia` — change filter from FINALIZADA to a non-terminal status

*(Wave 0 gaps: 3 test modifications/additions — no new framework or config needed)*

## Security Domain

> `security_enforcement` is enabled (absent = true in config). This phase's code changes (query ordering) introduce no new security surface. Document-only tasks (API-01/02/03) involve no code changes.

### Applicable ASVS Categories
| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V4 Access Control | No | Existing `@RolesAllowed` annotations unchanged |
| V5 Input Validation | No | No new endpoints or input fields added |
| All others | No | Query modification is read-only, no auth/input changes |

**No new threat patterns introduced.** The existing role-based access (`@RolesAllowed`) on the list/filter endpoints remains the sole access control mechanism.

## Sources

### Primary (HIGH confidence)
- [VERIFIED: Hibernate ORM docs — CASE expressions](https://github.com/hibernate/hibernate-orm/blob/main/documentation/src/main/asciidoc/querylanguage/Expressions.adoc) — confirmed JPQL CASE WHEN syntax in ORDER BY
- [VERIFIED: Quarkus Panache docs — sorting](https://github.com/quarkusio/quarkus/blob/main/docs/src/main/asciidoc/hibernate-orm-panache.adoc) — confirmed `find("order by ...")` pattern and `find(String query, Map params)` signature
- [VERIFIED: Source code — OrdemDeServicoEntity.java:35] — status field is `String`, mapped as VARCHAR(30)
- [VERIFIED: Source code — OrdemDeServicoEntityMapperImpl.java:23,68] — mapping uses `name()` and `valueOf()`
- [VERIFIED: Source code — V18 migration](https://github.com/quarkusio/quarkus/blob/main/docs/src/main/asciidoc/hibernate-orm-panache.adoc) — `status VARCHAR(30) NOT NULL`

### Secondary (MEDIUM confidence)
- [CITED: OrdemDeServicoRepositoryImpl.java:119-150] — existing findAllWithFilters implementation
- [CITED: OrdemDeServicoRepositoryImplTest.java] — existing test patterns
- [CITED: StatusOS.java] — all 8 enum values confirmed

### Tertiary (LOW confidence)
- None — all findings cross-verified with source code or official docs

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - all tools (Quarkus 3.36, Panache, Hibernate 6.x) verified in project
- Architecture: HIGH - code change scope is single method, verified in source
- Pitfalls: HIGH - test breakage and query string behavior verified by reading both source code and Quarkus docs
- JPQL CASE WHEN syntax: HIGH - confirmed via Hibernate ORM docs and Quarkus Panache examples

**Research date:** 2026-08-08
**Valid until:** 2026-09-08 (stable tech stack — 30 day validity)