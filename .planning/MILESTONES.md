# Milestones

## v1.0 v1.0 (Shipped: 2026-08-08)

**Phases completed:** 1 phases, 7 plans, 16 tasks

**Key accomplishments:**

- 3 endpoints abertos para acesso anônimo (@PermitAll), ClienteService.updateCliente agora aplica campos, e ApiExceptionMapper retorna 409 para shared AppException (antes 500)
- Reserva atômica de estoque (coluna saldo_reservado, V34) com operações UPDATE guardadas, estoque mínimo por saldo disponível, e pecaId opcional nos itens de orçamento com serialização retrocompatível
- NF de entrada dispara evento de estoque minimo com quantidade de reposicao calculada (D-05); admin edita e exclui pecas via PUT/DELETE com saldo preservado
- 1. [Rule 1 - Bug] HQL query in ProcessedEventPanacheRepository used column name instead of entity field name
- Todas as transições de OS auditadas via OsAuditEventPublisher; OSEntregueListener corrigido para observar evento real; SlaExpiryJob cancela OS por SLA com liberação de reserva
- Verificação formal retroativa das 3 fases com evidência real de testes (510 tests, 0 failures), nyquist_compliant: true em todas as VALIDATION.md, e REQUIREMENTS.md com 37/37 requisitos v1 Complete.
- Completa os 5 gaps residuais da auditoria v1.0: findAllWithFilters tests + tempo medio breakdown por mecânico, detalhamento real (remove placeholder), assert de crédito de saldo em NF, e remoção do dead code EntregaService.

---

## v1.0 v1.0 (Shipped: 2026-08-08)

**Phases completed:** 1 phases, 7 plans, 16 tasks

**Key accomplishments:**

- 3 endpoints abertos para acesso anônimo (@PermitAll), ClienteService.updateCliente agora aplica campos, e ApiExceptionMapper retorna 409 para shared AppException (antes 500)
- Reserva atômica de estoque (coluna saldo_reservado, V34) com operações UPDATE guardadas, estoque mínimo por saldo disponível, e pecaId opcional nos itens de orçamento com serialização retrocompatível
- NF de entrada dispara evento de estoque minimo com quantidade de reposicao calculada (D-05); admin edita e exclui pecas via PUT/DELETE com saldo preservado
- 1. [Rule 1 - Bug] HQL query in ProcessedEventPanacheRepository used column name instead of entity field name
- Todas as transições de OS auditadas via OsAuditEventPublisher; OSEntregueListener corrigido para observar evento real; SlaExpiryJob cancela OS por SLA com liberação de reserva
- Verificação formal retroativa das 3 fases com evidência real de testes (510 tests, 0 failures), nyquist_compliant: true em todas as VALIDATION.md, e REQUIREMENTS.md com 37/37 requisitos v1 Complete.
- Completa os 5 gaps residuais da auditoria v1.0: findAllWithFilters tests + tempo medio breakdown por mecânico, detalhamento real (remove placeholder), assert de crédito de saldo em NF, e remoção do dead code EntregaService.

---

## v1.0 v1.0 (Shipped: 2026-08-08)

**Phases completed:** 1 phases, 7 plans, 16 tasks

**Key accomplishments:**

- 3 endpoints abertos para acesso anônimo (@PermitAll), ClienteService.updateCliente agora aplica campos, e ApiExceptionMapper retorna 409 para shared AppException (antes 500)
- Reserva atômica de estoque (coluna saldo_reservado, V34) com operações UPDATE guardadas, estoque mínimo por saldo disponível, e pecaId opcional nos itens de orçamento com serialização retrocompatível
- NF de entrada dispara evento de estoque minimo com quantidade de reposicao calculada (D-05); admin edita e exclui pecas via PUT/DELETE com saldo preservado
- 1. [Rule 1 - Bug] HQL query in ProcessedEventPanacheRepository used column name instead of entity field name
- Todas as transições de OS auditadas via OsAuditEventPublisher; OSEntregueListener corrigido para observar evento real; SlaExpiryJob cancela OS por SLA com liberação de reserva
- Verificação formal retroativa das 3 fases com evidência real de testes (510 tests, 0 failures), nyquist_compliant: true em todas as VALIDATION.md, e REQUIREMENTS.md com 37/37 requisitos v1 Complete.
- Completa os 5 gaps residuais da auditoria v1.0: findAllWithFilters tests + tempo medio breakdown por mecânico, detalhamento real (remove placeholder), assert de crédito de saldo em NF, e remoção do dead code EntregaService.

---
