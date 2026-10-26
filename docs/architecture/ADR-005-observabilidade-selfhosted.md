# ADR-005 — Observabilidade self-hosted (Alloy + Loki + Prometheus + Grafana)

- Status: Aceito
- Data: 2026-10

## Contexto

Requisitos de monitoramento: latência das APIs, consumo de recursos do Kubernetes, healthchecks/uptime, alertas de falha no processamento de OS, logs estruturados JSON com correlação e dashboards (volume diário de OS, tempo médio por status, erros de integração).

## Opções consideradas

| Opção | Avaliação |
|-------|-----------|
| **Self-hosted: Grafana + Prometheus + Loki + Alloy** | ✅ Custo ~zero (EC2/ECS pequeno), stack aberta amplamente conhecida, dashboards provisionados como código, alertas por e-mail |
| Datadog | Excelente produto, porém **custo por host/GB proibitivo** para conta em Free Tier |
| New Relic | Mesmo problema de licenciamento |
| CloudWatch puro | Coberto pelo desafio, mas dashboards/logs estruturados ficam caros e menos flexíveis |

## Decisão

Stack self-hosted em repositório dedicado ([Mekano-infrastructure-Observability](https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-infrastructure-Observability)):

- **Prometheus** coleta `/q/metrics` (Micrometer) da API a cada 15s;
- **Alloy** coleta logs JSON do stdout dos containers (labels `service_name`) e faz remote_write/loki push;
- **Loki** armazena logs com **correlação por `correlationId`** (emitido pela API em cada request, presente nos logs e no header de resposta);
- **Grafana** com 4 dashboards provisionados como código (`system_metrics`, `OS FLOW TRAFFIC`, `LOKI`, `Evolution API — Integração WhatsApp`);
- **5 regras de alerta provisionadas** (`alerting.yml`) → e-mail: API fora do ar, falhas no processamento de OS, falhas na integração Evolution/WhatsApp, taxa 5xx > 5%, latência p95 > 2s;
- Métricas do **cluster Kubernetes** (CPU/memória por pod) coletadas por agente Alloy no EKS via `remote_write` (kube-state-metrics + cAdvisor).

## Consequências

- ✅ Todos os requisitos de monitoramento/dashboards/alertas atendidos com custo marginal;
- ✅ Provisionamento 100% como código (reprodutível);
- ⚠️ Operação da stack é responsabilidade da equipe (atualizações, storage do Loki);
- ⚠️ Métricas de infraestrutura do cluster limitadas ao nó único atual (RFC-003).
