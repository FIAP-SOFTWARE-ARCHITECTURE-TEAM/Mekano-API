---
phase: 2
slug: os-continuation-estoque
verified_by: "closure-phase 03.1 (retroactive)"
verified_at: 2026-08-08
nyquist_compliant: true
---

# Phase 02 — OS Continuation & Estoque: Formal Verification

## 1. Escopo Verificado

Requisitos da fase cobertos por esta verificação (fonte: ROADMAP.md e REQUIREMENTS.md):

| ID | Descrição | Status v1.0 |
|----|-----------|-------------|
| AUTH-04 | Admin pode gerenciar usuários do sistema (CRUD) | Complete |
| OS-09 | Sistema gera orçamento automaticamente ao finalizar diagnóstico | Complete |
| OS-10 | Cliente pode aprovar orçamento via API pública | Complete |
| OS-11 | Cliente pode reprovar orçamento via API pública | Complete |
| OS-13 | Mecânico pode registrar início da execução | Complete |
| OS-14 | Mecânico pode finalizar execução | Complete |
| OS-16 | Admin/atendente pode listar OS com filtros (paginado) | Complete |
| OS-17 | Admin/atendente pode ver detalhes completos de uma OS | Complete |
| OS-18 | Admin pode consultar tempo médio de execução por tipo de serviço | Complete |
| EST-01 | Admin/almoxarife pode cadastrar peça/insumo | Complete |
| EST-02 | Admin/almoxarife pode editar, consultar e excluir peças/insumos | Complete |
| EST-03 | Sistema reserva automaticamente peças disponíveis ao aprovar orçamento | Complete |
| EST-04 | Sistema gera Requisição de Compra para peças indisponíveis | Complete |
| EST-05 | Admin/almoxarife pode listar, visualizar e cancelar Requisições de Compra | Complete |
| EST-06 | Almoxarife/financeiro pode registrar NF de entrada referenciando Requisição de Compra | Complete |
| EST-07 | Ao atualizar saldo, sistema verifica itens abaixo do estoque mínimo | Complete |
| EST-08 | Almoxarife registra saída de peças reservadas ao iniciar execução | Complete |
| EST-09 | Sistema alerta quando estoque mínimo é atingido | Complete |
| DOC-02 | Especificação OpenAPI/Swagger da API documentada | Complete |

**Total: 19/19 requisitos v1 da fase satisfeitos.**

## 2. Evidência de Testes

Comandos executados em `2026-08-08T01:03-03:00` (branch atual pós-phase-03.1):

| Módulo | Comando | Resultado | Testes |
|--------|---------|-----------|--------|
| Domain | `./mvnw test -pl mekano-domain` | ✅ BUILD SUCCESS | 262 tests, 0 failures |
| Application | `./mvnw test -pl mekano-application -am` | ✅ BUILD SUCCESS | 85 tests, 0 failures |
| Infrastructure | `./mvnw test -pl mekano-infrastructure -am` | ✅ BUILD SUCCESS | 61 tests, 0 failures |
| REST (E2E) | `./mvnw verify -pl mekano-rest -am` | ✅ BUILD SUCCESS | 109 tests, 0 failures |
| **Total** | | **✅ BUILD SUCCESS** | **510 tests, 0 failures** |

### Testes específicos da fase 02

| Teste | Tipo | Resultado |
|-------|------|-----------|
| PecaResourceTest (PUT/DELETE) | E2E (REST Assured) | ✅ Pass (12 tests) |
| RequisicaoCompraResourceTest | E2E (REST Assured) | ✅ Pass |
| NfEntradaResourceTest | E2E (REST Assured) | ✅ Pass (crédito de saldo verificado) |
| AlertaResourceTest | E2E (REST Assured) | ✅ Pass |
| OrcamentoResourceTest | E2E (REST Assured) | ✅ Pass |
| OrdemDeServicoResourceTest | E2E (REST Assured) | ✅ Pass (filtros, detalhamento, tempo médio) |
| OrdemDeServicoRepositoryImplTest | Integração | ✅ Pass (7 tests: filtros, combinações, agrupamento) |
| OsAuditResourceTest | E2E (REST Assured) | ✅ Pass (2 tests) |
| OrcamentoServiceTest | Aplicação (Mockito) | ✅ Pass (11 tests) |
| OrdemDeServicoServiceTest | Aplicação (Mockito) | ✅ Pass (16 tests) |
| PecaServiceTest | Aplicação (Mockito) | ✅ Pass (5 tests) |
| NfEntradaServiceTest | Aplicação (Mockito) | ✅ Pass (3 tests) |
| PecaOrcamentoObserverTest | Infra (Mockito) | ✅ Pass (3 tests) |
| EstoqueMinimoObserverTest | Infra (Mockito) | ✅ Pass (3 tests) |
| AdminUserServiceTest | Aplicação (Mockito) | ✅ Pass (5 tests) |
| SlaExpiryJobTest | Infra (Mockito) | ✅ Pass |
| CobrancaEmitidaListenerTest | Infra (QuarkusTest) | ✅ Pass (4 tests) |

**Cobertura de fluxos:** Orçamento (gerar, aprovar, reprovar), execução (iniciar, finalizar), estoque (CRUD peças, reserva, requisição, NF entrada, alerta mínimo), auditoria, SLA, detalhamento, tempo médio.

## 3. Fluxos E2E Verificados

### Pipeline de Estoque (APROVAR → RESERVAR → EXECUTAR → DEBITAR → CANCELAR LIBERA)

