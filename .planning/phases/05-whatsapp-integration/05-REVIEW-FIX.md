---
phase: 05-whatsapp-integration
fixed_at: 2026-08-18T09:47:00-03:00
review_path: .planning/phases/05-whatsapp-integration/05-REVIEW.md
iteration: 1
findings_in_scope: 6
fixed: 6
skipped: 0
status: all_fixed
---

# Phase 05: Code Review Fix Report — WhatsApp Integration (WPP-01)

**Fixed at:** 2026-08-18T09:47:00-03:00
**Source review:** `.planning/phases/05-whatsapp-integration/05-REVIEW.md`
**Iteration:** 1

**Summary:**
- Findings in scope (Critical + Warning): 6
- Fixed: 6
- Skipped: 0

**Nota (working tree):** as mudanças não commitadas existentes na review (remoção do prefixo "Olá {nome}, " da confirmação SIM/NÃO — port, impl, service e testes) foram preservadas e commitadas como `fc84a95` antes dos fixes, para que cada commit de finding ficasse atômico e o tree final ficasse limpo.

**Validação:** `./mvnw.cmd -B -ntp test -pl mekano-application,mekano-infrastructure,mekano-rest -am` → **BUILD SUCCESS** (domain 4.2s, application 93 testes, infrastructure 74 testes, rest 115 testes).

## Fixed Issues

### CR-01: `formatPhone` misclassifies DDD-55 numbers as already having country code → message sent to wrong recipient (PII leak)

**Files modified:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/whatsapp/EvolutionApiNotifier.java`, `mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/whatsapp/EvolutionApiNotifierTest.java`
**Commit:** `754c1f1`
**Applied fix:** `formatPhone` agora trata "55" como DDI apenas quando `digits.length() > 11` (12/13 dígitos, já E.164). Números nacionais de 10/11 dígitos que começam com DDD 55 (RS — DDD válido em `Telefone.java`) recebem o prefixo: `55991234567` → `5555991234567` e `5533344556` → `555533344556`. Teste existente `telefoneCom55_naoDuplicaPrefixo` (13 dígitos) mantido e renomeado; 2 novos testes de regressão DDD-55 adicionados. Javadoc atualizado com a justificativa (CR-01).

### CR-02: Webhook endpoint unauthenticated in every shipped configuration — anyone can approve/reject orçamentos

**Files modified:** `mekano-rest/src/main/java/com/fiap/mekano/rest/api/WebhookEvolutionResource.java`, `mekano-rest/src/main/resources/api-config.yml`, `docker-compose.yml`, `.env.example`, `mekano-rest/src/test/java/com/fiap/mekano/rest/api/WebhookEvolutionResourceTest.java`
**Commit:** `2bdaf04`
**Applied fix:**
1. **Fail closed:** `tokenValido()` retorna 401 quando o header `x-webhook-token` está ausente, vazio, ou quando `evolution.webhook-token` não está configurado — o evento nunca é processado anonimamente (o antigo `webhookToken.get().isBlank() → permite` foi removido).
2. **Comparação em tempo constante:** `MessageDigest.isEqual` (também endereça IN-08).
3. **Constructor injection** no resource (convenção AGENTS.md; endereça IN-07 incidentalmente).
4. **Env var consistente:** `.env.example` renomeia `EVOLUTION_WEBHOOK_SECRET` (nada lia) → `EVOLUTION_WEBHOOK_TOKEN`; `docker-compose.yml` passa `EVOLUTION_WEBHOOK_TOKEN: ${EVOLUTION_WEBHOOK_TOKEN:-change_me_webhook_token}` ao serviço `mekano`; `api-config.yml` já usava `EVOLUTION_WEBHOOK_TOKEN` (mantido, com comentário fail-closed).
5. **Testes:** token de teste via perfil `%test` no `api-config.yml`; testes existentes passam a enviar o header; novos testes `semToken_deveRetornar401` e `tokenIncorreto_deveRetornar401` (6 testes no total, todos verdes).

### WR-03: Prefix matching accepts arbitrary text as SIM/NÃO — unintended approvals/rejections

**Files modified:** `mekano-application/src/main/java/com/fiap/mekano/application/service/whatsapp/WhatsAppOrcamentoRespostaService.java`, `mekano-application/src/test/java/com/fiap/mekano/application/service/whatsapp/WhatsAppOrcamentoRespostaServiceTest.java`
**Commit:** `90d5e6f` (+ ajuste de testes `32e8254`)
**Applied fix:** a resposta agora casa apenas a **primeira palavra exata** (`split("\\s+")[0]`) contra `sim`/`s`/`nao`/`não`/`n` (normalizada: lowercase sem acentos) — `startsWith` removido. "simples assim" e "naoentendi o valor" são ignorados; "sim pode aprovar" aprova. Testes novos: `deveIgnorarTextoQueApenasComecaComNao`, `deveIgnorarTextoQueApenasComecaComSim`, `deveAprovarQuandoPrimeiraPalavraExata`.
**Status: `fixed: requires human verification`** — o fix segue a direção prescrita na review (primeira palavra exata), mas o exemplo do Issue ("não entendi o valor" — frase cuja primeira palavra É "não") continua casando com o token "nao" por design do próprio código de fix sugerido na review. Se a intenção de negócio for ignorar frases interrogativas iniciadas por "não", será necessário restringir para resposta de palavra única (decisão de produto — deixada para verificação humana).

### WR-04: `findByTelefone` LIKE fallback can resolve to the wrong client and approve/reject the wrong orçamento

**Files modified:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/ClienteRepositoryImpl.java`, `mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/repository/ClienteRepositoryImplTest.java` (novo)
**Commit:** `e20a217` (+ ajuste de teste `4722c0e`)
**Applied fix:** fallback reescrito: (a) sufixo passa a incluir o DDD (últimos **10** dígitos, não 8); (b) `ORDER BY createdAt DESC` (determinístico); (c) retorna cliente **apenas quando há uma única correspondência** — múltiplos clientes com o mesmo número local em DDDs diferentes (ex.: 91984847811 vs 21984847811) resultam em `Optional.empty()` (ambíguo → ignorado, nunca aprova/reprova o orçamento errado). Novo `ClienteRepositoryImplTest` com 3 casos: match exato, fallback único com DDI, fallback ambíguo → vazio (H2, `@TestTransaction`).

