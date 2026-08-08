---
phase: 03-pagamento-delivery
plan: 3
subsystem: payments
tags: [quarkus, cd-events, idempotency, mock-payment, flyway, state-machine, rest-assured]

# Dependency graph
requires:
  - phase: 02-os-continuation-estoque
    provides: OS finalization (OSFinalizadaEvent), Orcamento aggregate, estoque integrado, os_audit_log
provides:
  - Payment modeled as fields on OrdemDeServico (StatusPagamento state machine: NAO_COBRADO → AGUARDANDO_PAGAMENTO → CONFIRMADO/CANCELADO)
  - Automatic charge emission on OS finalization (CobrancaEmitidaEvent)
  - MockPaymentService with 2s delay simulation and processed_events idempotency guard (V30)
  - Delivery flow guarded by payment confirmation (PATCH /entregar, StatusEntrega: NAO_LIBERADA → LIBERADA_PARA_ENTREGA → ENTREGUE)
  - PATCH /confirmar-pagamento + PATCH /entregar REST endpoints on OrdemDeServicoResource
  - Payment/delivery info surfaced in GET /os/{id} and /detalhamento (D-22)
  - CONTRIBUTING.md with setup, test commands, gotchas G1-G13, payment flow diagrams
affects: [maintenance, ops, future billing integrations]

# Tech tracking
tech-stack:
  added: [none (no new dependencies — CDI events + H2/Postgres only)]
  patterns:
    - "Payment as OS fields, not separate aggregate (D-01)"
    - "State machines via transition matrix Map<Status, Set<Status>> + podeTransicionarPara()"
    - "Idempotency via processed_events table checked before processing (D-15)"
    - "Two-phase validation: service business rules + domain invariants"
    - "Mock external service as ApplicationScoped service injectable in tests"

key-files:
  created:
    - mekano-domain/src/main/java/com/fiap/mekano/domain/event/CobrancaEmitidaEvent.java
    - mekano-domain/src/main/java/com/fiap/mekano/domain/event/PagamentoConfirmadoEvent.java
    - mekano-domain/src/main/java/com/fiap/mekano/domain/event/EntregaConfirmadaEvent.java
    - mekano-domain/src/main/java/com/fiap/mekano/domain/event/OSEntregueEvent.java
    - mekano-domain/src/main/java/com/fiap/mekano/domain/os/StatusPagamento.java
    - mekano-domain/src/main/java/com/fiap/mekano/domain/os/StatusEntrega.java
    - mekano-application/src/main/java/com/fiap/mekano/application/service/MockPaymentService.java
    - mekano-application/src/main/java/com/fiap/mekano/application/service/EntregaService.java
    - mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/entity/ProcessedEventEntity.java
    - mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/ProcessedEventRepositoryImpl.java
    - mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/listener/PagamentoConfirmadoListener.java
    - mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/listener/OSEntregueListener.java
    - mekano-infrastructure/src/main/resources/db/migration/V27__add_payment_fields_to_ordens_de_servico.sql
    - mekano-infrastructure/src/main/resources/db/migration/V29__add_pagamento_entrega_to_os.sql
    - mekano-infrastructure/src/main/resources/db/migration/V30__create_processed_events_table.sql
    - mekano-infrastructure/src/main/resources/db/migration/V31__fix_ordens_de_servico_pagamento_entrega_columns.sql
    - mekano-infrastructure/src/main/resources/db/migration/V33__fix_ordens_de_servico_status_pagamento_entrega_column_size.sql
    - mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/PagamentoResponse.java
    - CONTRIBUTING.md
  modified:
    - mekano-domain/src/main/java/com/fiap/mekano/domain/model/OrdemDeServico.java (payment/entrega fields + transitions)
    - mekano-rest/src/main/java/com/fiap/mekano/rest/api/OrdemDeServicoResource.java (confirmar-pagamento, entregar)

key-decisions:
  - "Payment modeled as fields on OS, not separate entity (D-01) — avoids split aggregate complexity"
  - "Idempotency via processed_events table — duplicate calls have no side effect (D-15)"
  - "Mock bank as in-app service with simulated delay, no external gateway (D-25)"
  - "Payment/delivery data returned inside GET /os/{uuid} — no dedicated payment endpoint (D-22)"
  - "Delivery guard: payment CONFIRMADO (service) + OS FINALIZADA (domain) (D-08/D-09)"
  - "Migrations renumbered V27→V29, V28→V30 to avoid conflict with develop branch (G12)"
  - "Add AGUARDANDO_EXECUCAO state between approval and execution"

