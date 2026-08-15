# Phase 7: API Improvements - Context

**Gathered:** 2026-08-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Verificar e documentar os endpoints da API de OS, e modificar a listagem de ordens de serviço para ordenação por prioridade de status, excluindo estados terminais.

</domain>

<decisions>
## Implementation Decisions

### API-01: "APIs" — Endpoints ou Múltiplas APIs?
- **D-01:** Dúvida a ser esclarecida com o professor. A task deve especificar que a questão está em aberto e documentar ambos os cenários possíveis. Não implementar nada até esclarecimento.

### API-02: Endpoint de Abertura de OS
- **D-02:** VERIFICADO — `POST /api/v1/os` (OrdemDeServicoResource.java:72) existe e cria OS com status RECEBIDA. Documentar na task como confirmado.

### API-03: Endpoint de Consulta de Status da OS
- **D-03:** VERIFICADO — `GET /api/v1/os/{id}/status` (OrdemDeServicoResource.java:101-102) existe, é público via @PermitAll. Documentar na task como confirmado.

### API-04: Ordenação da Listagem de OS
- **D-04:** Ordenar por prioridade de status via `ORDER BY CASE`: EM_EXECUCAO > AGUARDANDO_APROVACAO > EM_DIAGNOSTICO > RECEBIDA > AGUARDANDO_EXECUCAO
- **D-05:** Dentro de cada status, ordenar por createdAt ASC (mais antigas primeiro)
- **D-06:** Excluir da listagem OS com status FINALIZADA, ENTREGUE e CANCELADA
- **D-07:** Aplicar à query `findAllWithFilters` no OrdemDeServicoRepositoryImpl, adicionando ORDER BY + filtro de exclusão

### the agent's Discretion
- Detalhes da implementação do CASE WHEN no JPQL (sintaxe exata)
- Se a ordenação deve ser aplicada também no endpoint `/filtro` ou apenas no `GET /api/v1/os`

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/OrdemDeServicoResource.java` — endpoints existentes
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/OrdemDeServicoRepositoryImpl.java` — findAllWithFilters (linhas 119-148)
- `mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/repository/OrdemDeServicoRepositoryImplTest.java` — testes existentes de filtro
- `mekano-rest/src/test/java/com/fiap/mekano/rest/api/OrdemDeServicoResourceTest.java` — testes E2E
- `.planning/REQUIREMENTS.md` §API-01, API-02, API-03, API-04
- `.planning/ROADMAP.md` §Phase 7

</canonical_refs>

<code_context>
## Existing Code Insights

### Endpoints de OS
- `POST /api/v1/os` — criar OS (line 72) ✅
- `GET /api/v1/os` — listar OS (line 87)
- `GET /api/v1/os/{id}/status` — status público (line 101) ✅
- `GET /api/v1/os/{id}` — detalhes (line 113)
- `GET /api/v1/os/{id}/detalhamento` — detalhamento completo (line 230)
- `GET /api/v1/os/filtro` — listagem com filtros (line 278)
- `GET /api/v1/os/tempo-medio` — tempo médio (line 262)

### findAllWithFilters Atual
- Query HQL com filtros dinâmicos (status, clienteUuid, veiculoUuid, dataInicio, dataFim)
- SEM ordenação por prioridade de status
- SEM exclusão de status terminais
- Paginação via `page` e `size`

### StatusOS Enum
- `RECEBIDA`, `EM_DIAGNOSTICO`, `AGUARDANDO_APROVACAO`, `AGUARDANDO_EXECUCAO`, `EM_EXECUCAO`, `FINALIZADA`, `ENTREGUE`, `CANCELADA`

</code_context>

<specifics>
## Specific Ideas

- API-01: task deve documentar "pendente de esclarecimento com professor — cenários: (a) endpoints individuais, (b) microsserviços separados"
- API-02 e API-03: tasks de verificação pura, sem implementação — apenas documentar evidência
- API-04: implementar no repository, testar no repository test, verificar via resource test

</specifics>

<deferred>
## Deferred Ideas

- Nenhum — fase focada em verificação e ordenação simples

</deferred>

---

*Phase: 7-API Improvements*
*Context gathered: 2026-08-08*