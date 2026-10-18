# 09-01 — SUMMARY — Fechar feedback da avaliação (issue #146)

**Branch:** `feature/fase-9-feedback-closure` · **Base:** `develop` · **Data:** 2026-10-17
**Verificação final:** `./mvnw verify -pl mekano-rest -am` → **BUILD SUCCESS**

## Rastreabilidade feedback → task → evidência

| # | Prioridade do feedback | Task | Arquivos alterados | Verificação | Resultado |
|---|------------------------|------|--------------------|-------------|-----------|
| 1 | Idempotência do orçamento | T1 | `mekano-domain/.../model/Orcamento.java`, `mekano-application/.../orcamento/OrcamentoService.java` | `mvnw test -pl mekano-domain -Dtest=OrcamentoTest`; `mvnw test -pl mekano-application -am -Dtest=OrcamentoServiceTest` | 16 + 19 testes verdes |
| 1 | Idempotência E2E | T2 | `mekano-rest/src/test/.../OrcamentoFluxoCriticoE2ETest.java` (novo, beans reais) | `mvnw test -pl mekano-rest -am -Dtest=OrcamentoFluxoCriticoE2ETest` | 7/7 verdes (replay 200+200, conflito 422 problem+json) |
| 2 | Exclusão lógica na listagem | T3 | `mekano-infrastructure/.../OrdemDeServicoRepositoryImpl.java` | `mvnw test -pl mekano-infrastructure -am -Dtest=OrdemDeServicoRepositoryImplTest` | 29 testes verdes (24 existentes + 5 novos) |
| 2+3 | Listagem e status E2E | T4 | `mekano-rest/src/test/.../OrdemDeServicoListagemE2ETest.java` (novo, beans reais) | `mvnw test -pl mekano-rest -am -Dtest=OrdemDeServicoListagemE2ETest` | 7/7 verdes |
| 3b | Paginação GET /os/filtro (bônus decidido) | T3b | `OrdemDeServicoRepositoryPort`, `OrdemDeServicoServicePort`, `OrdemDeServicoService`, `OrdemDeServicoResource` | `filtro_paginaUnica_totalPagesCalculadoDeVerdade` (E2E) | total=2/totalPages=2 com size=1 (antes hardcoded 1) |
| 4 | Clean Code | T5 | `OrdemDeServicoResource.java`, `OrcamentoResource.java`, `OrcamentoService.java`, `messages.properties` | `mvnw test -pl mekano-rest -am` | BUILD SUCCESS, zero mudança de status code |
| 5 | Conformidade Docker/K8s | T6 | nenhum desvio — correção não necessária | `docker compose config -q`; `kubectl apply --dry-run=client -f k8s/`; `docker build -f mekano-rest/src/main/docker/Dockerfile.jvm .` | COMPOSE OK; 13 manifests dry-run OK; imagem `mekano:jvm-validate` (624MB) buildada |
| 5 | Conformidade Terraform/CD | T7 | nenhum desvio — correção não necessária | yq parse de `ci.yml`/`cd.yml`; conferência README-INFRA ↔ `variables.tf` | Workflows YAML válidos; README convergente; **Terraform pendência manual** (abaixo) |
| — | Verificação final | T8 | `mvnw verify -pl mekano-rest -am` | suíte completa | **BUILD SUCCESS** |

## Detalhes por task

### T1 — Idempotência (domínio + serviço)
- `Orcamento.aprovar()`: já APROVADO → no-op; REPROVADO/EXPIRADO → `AppException(422)` (inalterado)
- `Orcamento.reprovar()`: já REPROVADO → no-op; APROVADO/EXPIRADO → 422
- `OrcamentoService.aprovar/reprovar`: guarda early-return antes de transição de OS, save, eventos e auditoria
- Testes atualizados intencionalmente: `deveLancar422SeOrcamentoJaAprovado` e `deveLancar422SeOrcamentoJaReprovado` → substituídos por testes idempotentes + conflitos explícitos (REPROVADO→aprovar, APROVADO→reprovar, EXPIRADO→aprovar)
- Prova de não-duplicação: `verify(eventPublisher, times(1))` após replay com OS associada

