# Plano de Execução — Issue #25 (Adaptado para o Projeto Atual)

## Contexto

A ISSUE #25 ("Wave 3: Execução de OS + métricas + cancelamento com liberação de estoque") foi parcialmente implementada por outros desenvolvedores em PRs recentes (#61, #62, feat/28-endpoints-orcamento, etc.). Este plano adapta o escopo original para o estado atual do código em `develop`, evitando duplicação e conflitos.

### O que JÁ foi implementado por outros (NÃO refazer)

| Camada | O que já existe |
|--------|----------------|
| **Domain** | `OrdemDeServico.java` com `iniciarExecucao()`, `finalizarExecucao()`, `cancelar()` + campos `mecanicoUuid`, `execucaoIniciadaEm`, `execucaoFinalizadaEm`, `observacaoExecucao` |
| **Domain** | `IniciarExecucaoCommand`, `FinalizarExecucaoCommand`, `CancelarOSCommand`, `CriarOSCommand` records |
| **Domain** | `OsTransitionedEvent` (evento genérico de transição) |
| **Domain** | `StatusOS` com matriz de transições completa |
| **Domain** | Mensagens `os.mecanico.required`, `os.motivo_cancelamento.required` |
| **Application** | `OrdemDeServicoService` com `create`, `findById`, `update`, `cancelar`, `finalizar`, `entregar`, `iniciarDiagnostico`, `finalizarDiagnostico` |
| **Application** | `OrdemDeServicoServicePort` com métodos equivalentes |
| **Infrastructure** | `OrdemDeServicoRepositoryImpl` com `findAllWithFilters`, `calcularTempoMedioExecucao`, `findByIdWithItems` |
| **Infrastructure** | `OrdemDeServicoEntityMapperImpl` mapeando todos os campos de execução |
| **Infrastructure** | `OrdemDeServicoEntity` com `mecanicoUuid`, timestamps, `observacaoExecucao` |
| **REST** | `OrdemDeServicoResource` em `/os` com CRUD + transições básicas |
| **REST** | `OrdemDeServicoResponse`, `OrdemDeServicoPageResponse` |
| **REST** | `IniciarExecucaoRequest`, `FinalizarExecucaoRequest`, `CancelarOSRequest` |
| **REST** | `OrdemDeServicoDtoMapper` (MapStruct, atualmente não usado pelo Resource) |

---

## Fase 1 — Domain Layer (estender contratos existentes)

### 1.1 Estender `OrdemDeServicoRepositoryPort`
**Arquivo:** `mekano-domain/.../port/out/OrdemDeServicoRepositoryPort.java`

```diff
- List<OrdemDeServico> findAllWithFilters(String status, UUID clienteUuid, int page, int size);
+ List<OrdemDeServico> findAllWithFilters(String status, UUID clienteUuid, UUID veiculoUuid,
+                                          LocalDateTime dataInicio, LocalDateTime dataFim,
+                                          int page, int size);

- Optional<Double> calcularTempoMedioExecucao();
+ Optional<Double> calcularTempoMedioExecucao(LocalDateTime dataInicio, LocalDateTime dataFim);

+ boolean existsByClienteUuidAndStatusIn(UUID clienteUuid, List<String> statuses);
+ Optional<UUID> findOrcamentoUuidByOsId(UUID osId);
```

### 1.2 Estender `OrcamentoRepositoryPort`
**Arquivo:** `mekano-domain/.../port/out/OrcamentoRepositoryPort.java`

```diff
+ boolean existsByPecaIdVinculadaAOrdemComStatus(UUID pecaId, List<String> statuses);
```

### 1.3 Criar `OSFinalizadaEvent`
**Arquivo:** `mekano-domain/.../event/OSFinalizadaEvent.java` (novo)

Evento de domínio específico para OS finalizada (complementar ao `OsTransitionedEvent` genérico).

### 1.4 Estender `PecaRepositoryPort` (opcional)
**Arquivo:** `mekano-domain/.../port/out/PecaRepositoryPort.java`

```diff
+ void remover(UUID id);
```

### 1.5 Adicionar mensagens no `messages.properties`
**Arquivo:** `mekano-domain/.../resources/messages.properties`

Chaves novas:
```properties
os.execucao.status.invalido.iniciar = Só é possível iniciar execução de OS em AGUARDANDO_APROVACAO. Status atual: {0}
os.execucao.status.invalido.finalizar = Só é possível finalizar execução de OS em EM_EXECUCAO. Status atual: {0}
os.cliente.possui.os.ativa          = Cliente possui Ordem de Serviço ativa. Exclusão bloqueada
os.peca.vinculada.os.ativa          = Peça vinculada a Orçamento de OS ativa. Exclusão bloqueada
```