### WR-05: `AFTER_SUCCESS` observer throws after commit — successful operation returns error to caller

**Files modified:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/observer/WhatsAppOrcamentoObserver.java`, `mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/observer/WhatsAppOrcamentoObserverTest.java`
**Commit:** `0e9c783`
**Applied fix:** corpo do observer envolvido em try/catch — qualquer falha de lookup/notificação pós-commit é logada como `warn` (UUID da OS, sem PII) e não propaga ao caller (`OrdemDeServicoService.finalizarDiagnostico`). Novo teste `erroNoLookup_naoDevePropagar` (AppException 404 no lookup → `assertDoesNotThrow` e notifier nunca chamado).

### WR-06: Compose never delivers webhook events to Mekano — integration is a silent no-op out of the box

**Files modified:** `docker-compose.yml`
**Commit:** `2f5c380`
**Applied fix:** serviço `evolution-api` agora configura o webhook global: `WEBHOOK_GLOBAL_ENABLED: "true"`, `WEBHOOK_GLOBAL_URL: http://mekano:8080/api/v1/webhooks/evolution`, `WEBHOOK_GLOBAL_EVENTS: MESSAGES_UPSERT` — instâncias criadas via API/manager herdam esta config. Comentário no compose documenta que o header `x-webhook-token` deve ser configurado na instância (webhookHeaders) com o mesmo valor de `EVOLUTION_WEBHOOK_TOKEN` (CR-02); `.env.example` também documenta o requisito.

## Skipped Issues

None — all in-scope findings were fixed.

---

_Fixed: 2026-08-18T09:47:00-03:00_
_Fixer: the agent (gsd-code-fixer)_
_Iteration: 1_