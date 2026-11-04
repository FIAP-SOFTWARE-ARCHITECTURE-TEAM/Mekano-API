# Sequência — Autenticação por CPF (Lambda → JWT)

Requisito: proteger rotas sensíveis com autenticação **via CPF**, através de function serverless que valida o CPF, consulta o cliente na base e devolve um JWT.

> Implementação: [Mekano-Serverless](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-Serverless) · Decisão: [RFC-001](RFC-001-autenticacao-cpf-lambda.md)

```mermaid
sequenceDiagram
    autonumber
    actor C as Cliente (oficina)
    participant GW as API Gateway HTTP<br/>(POST /auth/token)
    participant L as Lambda auth<br/>(Quarkus Java 17)
    participant SM as Secrets Manager<br/>(JWT private key)
    participant DB as RDS PostgreSQL<br/>(via VPC peering)
    participant API as Mekano API (EKS)<br/>rotas @RolesAllowed

    C->>GW: POST /auth/token {"cpf": "123.456.789-09"}
    GW->>L: invoke (payload v2.0)

    Note over L: 1. Validação do CPF
    L->>L: normaliza (só dígitos) + dígito verificador
    alt CPF inválido
        L-->>GW: 400 {"code": "CPF_INVALID"}
        GW-->>C: 400 CPF_INVALID
    end

    Note over L: 2. Consulta do cliente na base
    L->>SM: GetSecretValue(mekano/<env>/database)
    SM-->>L: credenciais do banco
    L->>DB: SELECT ... FROM clientes WHERE cpf = :cpf
    alt cliente não existe
        L-->>C: 404 CUSTOMER_NOT_FOUND
    else cliente inativo/bloqueado
        L-->>C: 403 CUSTOMER_INACTIVE
    end

    Note over L: 3. Emissão do JWT
    L->>SM: GetSecretValue(mekano/lambda-auth/<env>/jwt-private-key)
    SM-->>L: Ed25519 private key (PEM)
    L->>L: JWT Ed25519 { sub=cpf, iss=mekano-auth,<br/>aud=mekano-api, exp=+15min }
    L-->>GW: 200 {"token": "..."}
    GW-->>C: 200 {"token": "eyJ..."}

    Note over C,API: Consumo das rotas protegidas
    C->>API: GET /api/v1/...<br/>Authorization: Bearer eyJ...
    API->>API: valida assinatura com public key<br/>(MP_JWT_VERIFY_PUBLICKEY_LOCATION)<br/>+ exp/aud
    API-->>C: 200 recurso
    C->>API: request sem token
    API-->>C: 401 Unauthorized (RFC 7807)
```

## Pontos de segurança

| Aspecto | Tratamento |
|---------|-----------|
| Chave de assinatura | Ed25519, privada **apenas no Secrets Manager** — nunca em código/repo; API conhece só a pública |
| Exposição do banco | Lambda dentro da VPC do app; RDS privado; acesso 5432 restrito ao CIDR da VPC (peering) |
| Validade do token | 15 min (`exp`), claims `iss`/`aud` verificados pela API |
| Entrada maliciosa | CPF normalizado/validado antes de qualquer query (prepared statement) |
| Throttling | API Gateway: 25 rps / burst 50 |
| Auditoria | Falhas de autenticação aparecem nos logs JSON com `correlationId` (dashboard/alertas da observabilidade) |

## Status

- ✅ Código + testes (14 testes), Terraform, API Gateway e smoke test de deploy prontos no repo Mekano-Serverless (branch `feature/mekano-serveless-lb`);
- ⚠️ Pendente: merge para `main` (responsável do repo) + secrets `mekano/<env>/database` e `mekano/lambda-auth/<env>/jwt-private-key` no Secrets Manager — ver [RFC-001](RFC-001-autenticacao-cpf-lambda.md).
