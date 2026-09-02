# VPC Scope Architecture — Mekano API (AWS)

> Diagrama de arquitetura da VPC do sistema Mekano em produção na AWS.  
> Escopo: layout de subnets, componentes de rede e fluxo de tráfego.

---

## Diagrama da VPC

```mermaid
graph TB
    classDef internet fill:#232f3e,color:#a855f7,stroke:#a855f7,stroke-width:2px
    classDef aws_network fill:#232f3e,color:#ff9900,stroke:#ff9900,stroke-width:2px
    classDef aws_compute fill:#232f3e,color:#3b82f6,stroke:#3b82f6,stroke-width:2px
    classDef aws_database fill:#232f3e,color:#10b981,stroke:#10b981,stroke-width:2px
    classDef external fill:#232f3e,color:#a855f7,stroke:#a855f7,stroke-width:2px
    classDef k8s fill:#326ce5,color:#fff,stroke:#326ce5,stroke-width:2px
    classDef public_subnet fill:#232f3e,color:#60a5fa,stroke:#3b82f6,stroke-width:2px
    classDef private_subnet fill:#232f3e,color:#34d399,stroke:#10b981,stroke-width:2px
    classDef db_subnet fill:#232f3e,color:#f87171,stroke:#ef4444,stroke-width:2px

    INTERNET[("🌐 Internet")]

    subgraph VPC["☁️ VPC mekano-dev — CIDR 10.0.0.0/16"]
        direction TB

        IGW["🚪 Internet<br/>Gateway"]

        subgraph PUBLIC["📍 Public Subnets"]
            direction LR
            SUB_A["Public Subnet A<br/><b>10.0.1.0/24</b><br/>AZ-a"]
            SUB_B["Public Subnet B<br/><b>10.0.2.0/24</b><br/>AZ-b"]

            ALB["⚖️ Application<br/>Load Balancer"]
            NAT["🔒 NAT<br/>Gateway"]
        end

        subgraph PRIVATE["📍 Private Subnets"]
            direction LR
            SUB_PA["Private Subnet A<br/><b>10.0.10.0/24</b><br/>AZ-a"]
            SUB_PB["Private Subnet B<br/><b>10.0.20.0/24</b><br/>AZ-b"]

            subgraph EKS_WORKERS["⎈ EKS Worker Nodes"]
                direction TB
                INGRESS["🔀 NGINX Ingress<br/>Controller"]

                subgraph MEKANO_PODS["🟦 Mekano Pods"]
                    PM1["Pod 1<br/>Quarkus"]
                    PM2["Pod 2<br/>Quarkus"]
                end

                subgraph EVOLUTION_PODS["🟩 Evolution Pods"]
                    PE1["Pod 1<br/>WhatsApp GW"]
                    PE2["Pod 2<br/>WhatsApp GW"]
                end
            end
        end

        subgraph DATABASE["🗄️ Database Subnets"]
            direction LR
            SUB_DA["DB Subnet A<br/><b>10.0.100.0/24</b><br/>AZ-a"]
            SUB_DB["DB Subnet B<br/><b>10.0.200.0/24</b><br/>AZ-b"]

            RDS["🗄️ RDS PostgreSQL<br/>Shared (Mekano + Evolution)<br/><b>db.t4g.micro</b><br/>Single-AZ"]
        end
    end

    INTERNET --> IGW
    IGW --> ALB
    SUB_A --> ALB
    SUB_B --> ALB
    ALB --> INGRESS
    INGRESS --> MEKANO_PODS
    INGRESS --> EVOLUTION_PODS
    MEKANO_PODS -->|"db: mekano"| RDS
    EVOLUTION_PODS -->|"db: evolution"| RDS
    PRIVATE -.->|"saída via NAT"| NAT
    NAT --> IGW

    class INTERNET external
    class IGW,NAT aws_network
    class SUB_A,SUB_B public_subnet
    class SUB_PA,SUB_PB private_subnet
    class SUB_DA,SUB_DB db_subnet
    class ALB,INGRESS aws_compute
    class PM1,PM2,PE1,PE2 k8s
    class RDS aws_database
```

---

## Fluxo de Tráfego