---

## Fase 2 — Application Layer (estender serviços existentes)

### 2.1 Estender `OrdemDeServicoServicePort`
**Arquivo:** `mekano-domain/.../port/in/OrdemDeServicoServicePort.java`

Adicionar métodos:
```java
OrdemDeServico iniciarExecucao(UUID id, UUID mecanicoUuid, String observacao);
OrdemDeServico finalizarExecucao(UUID id, String observacao);
OrdemDeServico cancelar(UUID id, String motivo); // já existe — manter
List<OrdemDeServico> findAllWithFilters(String status, UUID clienteUuid, UUID veiculoUuid,
                                        LocalDateTime dataInicio, LocalDateTime dataFim,
                                        int page, int size);
Optional<OrdemDeServico> findByIdWithItems(UUID id);
Optional<UUID> findOrcamentoUuidByOsId(UUID osId);
Optional<Double> calcularTempoMedioExecucao(LocalDateTime dataInicio, LocalDateTime dataFim);
boolean clientePossuiOsAtiva(UUID clienteUuid);
```

### 2.2 Estender `OrdemDeServicoService`
**Arquivo:** `mekano-application/.../service/ordemdeservico/OrdemDeServicoService.java`

Adicionar/sobrescrever métodos:

| Método | Descrição |
|--------|-----------|
| `iniciarExecucao(UUID id, UUID mecanicoUuid, String observacao)` | Busca OS, valida status, chama `os.iniciarExecucao()`, salva |
| `finalizarExecucao(UUID id, String observacao)` | Busca OS, chama `os.finalizarExecucao()`, salva, publica `OSFinalizadaEvent` |
| `findAllWithFilters(...)` | Delega ao repository com todos os filtros |
| `findByIdWithItems(UUID id)` | Delega ao repository |
| `findOrcamentoUuidByOsId(UUID osId)` | Delega ao repository |
| `calcularTempoMedioExecucao(dataInicio, dataFim)` | Delega ao repository |
| `clientePossuiOsAtiva(UUID clienteUuid)` | Verifica OS em AGUARDANDO_APROVACAO/EM_EXECUCAO |

**Injeções adicionais necessárias:** `OrcamentoRepositoryPort` (para `buscarOrcamentoDaOs`), `PecaRepositoryPort` (para liberar estoque no cancelamento).

**Cancelamento com liberação de estoque:** No método `cancelar(UUID id, String motivo)`, estender para:
1. Buscar orçamento da OS via `orcamentoRepository.findByOrdemServicoUuid()`
2. Para cada `ItemOrcamento`, buscar peça por descrição e creditar saldo
3. Só então chamar `os.cancelar(motivo)` (transição via entidade)

### 2.3 Adicionar guarda em `ClienteService.deleteCliente()`
**Arquivo:** `mekano-application/.../service/cliente/ClienteService.java`

- Injetar `OrdemDeServicoRepositoryPort`
- Antes do `markAsDeleted`, verificar se existem OS em `EM_EXECUCAO` ou `AGUARDANDO_APROVACAO` para o cliente
- Se existir, lançar `AppException(409, Messages.get("os.cliente.possui.os.ativa"))`

### 2.4 Adicionar método `excluir` em `PecaService`
**Arquivo:** `mekano-application/.../service/peca/PecaService.java`

- Injetar `OrcamentoRepositoryPort`
- Novo método `excluir(UUID pecaId)`: 
  1. Verifica se peça está vinculada a orçamento de OS ativa via `orcamentoRepository.existsByPecaIdVinculadaAOrdemComStatus()`
  2. Se vinculada, lançar `AppException(409, Messages.get("os.peca.vinculada.os.ativa"))`
  3. Caso contrário, chamar `pecaRepository.remover(pecaId)` (soft delete)

---

## Fase 3 — Infrastructure Layer (implementar contratos)

### 3.1 Estender `OrdemDeServicoRepositoryImpl`
**Arquivo:** `mekano-infrastructure/.../repository/OrdemDeServicoRepositoryImpl.java`

- `findAllWithFilters`: adicionar parâmetros `veiculoUuid`, `dataInicio`, `dataFim` à query dinâmica
- `calcularTempoMedioExecucao`: aceitar `dataInicio`/`dataFim` como filtro de período
- Novo `existsByClienteUuidAndStatusIn`: `count` com `clienteUuid = ?1 AND status IN (?2) AND isActive = true`
- Novo `findOrcamentoUuidByOsId`: buscar por UUID e retornar `orcamentoUuid` do mapper

