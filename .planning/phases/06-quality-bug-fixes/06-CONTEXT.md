# Phase 6: Quality & Bug Fixes - Context

**Gathered:** 2026-08-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Elevar a qualidade do código para padrão de produção: 80% de cobertura de testes (JaCoCo), corrigir bugs conhecidos, e realizar revisão geral de código para eliminar tech debt estrutural (naming, injeção, estilo, duplicação).

</domain>

<decisions>
## Implementation Decisions

### JaCoCo 80% Coverage (QLD-01)
- **D-01:** Usar `report-aggregate` no módulo `mekano-rest` para visão unificada (não apenas BUNDLE por módulo)
- **D-02:** Adicionar exclusions de `**/*MapperImpl.class` (MapStruct generated), `**/*PanacheRepository*.class`, e classes geradas
- **D-03:** Pipeline CI falha se cobertura abaixo de 80% — gate já existe no `verify` phase

### Bug Fixes
- **D-04:** Corrigir NfEntradaRepositoryImpl — `pecaId` e `requisicaoCompraId` ambos setados para `nfEntrada.getId()` (copy-paste bug). `pecaId` deve vir do `requisicao.getPecaId()`, `requisicaoCompraId` do `requisicao.getId()`

### Tech Debt Items (QLD-02 — Open-ended Code Review)
- **D-05:** Padronizar nomenclatura dos ports: `salvar`/`buscarPorId` → `save`/`findById` em PecaRepositoryPort, NfEntradaRepositoryPort, RequisicaoCompraRepositoryPort
- **D-06:** Trocar field injection (`@Inject`) por constructor injection nos 3 stubs/stub services que ainda usam field injection
- **D-07:** Padronizar estilo de entidades — avaliar @Data vs @Getter/@Setter e definir um padrão único
- **D-08:** Remover mappers vazios: PecaEntityMapper, RequisicaoCompraEntityMapper, NfEntradaEntityMapper (sem métodos, dead code)
- **D-09:** Unificar VOs duplicados: `Placa.java` e `PlacaVeiculo.java` (regex diferentes, mesmo conceito)
- **D-10:** Mover `ItemOrcamento` de `model/` para `valueobject/` (é Value Object, não entidade)
- **D-11:** Unificar `StatusPagamento` — existe em `domain/model/` e `domain/os/`
- **D-12:** Adicionar `@Retry` + `@Timeout` + `@CacheResult` nos repositórios que ainda não têm: Cliente, Peca, RequisicaoCompra, NfEntrada
- **D-13:** Revisão é aberta — o executor pode identificar e corrigir mais itens durante a execução

### the agent's Discretion
- Ordem de correção dos itens de tech debt (qual prioridade)
- Decisão final sobre @Data vs @Getter/@Setter (avaliar consistência atual)
- Se deve mover ItemOrcamento ou apenas criar o VO no package correto e manter compatibilidade

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### JaCoCo
- `pom.xml` (raiz) — configuração atual do JaCoCo (prepare-agent, report, check com 80% LINE)
- `mekano-rest/pom.xml` — onde adicionar report-aggregate

### Tech Debt
- `./AGENTS.md` §"Key Inconsistencies" — lista completa dos 9 itens de tech debt verificados
- `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/PecaRepositoryPort.java` — PT-BR naming
- `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/NfEntradaRepositoryPort.java` — PT-BR naming
- `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/RequisicaoCompraRepositoryPort.java` — PT-BR naming
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/NfEntradaRepositoryImpl.java` — bug copy-paste
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/ItemOrcamento.java` — misplaced VO
- `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/Placa.java` — VO duplicado
- `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/PlacaVeiculo.java` — VO duplicado

### Requisitos
- `.planning/REQUIREMENTS.md` §QLD-01, QLD-02
- `.planning/ROADMAP.md` §Phase 6

</canonical_refs>

<code_context>
## Existing Code Insights

### JaCoCo Config
- `pom.xml` lines 124-176: JaCoCo plugin com prepare-agent, report, check (80% LINE, BUNDLE)
- Excludes atuais: `**/*Dto.class`, `**/*DTO.class`, `**/*Request.class`, `**/*Response.class`, `**/*ExceptionMapper.class`, `**/*Config.class`, `**/*Resource.class`, `**/*Entity.class`
- Faltam excludes: `**/*MapperImpl.class`, `**/*PanacheRepository*.class`

### Known Bugs
- NfEntradaRepositoryImpl.java: `pecaId` e `requisicaoCompraId` ambos = `nfEntrada.getId()` — deve ser `requisicao.getPecaId()` e `requisicao.getId()`

### Test Coverage
- 517 testes atualmente (todos passando)
- Domínio: ~262 testes (alta cobertura)
- Application: ~85 testes
- Infrastructure: ~61 testes
- REST: ~109 testes

</code_context>

<specifics>
## Specific Ideas

- JaCoCo exclusions devem incluir também classes geradas pelo MapStruct (`*MapperImpl`) e classes Panache (`*PanacheRepository*`)
- Mudança de PT-BR para EN nos ports deve ser feita com cuidado: mudar a interface + todas as implementações de uma vez para não quebrar compilação
- Mappers vazios podem ser removidos se nenhum código os referencia — verificar antes com grep
- Unificação de Placa/PlacaVeiculo: avaliar qual regex é mais abrangente (Mercosul + antigo)
- StatusPagamento duplicado: verificar qual dos dois é usado atualmente e remover o inativo

</specifics>

<deferred>
## Deferred Ideas

- CI/CD com JaCoCo coverage gate no PR — já existe no verify phase
- Melhoria de performance de testes — fora do escopo
- Documentação de cobertura por módulo — pode ser extraída do report-aggregate

</deferred>

---

*Phase: 6-Quality & Bug Fixes*
*Context gathered: 2026-08-08*