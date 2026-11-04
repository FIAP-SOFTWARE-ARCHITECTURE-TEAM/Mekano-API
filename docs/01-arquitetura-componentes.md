# 01 — Arquitetura e Diagrama de Componentes

## Objetivo

Apresentar a visão consolidada da arquitetura Mekano, cobrindo nuvem, API Gateway, EKS, Lambda, banco, Evolution API e observabilidade.

## Repositórios analisados

- `Mekano-API`
- `Mekano-Infrastructure-K8s`
- `Mekano-Infrastructure-Database`
- `Mekano-Serverless`
- `Mekano-infrastructure-Observability`

## Visão geral

```text
Cliente
  ↓
API Gateway
  ├── POST /auth/token
  │       ↓
  │    Lambda Auth
  │       ↓
  │    PostgreSQL
  │       ↓
  │      JWT
  │
  └── /api/v1/*, /q/*, /evolution/*
          ↓
       VPC Link
          ↓
       NLB interno
          ↓
      Ingress NGINX
       ├── Mekano API
       └── Evolution API

Persistência:
Mekano API / Lambda / Evolution → RDS PostgreSQL 16

Observabilidade:
Pods → Alloy → Loki → Grafana
Mekano API /q/metrics → Prometheus → Grafana
```

## Diagrama de Componentes

```mermaid
flowchart TB

    USER["Cliente / Atendente / Postman / Browser"]

    subgraph AWS["AWS - us-east-1"]

        APIGW["Amazon API Gateway HTTP\nmekano-dev"]

        subgraph VPC["VPC Mekano 10.20.0.0/16"]

            subgraph SERVERLESS["Camada Serverless"]
                LAMBDA["Lambda Auth\nCPF → JWT Ed25519"]
            end

            subgraph EKS["Amazon EKS - mekano-eks"]
                NLB["NLB interno"]
                NGINX["Ingress NGINX"]

                API["Mekano API\nJava 17 + Quarkus\nDDD + Clean Architecture"]

                EVO["Evolution API\nWhatsApp"]

                HPA["HPA\n1-4 pods\nCPU 70% / Memória 80%"]

                ALLOYEKS["Grafana Alloy\nDaemonSet de logs"]
            end

            RDS[("Amazon RDS\nPostgreSQL 16\nPrivado")]
        end

        subgraph OBS["Observabilidade"]
            OBSALB["ALB Observability"]
            ALLOYGW["Alloy Gateway"]
            LOKI[("Loki")]
            PROM[("Prometheus")]
            GRAFANA["Grafana"]
        end

        CW["CloudWatch"]
        SSM["AWS SSM"]
    end

    USER -->|"HTTPS"| APIGW

    APIGW -->|"POST /auth/token"| LAMBDA
    APIGW -->|"ANY / e ANY /{proxy+}"| NLB

    NLB --> NGINX

    NGINX -->|"/api/v1/* e /q/*"| API
    NGINX -->|"/evolution/*"| EVO

    HPA -.-> API

    API -->|"JDBC 5432"| RDS
    EVO -->|"JDBC 5432"| RDS
    LAMBDA -->|"JDBC 5432"| RDS

    API -->|"HTTP interno"| EVO

    API -->|"/q/metrics"| PROM
    API -->|"stdout JSON"| ALLOYEKS
    EVO -->|"stdout"| ALLOYEKS

    ALLOYEKS -->|"Bearer /loki/api/v1/push"| OBSALB
    OBSALB --> ALLOYGW
    ALLOYGW --> LOKI

    PROM --> GRAFANA
    LOKI --> GRAFANA

    LAMBDA --> CW
    SSM -.-> API
```

## Fluxo do API Gateway

```mermaid
flowchart LR
    C["Cliente"] --> GW["API Gateway"]

    GW -->|"/auth/token"| L["Lambda Auth"]
    GW -->|"/api/v1/*"| VL["VPC Link"]
    GW -->|"/q/*"| VL
    GW -->|"/evolution/*"| VL

    VL --> NLB["NLB interno"]
    NLB --> ING["Ingress NGINX"]

    ING --> API["Mekano API"]
    ING --> EVO["Evolution API"]
```

## Responsabilidades

### API Gateway
- ponto único de entrada pública;
- direciona `/auth/token` para a Lambda;
- envia as demais rotas por VPC Link;
- evita exposição direta dos workloads internos.

### Lambda Auth
- valida CPF;
- consulta o cliente;
- verifica status;
- gera JWT Ed25519.

### Mekano API
- regras de negócio;
- validação de JWT;
- autorização por roles;
- OS, orçamento, estoque, clientes e veículos.

### Evolution API
- comunicação com WhatsApp;
- envio e recebimento de mensagens;
- webhooks integrados à Mekano API.

### PostgreSQL
- persistência transacional;
- dados da aplicação;
- dados utilizados na autenticação;
- armazenamento utilizado pela Evolution conforme configuração.

### Observabilidade
- Prometheus: métricas;
- Alloy: coleta de logs;
- Loki: armazenamento e consulta de logs;
- Grafana: dashboards e alertas.

## Observação de arquitetura

A documentação consolidada considera a arquitetura atual com:

```text
VPC Mekano 10.20.0.0/16
├── EKS
├── Lambda
└── RDS
```

Documentos antigos que ainda mencionem VPC Peering devem ser revisados.
