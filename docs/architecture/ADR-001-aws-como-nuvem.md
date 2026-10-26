# ADR-001 — AWS como provedor de nuvem

- Status: Aceito
- Data: 2026-10
- Decisores: equipe Mekano (Tech Challenge FIAP — Software Architecture)

## Contexto

A oficina expandiu para múltiplas unidades e o sistema precisa de segurança, escalabilidade e alta disponibilidade. O desafio exige API Gateway, função serverless, banco gerenciado, cluster Kubernetes com escalabilidade e infraestrutura como código — tudo com deploy automático.

## Opções consideradas

| Opção | Prós | Contras |
|-------|------|---------|
| **AWS** | Maior maturidade de serviços gerenciados (EKS, RDS, Lambda, Secrets Manager); créditos Free Tier renovados; material e comunidade abundantes; IAM/OIDC para CI/CD sem chaves estáticas | Console complexo; billing exige atenção |
| Azure | Integração com ecossistema Microsoft; AKS sólido | Equipe sem experiência prévia; Free Tier menos generoso para EKS-equivalente |
| GCP | GKE é referência técnica | Menos familiar à equipe; créditos menores para o cenário |
| On-premise/VM | Controle total | Sem elasticidade; exige operação de datacenter — contrário ao objetivo |

## Decisão

Adotar **AWS** como provedor único, com:

- **EKS** para a aplicação (Kubernetes gerenciado);
- **RDS PostgreSQL 16** como banco gerenciado;
- **Lambda + API Gateway HTTP** para a função serverless de autenticação;
- **Secrets Manager/SSM** para segredos e configuração;
- **Terraform** como IaC em todos os repos de infraestrutura;
- **GitHub Actions com OIDC** (`role-to-assume`) — **zero chaves AWS estáticas** nos repositórios.

## Consequências

- ✅ Todos os requisitos obrigatórios têm serviço gerenciado correspondente;
- ✅ CI/CD sem credenciais de longa duração (trust por `repo:`+`ref:` no subject do OIDC);
- ✅ Composição da organização já validada (observabilidade roda em ECS na mesma conta);
- ⚠️ Conta em Free Tier restringe tipos de instância EC2 — ver [RFC-003](RFC-003-free-tier-single-node.md);
- ⚠️ Custo do EKS control plane é coberto apenas parcialmente por créditos — monitorar.