### 3.2 Adicionar `remover` em `PecaRepositoryImpl`
**Arquivo:** `mekano-infrastructure/.../repository/PecaRepositoryImpl.java`

Implementar soft delete: `isActive = false`, `deletedAt = LocalDateTime.now()`

### 3.3 Adicionar `existsByPecaIdVinculadaAOrdemComStatus` em `OrcamentoRepositoryImpl`
**Arquivo:** `mekano-infrastructure/.../repository/OrcamentoRepositoryImpl.java`

- Buscar orçamentos vinculados a OS com status `AGUARDANDO_APROVACAO`/`EM_EXECUCAO`/`FINALIZADA`
- Verificar se `itensJson` contém a `pecaId` via LIKE ou Jackson parse

---

## Fase 4 — REST Layer (endpoints faltantes)

### 4.1 Criar DTOs faltantes
**Arquivos novos** em `mekano-rest/.../rest/api/dto/`:

| DTO | Tipo | Descrição |
|-----|------|-----------|
| `OrdemDeServicoDetailResponse` | `record` | OS + UUID do orçamento + itens (preparado para futuro) |
| `TempoMedioResponse` | `record` | Output: `tempoMedioHoras` (Double) |

### 4.2 Estender `OrdemDeServicoResource`
**Arquivo:** `mekano-rest/.../rest/api/OrdemDeServicoResource.java` (modificado)

Adicionar endpoints ao resource existente em `/os`:

| Método | Path | Nome método | Roles | Descrição |
|--------|------|-------------|-------|-----------|
| `PUT` | `/os/{id}/iniciar-execucao` | `iniciarExecucao` | mecanico, admin | Inicia execução da OS |
| `PUT` | `/os/{id}/finalizar-execucao` | `finalizarExecucao` | mecanico, admin | Finaliza execução |
| `GET` | `/os/{id}/detalhamento` | `getDetalhamento` | admin, atendente | Detalhamento da OS |
| `GET` | `/os/tempo-medio` | `getTempoMedio` | admin, atendente | Tempo médio por período |

O endpoint `cancelar` já existe em `PUT /os/{id}/cancelar`.

### 4.3 Estender `OrdemDeServicoResponse` (opcional)
**Arquivo:** `mekano-rest/.../rest/api/dto/OrdemDeServicoResponse.java`

Adicionar campos opcionais: `mecanicoUuid`, `execucaoIniciadaEm`, `execucaoFinalizadaEm`, `observacaoExecucao`, `orcamentoUuid` — sem quebrar compatibilidade.

---

## Resumo de artefatos por camada

| Camada | Novos | Modificados |
|--------|-------|-------------|
| **Domain** | `OSFinalizadaEvent.java` | `OrdemDeServicoRepositoryPort.java`, `OrcamentoRepositoryPort.java`, `PecaRepositoryPort.java`, `messages.properties` |
| **Application** | — | `OrdemDeServicoService.java`, `OrdemDeServicoServicePort.java`, `ClienteService.java`, `PecaService.java` |
| **Infrastructure** | — | `OrdemDeServicoRepositoryImpl.java`, `PecaRepositoryImpl.java`, `OrcamentoRepositoryImpl.java` |
| **REST** | `OrdemDeServicoDetailResponse.java`, `TempoMedioResponse.java` | `OrdemDeServicoResource.java`, `OrdemDeServicoResponse.java` |

**Total:** 3 novos + 11 modificados = 14 artefatos

---

## Dependência entre tarefas

```
Domain ports (Fase 1) ──→ Application services (Fase 2) ──→ Infrastructure (Fase 3) ──→ REST (Fase 4)
       │                                                                                        │
       └── Evento OSFinalizadaEvent ─────────────────────────────────────────────────────────────┘
```

## O que NÃO fazer (já implementado por outros)

- ❌ NÃO criar nova entidade `OrdemDeServico` — já existe
- ❌ NÃO adicionar campos de execução no model — já existem
- ❌ NÃO criar `IniciarExecucaoRequest`/`FinalizarExecucaoRequest`/`CancelarOSRequest` — já existem
- ❌ NÃO criar `OrdemDeServicoDtoMapper` — já existe
- ❌ NÃO recriar endpoint `cancelar` — já existe em `PUT /os/{id}/cancelar`
- ❌ NÃO modificar `OrdemDeServicoEntity` — já tem campos de execução
- ❌ NÃO modificar `OrdemDeServicoEntityMapperImpl` — já mapeia todos os campos
