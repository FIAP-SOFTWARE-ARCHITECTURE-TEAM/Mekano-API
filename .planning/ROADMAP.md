# Roadmap: Mekano

**Core Value:** Gerenciar o ciclo de vida completo das Ordens de Serviço — do recebimento do veículo à entrega — com rastreabilidade, controle de estoque e cobrança integrados.

**Granularity:** standard
**Total v1 Requirements:** 37 (Complete)
**Total v2 Requirements:** 22
**Timeline:** 10 days
**Team:** 5 developers

---

## Milestones

- ✅ **v1.0 MVP** — Phases 1-3.1 (shipped 2026-08-08)
- 🚧 **v2.0 infra-docs-quality-whatsapp** — Phases 4-8 (in progress)

---

## Phases

- [ ] **Phase 4: Infrastructure Foundation** — Docker revisado, manifestos K8s, Terraform, pipeline CD
- [ ] **Phase 5: WhatsApp Integration** — Notificação via WhatsApp para orçamento e OS finalizada
- [ ] **Phase 6: Quality & Bug Fixes** — 80% cobertura JaCoCo, refactoring Clean Code + SOLID
- [ ] **Phase 7: API Improvements** — Listagem ordenada de OS por prioridade, verificação de endpoints
- [ ] **Phase 8: Documentation & Polish** — README completo, diagramas, Swagger, Miro, vídeo demonstrativo

---

## Phase Details

### 🚧 v2.0 infra-docs-quality-whatsapp (In Progress)

**Milestone Goal:** Evoluir o projeto de uma API funcional para um produto entregável com infraestrutura de produção, documentação completa, qualidade assegurada e integração externa com WhatsApp.

---

### Phase 4: Infrastructure Foundation
**Goal**: Infraestrutura de produção validada: Docker revisado, cluster K8s com manifests, Terraform para provisionamento, pipeline CD funcional
**Depends on**: Nothing (infra foundation — paralelizável com Phase 5)
**Requirements**: INF-01, INF-02, INF-03, INF-04, INF-05
**Success Criteria** (what must be TRUE):
  1. Dockerfile e docker-compose revisados funcionam em produção (JVM e Native builds)
  2. Manifestos K8s (Deployment, Service, ConfigMap, Secret, HPA, Ingress) aplicáveis em cluster Kind, com health probes configuradas
  3. Scripts Terraform provisionam cluster EKS e banco RDS com backend S3 e state locking
  4. Pipeline CD no GitHub Actions faz build, push para registry e deploy automático no cluster
  5. Pipeline CI/CD documentada com diagrama Mermaid no repositório
**Plans**: 2 plans
**Plan list**:
  - [ ] 04-01-PLAN.md — Docker compose refinement (restart policies, .dockerignore, .env.example, troubleshooting README)
  - [ ] 04-02-PLAN.md — Azure DevOps task descriptions for Elias (K8s, Terraform, CD pipeline, Mermaid)
**UI hint**: no

---

### Phase 5: WhatsApp Integration
**Goal**: Clientes notificados via WhatsApp sobre aprovação/recusa de orçamento e finalização de OS
**Depends on**: Nothing (port/adapter pattern — paralelizável com Phase 4)
**Requirements**: WPP-01, WPP-02, API-05
**Success Criteria** (what must be TRUE):
  1. Cliente recebe notificação WhatsApp com link para aprovar/recusar orçamento quando orçamento é gerado
  2. Cliente recebe notificação WhatsApp informando que veículo está pronto para retirada quando OS é finalizada
  3. Escopo de atualização de status via ferramenta externa verificado e documentado (aplicável somente a aprovar/recusar orçamento)
**Plans**: 3 plans
**Plan list**:
  - [ ] 05-01-PLAN.md — WhatsApp notifier port + Evolution API REST Client + docker-compose + orçamento observer (Wave 1)
  - [ ] 05-02-PLAN.md — WhatsApp retirada notification + webhook endpoint for interactive approve/reject (Wave 2)
  - [ ] 05-03-PLAN.md — Verify and document scope of external status update via WhatsApp (Wave 3)
**UI hint**: no

---

### Phase 6: Quality & Bug Fixes
**Goal**: Código com 80% de cobertura de testes (JaCoCo LINE) e aderente a princípios Clean Code e SOLID
**Depends on**: Phase 4 (infra CI para gate de qualidade), Phase 5 (testes do adapter WhatsApp)
**Requirements**: QLD-01, QLD-02
**Success Criteria** (what must be TRUE):
  1. Relatório JaCoCo `report-aggregate` mostra ≥80% LINE coverage no projeto completo (excluindo DTOs, Entities, Mappers gerados, REST Client proxies)
  2. Pipeline CI falha se cobertura ficar abaixo de 80%
  3. Bugs conhecidos corrigidos (NfEntradaRepositoryImpl copy-paste, ClienteService.updateCliente no-op, field injection em stubs)
  4. Revisão geral de código concluída: inconsistências de naming (PT-BR vs EN), field injection, estilo de entidades, mappers vazios, VOs duplicados e packages incorretos endereçados sem introduzir regressões
**Plans**: TBD
**UI hint**: no

