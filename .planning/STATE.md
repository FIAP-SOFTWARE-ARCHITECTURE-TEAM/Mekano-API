---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: "gaps: estoque pipeline, pagamento/audit, requisitos publicos e SLA"
status: Ready to execute
last_updated: "2026-08-08T03:26:46.107Z"
progress:
  total_phases: 1
  completed_phases: 0
  total_plans: 7
  completed_plans: 6
  percent: 0
---

# STATE: Mekano

**Init:** 2026-06-20
**Core Value:** Gerenciar o ciclo de vida completo das Ordens de Serviço — do recebimento do veículo à entrega — com rastreabilidade, controle de estoque e cobrança integrados.

---

## Project Reference

| Key | Value |
|-----|-------|
| Stack | Java 17, Quarkus 3.36.0, PostgreSQL 16, Maven multi-module |
| Architecture | Clean Architecture (domain → application → infrastructure → rest) |
| Bounded Contexts | User/Auth, OS, Estoque, Pagamento (todos implementados) |
| Team | 5 developers |
| Timeline | 10 days (Dias 1-10 decorridos) |
| Granularity | standard |
| Mode | mvp |

## Current Position

Phase: 03.1 (close-v1-0-gaps-estoque-pipeline-pagamento-audit-requisitos-) — EXECUTING
Plan: 7 of 7
| Attribute | Value |
|-----------|-------|
| **Phase** | 03.1 — Close v1.0 gaps (INSERTED após Fase 3) |
| **Status** | ⚠️ Auditoria v1.0 encontrou gaps — fase de closure inserida |
| **Progress** | ████████████████████ 100% (v1.0) → gaps em fechamento |
| **Milestone** | v1 |

## Performance Metrics

