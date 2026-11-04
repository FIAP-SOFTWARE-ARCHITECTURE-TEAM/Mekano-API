# 03 — RFCs e ADRs

# Parte A — RFCs

## RFC-001 — AWS como Plataforma de Nuvem

- **Status:** Aceito

### Contexto

A solução exige:

- Kubernetes;
- serverless;
- API Gateway;
- banco gerenciado;
- observabilidade;
- CI/CD;
- infraestrutura como código.

### Proposta

Adotar AWS utilizando:

- Amazon EKS;
- Amazon RDS;
- AWS Lambda;
- Amazon API Gateway;
- CloudWatch;
- SSM;
- IAM;
- GitHub Actions + OIDC;
- Terraform.

### Alternativas

- Azure;
- GCP;
- infraestrutura local.

### Decisão

AWS foi escolhida pela integração dos serviços, aderência aos requisitos e familiaridade da equipe.

---

## RFC-002 — PostgreSQL 16 no Amazon RDS

- **Status:** Aceito

### Contexto

O domínio possui forte necessidade de consistência:

- OS;
- orçamento;
- estoque;
- reserva de peças;
- requisição de compra;
- nota fiscal;
- pagamento;
- auditoria.

### Proposta

PostgreSQL 16 no Amazon RDS.

### Alternativas

- MySQL;
- DynamoDB;
- MongoDB/DocumentDB;
- PostgreSQL autogerenciado.

### Justificativa

PostgreSQL fornece:

- ACID;
- constraints;
- índices;
- joins;
- UUID;
- JSONB;
- optimistic locking;
- integração com Hibernate/Panache;
- Flyway;
- Testcontainers/DevServices.

### Decisão

PostgreSQL 16 gerenciado no RDS.

---

## RFC-003 — Autenticação por CPF com Lambda e JWT

- **Status:** Aceito

### Contexto

Clientes precisam autenticar usando CPF sem depender do login administrativo.

### Proposta

```text
POST /auth/token
       ↓
API Gateway
       ↓
Lambda Auth
       ↓
Validação CPF
       ↓
PostgreSQL
       ↓
JWT Ed25519
```

### Fluxo

1. cliente envia CPF;
2. Gateway recebe;
3. Gateway invoca Lambda;
4. Lambda valida CPF;
5. consulta `auth_customer_view`;
6. verifica status;
7. gera JWT;
8. cliente usa JWT nas APIs protegidas.

### Decisões

- Java 17 + Quarkus;
- AWS Lambda;
- API Gateway HTTP;
- JDBC;
- JWT Ed25519;
- autorização permanece na Mekano API.

---

# Parte B — ADRs

## ADR-001 — AWS como Provedor

- **Status:** Aceito

### Decisão
AWS é o provedor principal.

### Serviços
- EKS;
- RDS;
- Lambda;
- API Gateway;
- CloudWatch;
- SSM;
- IAM/OIDC.

### Consequências
- integração nativa;
- IaC;
- CI/CD sem Access Keys permanentes;
- necessidade de controle de custos.

---

## ADR-002 — PostgreSQL 16 no RDS

- **Status:** Aceito

### Decisão

PostgreSQL 16 no Amazon RDS.

### Configuração

```text
publicly_accessible = false
porta = 5432
criptografia = habilitada
```

### Consequências
- consistência;
- banco privado;
- menor carga operacional;
- possibilidade futura de Multi-AZ.

---

## ADR-003 — EKS + Horizontal Pod Autoscaler

- **Status:** Aceito

### Decisão

EKS com HPA `autoscaling/v2`.

### Configuração atual

```yaml
minReplicas: 1
maxReplicas: 4

cpu:
  averageUtilization: 70

memory:
  averageUtilization: 80
```

### Consequências
- elasticidade;
- padrão Kubernetes;
- dependência da capacidade dos nodes.

---

## ADR-004 — REST Síncrono + Eventos Internos

- **Status:** Aceito

### Comunicação externa

```text
HTTP / REST
```

### Comunicação interna

```text
Domain Events / CDI
```

### Motivo

Evitar Kafka/SQS sem necessidade atual.

### Consequências
- simplicidade;
- transações locais;
- menor custo;
- possibilidade futura de evolução para mensageria.

---

## ADR-005 — Observabilidade

- **Status:** Aceito

### Decisão

```text
Métricas:
Mekano API /q/metrics
        ↓
Prometheus
        ↓
Grafana

Logs:
Pods
 ↓
Alloy
 ↓
Alloy Gateway
 ↓
Loki
 ↓
Grafana
```

### Labels

```text
namespace=mekano-system
service_name=mekano-api
service_name=evolution-api
```

### Consequências
- observabilidade centralizada;
- dashboards versionados;
- stack self-hosted exige operação.

---

## ADR-006 — VPC Compartilhada

- **Status:** Aceito para a arquitetura atual

### Contexto

Documentos antigos descreviam VPC Peering.

O estado atual do repositório de Database define EKS, Lambda e RDS na mesma VPC.

### Decisão

```text
VPC Mekano 10.20.0.0/16
├── EKS
├── Lambda Auth
└── RDS PostgreSQL
```

### Isolamento

- subnets;
- Security Groups;
- RDS privado;
- NLB interno;
- VPC Link.

### Benefícios
- menor complexidade;
- elimina peering desnecessário;
- tráfego privado;
- states Terraform continuam independentes.
