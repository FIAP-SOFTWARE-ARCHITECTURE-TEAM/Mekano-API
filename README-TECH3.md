# Mekano — Estrutura de Repositórios Git

```text
┌───────────────────────────────────────────────┐
│ 1. Mekano-API                                 │
│ Aplicação principal                           │
│ CI/CD + Docker + testes                       │
└───────────────────────────────────────────────┘

┌───────────────────────────────────────────────┐
│ 2. Mekano-Lambda                              │
│ Autenticação CPF/JWT                          │
│ CI/CD + deploy serverless                     │
└───────────────────────────────────────────────┘

┌───────────────────────────────────────────────┐
│ 3. Mekano-Infrastructure-K8s                  │
│ Terraform + EKS + Kubernetes + Observability  │
│ CI/CD de infraestrutura                       │
└───────────────────────────────────────────────┘

┌───────────────────────────────────────────────┐
│ 4. Mekano-Infrastructure-Database             │
│ Terraform + RDS PostgreSQL                    │
│ CI/CD de infraestrutura                       │
└───────────────────────────────────────────────┘
```

```mermaid
flowchart TD
    subgraph APPS["Repositórios de Aplicação"]
        API["1. Mekano-API<br/>Aplicação principal<br/>CI/CD + Docker + testes"]
        LAMBDA["2. Mekano-Lambda<br/>Autenticação CPF/JWT<br/>CI/CD + deploy serverless"]
    end

    subgraph INFRA["Repositórios de Infraestrutura"]
        K8S["3. Mekano-Infrastructure-K8s<br/>Terraform + EKS + Kubernetes + Observability<br/>CI/CD de infraestrutura"]
        DB["4. Mekano-Infrastructure-Database<br/>Terraform + RDS PostgreSQL<br/>CI/CD de infraestrutura"]
    end

    LAMBDA -->|"autenticação"| API
    K8S -->|"orquestra e provisiona"| API
    K8S -->|"orquestra e provisiona"| LAMBDA
    DB -->|"provisiona banco"| K8S
    DB -->|"conexão"| API
```
