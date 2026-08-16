# Phase 5: WhatsApp Integration - Context

**Gathered:** 2026-08-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Integrar notificações WhatsApp via Evolution API (self-hosted) para comunicação com clientes sobre orçamentos e retirada de veículos. Cliente pode interagir via WhatsApp para aprovar/recusar orçamento — o sistema processa a resposta via webhook.

</domain>

<decisions>
## Implementation Decisions

### WhatsApp Provider
- **D-01:** Evolution API (self-hosted, https://evolution-api.com) como provedor — mais flexível que Cloud API, sem necessidade de templates pré-aprovados
- **D-02:** Evolution API roda em container Docker junto com o Mekano (docker-compose), com referência de implementação em `C:\Users\victo\Desktop\Empresas\Paperclip-Organization\Groom\API`
- **D-03:** Token de instância fixo via environment variable (Evolution não tem expiração de 24h como Cloud API)

### Trigger Points & Events
- **D-04:** WPP-01: Notificar quando orçamento é criado (após finalizarDiagnostico, status AGUARDANDO_APROVACAO) — cliente precisa decidir. Usar link para endpoints públicos @PermitAll (`POST /orcamentos/{uuid}/aprovar` e `reprovar`)
- **D-05:** WPP-02: Notificar sobre retirada quando pagamento for confirmado (`PagamentoConfirmadoEvent`) — OS finalizada + paga = veículo pronto
- **D-06:** Cliente pode interagir via WhatsApp para aprovar/recusar orçamento (não apenas notificação unilateral). Sistema expõe webhook para Evolution API chamar quando cliente responde

### Message Templates
- **D-07:** Mensagem de orçamento: texto com resumo do orçamento + link para aprovar + link para recusar (ou interação via WhatsApp pelo webhook)
- **D-08:** Mensagem de retirada: texto informando que veículo está pronto para retirada, com link para consultar status (`GET /os/{uuid}/status`)

### Architecture
- **D-09:** Seguir o padrão existente: domain port (`WhatsAppNotifier`) → infra implementation (`EvolutionApiNotifier`) usando REST Client do Quarkus
- **D-10:** Webhook receiver: novo endpoint REST em mekano-rest (`POST /api/v1/webhooks/whatsapp`) para Evolution API chamar
- **D-11:** Notificações disparam via CDI events (AFTER_SUCCESS) — observer escuta evento existente e chama o notifier

### External Status Scope (API-05)
- **D-12:** Cliente pode aprovar/recusar orçamento via WhatsApp (webhook processa resposta e chama endpoint de aprovação interno). WhatsApp não substitui a API pública — é um canal adicional

### the agent's Discretion
- Formato exato das mensagens (texto, emojis, número do pedido)
- Webhook security (token de validação para garantir que chamadas são da Evolution API)
- Estrutura do container Evolution (MongoDB + Evolution API + Mekano no mesmo docker-compose? Ou Evolution externo?)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Projeto Referência
- `C:\Users\victo\Desktop\Empresas\Paperclip-Organization\Groom\API` — exemplo de implementação Evolution API (estrutura docker-compose, chamadas HTTP)

### Domínio
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/OrcamentoAprovadoEvent.java` — evento existente de aprovação de orçamento
- `mekano-domain/src/main/java/com/fiap/mekano/domain/event/PagamentoConfirmadoEvent.java` — evento existente de pagamento confirmado
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/Cliente.java` — entidade Cliente com campo `telefone`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/OrcamentoResource.java` — endpoints @PermitAll de aprovar/reprovar

### Infraestrutura
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/observer/` — padrão de observers CDI existentes
- `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/EventPublisher.java` — interface de publicação de eventos

### Requisitos
- `.planning/REQUIREMENTS.md` §WPP-01, WPP-02, API-05
- `.planning/ROADMAP.md` §Phase 5

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Patterns
- CDI event observers em `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/observer/` (PecaOrcamentoObserver, EstoqueMinimoObserver, OsAuditLogObserver)
- REST Client via Quarkus (`@RegisterRestClient`) para chamadas HTTP externas
- `docker-compose.yml` existente com 3 serviços (postgres, keygen, mekano) — Evolution API + MongoDB adicionados como novos serviços

### Integration Points
- `OrcamentoAprovadoEvent` — publicado em OrcamentoService.aprovar (já conectado ao pipeline de estoque)
- `PagamentoConfirmadoEvent` — publicado em MockPaymentService.confirmarPagamento (após confirmação)
- `Cliente.telefone` — campo existente, usado como destino do WhatsApp
- Endpoints públicos: `POST /api/v1/orcamentos/{uuid}/aprovar`, `POST /api/v1/orcamentos/{uuid}/reprovar` — URL para links

</code_context>

<specifics>
## Specific Ideas

- Mensagem de orçamento deve incluir: nome do cliente, valor do orçamento, links de ação
- Mensagem de retirada deve incluir: nome do cliente, placa do veículo, endereço da oficina
- Webhook precisa de validação (API key ou token) para evitar chamadas maliciosas
- Evolution API usa MongoDB como storage — adicionar serviço mongo no docker-compose

</specifics>

<deferred>
## Deferred Ideas

- Lembrete automático se cliente não agir no orçamento após X horas — v2.x
- Notificação para múltiplos contatos (cliente + financeiro) — v2.x
- Relatório de entregas via WhatsApp — v2.x

</deferred>

---

*Phase: 5-WhatsApp Integration*
*Context gathered: 2026-08-08*