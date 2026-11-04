# Arquitetura — Mekano (Tech Challenge)

Documentação arquitetural da solução: componente de nuvem, decisões formais (ADRs/RFCs), modelo de dados e fluxos de sequência.

## Repositórios

| # | Repo | Papel | CI/CD |
|---|------|-------|-------|
| 1 | **Mekano-API** (este repo) | Aplicação Quarkus (Clean Architecture) | CI (PR) + CD: build/testes → imagem no GHCR |
| 2 | **Mekano-Serverless** | Lambda de autenticação (CPF → JWT) + API Gateway HTTP | CD: deploy automático na AWS via OIDC |
| 3 | **Mekano-Infrastructure-K8s** | Terraform (VPC + EKS) + manifests Kubernetes + deploy | CD: plan em PR, apply em main |
| 4 | **Mekano-Infrastructure-Database** | Terraform (VPC dedicada + RDS PostgreSQL) + peering | CD: `workflow_dispatch` (plan/apply/destroy com guarda) |
| + | **Mekano-infrastructure-Observability** | Stack Alloy + Loki + Prometheus + Grafana (ECS) | CD + Terraform |

## Diagrama de Componentes (visão de nuvem)

```mermaid
flowchart TD
    CLIENTE["Cliente / Newman / Dev"] -->|HTTP| LB

    subgraph AWS["AWS us-east-1 — conta 563691830014"]
        LB["ALB (ingress-nginx)<br/>LoadBalancer público"]

        subgraph VPCAPP["VPC aplicação 10.20.0.0/16"]
            subgraph EKS["EKS mekano-eks (K8s 1.34)"]
                NGINX["ingress-nginx"] --> API["Mekano API<br/>(Quarkus, 2 pods + HPA)"]
                EVO["Evolution API<br/>(WhatsApp)"]
                OBS["Alloy agent<br/>(métricas do cluster)"]
            end
        end

        subgraph VPCDB["VPC database 10.30.0.0/16"]
            RDS[("RDS PostgreSQL 16<br/>mekano + evolution")]
        end

        PEER["VPC Peering pcx-…"] --- VPCAPP
        PEER --- VPCDB

        subgraph SERVERLESS["Mekano-Serverless"]
            APIGW["API Gateway HTTP<br/>POST /auth/token"] --> LAMBDA["Lambda auth<br/>(CPF → consulta DB → JWT)"]
        end

        SM["Secrets Manager<br/>JWT key + DB creds"]
        SSM["SSM Parameter<br/>/mekano/db/endpoint"]
    end

    subgraph OBSSTACK["Observabilidade (ECS, repo dedicado)"]
        LOKI[("Loki")] --- GRAFANA["Grafana<br/>(dashboards + alertas email)"]
        PROM[("Prometheus")] --- GRAFANA
        ALLOYGW["Alloy gateway"]
    end

    GHCR["GHCR<br/>ghcr.io/…/mekano-api"]

    LB --> NGINX
    API -->|"via peering:5432"| RDS
    EVO -->|"via peering:5432"| RDS
    LAMBDA -->|"via peering:5432"| RDS
    LAMBDA -.-> SM
    API -.-> SM
    API -.->|"logs JSON + /q/metrics"| OBSSTACK
    OBS -->|"remote_write"| PROM
    GHA["GitHub Actions"] -->|"OIDC AssumeRole"| AWS
    GHA -->|"push imagem"| GHCR
    SSM -.->|"endpoint do RDS"| GHA
```

## Documentos

| Tipo | Documento | Assunto |
|------|-----------|---------|
| ADR | [ADR-001 — AWS como provedor de nuvem](ADR-001-aws-como-nuvem.md) | Escolha da nuvem |
| ADR | [ADR-002 — PostgreSQL gerenciado (RDS)](ADR-002-postgresql-rds.md) | Escolha do banco |
| ADR | [ADR-003 — EKS + HPA](ADR-003-eks-hpa.md) | Orquestração e autoscaling |
| ADR | [ADR-004 — Comunicação REST síncrona + eventos internos](ADR-004-comunicacao-rest-eventos.md) | Padrão de comunicação |
| ADR | [ADR-005 — Observabilidade self-hosted](ADR-005-observabilidade-selfhosted.md) | Alloy/Loki/Prometheus/Grafana |
| RFC | [RFC-001 — Autenticação CPF com Lambda serverless](RFC-001-autenticacao-cpf-lambda.md) | Estratégia de autenticação |
| RFC | [RFC-002 — Banco em VPC dedicada com VPC peering](RFC-002-vpc-peering-banco-dedicado.md) | Topologia de rede |
| RFC | [RFC-003 — Nó único no Free Tier](RFC-003-free-tier-single-node.md) | Restrição de custo |
| Modelo | [Modelo de dados (ER) + justificativa do banco](modelo-de-dados-er.md) | ER, relacionamentos e ajustes |
| Sequência | [Autenticação CPF (Lambda → JWT)](sequencia-autenticacao-cpf.md) | Fluxo de autenticação |
| Sequência | [Abertura de Ordem de Serviço](../sequence-diagrams/criar-os.md) | Fluxo de negócio (pré-existente) |

## Fluxos complementares (já documentados em `docs/sequence-diagrams/`)

- [Ciclo completo da OS](../sequence-diagrams/fluxo-completo-os-lifecycle.md)
- [Requisição de compra por estoque mínimo](../sequence-diagrams/requisicao-compra-estoque-minimo.md)
- [Consulta pública de status](../sequence-diagrams/consulta-publica-status.md)
