---
phase: 05-whatsapp-integration
reviewed: 2026-08-18T12:00:00Z
depth: standard
files_reviewed: 18
files_reviewed_list:
  - .env.example
  - docker-compose.yml
  - mekano-application/src/main/java/com/fiap/mekano/application/service/whatsapp/WhatsAppOrcamentoRespostaService.java
  - mekano-application/src/test/java/com/fiap/mekano/application/service/whatsapp/WhatsAppOrcamentoRespostaServiceTest.java
  - mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/ClienteRepositoryPort.java
  - mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/WhatsAppNotifierPort.java
  - mekano-infrastructure/pom.xml
  - mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/observer/WhatsAppOrcamentoObserver.java
  - mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/ClienteRepositoryImpl.java
  - mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/whatsapp/EvolutionApiNotifier.java
  - mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/whatsapp/EvolutionApiRestClient.java
  - mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/whatsapp/dto/SendMessageResponse.java
  - mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/whatsapp/dto/SendTextRequest.java
  - mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/observer/WhatsAppOrcamentoObserverTest.java
  - mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/whatsapp/EvolutionApiNotifierTest.java
  - mekano-rest/src/main/java/com/fiap/mekano/rest/api/WebhookEvolutionResource.java
  - mekano-rest/src/main/resources/api-config.yml
  - mekano-rest/src/test/java/com/fiap/mekano/rest/api/WebhookEvolutionResourceTest.java
findings:
  critical: 2
  warning: 4
  info: 6
  total: 12
status: issues_found
---

# Phase 05: Code Review Report — WhatsApp Integration (WPP-01)

**Reviewed:** 2026-08-18
**Depth:** standard
**Files Reviewed:** 18
**Status:** issues_found

## Summary

WPP-01 review of the Evolution API integration (notify orçamento → SIM/NÃO webhook → aprovar/reprovar → confirm). Working-tree state reviewed, including the uncommitted removal of the "Olá {nome}, " prefix from `notificarRespostaOrcamento` — that change is consistent across the port (`WhatsAppNotifierPort`), impl (`EvolutionApiNotifier`), caller (`WhatsAppOrcamentoRespostaService`) and both tests, so no finding there.

Architecture compliance is good: constructor injection in services/observer, `@Transactional` confined to the use case (`OrcamentoService.aprovar/reprovar`), no transaction held during external HTTP (D-11), `AFTER_SUCCESS` observer ordering against `DiagnosticoFinalizadoObserver` is correct, phone masking in logs (PII V8), single `AppException`, and double-approval is guarded by `Orcamento.aprovar()` (422 on non-PENDENTE). Cross-module contracts verified: `findAllWithFilters(status, clienteUuid, veiculoUuid, dataInicio, dataFim, page, size)` call is aligned and orders by `createdAt DESC` (most recent OS first); `AprovarOrcamentoCommand`/`ReprovarOrcamentoCommand` record accessors match; `Messages` keys exist.

However, there are two critical issues: (1) `formatPhone` misroutes messages for clients with DDD 55 (RS), potentially delivering orçamento/vehicle data to an unrelated WhatsApp user; (2) the webhook endpoint is effectively unauthenticated in every shipped configuration (`@PermitAll` + empty-by-default token + compose never sets it + `.env.example` documents the wrong variable name), allowing anyone to approve/reject orçamentos.

## Critical Issues

### CR-01: `formatPhone` misclassifies DDD-55 numbers as already having country code → message sent to wrong recipient (PII leak)

