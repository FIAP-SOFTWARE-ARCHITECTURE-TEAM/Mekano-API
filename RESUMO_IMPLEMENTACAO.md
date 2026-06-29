# Resumo da Implementação — Issue #25

## Wave 3: Execução de OS + métricas + cancelamento com liberação de estoque

**Issue:** [#25](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/issues/25)
**Status:** Implementado e compilando (✓ domain tests, ✓ application tests)

---

## O que foi feito

### Domain (5 arquivos)

| Arquivo | Ação | Detalhes |
|---------|------|----------|
| `mekano-domain/.../model/OrdemDeServico.java` | Modificado | +4 campos (`mecanicoUuid`, `execucaoIniciadaEm`, `execucaoFinalizadaEm`, `observacaoExecucao`), +2 métodos (`iniciarExecucao`, `finalizarExecucao`), `reconstitute` atualizado com novos params |
| `mekano-domain/.../event/OSFinalizadaEvent.java` | **Novo** | Evento de domínio `record(UUID ordemServicoId, LocalDateTime occurredAt)` |
| `mekano-domain/.../port/out/OrdemDeServicoRepositoryPort.java` | Modificado | `findAllWithFilters` extendido (veiculoUuid, dataInicio, dataFim), +`existsByClienteUuidAndStatusIn`, +`findOrcamentoUuidByOsId`, `calcularTempoMedioExecucao` com período |
| `mekano-domain/.../port/out/OrcamentoRepositoryPort.java` | Modificado | +`existsByPecaIdVinculadaAOrdemComStatus` |
| `mekano-domain/.../resources/messages.properties` | Modificado | +5 mensagens pt-BR (mecanico required, status inválido iniciar/finalizar, bloqueio cliente/peça) |

### Application (3 arquivos)

| Arquivo | Ação | Detalhes |
|---------|------|----------|
| `mekano-application/.../service/ordemservico/OrdemDeServicoExecucaoService.java` | **Novo** | `@ApplicationScoped`, constructor injection, `@Transactional` nos writes. Métodos: `iniciarExecucao`, `finalizarExecucao` (publica `OSFinalizadaEvent`), `cancelarExecucao` (libera estoque via busca por descrição), `listarComFiltros`, `buscarPorId`, `buscarComItens`, `buscarOrcamentoDaOs`, `calcularTempoMedioExecucao`, `clientePossuiOsAtiva` |
| `mekano-application/.../service/cliente/ClienteService.java` | Modificado | +guarda: injeta `OrdemDeServicoRepositoryPort`, verifica OS em EM_EXECUCAO/AGUARDANDO_APROVACAO antes de soft delete (retorna 409) |
| `mekano-application/.../service/peca/PecaService.java` | Modificado | +`OrcamentoRepositoryPort`, +método `excluir(UUID)` com verificação de peça vinculada a OS ativa |

### Infrastructure (4 arquivos)

| Arquivo | Ação | Detalhes |
|---------|------|----------|
| `mekano-infrastructure/.../repository/OrdemDeServicoRepositoryImpl.java` | Modificado | Query dinâmica com 5 filtros; `existsByClienteUuidAndStatusIn` via `count`; `findOrcamentoUuidByOsId`; `calcularTempoMedioExecucao` com período via JPQL dinâmico |
| `mekano-infrastructure/.../mapper/OrdemDeServicoEntityMapperImpl.java` | Modificado | `toEntity` mapeia `mecanicoUuid`, timestamps, `observacaoExecucao`; `toDomain` repassa para `reconstitute` |
| `mekano-infrastructure/.../repository/PecaRepositoryImpl.java` | Modificado | +`remover(UUID)` — soft delete com `isActive=false` + `deletedAt` |
| `mekano-infrastructure/.../repository/OrcamentoRepositoryImpl.java` | Modificado | +`existsByPecaIdVinculadaAOrdemComStatus` — busca orçamentos vinculados a OS e verifica `itensJson` |

### REST (9 arquivos)

| Arquivo | Ação | Tipo |
|---------|------|------|
| `mekano-rest/.../api/OrdemDeServicoResource.java` | **Novo** | Resource com 7 endpoints |
| `mekano-rest/.../api/mapper/OrdemDeServicoDtoMapper.java` | **Novo** | MapStruct `componentModel = "cdi"` |
| `mekano-rest/.../api/dto/OrdemDeServicoResponse.java` | **Novo** | Output record |
| `mekano-rest/.../api/dto/OrdemDeServicoPageResponse.java` | **Novo** | Resposta paginada |
| `mekano-rest/.../api/dto/OrdemDeServicoDetailResponse.java` | **Novo** | OS + itens orçados × executados lado a lado |
| `mekano-rest/.../api/dto/IniciarExecucaoRequest.java` | **Novo** | Input Lombok: `mecanicoUuid` |
| `mekano-rest/.../api/dto/FinalizarExecucaoRequest.java` | **Novo** | Input Lombok: `observacao` |
| `mekano-rest/.../api/dto/CancelarExecucaoRequest.java` | **Novo** | Input Lombok: `motivo` |
| `mekano-rest/.../api/dto/TempoMedioResponse.java` | **Novo** | Output record |

---

## Endpoints criados

| Método | Path | Descrição | Status code |
|--------|------|-----------|-------------|
| `GET` | `/api/v1/ordens-servico` | Lista OS com filtros (clienteUuid, veiculoUuid, status, dataInicio, dataFim, page, size) | 200 |
| `GET` | `/api/v1/ordens-servico/{id}` | Busca OS por ID | 200 / 404 |
| `GET` | `/api/v1/ordens-servico/{id}/detalhamento` | Itens orçados × executados lado a lado | 200 / 404 |
| `PUT` | `/api/v1/ordens-servico/{id}/iniciar-execucao` | Inicia execução (body: `mecanicoUuid`) | 200 / 400 / 404 |
| `PUT` | `/api/v1/ordens-servico/{id}/finalizar-execucao` | Finaliza execução (body: `observacao`), publica evento | 200 / 400 / 404 |
| `PUT` | `/api/v1/ordens-servico/{id}/cancelar` | Cancela OS + libera estoque (body: `motivo`) | 200 / 400 / 404 / 422 |
| `GET` | `/api/v1/ordens-servico/tempo-medio` | Tempo médio de execução (query: dataInicio, dataFim) | 200 |

---

## Total de artefatos

| Tipo | Quantidade |
|------|------------|
| Arquivos novos | **11** |
| Arquivos modificados | **9** |
| **Total** | **20** |

---

## Observações técnicas

1. **Liberação de estoque no cancelamento**: O `ItemOrcamento` não possui `pecaId`, apenas `descricao`. A liberação busca peças por descrição — funcionará corretamente quando descrições forem únicas/consistentes.
2. **Itens executados**: O campo `itensExecutados` no `OrdemDeServicoDetailResponse` está preparado para receber dados futuros de rastreamento de execução. Atualmente retorna lista vazia.
3. **Guardas de soft delete**: Cliente com OS em `EM_EXECUCAO`/`AGUARDANDO_APROVACAO` retorna 409. Peça vinculada a orçamento de OS com mesmo status também retorna 409.
4. **Roles**: Todos os endpoints exigem `admin` ou `atendente` — consistente com os demais recursos de negócio.

---

## Testes

- `mvnw test -pl mekano-domain` → OK
- `mvnw test -pl mekano-application -am` → OK
- `mvnw compile` → OK (projeto completo compila sem erros)
