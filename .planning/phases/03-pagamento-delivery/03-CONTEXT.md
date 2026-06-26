# Phase 3: Pagamento & Delivery — Context

**Gathered:** 2026-06-23
**Status:** Ready for planning

<domain>
## Phase Boundary

Cobrança automática ao finalizar execução da OS, pagamento confirmado via banco simulado, entrega do veículo liberada — ciclo completo da OS finalizado. Também inclui guia de contribuição (DOC-03) com diagramas de sequência.

Requisitos: PAG-01, PAG-02, PAG-03, DOC-03 (OS-12 implementado na Fase 2).

**Depende de:** Phase 1 (OS Aggregate Root, state machine, auth), Phase 2 (OS finalization, orçamento aprovado, estoque integrado).

**Time:** Dias 8-10, 3 devs OS + 2 devs Pagamento.
</domain>

<decisions>
## Implementation Decisions

### Modelo de Pagamento (na própria OS)

- **D-01:** Pagamento modelado como **campos na própria tabela `ordens_de_servico`** (não entidade separada). Campos: `status_pagamento` (VARCHAR: PENDENTE | CONFIRMADO), `data_pagamento` (TIMESTAMP), `valor_cobrado` (DECIMAL)
- **D-02:** Status máximo: `PENDENTE` e `CONFIRMADO`. Sem PROCESSANDO, FALHOU ou CANCELADO.
- **D-03:** Valor cobrado copiado do `Orcamento.valorTotal` no momento da emissão — recalculo não é necessário.
- **D-04:** Falha do mock retorna `AppException(503)` com mensagem "Pagamento indisponível no momento, tente novamente mais tarde". Cliente retenta chamando o mesmo endpoint. Sem retry automático.

### SLA do Orçamento (OS-12)

- **D-05:** OS-12 é implementado na **Fase 2** (decisões D-18..20 da Phase 2 context já capturadas: SLA 72h, job a cada 12h, OS cancelada com motivo "SLA expirado"). Fase 3 não implementa SLA.

### Fluxo de Entrega

- **D-06:** Admin + atendente podem registrar entrega (`@RolesAllowed({"admin", "atendente"})`).
- **D-07:** Endpoint dedicado: `PATCH /api/v1/os/{uuid}/entregar`.
- **D-08:** Validação de pagamento CONFIRMADO no **service layer** (não no domain entity). Service verifica `status_pagamento` antes de chamar `os.entregar()`.
- **D-09:** OS transita de `FINALIZADA` para `ENTREGUE`. Guard "pagamento CONFIRMADO" validado no service.

### Timing da Cobrança

- **D-10:** Cobrança emitida **automaticamente** ao finalizar execução (dentro do mesmo fluxo transacional de `finalizarExecucao()`).
- **D-11:** Emissão única. Se já existe cobrança (PENDENTE ou CONFIRMADO), bloqueia com HTTP 409.

### Mock de Pagamento

- **D-12:** Endpoint REST em `mekano-rest`: `POST /api/v1/mock-banco/pagamentos`.
- **D-13:** Payload: `{ "osUuid": "...", "valor": 1500.00 }`. Resposta: `{ "status": "confirmado", "transacaoId": "<uuid>" }`.
- **D-14:** Delay fixo de 2s (simulado via `Thread.sleep` ou `@Timeout`).
- **D-15:** Idempotência via tabela `processed_events` com unique key `(event_type, aggregate_uuid)` — Pitfall 4 do ROADMAP.

### Eventos CDI

- **D-16:** Dois eventos separados (não 1 evento único).
- **D-17:** `CobrancaEmitidaEvent(UUID osUuid, UUID pagamentoUuid, BigDecimal valor, LocalDateTime dataEmissao)` — publicado ao emitir cobrança.
- **D-18:** `PagamentoConfirmadoEvent(UUID osUuid, UUID transacaoId, BigDecimal valor, LocalDateTime dataConfirmacao)` — publicado quando mock confirma.
- **D-19:** Service publica e consome os eventos no mesmo fluxo síncrono (CDI observer no mesmo tx).

### Auditoria

- **D-20:** Reusa `os_audit_log` da Fase 2. Transições da OS (FINALIZADA → ENTREGUE) já são logadas automaticamente pelo mecanismo D-66..69.

### Soft Delete

- **D-21:** Permite soft-delete de OS com status_pagamento PENDENTE — sem bloqueio.

### Métricas / Endpoints

- **D-22:** Dados de pagamento retornados dentro do `GET /api/v1/os/{uuid}`. Sem endpoint dedicado de pagamentos.

### DOC-03 (CONTRIBUTING.md)

