# 02 — Diagramas de Sequência

# 1. Autenticação do Cliente por CPF

## Objetivo

Representar o fluxo de autenticação usando API Gateway, Lambda, PostgreSQL e JWT.

```mermaid
sequenceDiagram
    autonumber

    actor C as Cliente
    participant GW as API Gateway
    participant L as Lambda Auth
    participant DB as RDS PostgreSQL
    participant API as Mekano API

    C->>GW: POST /auth/token {"cpf":"529.982.247-25"}
    GW->>L: Invoca Lambda

    L->>L: Normaliza CPF
    L->>L: Valida dígitos verificadores

    alt CPF inválido
        L-->>GW: 400 CPF_INVALID
        GW-->>C: 400 Bad Request
    else CPF válido
        L->>DB: SELECT auth_customer_view WHERE cpf = ?

        alt Cliente inexistente ou inativo
            DB-->>L: Cliente não autenticável
            L-->>GW: 401 AUTHENTICATION_FAILED
            GW-->>C: 401 Unauthorized
        else Cliente ativo
            DB-->>L: customer_id, nome, status, roles
            L->>L: Gera JWT Ed25519
            L-->>GW: 200 access_token
            GW-->>C: JWT Bearer
        end
    end

    C->>GW: GET /api/v1/... + Authorization Bearer
    GW->>API: VPC Link → NLB → Ingress

    API->>API: Valida assinatura, expiração, audience e roles

    alt Token válido
        API-->>C: 200
    else Token inválido ou sem permissão
        API-->>C: 401 / 403
    end
```

## Resumo do fluxo

```text
Cliente informa CPF
        ↓
API Gateway
        ↓
Lambda Auth
        ↓
Validação matemática
        ↓
Consulta PostgreSQL
        ↓
Cliente ativo
        ↓
JWT Ed25519
        ↓
Cliente usa Bearer Token
        ↓
API Gateway
        ↓
Mekano API
        ↓
Validação JWT + Roles
```

---

# 2. Abertura de Ordem de Serviço

## Objetivo

Representar o fluxo da criação da Ordem de Serviço desde a entrada pública até a persistência.

```mermaid
sequenceDiagram
    autonumber

    actor A as Atendente/Admin
    participant GW as API Gateway
    participant ING as Ingress NGINX
    participant REST as OrdemDeServicoResource
    participant APP as OrdemDeServicoService
    participant DOM as OrdemDeServico
    participant REP as OrdemDeServicoRepository
    participant DB as PostgreSQL

    A->>GW: POST /api/v1/os + Bearer JWT
    GW->>ING: VPC Link → NLB
    ING->>REST: POST /api/v1/os

    REST->>REST: Valida autenticação e roles
    REST->>APP: criarOS(command)

    APP->>APP: Valida clienteUuid
    APP->>APP: Valida veiculoUuid

    alt Cliente ou veículo inexistente
        APP-->>REST: AppException 404
        REST-->>A: 404 Problem Details
    else Dados consistentes
        APP->>DOM: OrdemDeServico.create(...)
        DOM-->>APP: OS status RECEBIDA

        APP->>REP: save(os)
        REP->>DB: INSERT ordens_de_servico

        DB-->>REP: OS persistida + version
        REP-->>APP: OrdemDeServico

        APP->>APP: Publica OrdemDeServicoCriadaEvent
        APP-->>REST: OrdemDeServicoResponse

        REST-->>A: 201 Created + Location
    end
```

## Resumo

```text
Atendente/Admin
       ↓
API Gateway
       ↓
VPC Link
       ↓
NLB
       ↓
Ingress NGINX
       ↓
OrdemDeServicoResource
       ↓
OrdemDeServicoService
       ↓
Valida Cliente e Veículo
       ↓
Cria OS com status RECEBIDA
       ↓
Repository
       ↓
PostgreSQL
       ↓
201 Created
```

## Estado inicial da OS

A nova Ordem de Serviço é criada com:

```text
RECEBIDA
```

e segue o fluxo de negócio posteriormente para diagnóstico, orçamento, execução, finalização e entrega.
