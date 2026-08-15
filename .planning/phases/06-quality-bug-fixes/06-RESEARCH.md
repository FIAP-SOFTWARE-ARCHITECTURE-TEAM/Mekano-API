# Phase 6: Quality & Bug Fixes - Research

**Researched:** 2026-08-08
**Domain:** Code quality, test coverage, refactoring, bug fixing
**Confidence:** HIGH

## Summary

This phase addresses two requirements: QLD-01 (80% JaCoCo line coverage) and QLD-02 (Clean Code/SOLID code review). The work splits into three domains: (1) JaCoCo aggregated coverage gating, (2) bug fixes, and (3) structural tech debt remediation.

**Critical finding:** Several AGENTS.md claims about bugs and field injection are **outdated** — the actual source code already uses constructor injection and the NfEntradaRepositoryImpl bug may already be partially addressed. The planner must verify each claim against current source before creating tasks.

**Primary recommendation:** Execute tech debt items from most impactful to least: (1) JaCoCo config → (2) bug fix verification → (3) PT-BR→EN rename → (4) dead code removal → (5) unification items → (6) entity style → (7) FT/Cache additions.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Usar `report-aggregate` no módulo `mekano-rest` para visão unificada (não apenas BUNDLE por módulo)
- **D-02:** Adicionar exclusions de `**/*MapperImpl.class` (MapStruct generated), `**/*PanacheRepository*.class`, e classes geradas
- **D-03:** Pipeline CI falha se cobertura abaixo de 80% — gate já existe no `verify` phase
- **D-04:** Corrigir NfEntradaRepositoryImpl — `pecaId` e `requisicaoCompraId` ambos setados para `nfEntrada.getId()` (copy-paste bug). `pecaId` deve vir do `requisicao.getPecaId()`, `requisicaoCompraId` do `requisicao.getId()`
- **D-05:** Padronizar nomenclatura dos ports: `salvar`/`buscarPorId` → `save`/`findById` em PecaRepositoryPort, NfEntradaRepositoryPort, RequisicaoCompraRepositoryPort
- **D-06:** Trocar field injection (`@Inject`) por constructor injection nos 3 stubs/stub services que ainda usam field injection
- **D-07:** Padronizar estilo de entidades — avaliar @Data vs @Getter/@Setter e definir um padrão único
- **D-08:** Remover mappers vazios: PecaEntityMapper, RequisicaoCompraEntityMapper, NfEntradaEntityMapper (sem métodos, dead code)
- **D-09:** Unificar VOs duplicados: `Placa.java` e `PlacaVeiculo.java` (regex diferentes, mesmo conceito)
- **D-10:** Mover `ItemOrcamento` de `model/` para `valueobject/` (é Value Object, não entidade)
- **D-11:** Unificar `StatusPagamento` — existe em `domain/model/` e `domain/os/`
- **D-12:** Adicionar `@Retry` + `@Timeout` + `@CacheResult` nos repositórios que ainda não têm: Cliente, Peca, RequisicaoCompra, NfEntrada
- **D-13:** Revisão é aberta — o executor pode identificar e corrigir mais itens durante a execução

### The agent's Discretion
- Ordem de correção dos itens de tech debt (qual prioridade)
- Decisão final sobre @Data vs @Getter/@Setter (avaliar consistência atual)
- Se deve mover ItemOrcamento ou apenas criar o VO no package correto e manter compatibilidade

