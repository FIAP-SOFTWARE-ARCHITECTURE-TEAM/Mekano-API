# Phase 4: Infrastructure Foundation - Context

**Gathered:** 2026-08-08
**Status:** Ready for planning

<domain>
## Phase Boundary

Revisar e refinar a infraestrutura de deploy do Mekano: docker-compose funcional para toda a equipe, e preparar tasks no Azure DevOps para que Elias implemente K8s, Terraform, HPA e CD pipeline de forma independente.

</domain>

<decisions>
## Implementation Decisions

### Docker Compose
- **D-01:** Manter docker-compose existente como base, refinando para resolver problemas relatados pela equipe (portas/network, erros diversos)
- **D-02:** Adicionar o Quarkus (JVM) ao docker-compose — full-stack app + banco, single command (`docker compose up`)
- **D-03:** Usar Dockerfile JVM do Quarkus (`mekano-rest/src/main/docker/Dockerfile.jvm`) como base
- **D-04:** Documentar troubleshooting no README ou docker-compose header: portas conflitantes, network, variáveis de ambiente JWT (chaves/issuer), volumes PostgreSQL

### K8s, Terraform, HPA, CI/CD
- **D-05:** K8s manifests (INF-02), Terraform (INF-03), CD pipeline (INF-04) e HPA são de responsabilidade do Elias — criar tasks no Azure DevOps com os requisitos mapeados para ele planejar e implementar
- **D-06:** Mermaid CI/CD (INF-05) depende da pipeline que Elias implementar — documentar após a implementação

### the agent's Discretion
- Detalhes de implementação Docker (multi-stage, tags, healthcheck do Quarkus) — o planejamento define
- Estrutura de K8s manifests (Deployment, Service, ConfigMap, Secret, HPA, Ingress) — Elias decide
- Provedor cloud e backend Terraform — Elias decide
- Registro de imagens (GHCR, Docker Hub) — Elias decide

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Docker
- `docker-compose.yml` — compose atual (só PostgreSQL, precisa adicionar app)
- `mekano-rest/src/main/docker/Dockerfile.jvm` — Dockerfile JVM gerado pelo Quarkus
- `.github/workflows/ci.yml` — pipeline CI existente (build + test)

### Projeto
- `.planning/REQUIREMENTS.md` §INF-01..05 — requisitos detalhados da fase
- `.planning/ROADMAP.md` §Phase 4 — goal e success criteria
- `.planning/research/STACK.md` — recomendações de stack da pesquisa v2.0
- `.planning/research/PITFALLS.md` — armadilhas de JaCoCo, K8s probes e Terraform state

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `docker-compose.yml` — compose existente com PostgreSQL 16-alpine, healthcheck, volume nomeado
- `mekano-rest/src/main/docker/Dockerfile.jvm` — Dockerfile JVM multi-stage (build Maven + runtime)
- `.github/workflows/ci.yml` — CI com JDK 17, Maven wrapper, build e teste

### Integration Points
- App depende de PostgreSQL (via `quarkus.datasource.jdbc.url` em `datasource-config.yml`)
- JWT depende de chave Ed25519 (via `mp.jwt.verify.publickey.location` e `smallrye.jwt.sign.key.location`)
- Testes usam H2 in-memory (não precisam de PostgreSQL)

### Established Patterns
- Quarkus config via YAML em `mekano-rest/src/main/resources/` (datasource, api, logging, cache, openapi)
- Profile-aware config: `%dev`, `%prod`, `%test`
- Maven wrapper (`./mvnw`) para build reproduzível

</code_context>

<specifics>
## Specific Ideas

- docker-compose deve permitir `docker compose up` único para subir app + banco
- README deve listar pré-requisitos (Docker, Java 17), passo-a-passo e troubleshooting
- Problemas conhecidos da equipe: portas conflitantes, rede entre containers, variáveis JWT

</specifics>

<deferred>
## Deferred Ideas

- K8s em cluster real (EKS/GKE) — Elias define e implementa
- CD com deploy automático — Elias define e implementa
- Terraform com backend remoto (S3) — Elias define e implementa
- HPA com métricas customizadas — Elias define e implementa

</deferred>

---

*Phase: 4-Infrastructure Foundation*
*Context gathered: 2026-08-08*