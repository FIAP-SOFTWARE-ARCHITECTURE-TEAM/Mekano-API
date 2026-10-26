# RFC-001 — Autenticação por CPF com Lambda serverless e JWT

- Status: Aceito
- Data: 2026-10
- Implementação: repo [Mekano-Serverless](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-Serverless)

## Background

O requisito do Tech Challenge exige: proteger rotas sensíveis com autenticação **via CPF**, com uma **function serverless** que (1) valide o CPF, (2) consulte a existência e o status do cliente na base e (3) emita um token JWT para consumo das APIs protegidas. A API já possuía login usuário/senha (`/auth/login`) com JWT — o fluxo CPF é complementar, voltado ao cliente da oficina.

## Proposta

Arquitetura **API Gateway HTTP + Lambda** (Java 17/Quarkus, mesmo runtime da API):

1. `POST https://<api-id>.execute-api…/auth/token` com `{"cpf": "..."}`;
2. Lambda valida o CPF (dígito verificador + normalização) — entrada inválida → `400 CPF_INVALID`;
3. Lambda consulta o cliente no PostgreSQL via **JDBC** (peering Lambda → RDS; apenas clientes com status `ATIVO/ACTIVE` autenticam) — cliente inexistente/inativo → `404/403 CUSTOMER_*`;
4. Lambda assina um **JWT Ed25519** (RS/EdDSA family) com claims `sub=cpf`, `iss=mekano-auth`, `aud=mekano-api`, TTL de 15 min — chave privada vinda do **Secrets Manager** (`mekano/lambda-auth/<env>/jwt-private-key`);
5. A Mekano API valida o JWT com a **chave pública correspondente** (`MP_JWT_VERIFY_PUBLICKEY_LOCATION`) — mesmo par de chaves já distribuído no secret do cluster.

## Decisões relevantes

| Tema | Decisão | Justificativa |
|------|---------|---------------|
| Runtime | Java 17 + Quarkus `QuarkusStreamHandler` | Reuso do domínio/testes do projeto; SnapStart mitigaria cold start (pendente) |
| Trigger | API Gateway **HTTP API** (não REST API) | Latência/preço menores; CORS nativo |
| Chave de assinatura | Ed25519 no Secrets Manager | Chaves curtas e rápidas; rotação via Secrets Manager |
| Consulta ao banco | JDBC direto na view/tabela de clientes | Evita chamada HTTP em cadeia; Lambda dentro da VPC |
| Rotas protegidas | `@RolesAllowed` na API (já implementado) | Autorização continua na Mekano API; Lambda só autentica |

## Fluxo detalhado

Ver [sequencia-autenticacao-cpf.md](sequencia-autenticacao-cpf.md).

## Resultado

- ✅ CPF validado + cliente consultado + JWT devolvido — requisito atendido;
- ✅ Segredos fora do código (Secrets Manager);
- ⚠️ **Pendente no momento**: merge do trabalho do colega responsável (`feature/mekano-serveless-lb`) em `develop`/`main` + deploy efetivo (requer secrets `mekano/<env>/database` e a role OIDC do repo);
- ⚠️ Cold start Java (~ segundos) aceitável para o fluxo de login; SnapStart como evolução.
