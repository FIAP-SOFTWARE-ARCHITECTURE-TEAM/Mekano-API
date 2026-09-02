# Phase 5: WhatsApp Integration — Discussion Log

**Date:** 2026-08-08
**Status:** Completed

## Areas Discussed

### WhatsApp Provider
- **Decision:** Evolution API self-hosted (não Cloud API, não Twilio, não mock)
- **Deploy:** Docker local no mesmo docker-compose
- **Reference:** Projeto Groom em `C:\Users\victo\Desktop\Empresas\Paperclip-Organization\Groom\API`

### Trigger Points & Events
- **WPP-01 (orçamento):** Notificar na criação do orçamento (após finalizarDiagnostico), com links para aprovar/reprovar
- **WPP-02 (retirada):** Notificar após pagamento confirmado (PagamentoConfirmadoEvent)
- **Eventos existentes a observar:** Criar observer para orçamento gerado (novo evento ou hook em finalizarDiagnostico) + PagamentoConfirmadoEvent

### Token Management
- **Decision:** Token fixo da Evolution API (instância conectada via QR Code, sem refresh automático)
- **Config:** Environment variable

### Message Templates
- **Orçamento:** Texto + links de aprovação/recusa (apontando para endpoints @PermitAll existentes)
- **Retirada:** Texto informando que veículo está pronto + link de status

### External Status Scope (API-05)
- **Decision:** WhatsApp interativo — cliente pode aprovar/recusar via resposta no WhatsApp. Sistema expõe webhook que Evolution API chama. WhatsApp é canal adicional, não substituto da API pública.

## Not Discussed (but in scope)
- Formatação exata das mensagens (a cargo do executor)
- Webhook security token (a cargo do executor)
- Estrutura docker-compose com Evolution + MongoDB (a cargo do executor)

## Deferred Ideas
- Lembrete automático de orçamento pendente
- Múltiplos contatos
- Relatório de entregas via WhatsApp

---

*Discussion completed: 2026-08-08*