```mermaid
sequenceDiagram
    autonumber
    participant NET as 🌐 Internet
    participant IGW as 🚪 IGW
    participant ALB as ⚖️ ALB
    participant NGX as 🔀 NGINX
    participant API as 🟦 Mekano API
    participant EVO as 🟩 Evolution API
    participant DB as 🗄️ RDS<br/>(shared)

    rect rgb(35, 47, 62)
        Note over NET, NGX: 🔵 Tráfego de Entrada (Inbound)
        NET->>IGW: HTTPS request
        IGW->>ALB: Forward (port 443)
        ALB->>NGX: Route by host/path
        NGX->>API: /api/v1/* (port 8080)
        NGX->>EVO: /api/v1/webhooks/* (port 5033)
    end

    rect rgb(20, 40, 35)
        Note over API, DB: 🟢 Tráfego Interno (Private — Database Subnets)
        API->>DB: Query/Update (port 5432) — db: mekano
        DB-->>API: Result
        EVO->>DB: Query (port 5432) — db: evolution
        DB-->>EVO: Result
    end

    rect rgb(45, 35, 20)
        Note over NET, DB: 🟠 Tráfego de Saída (Outbound via NAT Gateway)
        API->>NGX: Response
        NGX->>ALB: Forward
        ALB->>IGW: HTTPS response
        IGW->>NET: Internet
    end
```

---

## Fluxo de Segurança — Security Groups

```mermaid
graph LR
    classDef sg_alb fill:#232f3e,color:#60a5fa,stroke:#3b82f6,stroke-width:2px
    classDef sg_eks fill:#232f3e,color:#34d399,stroke:#10b981,stroke-width:2px
    classDef sg_db fill:#232f3e,color:#f87171,stroke:#ef4444,stroke-width:2px
    classDef sg_cache fill:#232f3e,color:#fb923c,stroke:#f97316,stroke-width:2px

    subgraph SG_ALB["🔒 SG-ALB"]
        SG_ALB_IN["Entrada: 0.0.0.0/0<br/>:443, :80"]
        SG_ALB_OUT["Saída: SG-EKS<br/>:8080, :5033"]
    end

    subgraph SG_EKS["🔒 SG-EKS"]
        SG_EKS_IN["Entrada: SG-ALB<br/>:8080, :5033"]
        SG_EKS_OUT["Saída: SG-RDS<br/>:5432"]
    end

    subgraph SG_RDS["🔒 SG-RDS (Shared)"]
        SG_RDS_IN["Entrada: SG-EKS<br/>:5432"]
    end

    SG_ALB_OUT --> SG_EKS_IN
    SG_EKS_OUT --> SG_RDS_IN

    class SG_ALB_IN,SG_ALB_OUT sg_alb
    class SG_EKS_IN,SG_EKS_OUT sg_eks
    class SG_RDS_IN sg_db
```

---

## Security Groups

| Security Group | Regras de Entrada | Regras de Saída |
|----------------|-------------------|-----------------|
| **SG-ALB** | `0.0.0.0/0` :443, :80 | SG-EKS :8080, :5033 |
| **SG-EKS** | SG-ALB :8080, :5033 | SG-RDS :5432 |
| **SG-RDS (Shared)** | SG-EKS :5432 | — |

---

## Route Tables

| Route Table | Destino | Target |
|-------------|---------|--------|
| **RT-Public** | `10.0.0.0/16` | local |
| **RT-Public** | `0.0.0.0/0` | Internet Gateway |
| **RT-Private** | `10.0.0.0/16` | local |
| **RT-Private** | `0.0.0.0/0` | NAT Gateway |
| **RT-DB** | `10.0.0.0/16` | local |
| **RT-DB** | `0.0.0.0/0` | NAT Gateway |

---

## Subnets Summary

| Subnet | CIDR | AZ | Uso |
|--------|------|-----|-----|
| Public A | `10.0.1.0/24` | AZ-a | ALB, NAT Gateway |
| Public B | `10.0.2.0/24` | AZ-b | ALB (redundância) |
| Private A | `10.0.10.0/24` | AZ-a | EKS Workers, pods |
| Private B | `10.0.20.0/24` | AZ-b | EKS Workers, pods |
| DB A | `10.0.100.0/24` | AZ-a | RDS |
| DB B | `10.0.200.0/24` | AZ-b | RDS |

---

## Decisões de Rede

| Decisão | Justificativa |
|---------|---------------|
| **3 tiers de subnets** | Isolamento de segurança — público (ALB), privado (EKS), banco (RDS) |
| **RDS Single-AZ (Shared)** | Custo-eficiência — Evolution e Mekano compartilham a mesma instância RDS, databases separados |
| **NAT Gateway em Public Subnet** | Custo — um NAT por VPC; pods privados saem pela internet |
| **SGs mínimos** | Princípio de menor privilégio — cada SG só expõe portas necessárias |
| **DB Subnets sem rota para internet** | Segurança — bancos não acessíveis externamente |
| **CIDR /16** | 65.536 IPs disponíveis — espaço para crescimento |
| **Cache Local (não Redis)** | Evolution API usa cache local — sem ElastiCache Redis |