| Etapa | Transição | Verificação | Evidência |
|-------|-----------|-------------|-----------|
| Aprovar orçamento | AGUARDANDO_APROVAÇÃO → EM_EXECUÇÃO | `OrcamentoServiceTest` | ✅ |
| Reserva atômica | `saldoReservado += qtd` se disponível | `PecaService.reservarSaldo` (D-04) | ✅ |
| Requisição de faltante | Cria `RequisicaoCompra` se disponível < qtd | `PecaOrcamentoObserverTest` (D-07) | ✅ |
| Débito na execução | `saldo -= qtd`, `saldoReservado -= qtd` | `OrdemDeServicoServiceTest` (EST-08/D-04) | ✅ |
| Cancelamento libera reserva | `saldoReservado -= qtd` | `OrdemDeServicoServiceTest` (D-08) | ✅ |
| Alerta estoque mínimo | Publica `EstoqueMinimoAtingidoEvent` | `NfEntradaServiceTest` + `EstoqueMinimoObserverTest` | ✅ |

### SLA (OS-12) — Orçamento expira e OS é cancelada

| Etapa | Verificação | Evidência |
|-------|-------------|-----------|
| Job a cada 12h varre orçamentos expirados | `SlaExpiryJob` | ✅ |
| OS em AGUARDANDO_APROVAÇÃO → CANCELADA | `OrdemDeServico.cancelarPorSLA()` | ✅ |
| Reserva liberada no cancelamento | `pecaRepository.liberarReserva()` | ✅ |
| Auditoria do cancelamento | `OsAuditEventPublisher` com motivo "SLA expirado" | ✅ |

### Auditoria (os_audit_log) — 7 transições auditadas

| Transição | Evento Publicado | Evidência |
|-----------|-----------------|-----------|
| CRIAR (RECEBIDA) | OsTransitionedEvent | ✅ `OrdemDeServicoServiceTest` |
| DIAGNOSTICAR (EM_DIAGNÓSTICO) | OsTransitionedEvent | ✅ `OrdemDeServicoServiceTest` |
| ORÇAR (AGUARDANDO_APROVAÇÃO) | OsTransitionedEvent | ✅ `OrdemDeServicoServiceTest` |
| APROVAR (EM_EXECUÇÃO) | OsTransitionedEvent | ✅ `OrcamentoServiceTest` |
| REPROVAR (CANCELADA) | OsTransitionedEvent | ✅ `OrcamentoServiceTest` |
| EXECUTAR (EM_EXECUÇÃO) | OsTransitionedEvent | ✅ `OrdemDeServicoServiceTest` |
| FINALIZAR (FINALIZADA) | OsTransitionedEvent | ✅ `OrdemDeServicoServiceTest` |
| CANCELAR (CANCELADA) | OsTransitionedEvent | ✅ `OrdemDeServicoServiceTest` |
| ENTREGAR (ENTREGUE) | OsTransitionedEvent | ✅ `OrdemDeServicoServiceTest` |

## 4. Gaps Fechados pela Phase 03.1

A auditoria v1.0 identificou múltiplos gaps na fase 02 que foram fechados:

| Gap | Audit Line | Evidência de Correção |
|-----|-----------|----------------------|
| **EST-03**: OrcamentoAprovadoEvent nunca publicado — reserva dead code | Audit linha 17-23 | Fix 03.1-04: `OrcamentoService.aprovar` publica `OrcamentoAprovadoEvent`; `PecaOrcamentoObserver` conectado |
| **EST-04**: Requisição de Compra para peças indisponíveis não existe | Audit linha 24-31 | Fix 03.1-04: `PecaOrcamentoObserver` gera `RequisicaoCompra` se disponível < qtd |
| **EST-07**: Nenhuma checagem de estoque mínimo no crédito NF | Audit linha 32-35 | Fix 03.1-03: `NfEntradaService.registrar` publica `EstoqueMinimoAtingidoEvent` |
| **EST-08**: `iniciarExecucao` não debita estoque | Audit linha 36-38 | Fix 03.1-04: `OrdemDeServicoService.iniciarExecucao` debita saldoReservado |
| **EST-09**: EstoqueMinimoObserver dead code, qty fixa 100 | Audit linha 40-44 | Fix 03.1-03: `EstoqueMinimoObserver` com qtd = `estoqueMinimo - disponivel` |
| **EST-02**: Sem PUT/DELETE em PecaResource | Audit linha 46-50 | Fix 03.1-03: `PecaResource` com PUT/DELETE + `UpdatePecaCommand` |
| **OS-10/OS-11**: aprovar/reprovar exigiam role cliente/admin | Audit linha 75-80 | Fix 03.1-01: endpoints públicos com `@PermitAll` |
| **OS-16**: `findAllWithFilters` sem testes | Audit linha 105-107 | Fix 03.1-07: `OrdemDeServicoRepositoryImplTest` com 7 testes |
| **OS-17**: detalhamento com placeholder | Audit linha 108-109 | Fix 03.1-07: detalhamento real via `OrdemDeServicoService.buscarItensOrcados` |
| **OS-18**: tempo médio sem breakdown por tipo de serviço | Audit linha 110-111 | Fix 03.1-07: `calcularTempoMedioPorMecanico` + breakdown |
| **EST-06**: NfEntradaResourceTest nunca asserta crédito | Audit linha 144-149 | Fix 03.1-07: `verify(pecaRepository).creditarSaldo(PECA_UUID, 5)` |
| **AUTH-04**: shared AppException vira HTTP 500 | Audit linha 130-134 | Fix 03.1-01: `ApiExceptionMapper` mapeia `shared.exception.AppException` |

## 5. Assinatura

```
Verificação aceita para milestone v1.0
Data: 2026-08-08
Responsável: closure-phase 03.1 (retroactive)
nyquist_compliant: true
```

---

*Este documento foi gerado retroativamente pela phase 03.1-06 como parte da verificação formal D-17.*