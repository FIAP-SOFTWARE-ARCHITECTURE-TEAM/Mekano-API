# Global Scope Architecture — Mekano API (AWS)

> Diagrama de arquitetura global do sistema Mekano em produção na AWS.  
> Escopo: fluxo completo desde o cliente até os serviços externos.

---

## Arquitetura Principal

```mermaid
graph TB
    classDef internet fill:#1a1a2e,color:#fff,stroke:#16213e,stroke-width:2px
    classDef aws_network fill:#232f3e,color:#ff9900,stroke:#ff9900,stroke-width:2px
    classDef aws_compute fill:#232f3e,color:#3b82f6,stroke:#3b82f6,stroke-width:2px
    classDef aws_database fill:#232f3e,color:#10b981,stroke:#10b981,stroke-width:2px
    classDef aws_cache fill:#232f3e,color:#ef4444,stroke:#ef4444,stroke-width:2px
    classDef external fill:#1a1a2e,color:#a855f7,stroke:#a855f7,stroke-width:2px
    classDef k8s fill:#326ce5,color:#fff,stroke:#326ce5,stroke-width:2px

    CLIENT[("🌐 Internet<br/>Clients")]
    MOBILE["📱 Mobile / Web<br/>(App)"]
    EXT_API["📱 WhatsApp<br/>Cloud API"]

    subgraph AWS["☁️ AWS Cloud"]
        direction TB

        IGW["🚪 Internet<br/>Gateway"]

        subgraph PUBLIC["Public Subnets"]
            ALB["⚖️ Application<br/>Load Balancer"]
        end

        subgraph EKS["⎈ EKS Cluster"]
            direction TB
            INGRESS["🔀 NGINX Ingress<br/>Controller"]

            subgraph MEKANO_APP["Mekano Service"]
                SVC_MEK["Service: mekano<br/>ClusterIP :8080"]
                POD_M1["🟦 Mekano Pod 1<br/>Quarkus"]
                POD_M2["🟦 Mekano Pod 2<br/>Quarkus"]
            end

            subgraph EVOLUTION_APP["Evolution Service"]
                SVC_EVO["Service: evolution-api<br/>ClusterIP :5033"]
                POD_E1["🟩 Evolution Pod 1<br/>WhatsApp GW"]
                POD_E2["🟩 Evolution Pod 2<br/>WhatsApp GW"]
            end
        end

        subgraph PRIVATE["Private Subnets"]
            NAT["🔒 NAT<br/>Gateway"]
        end

        subgraph DATABASE["Database Subnets"]
            direction LR
            RDS_MEK["🗄️ RDS PostgreSQL<br/>Mekano<br/><b>db.r6g.large</b><br/>Multi-AZ"]
            RDS_EVO["🗄️ RDS PostgreSQL<br/>Evolution<br/><b>db.t4g.medium</b><br/>Single-AZ"]
            REDIS["⚡ ElastiCache<br/>Redis<br/><b>cache.t4g.micro</b>"]
        end
    end

    CLIENT -->|"API REST<br/>(todas funcionalidades)"| IGW
    MOBILE -.->|"não implementado<br/>(fora de escopo)"| CLIENT
    EXT_API -->|"webhook<br/>(aprovação/reprovação<br/>orçamento)"| IGW
    IGW --> ALB
    ALB --> INGRESS
    INGRESS --> SVC_MEK
    INGRESS --> SVC_EVO
    SVC_MEK --> POD_M1
    SVC_MEK --> POD_M2
    SVC_EVO --> POD_E1
    SVC_EVO --> POD_E2
    POD_M1 --> RDS_MEK
    POD_M2 --> RDS_MEK
    POD_E1 --> RDS_EVO
    POD_E2 --> RDS_EVO
    POD_E1 --> REDIS
    POD_E2 --> REDIS

    class CLIENT,MOBILE external
    class EXT_API aws_network
    class IGW aws_network
    class ALB aws_compute
    class INGRESS,SVC_MEK,SVC_EVO,POD_M1,POD_M2,POD_E1,POD_E2 k8s
    class RDS_MEK,RDS_EVO aws_database
    class REDIS aws_cache
    class NAT aws_network
```

> **Nota:** A aplicação Mobile/Web (front-end) **não faz parte do escopo** deste projeto.  
> O WhatsApp é integrado apenas para funcionalidades limitadas: aprovação/reprovação de orçamento via webhook.  
> O acesso principal à API é feito via Internet (HTTP/HTTPS), consumido por clientes que utilizam os endpoints REST.

