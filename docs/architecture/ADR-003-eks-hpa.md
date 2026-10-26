# ADR-003 — Amazon EKS com HPA (Horizontal Pod Autoscaler)

- Status: Aceito
- Data: 2026-10

## Contexto

O requisito pede "cluster Kubernetes com escalabilidade" e a aplicação enfrenta picos de uso por múltiplas unidades da oficina. O autoscaling precisa reagir a carga real (CPU/memória dos pods) sem intervenção manual.

## Opções consideradas

| Opção | Avaliação |
|-------|-----------|
| **EKS + HPA** | ✅ Kubernetes padrão de mercado; HPA nativo (metrics-server); node groups gerenciados; integra com ALB via ingress-nginx |
| ECS Fargate | Mais simples, mas o desafio pede explicitamente Kubernetes; menos portabilidade |
| EC2 com docker-compose | Sem autoscaling horizontal nativo — não atende |
| Karpenter/KEDA | Auto-scaling avançado — overkill para o estágio atual (YAGNI) |

## Decisão

**EKS 1.34** com node group gerenciado e **HPA v2** na Mekano API:

- Métrica: utilização média de **CPU 70%** e memória **80%**;
- Limites atuais (Free Tier, ver [RFC-003](RFC-003-free-tier-single-node.md)): `minReplicas: 1`, `maxReplicas: 4`, nó único `t3.small`;
- `metrics-server` instalado como addon do EKS;
- `progressDeadlineSeconds: 600` + `kubectl rollout undo` automático em falha de deploy.

## Consequências

- ✅ Escalabilidade horizontal real, demonstrável (stress test + `kubectl get hpa`);
- ✅ HPA é decisão permanente — crescer para múltiplos nós é só aumentar `maxReplicas` + node group;
- ⚠️ Com nó único atual, o scale horizontal é limitado pela memória do nó (documentado na RFC-003);
- ⚠️ `metrics-server` em 1 réplica (economia) — métricas de HPA continuam funcionais.
