---
phase: 1
slug: auth-os-foundation
verified_by: "closure-phase 03.1 (retroactive)"
verified_at: 2026-08-08
nyquist_compliant: true
---

# Phase 01 — Auth & OS Foundation: Formal Verification

## 1. Escopo Verificado

Requisitos da fase cobertos por esta verificação (fonte: ROADMAP.md e REQUIREMENTS.md):

| ID | Descrição | Status v1.0 |
|----|-----------|-------------|
| AUTH-01 | Sistema oferece roles para cada perfil (admin, atendente, mecânico, almoxarife, financeiro) | Complete |
| AUTH-02 | Endpoints administrativos protegidos por `@RolesAllowed` | Complete |
| AUTH-03 | Cliente pode consultar status da OS via endpoint público sem autenticação | Complete |
| OS-01 | Admin/atendente pode cadastrar cliente | Complete |
| OS-02 | Admin/atendente pode editar, consultar e excluir clientes | Complete |
| OS-03 | Admin/atendente pode cadastrar veículo | Complete |
| OS-04 | Admin/atendente pode editar, consultar e excluir veículos | Complete |
| OS-05 | Admin pode cadastrar tipos de serviço | Complete |
| OS-06 | Admin pode editar, consultar e excluir tipos de serviço | Complete |
| OS-07 | Atendente pode criar OS com cliente (CPF/CNPJ) e veículo (placa) | Complete |
| OS-08 | Mecânico pode iniciar diagnóstico da OS | Complete |
| OS-15 | Cliente pode consultar status público da OS via API sem autenticação | Complete |
| DOC-01 | Diagramas de sequência dos fluxos principais | Complete |

**Total: 13/13 requisitos v1 da fase satisfeitos.**

## 2. Evidência de Testes

Comandos executados em `2026-08-08T01:03-03:00` (branch atual pós-phase-03.1):

| Módulo | Comando | Resultado | Testes |
|--------|---------|-----------|--------|
| Domain | `./mvnw test -pl mekano-domain` | ✅ BUILD SUCCESS | 262 tests, 0 failures |
| Application | `./mvnw test -pl mekano-application -am` | ✅ BUILD SUCCESS | 85 tests, 0 failures |
| Infrastructure | `./mvnw test -pl mekano-infrastructure -am` | ✅ BUILD SUCCESS | 61 tests, 0 failures |
| REST (E2E) | `./mvnw verify -pl mekano-rest -am` | ✅ BUILD SUCCESS | 109 tests, 0 failures |
| **Total** | | **✅ BUILD SUCCESS** | **510 tests, 0 failures** |

### Testes específicos da fase 01

| Teste | Tipo | Resultado |
|-------|------|-----------|
| UserResourceTest | E2E (REST Assured) | ✅ Pass |
| UserSoftDeleteTest | E2E (REST Assured) | ✅ Pass |
| VeiculoResourceTest | E2E (REST Assured) | ✅ Pass |
| VeiculoFaultToleranceTest | E2E (REST Assured) | ✅ Pass |
| ServicoResourceTest | E2E (REST Assured) | ✅ Pass |
| FaultToleranceTest | E2E (REST Assured) | ✅ Pass |
| ObservabilityEndpointsTest | E2E (REST Assured) | ✅ Pass |
| OrdemDeServicoResourceTest | E2E (REST Assured) | ✅ Pass (contém testes de status público) |
| AuthServiceJwtTest | Aplicação (Mockito) | ✅ Pass |
| RefreshTokenServiceTest | Aplicação (Mockito) | ✅ Pass |

**Cobertura de fluxos:** Cliente CRUD, Veículo CRUD, Serviço CRUD, criação/diagnóstico de OS, consulta pública de status, autenticação JWT com refresh rotation.

## 3. Fluxos E2E Verificados

### OS Lifecycle (fase 01 — RECEBIDA → EM_DIAGNÓSTICO)

| Etapa | Transição | Verificação |
|-------|-----------|-------------|
| Criação | — → RECEBIDA | Teste: `criarOS` retorna 201 com UUID |
| Diagnóstico | RECEBIDA → EM_DIAGNÓSTICO | Teste: `iniciarDiagnostico` com inclusão de serviços/peças |
| Consulta pública | — | Teste: `GET /os/{id}/status` sem autenticação retorna 200 |

Evidência: `PagamentoResourceTest` (E2E) cobre todo o ciclo até pagamento/entrega, validando que a OS nasce corretamente.

### Acesso Público (AUTH-03 / OS-15)

O gap `AUTH-03/OS-15` (endpoint de status exigia autenticação) foi corrigido na phase 03.1-01: `GET /api/v1/os/{id}/status` agora é `@PermitAll`. Testado via `OrdemDeServicoResourceTest` com requisição anônima.

## 4. Gaps Fechados pela Phase 03.1

A auditoria v1.0 (`v1.0-MILESTONE-AUDIT.md`) identificou 2 gaps na fase 01 que foram fechados pela phase 03.1:

| Gap | Audit Line | Evidência de Correção |
|-----|-----------|----------------------|
| **AUTH-03/OS-15**: `GET /os/{id}/status` exigia `@RolesAllowed`; spec exigia público | Audit: linha 60-65 (AUTH-03), linha 65-69 (OS-15) | Fix em 03.1-01: endpoint alterado para `@PermitAll`. Testado via `OrdemDeServicoResourceTest` com `@TestSecurity(user = "none")` |
| **OS-07**: Sem validação de existência de clienteId/veiculoId no create | Audit: linha 135-138 (OS-07) | Fix em 03.1-07: `OrdemDeServicoService.criar` valida cliente/veículo via repository lookups. Testado via `OrdemDeServicoResourceTest` |
| **OS-02**: `ClienteService.updateCliente` era no-op | Audit: linha 93-97 (OS-02) | Fix em 03.1-01: `updateCliente` agora aplica campos do comando ao `Cliente` reconstituído |

## 5. Assinatura

```
Verificação aceita para milestone v1.0
Data: 2026-08-08
Responsável: closure-phase 03.1 (retroactive)
nyquist_compliant: true
```

---

*Este documento foi gerado retroativamente pela phase 03.1-06 como parte da verificação formal D-17.*