**File:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/whatsapp/EvolutionApiNotifier.java:98-101`

**Issue:** `formatPhone` treats any digits starting with `"55"` as already E.164. But DDD **55 is a valid ANATEL DDD** (RS — `Telefone.java:33` includes `"51","53","54","55"`), so a legitimate client mobile in DDD 55 stored as 11 digits (e.g., `55991234567`) is returned unchanged. Evolution then routes it as country 55 + DDD 99 (`+55 99 1234567`), delivering the message — client name, vehicle, placa, orçamento value, and for `notificarRetirada` the workshop address — to an unrelated WhatsApp user. Same bug for DDD-55 landlines (10 digits). The existing test `telefoneCom55_naoDuplicaPrefixo` (EvolutionApiNotifierTest.java:86-94) only covers a 13-digit number and codifies the wrong assumption.

**Fix:** Only treat `55` as DDI when the digits length exceeds the national maximum (12/13 digits):

```java
private String formatPhone(String telefone) {
    String digits = telefone.replaceAll("\\D", "");
    return (digits.length() > 11 && digits.startsWith(COUNTRY_CODE)) ? digits : COUNTRY_CODE + digits;
}
```

Add regression tests: `"55991234567"` (DDD 55 mobile) → `"5555991234567"`; `"5533344556"` (DDD 55 landline) → `"555533344556"`; `"5511999999999"` (13 digits) → unchanged.

### CR-02: Webhook endpoint unauthenticated in every shipped configuration — anyone can approve/reject orçamentos

**Files:** `mekano-rest/src/main/java/com/fiap/mekano/rest/api/WebhookEvolutionResource.java:36-38, 70-73`; `mekano-rest/src/main/resources/api-config.yml:25`; `docker-compose.yml:51-60`; `.env.example:10`

**Issue:** The endpoint is `@PermitAll` and the token check is skipped whenever the config is blank (`webhookToken.get().isBlank()`). The default is `${EVOLUTION_WEBHOOK_TOKEN:}` — empty. `docker-compose.yml` never passes `EVOLUTION_WEBHOOK_TOKEN` to the `mekano` service (only `EVOLUTION_API_URL/API_KEY/INSTANCE_NAME`), and `.env.example` documents `EVOLUTION_WEBHOOK_SECRET` — a variable name nothing reads, silently ignored. Net effect: in the shipped default deployment, an anonymous attacker who knows a client's phone can POST `{"event":"MESSAGES_UPSERT","data":{"key":{"remoteJid":"<phone>@s.whatsapp.net","fromMe":false},"message":{"conversation":"não"}}}` and cancel a pending OS (orçamento reprovado → OS `CANCELADA`), or approve one — an authorization gap on a state-changing endpoint.

**Fix:**
1. Fail closed: require the token and reject when absent, e.g. `@ConfigProperty(name = "evolution.webhook-token") String webhookToken;` with no empty default, and return 401 when `token == null || !MessageDigest.isEqual(...)`.
2. Fix `.env.example:10` → `EVOLUTION_WEBHOOK_TOKEN=change_me_webhook_token`.
3. Add `EVOLUTION_WEBHOOK_TOKEN: ${EVOLUTION_WEBHOOK_TOKEN:}` to the `mekano` service environment in `docker-compose.yml`.

## Warnings

### WR-03: Prefix matching accepts arbitrary text as SIM/NÃO — unintended approvals/rejections

**File:** `mekano-application/src/main/java/com/fiap/mekano/application/service/whatsapp/WhatsAppOrcamentoRespostaService.java:67-68, 96`

**Issue:** `resposta.startsWith("sim")` / `startsWith("nao")` matches any text beginning with those strings: "não entendi o valor" reprova o orçamento; "simples assim" aprova. A client asking a question or typing a casual message triggers a binding business decision (OS cancelada/execução). The unit test only covers clean "sim"/"NÃO"/"talvez amanhã".

**Fix:** Match the first token exactly:

```java
String[] tokens = resposta.split("\\s+");
String palavra = tokens[0];
if (!palavra.equals("sim") && !palavra.equals("s")
        && !palavra.equals("nao") && !palavra.equals("não") && !palavra.equals("n")) {
    return false;
}
boolean aprovado = palavra.equals("sim") || palavra.equals("s");
```

### WR-04: `findByTelefone` LIKE fallback can resolve to the wrong client and approve/reject the wrong orçamento

**File:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/ClienteRepositoryImpl.java:91-99`

**Issue:** When the exact match fails, the last 8 digits are used as a `%suffix` LIKE — the suffix excludes the DDD, so two clients in different DDDs with the same local number (common, e.g., 91984847811 vs 21984847811) both match, and `firstResultOptional()` without any `ORDER BY` returns an arbitrary row. The WhatsApp response then approves/rejects the *wrong* client's orçamento. Note the service (`WhatsAppOrcamentoRespostaService.normalizarTelefone`) deliberately strips DDI 55 for 12-digit inputs, which pushes valid 11-digit mobile lookups into this ambiguous fallback path (see IN-09 — the tests hide this).

**Fix:** Match on the full number (exact match after canonical DDI-strip in one place), and when falling back, include the DDD in the suffix (last 10 digits) plus `ORDER BY createdAt` and treat multiple matches as empty/ambiguous. At minimum:

```java
if (digits.length() >= 10) {
    String suffix = digits.substring(digits.length() - 10); // inclui DDD
    List<ClienteEntity> matches = panacheRepository.find(
        "telefone like ?1 and isActive = ?2", "%" + suffix, true)
        .list();
    if (matches.size() == 1) {
        return Optional.of(matches.get(0)).map(clienteEntityMapper::toDomain);
    }
}
return Optional.empty();
```

### WR-05: `AFTER_SUCCESS` observer throws after commit — successful operation returns error to caller

**File:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/observer/WhatsAppOrcamentoObserver.java:54-79`

**Issue:** The observer runs after commit; if any lookup throws (`AppException(404)` for a soft-deleted cliente or veículo, or a missing orçamento), the exception propagates to the `EventPublisher.publish(...)` caller (`OrdemDeServicoService.finalizarDiagnostico`), turning an already-committed business operation into a 4xx/5xx response. The `notifier` is protected by `@Retry`/`@Fallback`, but the repository lookups are not. The observer is a side effect and must never fail the primary flow.

**Fix:** Wrap the observer body in try/catch (log warn with masked data, return) or use `Optional`-style lenient lookups:

```java
try {
    // ... lookups and notifier call ...
} catch (Exception ex) {
    log.warn("Falha ao notificar orçamento via WhatsApp (evento {}): {}", event.osUuid(), ex.getMessage());
}
```

### WR-06: Compose never delivers webhook events to Mekano — integration is a silent no-op out of the box

**File:** `docker-compose.yml:108-137`

**Issue:** `WEBHOOK_GLOBAL_ENABLED: "false"` is set and no per-instance webhook URL/events are configured anywhere in the compose file. Evolution API will never POST to `/api/v1/webhooks/evolution`, so the SIM/NÃO flow never triggers unless someone manually configures the instance webhook (URL `http://mekano:8080/api/v1/webhooks/evolution`, events `MESSAGES_UPSERT`) via the Evolution API/manager after startup. Nothing in the repo documents or automates this step.