### Deferred Ideas (OUT OF SCOPE)
- CI/CD com JaCoCo coverage gate no PR — já existe no verify phase
- Melhoria de performance de testes — fora do escopo
- Documentação de cobertura por módulo — pode ser extraída do report-aggregate
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| QLD-01 | Garantir 80% de cobertura de testes (JaCoCo line coverage) | JaCoCo 0.8.15 report-aggregate in mekano-rest (depends on all modules). Exclusions for MapperImpl, PanacheRepository, generated classes. Check already exists at `verify` phase with 0.80 LINE minimum. |
| QLD-02 | Revisar e refatorar código para princípios Clean Code e SOLID | 9-10 tech debt items identified: port naming (PT-BR→EN), injection style, entity style, empty mappers, duplicate VOs, misplaced VO, duplicate StatusPagamento, FT/Cache coverage, NfEntrada bug, ClienteService update bug. Plus open review flag. |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| JaCoCo coverage gating | Build (Maven verify) | — | JaCoCo `check` goal runs during `verify` phase; `report-aggregate` merges module exec data in mekano-rest |
| PT-BR→EN port rename | Domain (interfaces) + Infrastructure (impls) | Application (callers) | Port interfaces in domain/; implementations in infrastructure/; callers in application/ and rest/ |
| Empty mapper removal | Infrastructure | — | Dead classes live in infrastructure/mapper/ |
| VO unification | Domain | — | Both Placa classes in domain/valueobject/; StatusPagamento enums in domain/ |
| FT/Cache additions | Infrastructure | — | Repositories in infrastructure/repository/ need annotations; cache config exists in cache-config.yml |
| ClienteService update fix | Application | — | Bug resides in ClienteService.java in application/ layer |

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| JaCoCo | 0.8.15 | Code coverage measurement & gating | Already configured in root pom.xml; just adding report-aggregate + exclusions |
| Quarkus SmallRye FT | via BOM | `@Retry`, `@Timeout` for repos | Already used in User/Veiculo/Servico repos; adding to remaining 4 repos |
| Quarkus Cache | via BOM | `@CacheResult`, `@CacheInvalidate` | Already used in User/Veiculo/Servico repos; cache config already exists |

**Version verification:**
```bash
# JaCoCo version in pom.xml: 0.8.15
# Quarkus version: 3.36.0 (manages SmallRye FT and Cache versions)
```

## Package Legitimacy Audit

> No new external packages are installed in this phase. All changes are edits to existing code and POM configuration.

| Package | Registry | Age | Downloads | Source Repo | slopcheck | Disposition |
|---------|----------|-----|-----------|-------------|-----------|-------------|
| (none) | — | — | — | — | — | N/A — no new packages |

## Architecture Patterns

### Pattern 1: JaCoCo report-aggregate in App Module
**What:** The `report-aggregate` goal merges `.exec` coverage data from all modules into a single report. It runs ONLY in a module that has compile dependencies on all other modules — `mekano-rest` is the natural place since it depends on domain, application, and infrastructure.

**How:**
```xml
<!-- In mekano-rest/pom.xml, inside <build><plugins> -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>jacoco-report-aggregate</id>
            <phase>verify</phase>
            <goals>
                <goal>report-aggregate</goal>
            </goals>
            <configuration>
                <excludes>
                    <exclude>**/*MapperImpl.class</exclude>
                    <exclude>**/*PanacheRepository*.class</exclude>
                </excludes>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Important:** The `jacoco:check` with 80% LINE minimum stays in the ROOT pom.xml (applies per-module). The `report-aggregate` is additional for the unified view. No removal of existing JaCoCo config.

### Pattern 2: PT-BR→EN Port Rename (Safe Refactoring)
**What:** Rename `salvar()` → `save()` and `buscarPorId()` → `findById()` in 3 port interfaces and all implementations/callers. Must be done atomically — rename interface + impl + all callers in one commit per port to avoid compilation breaks.

**Order by call volume:**
1. `PecaRepositoryPort` — ~15 call sites (PecaServiceImpl, PecaServiceTest, PecaResourceTest, NfEntradaService, etc.)
2. `NfEntradaRepositoryPort` — ~8 call sites (NfEntradaServiceImpl, NfEntradaServiceTest, NfEntradaResourceTest)
3. `RequisicaoCompraRepositoryPort` — ~6 call sites (RequisicaoCompraService, tests)

**Note:** `PecaService.buscarPorId()` is a **service method** separate from the port method — rename separately to `findById()` for consistency.

### Pattern 3: FT/Cache Annotations for Remaining Repos
**What:** Add `@Retry(maxRetries=3)`, `@Timeout(value=5, unit=ChronoUnit.SECONDS)`, `@CacheResult`, and `@CacheInvalidate` to ClienteRepositoryImpl, PecaRepositoryImpl, RequisicaoCompraRepositoryImpl, and NfEntradaRepositoryImpl.

**Reference implementation** — VeiculoRepositoryImpl (uses `@Inject` for PanacheRepository reference, `@CacheResult` on read methods, `@CacheInvalidate` on write methods):

```java
// Pattern from VeiculoRepositoryImpl.java (VERIFIED: mekano source)
@ApplicationScoped
public class VeiculoRepositoryImpl implements VeiculoRepositoryPort {
    @Inject
    VeiculoPanacheRepository panacheRepository;

