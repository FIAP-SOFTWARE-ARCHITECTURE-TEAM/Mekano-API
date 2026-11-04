# RFC-003 — Nó único `t3.small` no Free Tier

- Status: Aceito (restrição de custo, temporária)
- Data: 2026-10

## Background

A conta AWS utilizada (giovanni-admin) está no **novo AWS Free Tier**, que restringe os tipos de instância EC2 a uma lista *free-tier-eligible* (`t3.small`, `t3.micro`, `t4g.small`, `m7i-flex.large`, etc.). A primeira tentativa de provisionamento com `t3.medium` falhou (`InvalidParameterCombination: not eligible for Free Tier`).

## Proposta

Operar o EKS com **um único nó `t3.small`** (2 vCPU / 2 GiB), ajustando a pegada de memória:

| Ajuste | Valor |
|--------|-------|
| Node group | `t3.small`, `min 1 / desired 1 / max 2` |
| Addons `coredns`, `ebs-csi-controller` | 1 réplica cada (`configurationValues` do addon) |
| `metrics-server` | 1 réplica (`kubectl scale` no deploy) |
| Mekano API | request `200m/384Mi`, 1 réplica inicial, HPA `1–4` |
| Evolution API | request `100m/256Mi` (limite inalterado 1000m/1Gi) |
| Banco `evolution` | criado idempotentemente no deploy (o RDS só cria `mekano`) |

## Alternativas consideradas

| Alternativa | Por que não |
|-------------|-------------|
| `m7i-flex.large` (8 GiB) | Mais folha de memória, porém consome o crédito mensal do Free Tier (~US$ 35–40) |
| `t3.micro` | 1 GiB — insuficiente mesmo com requests mínimos |
| 2× `t3.small` | Dobra as horas EC2 além da franquia de 750h/mês |

## Resultado

- ✅ Ambiente funcional dentro da franquia (comprovado: deploy verde com API pública e banco conectado);
- ⚠️ **Nó único = ponto único de falha** — aceito para o estágio atual; aumentar `maxReplicas` do HPA + `maxSize` do node group + trocar para `m7i-flex.large` é a evolução natural ao sair do Free Tier;
- ⚠️ Evolução do app exige memória: limites mantidos em 1 GiB (request é só scheduling).