**Fix:** Either configure the instance webhook in compose (Evolution supports per-instance webhook env/config) or document the required manual step prominently (README/AGENTS.md) — and consider a startup smoke check that fails fast when the webhook is not registered.

## Info

### IN-07: Field injection in `WebhookEvolutionResource` violates the constructor-injection convention

**File:** `mekano-rest/src/main/java/com/fiap/mekano/rest/api/WebhookEvolutionResource.java:45-49`

**Issue:** AGENTS.md mandates constructor injection for resources/services (`@InjectMocks`-friendly, the pattern used everywhere else in this codebase — including the two other new classes in this feature). The `@InjectMock` Quarkus test masks the inconsistency.

**Fix:** `public WebhookEvolutionResource(WhatsAppOrcamentoRespostaService respostaService, @ConfigProperty(...) Optional<String> webhookToken) { ... }`

### IN-08: Webhook token compared with `String.equals` — timing side channel

**File:** `mekano-rest/src/main/java/com/fiap/mekano/rest/api/WebhookEvolutionResource.java:71`

**Issue:** Plain `equals` comparison leaks length/prefix timing. Low impact for a non-interactive attacker, but cheap to fix.

**Fix:** `MessageDigest.isEqual(webhookToken.get().getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))` after a length check.

### IN-09: Test fixture inconsistent — mocks an exact phone match that cannot happen in production

**File:** `mekano-application/src/test/java/com/fiap/mekano/application/service/whatsapp/WhatsAppOrcamentoRespostaServiceTest.java:50-54, 90`

**Issue:** Input `"559184847811@s.whatsapp.net"` normalizes to `"9184847811"` (10 digits), but the client mock's phone is `"91984847811"` (11 digits). The mock stubs `findByTelefone("9184847811")`, so the test passes, but in production this exact match would fail and the flow depends on the ambiguous LIKE fallback (WR-04). The fixture should use a consistent 13-digit remoteJid: `"5591984847811@s.whatsapp.net"` → `"91984847811"`, exercising the exact-match path.

**Fix:** Change `TELEFONE_NORMALIZADO` to `"91984847811"` and the remoteJid in the test calls to `"5591984847811@s.whatsapp.net"` (or add a second test covering the DDI-strip for landlines).

### IN-10: Currency rendered with `BigDecimal.toString()` — not Brazilian format

**File:** `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/whatsapp/EvolutionApiNotifier.java:56-58`

**Issue:** `"R$ " + valorTotal` yields `"R$ 150.00"` / `"R$ 1234.5"` instead of `"R$ 150,00"` / `"R$ 1.234,50"` — user-facing message in a customer channel.

**Fix:**
```java
NumberFormat fmt = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
"... ficou em " + fmt.format(valorTotal) + ". Deseja aprovar? ..."
```

### IN-11: Test gaps on security-sensitive paths

**Files:** `mekano-rest/src/test/java/com/fiap/mekano/rest/api/WebhookEvolutionResourceTest.java`; `mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/whatsapp/EvolutionApiNotifierTest.java`

**Issue:** No test exercises the webhook token branch (401 on wrong token, processing with correct token), no test covers the DDD-55 formatting case that CR-01 depends on, and the `@Fallback` methods (`logFailure*`) are untested — the failure path that keeps the orçamento flow alive when Evolution is down has zero coverage.

**Fix:** Add a `@QuarkusTest` variant configuring `evolution.webhook-token` (e.g., `@TestProfile` or `quarkus.test.native-image-profile` config) asserting 401/200 behavior; add DDD-55 regression tests to `EvolutionApiNotifierTest`; add tests that the fallback methods swallow exceptions without throwing.

### IN-12: Evolution API key mismatch when `.env` is not configured

**Files:** `docker-compose.yml:59, 116`; `mekano-rest/src/main/resources/api-config.yml:23`

**Issue:** `mekano` defaults `EVOLUTION_API_KEY` to `dev-key`, while `evolution-api` uses `${EVOLUTION_API_KEY}` with no default (empty). Without a properly configured `.env`, Mekano sends `apikey: dev-key` and Evolution is running with an empty key — silent 401s until the environment is aligned.

**Fix:** Give both services the same default (`${EVOLUTION_API_KEY:-dev-key}` on the evolution-api service too) so the compose stack is consistent without `.env`, and document that the key must be changed for production.

---

_Reviewed: 2026-08-18_
_Reviewer: the agent (gsd-code-reviewer)_
_Depth: standard_
