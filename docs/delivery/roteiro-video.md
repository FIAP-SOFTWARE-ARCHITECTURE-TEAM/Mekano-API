# Roteiro do Vídeo — Tech Challenge (≤ 15 min)

> Gravação sugerida: OBS Studio ou Google Meet gravando tela.
> Ordem pensada para contar uma história: **arquitetura → CI/CD → produto no ar → observabilidade → autenticação → encerramento**.
> URLs vivas: API `http://a76974e53d9b94e98ad3b8f2408294e5-334902849.us-east-1.elb.amazonaws.com` · Grafana `http://alb-observability-133760226.us-east-1.elb.amazonaws.com`

## Pré-gravação (checklist)

- [ ] Aba 1: `docs/architecture/README.md` (diagrama de componentes)
- [ ] Aba 2: GitHub Actions da Mekano-API (última run verde)
- [ ] Aba 3: GitHub Actions do Mekano-Infrastructure-K8s (deploy verde)
- [ ] Aba 4: Swagger público (`<ALB>/q/swagger-ui`) + terminal com curl/Postman
- [ ] Aba 5: Grafana (`alb-observability…`) — dashboards `OS FLOW TRAFFIC`, `system_metrics`, `LOKI`, `Evolution API`
- [ ] Aba 6: repo Mekano-Serverless (código da Lambda) — se o merge já tiver acontecido, incluir a demo de `POST /auth/token`
- [ ] Terminal pronto com `kubectl`/`aws` para evidências rápidas (opcional)

---

## 0:00–1:00 · Abertura e arquitetura

**Tela:** `docs/architecture/README.md` (diagrama de componentes)

> "Este é o **Mekano**, sistema de gestão de ordens de serviço de uma oficina que expandiu para múltiplas unidades. O desafio exigiu segurança, escalabilidade, alta disponibilidade e visibilidade total. A solução está organizada em **4 repositórios**: a API Quarkus, a Lambda de autenticação, a infraestrutura Kubernetes e a infraestrutura do banco — cada um com CI/CD próprio e deploy automático na AWS."

## 1:00–3:00 · Infraestrutura como código

**Tela:** repos de infra + `terraform` (pode mostrar trecho do `main.tf` e outputs)

> "Toda a infraestrutura é Terraform: uma **VPC de aplicação (10.20)** com EKS 1.34, uma **VPC dedicada ao banco (10.30)** com RDS PostgreSQL 16, conectadas por **VPC peering** — o tráfego do banco nunca sai da rede privada. O deploy é feito por pipelines: `plan` nos PRs e `apply` na main, autenticados por **OIDC — nenhuma chave da AWS fica nos repositórios**."

**Evidência rápida (opcional):** `terraform output` do K8s mostrando `cluster_name`, `configure_kubectl`.

## 3:00–5:30 · CI/CD e deploy automatizado

**Tela:** aba 2 (Actions da API) → disparar ou mostrar a última run; aba 3 (Actions do K8s, run de deploy verde)

> "Aqui está o ciclo completo. Um push na main da API dispara: build, testes (mais de 500) e publicação da imagem no GHCR. Do lado do repositório de infra, o pipeline roda `terraform plan` e **aplica na main**, e o job de deploy faz `kubectl apply` com **rollout status e rollback automático**. Este run abaixo é real: a API foi implantada no EKS com sucesso." *(mostrar o step `Verify deployment` com pods 1/1 Running)*

**Evidência viva:** run `40256403606` (K8s) e `40251544759` (API) — links no PDF.

## 5:30–7:30 · APIs no ar (consumo)

**Tela:** Swagger + terminal

> "A API está publicada atrás de um LoadBalancer com ingress-nginx. No Swagger vemos todos os endpoints. Health check:" `curl <ALB>/q/health` *(status UP, database UP — prova do peering)*.

> "Rotas protegidas exigem JWT: chamando sem token → **401**; fazendo login → recebo o token; com o token no header → **200**." *(curl: `POST /api/v1/auth/login` → `Authorization: Bearer …` → `GET` de recurso protegido)*

> "E a consulta pública de status de OS, usada pelo cliente final:" `curl <ALB>/api/v1/os/{uuid}/status` *(mostra máquina de estados sem expor dados)*

## 7:30–9:30 · Observabilidade ao vivo

**Tela:** Grafana

> "Visibilidade total, 100% self-hosted: **Alloy coleta os logs JSON da API com correlationId**, o **Prometheus** raspa as métricas a cada 15 segundos e o **Loki** armazena os logs. No dashboard `OS FLOW TRAFFIC` vemos **volume diário de OS, tempo médio por etapa — Diagnóstico, Execução e Finalização — e taxa de erros**. No `system_metrics`, CPU e memória. Em `LOKI`, os logs estruturados — posso filtrar por um correlationId e seguir a requisição de ponta a ponta." *(abrir um log e copiar o correlationId para o filtro)*

> "E temos **5 alertas provisionados**: API fora do ar, falha no processamento de OS, falha na integração WhatsApp, 5xx acima de 5% e latência p95 acima de 2 segundos — todos entregues por e-mail." *(mostrar `alerting.yml` ao lado)*

## 9:30–10:30 · Escalabilidade (HPA)

**Tela:** terminal (`kubectl get hpa`, `kubectl get nodes`) ou Grafana

> "O HPA escala a API entre 1 e 4 réplicas com base em CPU 70% e memória 80%, sobre um node group gerenciado. Hoje operamos um único nó `t3.small` dentro do Free Tier — a arquitetura escala horizontalmente aumentando o node group, sem mudança de código." *(ver [RFC-003](../architecture/RFC-003-free-tier-single-node.md))*

## 10:30–12:30 · Autenticação por CPF (Lambda) ⚠️ *gravar após o merge*

**Tela:** repo Mekano-Serverless + curl para o API Gateway

> "A autenticação do cliente final é **serverless**: um API Gateway HTTP recebe o CPF, a **Lambda em Java 17** valida o CPF, consulta o cliente no PostgreSQL e devolve um **JWT Ed25519** assinado com chave do Secrets Manager — a API valida com a chave pública. 14 testes unitários e smoke test automático no deploy." *(curl: `POST <api-gw>/auth/token` com CPF inválido → `400 CPF_INVALID`; CPF válido → token; usar o token numa rota protegida)*

**Se o merge não tiver acontecido até a gravação:** mostrar o código + testes + o diagrama de sequência (`docs/architecture/sequencia-autenticacao-cpf.md`) e explicar o status no PDF.

## 12:30–14:00 · Documentação e encerramento

**Tela:** `docs/architecture/` (lista de ADRs/RFCs) → abrir ER e um ADR

> "Toda a documentação está versionada: **5 ADRs** com as decisões permanentes — nuvem, banco, HPA, comunicação, observabilidade — **3 RFCs**, o **diagrama entidade-relacionamento com a justificativa formal do PostgreSQL** e os **diagramas de sequência** dos fluxos críticos. Os 4 repositórios estão na organização, com o avaliador adicionado. Obrigado!"

---

## Evidências que valem zoom (links rápidos)

| O quê | Link |
|-------|------|
| Deploy verde do K8s | https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-Infrastructure-K8s/actions/runs/40256403606 |
| CD da API (build + GHCR) | https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-API/actions/runs/40251544759 |
| Apply do banco (RDS) | https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-Infrastructure-Database/actions/runs/40240987499 |
| Apply da observabilidade | https://github.com/FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-infrastructure-Observability/actions/runs/40262989559 |
| Health da API | `<ALB>/q/health` |
| Grafana | `http://alb-observability-133760226.us-east-1.elb.amazonaws.com` |
