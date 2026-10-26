# Entrega — Tech Challenge (Software Architecture)

> **Entregar em formato PDF.** Este documento é o esqueleto completo: substitua os placeholders `[...]` e exporte (GitHub → print, ou Word/Google Docs → PDF).

## 1. Identificação

| Campo | Valor |
|-------|-------|
| Projeto | **Mekano** — gestão de ordens de serviço para oficina multi-unidade |
| Disciplina/Grupo | Tech Challenge — Software Architecture — FIAP |
| Equipe | [PREENCHER: nomes + RM] |
| Data de entrega | [PREENCHER] |

## 2. Repositórios (4 + observabilidade)

| # | Repositório | Propósito |
|---|-------------|-----------|
| 1 | [Mekano-API](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API) | Aplicação principal (Quarkus, Clean Architecture) — CI: build + 500+ testes; CD: imagem no GHCR |
| 2 | [Mekano-Serverless](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-Serverless) | Lambda de autenticação por CPF + API Gateway HTTP — CD: deploy automático AWS via OIDC |
| 3 | [Mekano-Infrastructure-K8s](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-Infrastructure-K8s) | Terraform (VPC + EKS) + manifests K8s — CD: plan em PR, apply + deploy em main |
| 4 | [Mekano-Infrastructure-Database](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-Infrastructure-Database) | Terraform (VPC dedicada + RDS PostgreSQL 16) + VPC peering |
| + | [Mekano-infrastructure-Observability](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-infrastructure-Observability) | Stack Grafana + Prometheus + Loki + Alloy (ECS) — dashboards e alertas |

## 3. Vídeo de demonstração

| Campo | Valor |
|-------|-------|
| Link | `[PREENCHER: YouTube/Vimeo — público ou não listado]` |
| Duração | ≤ 15 minutos |
| Roteiro gravado | [`docs/delivery/roteiro-video.md`](roteiro-video.md) |

Demonstrações cobertas: autenticação CPF · pipeline CI/CD · deploy automatizado · consumo de APIs protegidas · dashboard de monitoramento ao vivo · logs com correlação.

## 4. Deploys ativos (links)

| Serviço | URL |
|---------|-----|
| **API (Swagger)** | http://a76974e53d9b94e98ad3b8f2408294e5-334902849.us-east-1.elb.amazonaws.com/q/swagger-ui |
| **Health check** | http://a76974e53d9b94e98ad3b8f2408294e5-334902849.us-east-1.elb.amazonaws.com/q/health |
| **Grafana** (dashboards + alertas) | http://alb-observability-133760226.us-east-1.elb.amazonaws.com |
| API Gateway HTTP (autenticação CPF) | `[PREENCHER após deploy da Lambda: https://<api-id>.execute-api.us-east-1.amazonaws.com/auth/token]` |

## 5. Documentação arquitetural

Local: [`docs/architecture/`](../architecture/README.md) na Mekano-API.

| Documento | Link |
|-----------|------|
| Diagrama de componentes (visão de nuvem) | [docs/architecture/README.md](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/blob/main/docs/architecture/README.md) |
| ADR-001 — AWS como provedor de nuvem | [link](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/blob/main/docs/architecture/ADR-001-aws-como-nuvem.md) |
| ADR-002 — PostgreSQL gerenciado (RDS) | [link](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/blob/main/docs/architecture/ADR-002-postgresql-rds.md) |
| ADR-003 — EKS + HPA | [link](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/blob/main/docs/architecture/ADR-003-eks-hpa.md) |
| ADR-004 — Comunicação REST + eventos | [link](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/blob/main/docs/architecture/ADR-004-comunicacao-rest-eventos.md) |
| ADR-005 — Observabilidade self-hosted | [link](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/blob/main/docs/architecture/ADR-005-observabilidade-selfhosted.md) |
| RFC-001 — Autenticação CPF (Lambda) | [link](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/blob/main/docs/architecture/RFC-001-autenticacao-cpf-lambda.md) |
| RFC-002 — VPC peering (banco dedicado) | [link](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/blob/main/docs/architecture/RFC-002-vpc-peering-banco-dedicado.md) |
| RFC-003 — Nó único no Free Tier | [link](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/blob/main/docs/architecture/RFC-003-free-tier-single-node.md) |
| **Diagrama ER + justificativa do PostgreSQL** | [link](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/blob/main/docs/architecture/modelo-de-dados-er.md) |
| **Sequência — autenticação CPF** | [link](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/blob/main/docs/architecture/sequencia-autenticacao-cpf.md) |
| **Sequência — abertura de OS** | [link](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/blob/main/docs/sequence-diagrams/criar-os.md) |

## 6. Evidências de CI/CD (runs reais)

| Pipeline | Run (verde) |
|----------|-------------|
| Mekano-API — build + testes + publicação GHCR | [run 40251544759](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/actions/runs/40251544759) |
| K8s — terraform apply + deploy (rollout verificado) | [run 40256403606](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-Infrastructure-K8s/actions/runs/40256403606) |
| Database — apply do RDS | [run 40240987499](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-Infrastructure-Database/actions/runs/40240987499) |
| Observability — infra (60 recursos) | [run 40262989559](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-infrastructure-Observability/actions/runs/40262989559) |
| Observability — build imagens + deploy ECS | [run 40263374267](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-infrastructure-Observability/actions/runs/40263374267) |

## 7. Confirmação do usuário `soat-architecture`

> Convites de colaboração (permissão de leitura) enviados nos 5 repositórios.
> **[PREENCHER: screenshot de Settings → Collaborators de cada repo]**

| Repositório | Status |
|-------------|--------|
| Mekano-API | Convite aceito (read) |
| Mekano-Serverless | Convite enviado |
| Mekano-Infrastructure-K8s | Convite enviado |
| Mekano-Infrastructure-Database | Convite enviado |
| Mekano-infrastructure-Observability | Convite enviado |

## 8. Segurança (resumo)

- **Zero chaves AWS estáticas**: todos os pipelines autenticam por **OIDC** (`role-to-assume`), com trust por repositório/branch;
- RDS privado (`publicly_accessible=false`), **encrypted at rest**, acesso 5432 restrito ao CIDR da VPC da aplicação via peering;
- JWT **Ed25519** (chave privada no Secrets Manager), rotas sensíveis com `@RolesAllowed`, rate limiting em `/auth` e webhook;
- Secret scanning: nenhum segredo versionado (`privateKey.pem` coberto pelo `.gitignore`).

## 9. Como executar localmente (resumo)

| Repo | Instruções |
|------|-----------|
| Mekano-API | `docker-compose up -d` (PostgreSQL) → `./mvnw quarkus:dev` → Swagger em `http://localhost:8080/q/swagger-ui` |
| Mekano-Serverless | ver README do repo (SAM/local + testes `mvn verify`) |
| Mekano-Infrastructure-K8s | `docs/RUNBOOK.md` — 9 etapas do zero ao deploy |
| Mekano-Infrastructure-Database | `docs/RUNBOOK.md` — bootstrap + apply |
| Observability | `docker compose up -d` local ou `terraform apply` (ECS) — ver README |