---

## Fluxo de Dados — WhatsApp

```mermaid
sequenceDiagram
    autonumber
    participant C as 🌐 Cliente WhatsApp
    participant WA as 📱 WhatsApp Cloud API
    participant EVO as 🟩 Evolution API
    participant MW as 🔀 Webhook Resource
    participant API as 🟦 Mekano API
    participant DB as 🗄️ RDS PostgreSQL

    rect rgb(30, 41, 59)
        Note over C, DB: Fluxo de Mensagem Recebida
        C->>WA: Envia mensagem (SIM/NÃO)
        WA->>EVO: POST /webhook
        EVO->>MW: POST /api/v1/webhooks/evolution
        MW->>MW: Valida token (x-webhook-token)
        alt Token inválido
            MW-->>WA: 401 Unauthorized
        else Token válido
            MW->>API: processarResposta(remoteJid, texto)
            API->>API: Normaliza texto (SIM/s/NÃO/n)
            API->>DB: @Transactional — atualiza status OS
            DB-->>API: OK
            API-->>MW: Orçamento atualizado
            MW-->>WA: 200 OK
        end
    end

    rect rgb(30, 41, 59)
        Note over C, DB: Fluxo de Notificação
        API->>API: Evento de Domínio (ex: DiagnosticoFinalizadoEvent)
        API->>EVO: notificarCliente(telefone, mensagem)
        EVO->>WA: Envia mensagem WhatsApp
        WA-->>C: Notificação recebida
    end
```

---

## Fluxo de Dados — HTTP API

```mermaid
sequenceDiagram
    autonumber
    participant U as 👤 Usuário
    participant ALB as ⚖️ ALB
    participant NGX as 🔀 NGINX
    participant API as 🟦 Mekano API
    participant DB as 🗄️ RDS PostgreSQL

    rect rgb(30, 41, 59)
        Note over U, DB: Requisição HTTP (GET/POST/PUT/DELETE)
        U->>ALB: HTTPS request
        ALB->>NGX: Forward (port 80)
        NGX->>API: Route by path (/api/v1/*)
        API->>API: @RolesAllowed validation
        API->>DB: Query/Update
        DB-->>API: Result
        API-->>U: JSON Response (RFC 7807 on error)
    end
```

---

## Componentes

| Componente | Tipo | Função |
|------------|------|--------|
| **Internet Gateway** | AWS Network | Ponto de entrada/saída da VPC para a internet |
| **Application Load Balancer** | AWS Compute | Distribui tráfego HTTPS entre pods EKS |
| **EKS Cluster** | AWS Compute | Kubernetes gerenciado — orquestra pods |
| **NGINX Ingress Controller** | K8s Ingress | Roteamento L7, TLS termination, rate limiting |
| **Mekano API (Pods)** | K8s Deployment | API REST Quarkus — gestão de oficina mecânica |
| **Evolution API (Pods)** | K8s Deployment | Gateway WhatsApp — mensagens e webhooks |
| **RDS PostgreSQL Mekano** | AWS Database | Banco principal — clientes, OS, peças, estoque |
| **RDS PostgreSQL Evolution** | AWS Database | Banco Evolution API — instâncias WhatsApp |
| **ElastiCache Redis** | AWS Cache | Cache Evolution API — sessões e mensagens |
| **NAT Gateway** | AWS Network | Saída de rede para pods em subnets privadas |
| **WhatsApp Cloud API** | External | API oficial Meta para mensagens WhatsApp |

---

## Decisões de Arquitetura

| Decisão | Justificativa |
|---------|---------------|
| **EKS (não ECS)** | Flexibilidade de scaling, suporte a NGINX Ingress, Helm charts |
| **RDS Multi-AZ (Mekano)** | Alta disponibilidade para banco principal |
| **RDS Single-AZ (Evolution)** | Custo-eficiência — dados de sessão, não-críticos |
| **ElastiCache (não RDS)** | Performance para cache de sessões WhatsApp |
| **NGINX Ingress** | Rate limiting, TLS, roteamento por path |
| **Subnets de DB isoladas** | Segurança — acesso apenas via security groups internos |
| **NAT Gateway em Private Subnet** | Pods privados acessam internet (updates, webhooks externos) |
