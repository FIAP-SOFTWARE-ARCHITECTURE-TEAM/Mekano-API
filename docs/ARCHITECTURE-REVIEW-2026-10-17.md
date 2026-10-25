# Revisão Arquitetural — Mekano API

**Data:** 2026-10-17 · **Branch base:** `develop` · **Método:** inspeção de código verificado (não documental)

## Sumário executivo

A fundação está **sólida**: Clean Architecture real (domínio zero-dependência verificado), máquina de estados da OS com 81 testes de transição, padrões consistentes (factory `create`/`reconstitute`, two-class repository, RFC 7807). O problema não é o desenho — é **consistência e higienização**: há 1 bug de perda de dados, documentação que mente sobre o código, e ~15% de código morto/legado convivendo com código novo.

**Adensamento:** 285 classes main / 90 testes (~31%). Complexidade concentrada no contexto OS: `OrdemDeServico` (371 linhas) + `OrdemDeServicoService` (336) + `OrdemDeServicoResource` (372) — o trio é o hot spot.

## Pontos fortes (manter e proteger)

- **Domínio puro**: `mekano-domain` sem import de framework — a barreira está sendo respeitada
- **Máquina de estados explícita** (D-26): transições via métodos, matriz 8×8 testada (64 casos)
- **Eventos de domínio como records + `ProcessedEvents` (inbox pattern)**: idempotência de consumidores resolvida em infra
- **Audit auto-fill** via `AuditoriaListener` com fallbacks `PUBLICO`/`SISTEMA`
- **Webhook fail-closed** com comparação constant-time (`MessageDigest.isEqual`)
- **Disciplina de decisões** (D-01..D-26 registradas)

## Achados por severidade

### P0 — Corrigir antes de qualquer feature nova

**1. ~~Perda de dados em `NfEntradaRepositoryImpl.save`~~ CORREÇÃO (mesma data):** **NÃO é bug.** Verificação adicional mostrou que a cadeia request→domain→entity→response é **consistentemente enxuta** (`CreateNfEntradaRequest` tem só chaveAcesso/valorTotal/requisicaoCompraId; itens derivados da RequisicaoCompra). Quem mentia era o AGENTS.md, que descrevia DTOs com numero/serie/cnpj/pecaId que não existem mais. Ação real: sincronizar documentação (feito no mesmo dia).

**2. AGENTS.md desatualizados (drift grave) — CONFIRMADO E PIOR DO QUE O INICIAL** — risco direto para desenvolvimento assistido por IA:

| Doc diz | Realidade verificada |
|---|---|
| `AuthResource` NÃO existe | Existe — `login/refresh/logout` + `AuthServicePort` + `RefreshTokenRepositoryPort` no domínio |
| Bug em `ClienteService.updateCliente` | **Corrigido** — `cliente.atualizar(...)` + save presentes |
| `PecaService`/`NfEntradaService` são stubs com field injection | **Implementações reais** — constructor injection, `@Transactional`, validações, testes |
| 3 mappers vazios (dead code) | **Já deletados** do repositório |
| `Placa`/`PlacaVeiculo` duplicados | **Resolvido** — só `PlacaVeiculo` existe |
| `ClienteResource` não existe | **Existe** com resource + testes |
| 35 migrations V1–V35 | 33 arquivos no diretório |
| Campos NF-e no request/response | Modelo enxuto por design (3 campos de negócio) |

### P1 — Segurança e superfície de API

**3. Mutação de estado anônima**: `POST /orcamentos/{uuid}/aprovar|reprovar` são `@PermitAll`. A defesa é "UUID como capacete" (D-02), mas UUID em log/URL vaza e a operação tem efeitos colaterais sérios (transição de OS, estoque, eventos). A idempotência da issue #146 reduziu o dano de replay, mas não substitui autorização. Exigir role `cliente`; manter rota pública **apenas de leitura** (`GET /os/{id}/status`)
**4. Roles inconsistentes**: `OrcamentoResource` classe-level `@RolesAllowed({"admin","atendente","mecanico","cliente"})` — mecanico aprovar orçamento de cliente? Restrito a `admin`+`cliente`
**5. Sem rate limiting** no `AuthResource` (brute force) nem no webhook — com auth em produção virou prioridade
**6. ~~`ClienteResource` inexistente~~ CORREÇÃO:** existe (resource + testes). Residual: confirmar `@RolesAllowed` correto no controller

### P2 — Consistência interna

**7. Código morto (inventário revisado)**: `OrdemDeServico3.java` (239 linhas 100% comentadas) e `reconstitute()` antigo comentado em `OrdemDeServico.java:106-123`. ~~Mappers vazios~~ e ~~VO duplicado~~ já foram resolvidos no repo. Regra sugerida: **proibido bloco comentado > 5 linhas — deleta** (git guarda)

**8. ~~Stubs `PecaService`/`NfEntradaService`~~ CORREÇÃO:** não são stubs — são implementações reais com constructor injection e testes. Dívida real: **não implementam port de entrada** (resources chamam a service direto) — extrair `PecaServicePort`/`NfEntradaServicePort` para consistência com os demais contextos
**9. FT/cache assimétrico**: só User/Veiculo/Servico têm `@Retry`+`@Timeout`+`@CacheResult`
**10. Mensagens fora do bundle**: `"E-mail é obrigatório"` (ClienteService:77), `"Conflito de versão..."` (OrdemDeServicoRepositoryImpl:85)

### P3 — Evolução

**11. Agregado OS grande demais** — extrair contexto de Faturamento quando crescer
**12. `AppException` carrega HTTP status no domínio** — vazamento de transporte
**13. H2 (testes) vs PostgreSQL (prod)** — migrations V23/V24/V31/V33 existem por causa disso; migrar para Testcontainers
**14. Observabilidade** — falta correlation-id por request

## Matriz de priorização

| Ação | Impacto | Esforço | Ordem |
|---|---|---|---|
| ~~Corrigir `NfEntradaRepositoryImpl`~~ (corrigido — não era bug) | — | — | — |
| Atualizar os 4 AGENTS.md contra o código | Alto | M | **Agora** |
| Deletar código morto (`OrdemDeServico3`, bloco comentado) | Médio | S | **Agora** |
| Exigir auth em aprovar/reprovar + roles corretas | Alto | S | Sprint 1 |
| Rate limit em `/auth/*` e webhook | Alto | M | Sprint 1 |
| Extrair ports de entrada para Peca/NfEntrada/Requisicao | Médio | M | Sprint 1 |
| Confirmar `@RolesAllowed` do `ClienteResource` | Baixo | S | Sprint 1 |
| Padronizar FT/cache | Médio | M | Sprint 2 |
| Testcontainers PostgreSQL | Médio | M | Sprint 2 |
| Completar `Messages` bundle (mensagens hardcoded) | Baixo | S | Sprint 2 |
| Correlation-id / tracing | Médio | M | Sprint 3 |
| Extrair contexto Faturamento | Alto | G | Backlog |

## Recomendação final

Sprint de higienização (P0 + itens 7/8 + auth nos orçamentos) antes das fases restantes do roadmap — a phase 10 (split de repos) depende de AGENTS.md corretos para não propagar documentação errada para 4 repos.