    @Override
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CacheInvalidate(cacheName = CacheNames.VEHICLES)
    public Veiculo save(Veiculo veiculo) { ... }

    @Override
    @Retry(maxRetries = 3)
    @CacheResult(cacheName = CacheNames.VEHICLES)
    public Optional<Veiculo> findById(UUID id) { ... }
}
```

**Cache names already exist** in `CacheNames.java` and `cache-config.yml` for all 4 repos (`clientes`, `pecas`, `requisicoes`, `nf_entradas`).

### Anti-Patterns to Avoid
- **Renaming ports in separate commits** — causes compilation failure mid-chain. Rename interface + impl + all callers atomically per port.
- **Field injection** — already fixed in code (verified), but verify before making claims.
- **Adding FT/Cache without understanding existing pattern** — PecaRepositoryImpl already has `@CacheResult` and `@CacheInvalidate` on some methods but lacks `@Retry`/`@Timeout`.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Test coverage aggregation | Custom script to merge .exec files | JaCoCo `report-aggregate` goal | Maven-native, zero config, no dependencies to maintain |
| Placa formatting | Custom formatter | Keep `PlacaVeiculo.formatted()` from `Placa.java` | Already exists in the dead `Placa.java` — just port it |
| Retry/Timeout logic | Manual try-catch loops | `@Retry`/`@Timeout` from SmallRye FT | Already in the project. Declarative, configurable, tested |

## Common Pitfalls

### Pitfall 1: Broken Compilation from Port Rename
**What goes wrong:** Renaming `salvar()` to `save()` in the interface without updating all callers causes compilation failure.
**Why it happens:** Java interface contract enforcement at compile time.
**How to avoid:** Use IDE rename refactoring or grep all callers first. Create a single commit per port that renames interface + impl + all callers.
**Warning signs:** `mvn compile` fails after first rename.

### Pitfall 2: JaCoCo report-aggregate Exclusion Mismatch
**What goes wrong:** Adding exclusions to `report-aggregate` but not to `jacoco:check`, causing the check to count excluded classes against the 80% threshold.
**Why it happens:** `report`/`report-aggregate` and `check` have separate `<configuration>` blocks.
**How to avoid:** Add exclusions to BOTH the `check` execution (root pom.xml) and `report-aggregate` execution (mekano-rest pom.xml).
**Warning signs:** Coverage drops dramatically after adding exclusions to reports but not check.

### Pitfall 3: Dead Code Removal Without Verification
**What goes wrong:** Removing an empty mapper class that is injected somewhere via CDI.
**Why it happens:** Empty `@ApplicationScoped` classes can still be injected as no-op beans.
**How to avoid:** Grep for the class name (not just import) to verify zero references. Verified: PecaEntityMapper, RequisicaoCompraEntityMapper, NfEntradaEntityMapper have ZERO references outside their own file.

### Pitfall 4: Moving ItemOrcamento Breaks Existing Imports
**What goes wrong:** Moving ItemOrcamento.java to valueobject/ breaks 20+ import statements across the codebase.
**Why it happens:** Moving a Java file changes its package, requiring import updates in every consumer.
**How to avoid:** If moving, do it as a single commit with all import updates. The discretion area allows keeping a deprecated stub in model/ for backward compat.

## Code Examples

### JaCoCo report-aggregate in mekano-rest pom.xml
```xml
<!-- Add INSIDE <build><plugins>, after the jandex plugin -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <id>jacoco-report-aggregate</id>
            <phase>verify</phase>
            <goals>
                <goal>report-aggregate</goal>
            </goals>
            <configuration>
                <excludes>
                    <exclude>**/*MapperImpl.class</exclude>
                    <exclude>**/*PanacheRepository*.class</exclude>
                </excludes>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Updated JaCoCo check exclusions in root pom.xml