patterns-established:
  - "Idempotency pattern: check processed_events before processing, skip silently if exists"
  - "Mock external integration: ApplicationScoped service implementing port, swappable in tests"
  - "Migration renumbering discipline when parallel branches collide (G12)"

requirements-completed: [OS-12, PAG-01, PAG-02, PAG-03, DOC-03]

# Metrics
duration: multiday
completed: 2026-06-30
---

# Phase 3: Pagamento & Delivery Summary

**Cobrança automática ao finalizar execução, confirmação de pagamento com idempotência via processed_events e entrega do veículo bloqueada até pagamento confirmado — ciclo de vida completo da OS (RECEBIDA → ENTREGUE) com CONTRIBUTING.md documentando setup, padrões e gotchas**

## Performance

- **Duration:** 2-3 semanas (paralelo: 3 devs OS finalization + 2 devs Pagamento)
- **Completed:** 2026-06-30
- **Tasks:** 20 (plan) / 7 issues GitHub (#32-#38)
- **Issues closed:** 7/7 + #58 (logout infinito, fix pós-entrega)

## Accomplishments

- Cobrança emitida automaticamente ao finalizar execução (`OSFinalizadaEvent` → `CobrancaEmitidaEvent`, `valorCobrado` vindo do Orcamento)
- `PATCH /api/v1/os/{id}/confirmar-pagamento` via `MockPaymentService` (delay simulado 2s, gera `transacaoId`) com guarda de idempotência `processed_events` (V30)
- `PATCH /api/v1/os/{id}/entregar` registra entrega; 422 se pagamento pendente ou OS não finalizada
- State machines `StatusPagamento` e `StatusEntrega` com matriz de transição e eventos `PagamentoConfirmadoEvent`/`EntregaConfirmadaEvent`/`OSEntregueEvent`
- Audit de transições FINALIZADA → ENTREGUE via `os_audit_log` (listeners `PagamentoConfirmadoListener`, `OSEntregueListener`)
- Cobertura de testes em todas as camadas: unit (domain `OrdemDeServicoCicloCobrancaPagamentoEntregaTest`), Mockito (application `EntregaServiceTest`), integração (infra `PagamentoConfirmadoListenerTest`), E2E (rest `PagamentoResourceTest` + pagamento/entrega E2E)
- `CONTRIBUTING.md` criado (DOC-03): setup, comandos de teste por camada, gotchas G1-G13, diagramas Mermaid do fluxo de pagamento/entrega
- Fix pós-entrega: #58 logout infinito (refresh token invalidado reutilizável), estado `AGUARDANDO_EXECUCAO` entre aprovação e execução

## Task Commits

1. **Eventos de domínio + modelo pagamento/entrega na OS (#36)** — `05ae8d3` (feat)
2. **Listeners de auditoria pagamento/entrega (#35)** — `2839f7e` (feat)
3. **CobrancaEmitidaListener + MockPaymentService + EntregaService (#34)** — `4234bee` (feat)
4. **Infraestrutura pagamento/entrega + idempotência (#33)** — `e00e865` (feat, 1/2)
5. **Testes integração pagamento/entrega + idempotência (#33)** — `980ce67` (test, 2/2)
6. **valorCobrado a partir do Orcamento na emissão** — `b24e63c` (fix)
7. **EntregaConfirmadaEvent na entrega do veículo** — `7ab0833` (fix)
8. **Persistência dos campos pagamento/entrega no save da OS** — `e302a25` (fix)
9. **Migrations V27→V29/V28→V30 renumber (conflito develop)** — `baa76e2` + `ba76704` (fix)
10. **Endpoints REST confirmar-pagamento + DTOs expandidos (#37)** — `b289d97` (feat)
11. **E2E pagamento/entrega (#38)** — `75319bd` (test)
12. **Remove statusPagamento/statusEntrega duplicados** — `6462359` (fix)
13. **Estados pós-entrega: AGUARDANDO_EXECUCAO, remove finalizar deprecado, fix mecânico/swagger** — `d3ad56e`, `ac14755`, `052536f`, `2fd4def` (feat/fix)
14. **CONTRIBUTING.md (DOC-03)** — sessão de resume (docs)

## Files Created/Modified

- `mekano-domain/.../event/{CobrancaEmitidaEvent, PagamentoConfirmadoEvent, EntregaConfirmadaEvent, OSEntregueEvent}.java` — eventos imutáveis do ciclo pagamento/entrega
- `mekano-domain/.../os/{StatusPagamento, StatusEntrega}.java` — enums com matriz de transição
- `mekano-domain/.../model/OrdemDeServico.java` — campos pagamento/entrega + transições
- `mekano-application/.../MockPaymentService.java` — banco simulado + idempotência
- `mekano-application/.../EntregaService.java` — guarda de entrega
- `mekano-infrastructure/.../{ProcessedEventEntity, ProcessedEventRepositoryImpl}.java` — idempotência real (substitui stub)
- `mekano-infrastructure/.../listener/{PagamentoConfirmadoListener, OSEntregueListener}.java` — auditoria
- `mekano-infrastructure/.../migration/V{27,29,30,31,33}__*.sql` — campos pagamento/entrega + processed_events
- `mekano-rest/.../OrdemDeServicoResource.java` — `PATCH /{id}/confirmar-pagamento`, `PATCH /{id}/entregar`
- `CONTRIBUTING.md` — guia de contribuição completo

## Decisions Made

- D-01: pagamento como campos da OS (não entidade separada) — evita aggregate gigante
- D-02/D-08/D-09: guardas em duas camadas (service + domain)
- D-15: idempotência via tabela `processed_events` — chamada duplicada vira no-op
- D-22: dados de pagamento dentro de `GET /os/{uuid}`, sem endpoint dedicado
- D-25: mock bancário in-app (`MockPaymentService`), delay 2s, `transacaoId` gerado
- Renumber de migrations (G12) para destravar conflito com develop

## Deviations from Plan

### Auto-fixed Issues

**1. [G12 - Migration conflict] Migrations V27/V28 renumeradas para V29/V30**
- **Found during:** Merge com branch develop
- **Issue:** Versões Flyway V27/V28 já existiam em develop (conflito de renumeração paralela)
- **Fix:** Renumeração V27→V29, V28→V30 + ajuste de colunas em V31/V33
- **Files modified:** mekano-infrastructure/src/main/resources/db/migration/
- **Verification:** Suíte completa verde com H2

**2. [Naming drift] Evento `OSEntregueEvent` vs `EntregaConfirmadaEvent`**
- **Found during:** Implementação
- **Issue:** Plano previa apenas `OSEntregueEvent`; implementação criou ambos (um para o listener de auditoria, outro para o fluxo de domínio)
- **Fix:** Mantidos ambos — `EntregaConfirmadaEvent` publicado na entrega (`7ab0833`), `OSEntregueEvent` no domain model
- **Files modified:** mekano-domain/.../event/
- **Verification:** Testes E2E e integração passando

**3. [Duplicate fields] `statusPagamento`/`statusEntrega` declarados duplicados na entidade**
- **Found during:** Compilação/testes
- **Issue:** Declarações duplicadas causavam erro de persistência
- **Fix:** Removida duplicata (`6462359`)
- **Verification:** Testes integração passando

---

**Total deviations:** 3 auto-fixed (1 build-blocking, 2 naming/cleanup)
**Impact on plan:** Todas necessárias para correção/build. Sem escopo creep.

## Issues Encountered

- Conflito de renumeração de migrations entre branches paralelas (resolvido com G12)
- Campos de pagamento não persistidos no save da OS (fix `e302a25`)
- Fix pós-entrega: logout infinito reutilizando refresh token invalidado (#58 — CLOSED)

## User Setup Required

None — nenhuma configuração externa: o banco simulado roda in-app (D-25).

## Next Phase Readiness

- ✅ **Milestone v1.0 completo**: 3/3 fases, 28/28 issues, 37/37 requirements mapeados
- Estado AGUARDANDO_EXECUCAO adicionado pós-entrega; branch main/develop alinhadas
- Pendente (fora de fase): CI/CD completa (ci.yml já existe), review de code drift (`StatusPagamento` duplicado em `domain/model/` e `domain/os/`, `ProcessedEventsRepositoryStub` ainda presente)

---
*Phase: 03-pagamento-delivery*
*Completed: 2026-06-30*
