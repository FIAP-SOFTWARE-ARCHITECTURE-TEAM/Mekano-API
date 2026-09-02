# Phase 8: Documentation & Polish - Context

**Gathered:** 2026-08-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Completar a documentação do projeto Mekano para v2.0: README completo, diagramas, Swagger, Miro, vídeo demonstrativo. Fase puramente documental — sem alterações de código.

</domain>

<decisions>
## Implementation Decisions

### README (DOC-05)
- **D-01:** README.md existente será reestruturado para incluir: descrição da solução, objetivos da v2.0, instruções de execução local (docker-compose), deploy K8s (após Elias), Terraform (após Elias), HPA e simulação de carga
- **D-02:** Seção de troubleshooting do docker-compose (portas, network, chaves JWT) — resolvido na Fase 4

### Sequence Diagram (DOC-06)
- **D-03:** Diagrama em Mermaid direto no README.md (versionável, sem ferramenta externa)
- **D-04:** Fluxo OS completo: criar OS → diagnosticar → orçar → aprovar → executar → finalizar → pagar → entregar

### CI/CD Mermaid (DOC-07)
- **D-05:** Aguardar Elias implementar CD (INF-04) para criar diagrama completo. Task deve documentar dependência.

### API Collection (DOC-08)
- **D-06:** Swagger/OpenAPI já disponível via Quarkus (quarkus-smallrye-openapi). Ajustar collection e disponibilizar link.
- **D-07:** Verificar se o Swagger UI está acessível em /q/swagger-ui ou /swagger

### Miro (DOC-09)
- **D-08:** Ajustar Miro refletindo a API atual — task manual para o responsável

### Component Docs (DOC-10)
- **D-09:** Documentar componentes da aplicação (4 módulos Maven), infraestrutura provisionada (Docker, K8s, RDS) e fluxo de deploy

### HPA & Load Simulation (DOC-11)
- **D-10:** Explicar HPA (CPU 70%, Memory 80%, min 2, max 10) e como simular aumento de carga com `kubectl run -i --tty --image=busybox --restart=Never -- /bin/sh -c "while true; do wget -q -O- http://mekano-service:8080/api/v1/servicos; done"`

### Demo Video (DOC-04)
- **D-11:** Vídeo de até 15 min cobrindo: (1) Fluxo OS completo, (2) Chamadas de API, (3) Infra/containers, (4) Testes

### the agent's Discretion
- Estrutura exata do README (seções, ordem)
- Ferramenta de gravação de vídeo
- Link para Swagger (se será /q/swagger-ui ou customizado)

</decisions>

<canonical_refs>
## Canonical References

- `README.md` — README existente (precisa reestruturar)
- `docs/` — documentação existente (diagramas, EventStorming)
- `CONTRIBUTING.md` — guia de contribuição existente
- `.github/workflows/ci.yml` — pipeline CI atual (base do Mermaid CI)
- `docker-compose.yml` — compose atualizado na Fase 4
- `.planning/REQUIREMENTS.md` §DOC-04..11
- `.planning/ROADMAP.md` §Phase 8

</canonical_refs>

<code_context>
## Existing Assets

- `README.md` — já existe com pré-requisitos, seção inicial
- `docs/sequence-diagrams/` — diagramas de sequência existentes
- `docs/MEKANO_DOCUMENTATION.md` — documentação do sistema
- `docs/EventStorming_Mermaid.md` — Event Storming em Mermaid
- `CONTRIBUTING.md` — guia de contribuição (criado na v1.0)
- Swagger/OpenAPI via Quarkus (smallrye-openapi)
- 517 testes, todos passando

</code_context>

<specifics>
## Specific Ideas

- Vídeo: gravar em etapas (OS + API + infra + testes) e editar para 15 min
- README: estrutura sugerida: (1) Descrição, (2) Stack, (3) Quick Start (docker-compose), (4) API, (5) Deploy, (6) Arquitetura, (7) Testes, (8) Troubleshooting
- CI/CD Mermaid: task deve dizer "SÓ pode ser iniciada após INF-04 (Elias)"

</specifics>

<deferred>
## Deferred Ideas

- CI/CD Mermaid diagram — depende de Elias implementar CD (INF-04)

</deferred>

---

*Phase: 8-Documentation & Polish*
*Context gathered: 2026-08-08*