### T3 — Exclusão lógica
- `TERMINAL_STATUSES = FINALIZADA, ENTREGUE, CANCELADA` — constante única compartilhada por `findAll`, `countAll` e `findAllWithFilters`
- `findAll`: sort default (blank/`createdAt,desc`) → `STATUS_PRIORITY_ORDER`; sort custom → `Sort.by` com exclusão
- `countAll`: mesma exclusão → `totalElements` de GET /os consistente

### T5 — Clean Code (lista fechada)
- `OrdemDeServicoResource.toItensCommands()` — dedup create/update
- `OrcamentoResource.buscarPorUuidOs()` — dedup path/query
- `Messages.get("orcamento.not.found.os")` — nova chave em `messages.properties` (formato unicode-escape)
- OpenAPI: 422 documentado como conflito de estado; replay documentado como 200 idempotente

## Fluxos críticos cobertos por testes (item 3 do feedback)

| Fluxo | Testes |
|-------|--------|
| Aprovação (replay idempotente) | `OrcamentoFluxoCriticoE2ETest.aprovar_replayIdempotente_retorna200DuasVezes`, `OrcamentoServiceTest.replayAprovacaoComOSNaoDuplicaEfeitos`, `OrcamentoTest.deveSerIdempotenteAoAprovarDuasVezes` |
| Reprovação (replay idempotente) | `OrcamentoFluxoCriticoE2ETest.reprovar_replayIdempotente_retorna200DuasVezes`, `OrcamentoServiceTest.deveSerIdempotenteSeOrcamentoJaReprovado` |
| Listagem (exclusão lógica) | `OrdemDeServicoListagemE2ETest.listAll_naoRetornaStatusTerminais_eTotalConsistente`, `OrdemDeServicoRepositoryImplTest.findAll_excluiStatusTerminais`, `countAll_excluiStatusTerminais` |
| Consulta de status público | `OrdemDeServicoListagemE2ETest.statusPublico_osAtiva_retorna200SemAutenticacao`, `statusPublico_uuidInexistente_retorna404ProblemJson`, `aprovar_aposReplay_osAvancaUmaUnicaVez` |

## Conformidade infra — evidências e pendências

| Check | Comando | Resultado |
|-------|---------|-----------|
| Docker Compose | `docker compose config -q` | OK |
| Imagem JVM multi-stage (contexto = raiz) | `docker build -f mekano-rest/src/main/docker/Dockerfile.jvm -t mekano:jvm-validate .` | OK — 624MB |
| Manifests K8s | `kubectl apply --dry-run=client -f k8s/` | OK — 13 recursos (namespace `mekano-system` consistente em 11 arquivos) |
| HPA métricas | inspeção `k8s/hpa.yaml` | cpu `averageUtilization: 70` + memory `80` (autoscaling/v2) |
| Deployment probes/resources | inspeção `k8s/deployment.yaml` | livenessProbe, readinessProbe, requests+limits presentes |
| Workflows CI/CD | `docker run mikefarah/yq eval ".jobs \| keys"` (actionlint ausente) | ci.yml: bootstrap-backend, infrastructure, infra-apply, deploy, build; cd.yml: infra-action, build-test, docker — mapeia provision→build→test→deploy |
| README-INFRA ↔ variables.tf | inspeção | `db_password` (única sem default) documentada via `TF_VAR_db_password`; `-backend-config=backend.hcl` bate com `backend.hcl.example`; caminho `terraform/` minúsculo |
| **Terraform fmt/validate** | `terraform -chdir=terraform init -backend=false && terraform -chdir=terraform validate` | ⚠ **PENDENTE** — Terraform CLI ausente na máquina local; comandos registrados para execução em ambiente com Terraform (ou via job `infra-action` do CD) |

## Testes existentes removidos
Nenhum. 2 testes foram **reescrevidos** com justificativa (semântica idempotente nova — item 1 do feedback):
`OrcamentoServiceTest.deveLancar422SeOrcamentoJaAprovado` → `deveSerIdempotenteSeOrcamentoJaAprovado`;
`OrcamentoServiceTest.deveLancar422SeOrcamentoJaReprovado` → `deveSerIdempotenteSeOrcamentoJaReprovado` + `deveLancar422AoReprovarOrcamentoAprovado`.

## Sobreposição com planos GSD existentes
- 07-02-PLAN (exclusão/prioridade em `findAllWithFilters`): escopo já presente no código — confirmado, não regrediu.
- Nenhum pacote/dependência nova instalada. Nenhuma classe de exceção nova (D-05 respeitado).
