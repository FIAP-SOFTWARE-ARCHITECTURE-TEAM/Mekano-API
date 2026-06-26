# Phase 3: Pagamento & Delivery — Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in `03-CONTEXT.md` — this log preserves the alternatives considered.

**Date:** 2026-06-23
**Phase:** 3-Pagamento & Delivery
**Areas discussed:** OS-12 SLA, Modelo OrdemDePagamento, Fluxo Entrega, Timing Cobrança, Eventos CDI, Mock Design, Soft Delete, DOC-03, Flyway, Módulo Mock

---

## OS-12: SLA pertence a qual fase?

| Option | Selected |
|--------|----------|
| Fase 2 (já decidido) | ✓ |
| Fase 3 (aqui) | |

**User's choice:** Fase 2 — D-18..20 já capturados, implementação junto com orçamento.

---

## Modelo da Ordem de Pagamento

| Option | Selected |
|--------|----------|
| Entidade separada (OrdemDePagamento) | |
| Campos na própria OS | ✓ |
| Status PENDENTE/CONFIRMADO | ✓ |
| Status PENDENTE/PROCESSANDO/CONFIRMADO/FALHOU | |
| Valor orçamento aprovado | ✓ |
| Recalcula na finalização | |
| Falha: admin retenta manualmente | |
| Falha: retry automático | |
| Falha: exception + tente mais tarde | ✓ |

**User's choice:** Campos na OS, status PENDENTE/CONFIRMADO, valor do orçamento, falha retorna AppException.

---

## Fluxo de Entrega

| Option | Selected |
|--------|----------|
| Só admin | |
| Admin + atendente | ✓ |
| PATCH /os/{uuid}/entregar | ✓ |
| PUT /os/{uuid}/status genérico | |

**User's choice:** Admin + atendente, endpoint dedicado PATCH.

---

## Timing da Cobrança

| Option | Selected |
|--------|----------|
| Automática ao finalizar execução | ✓ |
| Admin decide quando emitir | |
| Não reemite | ✓ |
| Reemite se PENDENTE | |

**User's choice:** Automática na finalização, sem reemissão.

---

## Eventos CDI

| Option | Selected |
|--------|----------|
| Um evento único | |
| Dois eventos separados | ✓ |
| Payload completo (osUuid + pagamentoUuid + valor + data) | ✓ |
| Payload mínimo (osUuid + valor) | |
| Service publica e consome no mesmo fluxo | ✓ |
| Eventos soltos sem observer | |

**User's choice:** Dois eventos, payload completo, fluxo síncrono.

---

## Mock de Pagamento

| Option | Selected |
|--------|----------|
| Endpoint REST em mekano-rest | ✓ |
| Serviço injetado em infrastructure | |
| POST + delay fixo 2s | ✓ |
| POST + delay variável + header falha | |

**User's choice:** Endpoint REST em mekano-rest, delay fixo 2s.

---

## Idempotência

| Option | Selected |
|--------|----------|
| processed_events table | ✓ |
| Idempotency key no header | |

**User's choice:** processed_events (Pitfall 4 do roadmap).

---

## Auditoria

| Option | Selected |
|--------|----------|
| Reusa os_audit_log da Fase 2 | ✓ |
| Tabela separada pagamento_audit_log | |

**User's choice:** Reusa os_audit_log.

---

## Soft Delete de OS com Pagamento Pendente

| Option | Selected |
|--------|----------|
| Bloqueia (HTTP 409) | |
| Permite | ✓ |

**User's choice:** Permite soft delete.

---

## DOC-03 (CONTRIBUTING.md)

| Option | Selected |
|--------|----------|
| Setup + padrões | |
| Setup + padrões + diagramas | ✓ |
| Diagramas linkados do DOC-01 | |
| Diagramas incluídos no CONTRIBUTING.md | ✓ |

**User's choice:** Setup + padrões + diagramas embutidos.

---

## Flyway Numeração

| Option | Selected |
|--------|----------|
| V18+ | ✓ |

**User's choice:** V18 para ALTER TABLE ordens_de_servico.

---

## Módulo do Mock

| Option | Selected |
|--------|----------|
| mekano-rest (endpoint REST) | ✓ |
| mekano-infrastructure (serviço injetado) | |

**User's choice:** MockPaymentResource em mekano-rest.

---

## Métricas / Endpoints

| Option | Selected |
|--------|----------|
| Dados na própria OS | ✓ |
| Endpoint dedicado GET /pagamentos | |

**User's choice:** Dados de pagamento no GET /os/{uuid}.

---

## Deferred Ideas

- **Estorno de pagamento:** Fora do escopo acadêmico.
- **GET /pagamentos:** Se houver necessidade futura de dashboard financeiro.
- **Múltiplos métodos de pagamento:** V2 (PAG-04, PAG-05).
