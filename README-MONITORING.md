# Mekano — Monitoramento e Observabilidade

Documentação da stack de observabilidade do Mekano-API: **Prometheus** (métricas), **Grafana Alloy** e **Loki** (logs) e **Grafana** (visualização e alertas). Todos os serviços fazem parte do `docker-compose.yml` na raiz do repositório.

## Arquitetura

```mermaid
flowchart TD
    subgraph PLATAFORMA["Plataforma Mekano"]
        API["MEKANO API<br/>(Quarkus / Micrometer)"]
    end

    subgraph COLETA["Coleta"]
        PROM
        ALLOY
    end

    subgraph ARMAZENAMENTO["Armazenamento"]
        LOKI
    end

    subgraph VISUALIZACAO["Visualização & Alertas"]
        GRAFANA
        DASH["Dashboards"]
        ALERT["Alertas<br/>(email)"]
    end

    API -->|"Metricas<br/>/q/metrics"| PROM[("Prometheus")]
    API -->|"Logs JSON<br/>(stdout)"| ALLOY[Alloy]
    ALLOY -->|"Loki push API"| LOKI[("Loki")]
    PROM --> GRAFANA
    LOKI --> GRAFANA
    GRAFANA --> DASH
    GRAFANA --> ALERT
```

O Mekano-API expõe métricas Micrometer em `/q/metrics` (via `quarkus-micrometer-registry-prometheus`) e emite logs estruturados em JSON (`quarkus.log.console.json=true`), coletados pelo Alloy a partir do socket do Docker.

## Componentes da stack

| Componente | Papel | Porta | Arquivo de configuração |
|------------|-------|-------|--------------------------|
| Prometheus | Coleta métricas do endpoint `/q/metrics` (scrape a cada 15s) | 9090 | `infra_metrics_monitoring/prometheus/prometheus.yml` |
| Grafana Alloy | Descobre containers Docker e coleta logs | 12345 | `infra_metrics_monitoring/alloy/config.alloy` |
| Loki | Armazena os logs enviados pelo Alloy | 3100 | config embutida (`local-config.yaml`) |
| Grafana | Dashboards, datasources e alertas | 3000 | `infra_metrics_monitoring/grafana/provisioning/` |

## Configuração e provisionamento

- **Prometheus** — `infra_metrics_monitoring/prometheus/prometheus.yml`: job `mekano-api` apontando para `mekano:8080/q/metrics`.
- **Alloy** — `infra_metrics_monitoring/alloy/config.alloy`: descoberta via `discovery.docker`, labels `job=docker` e `service_name` (nome do container), encaminhamento para a API push do Loki.
- **Grafana datasources** — `infra_metrics_monitoring/grafana/provisioning/datasource.yml`: **Prometheus** (data source padrão) e **Loki**.
- **Grafana dashboards** — `infra_metrics_monitoring/grafana/provisioning/dashboards.yml`: auto-provisioning na pasta **MEKANO API** a partir da pasta `grafana/dashboards/`.
- **Grafana alertas** — `infra_metrics_monitoring/grafana/provisioning/alerts/alerting.yml` e `contact-points.yml`.

## Pipeline em produção (EKS)

No EKS (`mekano-eks`), os logs dos pods chegam ao Loki da stack de observabilidade
(tasks Fargate: `loki`, `alloy`, `grafana`, `prometheus`) através de um **Alloy
DaemonSet** (`k8s/alloy.yaml`) instalado no namespace `mekano-system`:

```mermaid
flowchart LR
    P1["pods mekano / evolution-api<br/>(logs em /var/log/pods)"] --> DS["Alloy DaemonSet<br/>(DaemonSet k8s/alloy.yaml)"]
    DS -->|"POST /loki/api/v1/push<br/>header authorization: Bearer"| ALB["ALB alb-observability<br/>rota /loki/*"]
    ALB --> A2["Alloy (Fargate)<br/>loki.source.api :8080"]
    A2 --> L2[("Loki (Fargate)")]
    L2 --> G2["Grafana (Fargate)"]
```

Detalhes:

- **Labels por log**: `namespace`, `pod`, `container`, `service_name` e `job`
  (extraídos do caminho `/var/log/pods/<ns>_<pod>_<uid>/<container>/…`).
  O container `mekano` recebe `service_name="mekano-api"` para manter os painéis
  provisionados do Grafana funcionando sem alteração.
- **Autenticação**: o push exige o header `authorization: Bearer <ALLOY_INGEST_TOKEN>`
  validado por regra do ALB. O token é o mesmo do Secrets Manager
  `mekano/observability/alloy-ingest-token` e é distribuído ao cluster pelo CD
  como Secret `alloy-ingest-token` (GitHub Secret `ALLOY_INGEST_TOKEN`).
- **`base64=false` nos logs**: o Mekano já loga JSON no stdout; o `stage.cri` do
  pipeline remove o prefixo CRI do containerd para o `| json` funcionar no LogQL.
- **Verificação de saúde**: métricas do pod do Alloy em `:12345/metrics` —
  `loki_write_sent_entries_total` deve crescer e `loki_write_dropped_*_total`
  deve permanecer em zero.
- **Troubleshooting comum**: dashboard de logs vazio = DaemonSet do Alloy não
  existe/atualizado no cluster (ex.: após rebuild do cluster, o agente precisa
  ser recriado — o CD faz isso a partir de `k8s/alloy.yaml`).

## Dashboards

Pasta: `infra_metrics_monitoring/grafana/dashboards/`

| Dashboard | Arquivo | Conteúdo |
|-----------|---------|----------|
| `dashboard_metrics` | `dashboard_metrics.json` | Infra do servidor: CPU, memória, GC, threads, pool de conexões, SLA e uptime |
| `OS FLOW TRAFFIC` | `os_flow_monitoring.json` | Tráfego HTTP (req/s, latência, taxas 400/500) e fluxo de ordens de serviço (criadas, transições, tempo por etapa) |
| `LOKI` | `loki.json` | Logs estruturados por camada (OS, Orçamento, Req. Compra/NFe, WhatsApp) e métricas por level (INFO/WARN/ERROR) |

A especificação do conteúdo de cada painel está documentada em `grafana/dashboards/estrutura_dashboard.md`.

## Alertas

Regras provisionadas no `alerting.yml` (grupo de avaliação de 1 min), notificando por email:

| Alerta | Condição |
|--------|----------|
| MEKANO-API - TAXA HTTP 400 | Taxa de erros 400 ≥ 20% na última hora |
| MEKANO-API - TAXA HTTP 500 | Taxa de erros 500 ≥ 5% na última hora |
| MEKANO-API - SYSTEM DOWN | Métrica `up` igual a zero (API fora do ar) |

Destinatário configurado em `contact-points.yml` (*Dev - Sistema de Alertas*). O envio de email usa as variáveis `GF_SMTP_*` definidas no `docker-compose.yml`.

## Como executar

Todos os serviços de observabilidade sobem junto com a stack:

```powershell
docker compose up -d
```

Acessos:

| Serviço | URL |
|---------|-----|
| Grafana (dashboards/alertas) | http://localhost:3000 |
| Prometheus (métricas) | http://localhost:9090 |
| Loki (API de logs) | http://localhost:3100 |
| Alloy (UI de depuração) | http://localhost:12345 |