| Metric | Current | Target | Notes |
|--------|---------|--------|-------|
| Issues fechadas | 28/28 | 28 | ✓ Todas as fases completas (+#58 fix) |
| Requirements mapped | 37/37 | 37 | ✓ Full coverage |
| Phases defined | 3 | — | Auth & OS → OS Cont. & Estoque → Pagamento & Delivery |
| Fases concluídas | 3/3 | 3 | ✓ Milestone entregue |
| Phase 03.1 P01 | 6.5 | 3 tasks | 8 files |
| Phase 03.1-close-v1-0-gaps-estoque-pipeline-pagamento-audit-requisitos- P02 | 12min | 2 tasks | 17 files |
| Phase 03.1 P03 | 10 | 2 tasks | 10 files |
| Phase 03.1-07 | 8min | 2 tasks (T2+T3) | 11 files |

## Accumulated Context

### Key Decisions (from research/planning)

| Decision | Rationale | Status |
|----------|-----------|--------|
| Vertical slice strategy | Avoids integration chaos with 5 devs in parallel | Executado |
| OS state machine with transition matrix | Prevents illegal state transitions | Implementado |
| CDI events for inter-context comm | Decoupled contexts without Kafka overengineering | Implementado |
| Orcamento as separate aggregate | Prevents mega-aggregate (Pitfall 1) | Implementado |
| Atomic stock reservation (UPDATE ... WHERE saldo >= qtd) | Prevents race condition (Pitfall 3) | Implementado |
| Payment idempotency via processed_events table | Prevents double-processing (Pitfall 4) | Implementado (V30, ProcessedEventRepositoryImpl) |
| Auth construído como task da Phase 1 | Não revisar codebase existente, tratar como task | Implementado |
| JWT Ed25519 | Ed25519/EdDSA por segurança e performance | Implementado |
| Refresh rotation com PESSIMISTIC_WRITE | Previne ataque de reuse | Implementado |
| Pagamento como campos da OS (D-01) | Evita aggregate gigante | Implementado |
| JaCoCo 80% LINE + OWASP CVSS≥7 | Gates de qualidade | Configurado (ci.yml) |

### Pending Decisions

- [x] D-10: Duplicata de pagamento = 200 no-op
- [x] D-09: ProcessedEventRepositoryImpl ativo como bean real
- [x] D-08: Cancelamento libera reserva (n�o credita saldo)
- [x] D-07: Requisi��o de compra gerada no observer quando reserva falha
- [x] D-06: Segunda chamada de pagamento retorna 200 (no-op idempotente)
- [x] D-04: Reserva na aprova��o, d�bito na execu��o
- [x] **Roles**: admin, atendente, mecanico, almoxarife, financeiro, cliente — confirmadas e implementadas
- [x] **JWT strategy**: Ed25519 existente reutilizado
- [x] **SLA policy values**: Implementado na Fase 3
- [x] **Simulated bank service**: MockPaymentService com delay 2s + idempotência (#34, #37)

### Open Todos

- [x] Criar AGENTS.md para aplicar engenharia de contexto no projeto
- [x] PLAN.md: Phase 1 — Auth & OS Foundation (6 planos executados)
- [x] PLAN.md: Phase 2 — OS Continuation & Estoque (8 planos executados)
- [x] Criar tasks individuais para GitHub Issues
- [x] Fechar Issues #33 (idempotência real), #37 (REST pagamento/entrega)
- [x] Atualizar README.md com arquitetura e módulos
- [x] Criar CI/CD pipeline (.github/workflows/ci.yml)
- [x] Alinhar branches main/develop
- [x] Criar CONTRIBUTING.md (DOC-03) — fechado na sessão de resume

### Blockers

- Nenhum — milestone completo. Tech debt conhecido: `StatusPagamento` duplicado (`domain/model/` e `domain/os/`), `ProcessedEventsRepositoryStub` ainda presente (inativo), mappers vazios (Peca/RequisicaoCompra/NfEntrada), bug NfEntradaRepositoryImpl `pecaId`/`requisicaoCompraId`.

## Session Continuity

**Last session:** 2026-08-08T00:53:00Z
**Next action:** All plans executed — ready for re-audit and milestone completion

### Threads

| Thread | Phase | Status | Owner | Context |
|--------|-------|--------|-------|---------|
| Roadmap restructured | 1-3 | ✅ Done | GSD | All 37 v1 mapped, 3 phases, vertical slices |
| Phase 1 execution | 1 | ✅ Done | Equipe | 12 issues fechadas, 6 planos |
| Phase 2 execution | 2 | ✅ Done | Equipe | 9 issues fechadas, 8 planos |
| Phase 3 execution | 3 | ✅ Done | Equipe | 7 issues fechadas (#32-#38) + #58 |
| Milestone v1.0 close-out | 3 | ✅ Done | GSD | SUMMARY + ROADMAP/STATE atualizados (2026-08-07) |

### Next Phase Brief

**Fase 03.1 — Close v1.0 gaps (INSERTED, urgente):** a auditoria (`v1.0-MILESTONE-AUDIT.md`) encontrou 7 requisitos unsatisfied e 8 partial. Gaps:

1. **Estoque pipeline (EST-03/04/07/08/09):** `OrcamentoAprovadoEvent` nunca publicado; reserva atômica, requisição de compra e débito na execução são dead code
2. **Pagamento + audit (PAG-02):** `ProcessedEventsRepositoryStub` ativo — idempotência fake; `OsAuditEventPublisher` nunca invocado
3. **Requisitos públicos/SLA/bugs (AUTH-03, OS-15, OS-12, OS-02, AUTH-04):** status público exige auth; SLA não cancela OS; `updateCliente` no-op; shared AppException → 500
4. **Verificação formal:** VERIFICATION.md 0/3 fases; nyquist 2 partial + 1 missing; traceability 0/37

### Roadmap Evolution

- Phase 03.1 (INSERTED, URGENT) — after Phase 3: "Close v1.0 gaps: estoque pipeline, pagamento/audit, requisitos publicos e SLA"

**Caminho:** ~~`/gsd-discuss-phase 03.1`~~ → ~~`/gsd-plan-phase 03.1`~~ (7 planos prontos) → `/gsd-execute-phase 03.1` → re-audit → `/gsd-complete-milestone v1.0`

## Decisions

- [Phase ?]: D-01: @PermitAll no método sobrepõe @RolesAllowed da classe
- [Phase 03.1]: D-02: UUID da OS/orçamento é a chave de acesso para endpoints públicos; risco aceito para MVP
- [Phase 03.1]: D-03: Demais endpoints continuam exigindo autenticação (teste negativo 401)
- [Phase 03.1]: D-15: updateCliente aplica campos via Cliente.reconstitute + save, preservando CPF e createdAt
- [Phase 03.1]: D-16: shared AppException mapeada no ApiExceptionMapper entre domain.AppException e WebApplicationException
- [Phase 03.1]: D-01: @PermitAll no método sobrepõe @RolesAllowed da classe (Jakarta Security)
- [Phase ?]: D-04: Semântica reserva na aprovação, débito no início da execução (fiel à spec EST-03/EST-08)
- [Phase ?]: D-05: Reserva modelada como coluna saldoReservado na peça (V34), estoque mínimo = função do saldo disponível
- [Phase ?]: D-06/D-07 base: pecaId disponível no ItemOrcamento e no itens_json para conectar OrcamentoAprovadoEvent
- [Phase ?]: D-05 confirmado: qtd reposicao = estoqueMinimo - disponivel (plano 03.1-03)
- [Phase ?]: D-11: auditar todas as transições da OS
- [Phase ?]: D-12: ENTREGA_REALIZADA via EntregaConfirmadaEvent
- [Phase ?]: D-13: job SLA a cada 12h, motivo SLA expirado
- [Phase ?]: D-14: cancelamento SLA libera reserva de estoque