---

### Phase 7: API Improvements
**Goal**: API de listagem de OS ordenada por prioridade de status; verificação de endpoints existentes concluída
**Depends on**: Phase 6 (testes garantem que refatoração não quebrou endpoints existentes)
**Requirements**: API-01, API-02, API-03, API-04
**Success Criteria** (what must be TRUE):
  1. Verificação documentada se "APIs" refere-se a endpoints ou múltiplas APIs (API-01)
  2. Existência de endpoint de abertura de OS verificada e documentada (API-02)
  3. Existência de endpoint de consulta de status da OS verificada e documentada (API-03)
  4. Listagem de OS retorna ordenada por prioridade: Em Execução > Aguardando Aprovação > Diagnóstico > Recebida, mais antigas primeiro, omitindo finalizadas/entregues
**Plans**: TBD
**UI hint**: no

---

### Phase 8: Documentation & Polish
**Goal**: Documentação completa e profissional: README, diagramas, Swagger, Miro, vídeo demonstrativo
**Depends on**: Phase 4 (infra para docs de deploy), Phase 5 (WhatsApp para diagramas de sequência), Phase 7 (API final para Swagger)
**Requirements**: DOC-04, DOC-05, DOC-06, DOC-07, DOC-08, DOC-09, DOC-10, DOC-11
**Success Criteria** (what must be TRUE):
  1. README.md contém descrição da solução, objetivos da v2, instruções de execução local, deploy K8s e Terraform
  2. Diagrama de sequência do fluxo de consumo de endpoints e Mermaid do fluxo de CI/CD estão no README
  3. Collection Postman/Swagger revisada e link disponível; Miro atualizado em relação à API
  4. Documentação da API com componentes, infraestrutura provisionada e fluxo de deploy; explicação de HPA e simulação de carga
  5. Vídeo demonstrativo (até 15 min) gravado e disponibilizado mostrando ambiente em execução
**Plans**: TBD
**UI hint**: no

---

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 4. Infrastructure Foundation | 0/2 | Planning complete | - |
| 5. WhatsApp Integration | 0/3 | Plans created | - |
| 6. Quality & Bug Fixes | 0/0 | Not started | - |
| 7. API Improvements | 0/0 | Not started | - |
| 8. Documentation & Polish | 0/0 | Not started | - |

---

## Requirement Coverage

| Category | Total | Phase 4 | Phase 5 | Phase 6 | Phase 7 | Phase 8 |
|----------|-------|---------|---------|---------|---------|---------|
| Documentação | 8 | 0 | 0 | 0 | 0 | 8 |
| Qualidade | 2 | 0 | 0 | 2 | 0 | 0 |
| Infraestrutura | 5 | 5 | 0 | 0 | 0 | 0 |
| WhatsApp | 2 | 0 | 2 | 0 | 0 | 0 |
| API | 5 | 0 | 1 | 0 | 4 | 0 |
| **Total** | **22** | **5** | **3** | **2** | **4** | **8** |

✓ **22/22 v2 requirements mapped to phases**
✓ **No orphaned requirements**

---

## Risk Mitigation

| Risk | Impact | Mitigation | Phase |
|------|--------|------------|-------|
| WhatsApp token expira 24h | Notificações param silenciosamente | TokenManager no primeiro componente WhatsApp; token em K8s Secret; readiness check | 5 |
| JaCoCo multi-module false pass | 80% reportado mas real <80% | `report-aggregate` em mekano-rest; exclusions para MapperImpl e REST Client proxies | 6 |
| Clean Code refactoring scope creep | Bugs em 517+ testes | Lista fixa de 5-10 itens; sem mudanças estilísticas; full test suite após cada alteração | 6 |
| Notificação dentro de @Transactional | Conexão DB retida durante latência HTTP | CDI events com `TransactionPhase.AFTER_SUCCESS` | 5 |
| Terraform state local versionado | Perda/corrupção de estado | S3 backend + `use_lockfile` + .gitignore desde o primeiro init | 4 |
| Documentação drift | README desatualizado em dias | Swagger auto-gerado; gravar vídeo por último; docs de alto nível | 8 |

---

## Day-by-Day Allocation

| Day | Phase Focus | Team Split | Deliverable |
|-----|-------------|------------|-------------|
| 1-2 | 4 (Infra) + 5 (WhatsApp) | 3/5 Infra + 2/5 WhatsApp | Docker revisado, K8s manifests, Terraform scaffold, WhatsApp port/adapter |
| 3-6 | 5 (WhatsApp) + 6 (Quality) | 3/5 Quality + 2/5 WhatsApp | WhatsApp notifs funcionando, JaCoCo 80%, bugs corrigidos, CD pipeline |
| 5-7 | 6 (Quality) + 7 (API) | 3/5 Quality + 2/5 API | Coverage gate, OS listing prioritizada, endpoints verificados |
| 7-9 | 8 (Documentation) + 7 (API) | 3/5 Docs + 2/5 API | README, diagramas, Swagger, Miro |
| 9-10 | 8 (Documentation) | 5/5 Docs | Vídeo demonstrativo, revisão final, entrega |