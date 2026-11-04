# RFC-002 — Banco em VPC dedicada com VPC peering

- Status: Aceito
- Data: 2026-10
- Implementação: repo [Mekano-Infrastructure-Database](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-Infrastructure-Database)

## Background

O desafio pede a segregação da infraestrutura em repositórios independentes, incluindo o banco gerenciado. A primeira versão do Terraform mantinha VPC, EKS e RDS num **state único** no monorepo — acoplava o ciclo de vida do banco ao da aplicação e concentrava risco (um erro de state podia destruir o banco).

## Proposta

Separar o banco em **repositório e VPC próprios**, conectados por **VPC peering**:

| Item | Valor |
|------|-------|
| VPC do banco | `10.30.0.0/16` (repo Mekano-Infrastructure-Database) — subnets dedicadas `10.30.192.0/20` e `10.30.208.0/20` |
| VPC da aplicação | `10.20.0.0/16` (repo Mekano-Infrastructure-K8s) — CIDRs não se sobrepõem |
| Peering | `pcx-…`, mesmo account/region, criado pelo repo do banco (`auto_accept`) com `peer_vpc_id` da VPC do EKS |
| Rotas | Lado do banco: criado pelo repo do banco; lado do app: rotas `10.30.0.0/16` nas private route tables, apontando para o peering (data source localiza o peering **ativo** por `accepter-vpc-info.vpc-id` + `requester-vpc-info.cidr-block`) |
| Acesso | SG do RDS permite `5432` apenas do CIDR `10.20.0.0/16` (via peering); `publicly_accessible = false` |

## Alternativas consideradas

| Alternativa | Por que não |
|-------------|-------------|
| Tudo numa VPC (state único) | Acoplamento de ciclo de vida; risco de destruição do banco em applies da aplicação |
| RDS público com SG restrito | Superfície de ataque maior; viola boas práticas de dados |
| PrivateLink / Transit Gateway | Custo adicional sem benefício para 2 VPCs na mesma conta/região (overkill) |
| State surgery do monorepo (`terraform state mv`) | Risco alto de erro operacional no banco de produção; abandonado em favor de provisionamento novo |

## Resultado

- ✅ Ciclos de vida independentes: banco sobrevive a rebuilds do cluster;
- ✅ State, bucket e pipeline isolados por repositório (blast radius mínimo);
- ✅ Tráfego privado ponta a ponta (pod → peering → RDS), sem internet;
- ⚠️ Sequência de bootstrap é ordem-dependente: (1) apply do K8s → (2) apply do DB com `peer_vpc_id` → (3) re-apply do K8s com rotas — documentada no runbook de cada repo;
- ⚠️ Em contas diferentes no futuro, o peering passa a exigir aceitação cruzada e formato de sub diferente — ajuste pontual.