- **D-23:** CONTRIBUTING.md inclui: setup (docker-compose + mvnw quarkus:dev), comandos de teste por camada, tabela de gotchas (G1-G10), referência rápida de padrões (VOs, MapStruct, @Transactional no service), diagramas de sequência dos 4 fluxos principais (criar OS, aprovar orçamento, fluxo estoque, fluxo pagamento).

### Flyway

- **D-24:** Phase 1 = V1-V10. Phase 2 = V11-V17. Phase 3 = V18+. V18: `ALTER TABLE ordens_de_servico ADD COLUMN status_pagamento, data_pagamento, valor_cobrado`.

### Módulo do Mock

- **D-25:** `MockPaymentResource` em `mekano-rest` (endpoint REST `@PermitAll`). `MockPaymentService` em `mekano-infrastructure` (lógica do mock, delay, geração de transacaoId).

### the agent's Discretion
- Detalhes de implementação não cobertos seguem os padrões existentes no codebase (BaseEntity, two-class repository, MapStruct CDI, etc.)
- Estrutura de testes segue padrão já estabelecido (JUnit 5 + Mockito + REST Assured + AssertJ)
- Nesting de endpoints / mock-banco respeita o prefixo `/api/v1` configurado

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Roadmap & Requirements
- `.planning/ROADMAP.md` §Phase 3 — Goal, success criteria, requirements, Pitfall 4 (payment idempotency)
- `.planning/REQUIREMENTS.md` — Full requirement specs (PAG-01..03, DOC-03)
- `.planning/PROJECT.md` — Project constraints (10 days, 5 devs, clean architecture)
- `.planning/STATE.md` — Accumulated decisions (vertical slices, processed_events table, Pagamento context)

### Codebase Patterns
- `.planning/codebase/ARCHITECTURE.md` — Module structure, data flow, patterns
- `.planning/codebase/CONVENTIONS.md` — Naming, error handling, MapStruct, VO conventions
- `.planning/codebase/STACK.md` — Technology stack, dependencies, configuration
- `.planning/codebase/INTEGRATIONS.md` — Database, auth, caching, resilience, CDI events

### Phase Contexts (decisions carried forward)
- `.planning/phases/01-auth-os-foundation/01-CONTEXT.md` — D-25 (state machine), D-26 (transition methods)
- `.planning/phases/02-os-continuation-estoque/02-CONTEXT.md` — D-18..20 (SLA), D-58..60 (event pattern), D-66..69 (audit log)

### Domain Docs
- `docs/` — Event Storming documentation, Mermaid diagrams
- `CLAUDE.md` — Project conventions, gotchas (G1-G10), build commands

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `CdiEventPublisher` — Publicação de eventos CDI. Reusar para CobrancaEmitidaEvent e PagamentoConfirmadoEvent.
- `ApiExceptionMapper` — Mapeia AppException para RFC 7807. HTTP 503 para indisponibilidade do mock.
- `os_audit_log` — Log de transições. Captura automática da entrega.
- `BaseEntity` — Mas OS não é nova entidade, é ALTER TABLE (pagamento na OS).
- `@RequestScoped` em resources JAX-RS — MockPaymentResource também segue esse padrão.

### Established Patterns
- Clean Architecture: domain puro → application (@Transactional) → infrastructure → rest
- CDI events para comunicação entre contextos (já estabelecido na Fase 2)
- @Transactional APENAS no service layer
- `@PermitAll` em endpoints públicos (mock de pagamento)
- REST Client para chamar o mock endpoint (nova adição: quarkus-rest-client-reactive)

### Integration Points
- `mekano-infrastructure/src/main/resources/db/migration/` — Nova migration V18 (ALTER TABLE ordens_de_servico)
- `mekano-rest/pom.xml` — Adicionar `quarkus-rest-client-reactive` para chamar mock endpoint
- `.planning/phases/02-os-continuation-estoque/02-CONTEXT.md` — D-58..60 para padrão de eventos
- `mekano-rest/src/main/resources/application.properties` — Config mock timeout

</code_context>

<specifics>
## Specific Ideas

Nenhuma referência específica além dos padrões já estabelecidos no codebase.
</specifics>

<deferred>
## Deferred Ideas

- **Estorno de pagamento:** Fora do escopo acadêmico. Não há necessidade de estorno no MVP.
- **Endpoint dedicado de pagamentos (GET /pagamentos):** Decidiu-se manter dados embutidos na OS. Se houver necessidade futura de dashboard financeiro, criar endpoint separado.
- **Múltiplos métodos de pagamento (PIX, boleto, cartão):** V2 (PAG-04, PAG-05). Mock atual aceita qualquer "pagamento".

None — discussion stayed within phase scope.
</deferred>

---

*Phase: 3-Pagamento & Delivery*
*Context gathered: 2026-06-23*