```xml
<!-- In root pom.xml, inside jacoco-check <configuration><excludes> -->
<exclude>**/*MapperImpl.class</exclude>
<!-- Also add: -->
<exclude>**/*PanacheRepository*.class</exclude>
```

### Entity Style — @Getter/@Setter (recommended over @Data)
```java
// Existing pattern in UserEntity.java (VERIFIED: mekano source)
@Getter
@Setter
public class UserEntity extends BaseEntity {
    private String name;
    private String email;
    // ...
}
```

**Recommendation:** Convert `PecaEntity`, `RequisicaoCompraEntity`, `NfEntradaEntity` from `@Data` to `@Getter/@Setter` (private fields). This matches the pattern in UserEntity, ClienteEntity, VeiculoEntity, ServicoEntity. The `@Data` annotation generates public fields which break encapsulation — this was noted as a sign of newer entities vs older ones.

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Per-module JaCoCo BUNDLE report | report-aggregate in mekano-rest | This phase | Unified coverage view across all 4 modules |
| Field injection in 3 services | Constructor injection | Already implemented (code shows constructor) | AGENTS.md outdated — no work needed |
| NfEntrada bug (both IDs = getId()) | Current code uses correct getPecaId()/getRequisicaoCompraId() | Already implemented | AGENTS.md outdated — verify before creating fix tasks |

**Deprecated/outdated:**
- AGENTS.md claims about field injection in PecaService, NfEntradaService, RequisicaoCompraService — ALL THREE already use constructor injection
- AGENTS.md claim about NfEntradaRepositoryImpl bug — source currently shows correct field mappings
- AGENTS.md claim about ClienteService.updateCliente — source currently shows updates ARE applied via `Cliente.reconstitute()`

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | PecaService, NfEntradaService, RequisicaoCompraService use constructor injection | Common Pitfalls / State of the Art | LOW — verified by reading actual source |
| A2 | NfEntradaRepositoryImpl bug is partially addressed (current code uses correct getters) | State of the Art | MEDIUM — code at lines 39-40 is correct, but fix target described in D-04 differs (wants requisicao object) |
| A3 | ClienteService.updateCliente bug is fixed | State of the Art | MEDIUM — code applies updates via reconstitute(), but edge case may exist |
| A4 | `domain.model.StatusPagamento` is dead code | Common Pitfalls | LOW — zero references confirmed by grep |
| A5 | `Placa.java` is dead code | Common Pitfalls | LOW — zero non-test references confirmed by grep |

## Open Questions (RESOLVED)

1. **NfEntradaRepositoryImpl bug — is it actually fixed or still broken in a subtle way?**
   - What we know: Source lines 39-40 use `nfEntrada.getPecaId()` and `nfEntrada.getRequisicaoCompraId()` — these look correct. But D-04 prescribes using `requisicao.getPecaId()` and `requisicao.getId()`, which requires changing the repository method signature to accept RequisicaoCompra.
   - What's unclear: Whether the domain object NfEntrada already carries the correct values (it should, based on NfEntradaService creating it with `requisicao.getPecaId()` and `command.requisicaoCompraId()`).
   - Recommendation: Add test coverage for NfEntradaService.registrar() that verifies pecaId and requisicaoCompraId are persisted correctly. If the current code works, no repository-level fix is needed.

2. **ClienteService.updateCliente bug — is it truly fixed?**
   - What we know: The code creates a new Cliente via `Cliente.reconstitute()` with command values and saves it.
   - What's unclear: Whether `updateCliente` is actually called and tested with field updates to verify.
   - Recommendation: Verify existing test coverage for ClienteService.updateCliente (if none exists, add one).

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Java 17 | Build & tests | ✓ | (assumed) | — |
| Maven (wrapper) | Build & tests | ✓ | 3.9.15 | — |
| PostgreSQL 16 | Integration tests (DevServices) | ✓ | — | H2 in-memory (already configured) |
| `npx ctx7` | Documentation lookup | ✓ | latest | Training data with caveats |

**Missing dependencies with no fallback:** None

**Missing dependencies with fallback:** None

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + QuarkusTest + REST Assured + Mockito |
| Config file | none — per-module via parent pom |
| Quick run command | `./mvnw verify -pl mekano-rest -am` |
| Full suite command | `./mvnw verify -pl mekano-rest -am` |

### Phase Requirements → Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| QLD-01 | JaCoCo 80% LINE gate passes | build | `./mvnw verify -pl mekano-rest -am` | ❌ JaCoCo config needs update |
| QLD-02 | NfEntrada pecaId/requisicaoCompraId stored correctly | unit | `mvn test -pl mekano-infrastructure -am -Dtest=NfEntradaRepositoryImplTest` | ❌ no existing test for this specific assertion |
| QLD-02 | Port rename compiles | build | `./mvnw compile -pl mekano-rest -am` | ❌ compile check post-rename |
| QLD-02 | FT/Cache annotations compile | build | `./mvnw compile -pl mekano-rest -am` | ❌ compile check post-annotations |

### Sampling Rate
- **Per task commit:** `./mvnw compile -pl mekano-rest -am` (syntax check)
- **Per wave merge:** `./mvnw verify -pl mekano-rest -am` (full suite + coverage gate)
- **Phase gate:** Full suite green + JaCoCo 80% minimum before `/gsd-verify-work`

### Wave 0 Gaps
- [ ] JaCoCo exclusion set is incomplete — root pom.xml lacks `**/*MapperImpl.class` and `**/*PanacheRepository*.class`
- [ ] mekano-rest pom.xml has no `report-aggregate` execution
- [ ] No tests exist for NfEntradaRepositoryImpl pecaId/requisicaoCompraId correctness
- [ ] No tests exist for ClienteService.updateCliente field application

## Security Domain

> Security enforcement is enabled (`security_enforcement: true`, ASVS Level 1). This phase is primarily code quality — no new attack surface introduced.

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V5 Input Validation | no | No new endpoints added |
| V6 Cryptography | no | No crypto changes |
| V8 Data Protection | no | No data handling changes |

### Known Threat Patterns for Refactoring Phase

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Refactoring introduces subtle logic change | Tampering | All existing tests must pass after each rename/refactor. Full suite green before gate. |
| Removing dead code that was a security check | Elevation of Privilege | Grep for security annotations (`@RolesAllowed`, `@PermitAll`) before deleting any class. Verify mappers don't contain auth logic. |

## Sources

### Primary (HIGH confidence)
- [VERIFIED: mekano source code] — PecaService.java, NfEntradaService.java, RequisicaoCompraService.java (constructor injection verified)
- [VERIFIED: mekano source code] — NfEntradaRepositoryImpl.java lines 39-40 (correct getters verified)
- [VERIFIED: mekano source code] — ClienteService.java lines 54-67 (update logic verified)
- [VERIFIED: mekano source code] — PecaRepositoryPort.java, NfEntradaRepositoryPort.java, RequisicaoCompraRepositoryPort.java (PT-BR method names verified)
- [VERIFIED: mekano source code] — VeiculoRepositoryImpl.java (FT/Cache reference pattern)
- [VERIFIED: mekano source code] — CacheNames.java and cache-config.yml (all cache names already exist)
- [VERIFIED: mekano source code] — CreateVeiculoRequest.java (plate regex matches PlacaVeiculo format)
- [VERIFIED: pom.xml] — JaCoCo 0.8.15 current config
- [CITED: Context7 `/jacoco/jacoco`] — report-aggregate goal configuration pattern
- [VERIFIED: grep results] — domain.model.StatusPagamento: ZERO references (dead code)
- [VERIFIED: grep results] — Placa.java: ZERO non-test references (dead code)
- [VERIFIED: grep results] — PecaEntityMapper, RequisicaoCompraEntityMapper, NfEntradaEntityMapper: ZERO references

### Secondary (MEDIUM confidence)
- [VERIFIED: grep results] — `salvar()`/`buscarPorId()` callers quantified: ~50 across PecaRepository, NfEntradaRepository, RequisicaoCompraRepository + service methods

### Tertiary (LOW confidence)
- (none — all findings verified against current source code)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — only existing JaCoCo config needs extension, no new dependencies
- Architecture: HIGH — all patterns verified against actual source code
- Pitfalls: HIGH — based on verified source inconsistencies and Java compile-time guarantees

**Research date:** 2026-08-08
**Valid until:** 2026-09-08 (30 days — stable Maven/Quarkus tooling)