# Phase 10: Repository Split — 4 repos with CI/CD - Context

**Gathered:** 2026-09-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Organizar o monorepo Mekano em 4 repositórios separados na org GitHub **FIAP-SOFTWARE-ARCHITECTURE-TEAM** (Mekano-API, Mekano-Lambda, Mekano-Infra, Mekano-DB), cada um com CI/CD (GitHub Actions) e deploy automático para a nuvem comprovado: API→EKS, Lambda→AWS Lambda, Infra/DB→`terraform apply`. Cirurgia de Terraform state para separar RDS do stack monolítico. Repositório monolítico antigo arquivado com redirect. Escopo da exigência da disciplina (SPLIT-01..08) — sem extração de auth CPF/JWT para a Lambda.

</domain>

<decisions>
## Implementation Decisions

### Organização dos repositórios (DQ3 org)
- **D-01:** Os 4 repositórios novos ficam em **FIAP-SOFTWARE-ARCHITECTURE-TEAM** (`FIAP-SOFTWARE-ARCHITECTURE-TEAM/Mekano-{API,Lambda,Infra,DB}`), privados, default branch `main` — NÃO TechMinds-Group
- **D-02:** Path GHCR permanece sob a org FIAP (ex.: `ghcr.io/fiap-software-architecture-team/...`) — não migrar para outro registry/owner

### Mecânica do split e destino do repo antigo (DQ3+DQ4)
- **D-03:** **`git filter-repo`** por path para preservar a história (~580 commits) em cada repo novo
- **D-04:** Repo monolítico antigo: **archive** + README com redirect/banner para os 4 sucessores (rollback = unarchive)
- **D-05:** Ref fonte do split: `develop` (HEAD local, 166 commits à frente de `main` — confirmar contagens no preflight T1; conteúdo do `develop` vira `main` dos repos novos)

### State + Flyway (DQ1+DQ5+DQ7)
- **D-06:** **Flyway fica na Mekano-API** — 33 V-files em `mekano-infrastructure/.../db/migration/`, `migrate-at-start=true` permanece; Mekano-DB = só Terraform do RDS
- **D-07:** **VPC (e EKS) ficam no Mekano-Infra** — state key `mekano/infra/terraform.tfstate` mantida; Mekano-DB consome `vpc_id`/subnets via `terraform_remote_state`
- **D-08:** Separação do RDS: **`terraform state mv` manual com backup** (`state pull` + snapshot ARN/CreateTime do RDS antes; mv de `module.db.*` + `aws_security_group.rds` para key `mekano/db/terraform.tfstate`; job `workflow_dispatch` com gate humano; plan duplo exit 0 obrigatório; **proibido** `state rm`/destroy)
- **D-09:** `terraform/bootstrap/` (S3 + DynamoDB `mekano-terraform-locks`): **nunca re-rodar** — CI só `head-bucket`/`describe-table`
- **D-10:** Ordem de apply Infra→DB garantida por **gate automático entre repos**: workflow do Mekano-DB espera run verde do Mekano-Infra via API do GitHub antes de apply
- **D-11:** **`deletion_protection=true`** no RDS como primeira mudança no Mekano-DB
- **D-12:** CI de Infra/DB: **`terraform plan` em todo PR** (fmt+validate+plan); **apply automático só em push em `main`** com environment protection
- **D-13:** Runbook da cirurgia de state: **`docs/STATE-SPLIT.md` no repo Mekano-Infra** (versionado, revisável em PR, referenciado do DEPLOY.md)

### Deploy + segredos (DQ6+DQ8)
- **D-14:** **Mekano-Infra executa `kubectl`/`helm apply`** dos manifests `k8s/` (manifestos vão para o repo Infra); Mekano-API só builda e publica imagem
- **D-15:** OIDC: **1 role AWS estendida** com condição `sub=repo:FIAP-SOFTWARE-ARCHITECTURE-TEAM/<repo>:ref:refs/heads/main` por repositório (remover confiança do repo monolítico antigo)
- **D-16:** Endpoint do RDS chega à API via **SSM Parameter Store** — apply do Mekano-DB grava parâmetro; ConfigMap da API lê (não remote-state, não GH variable)
- **D-17:** Environment **`production` com required reviewers** nos 4 repos (deploy dispara em main e pausa para aprovação)
- **D-18:** Par de chaves JWT Ed25519: **somente na Mekano-API** — `privatekey.pem` nunca entra em repo (gitignore case-robust); `publicKey.pem` segue tracked; Lambda não recebe chave nesta fase

### Open (não discutido nesta sessão)
- **DQ2 — escopo/runtime/IaC da Lambda: NÃO RESOLVIDO.** Planner deve tratar como decisão em aberto e surface no plan (opções: emissão JWT extraída da API / função de domínio / função mínima demonstrativa; runtime Node|Python|Java; SAM|CDK|Terraform). Auth email/password existente permanece na API até decisão explícita em contrário.

### the agent's Discretion
- Estrutura exata dos workflows decompostos da `cd.yml` (538 linhas) por repo
- Ordem das tasks de preflight (secret scan, medição de branches, probe de nomes)
- Formato do DEPLOY.md / diagramas Mermaid
- Se publicar `db_endpoint` também como output TF além do SSM

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Requisitos e roadmap
- `.planning/REQUIREMENTS.md` §SPLIT-01..SPLIT-08 — escopo da disciplina (MUST)
- `.planning/ROADMAP.md` §Phase 10 — goal + success criteria (MUST)
- `.planning/PROJECT.md` — restrições de stack/prazo/time

### Código e pipelines existentes (fonte da verdade — AGENTS.md root é autoritativo, CLAUDE.md está stale)
- `AGENTS.md` — convenções verificadas do monorepo
- `.github/workflows/ci.yml` — CI atual (mvn verify + docker + Newman)
- `.github/workflows/cd.yml` — CD monolítico 538 linhas (decompor: build/GHCR, bootstrap, TF plan/apply, kubectl/helm)
- `terraform/` — VPC + EKS + RDS + bootstrap + backend (monolítico; split = D-06..D-13)
- `k8s/` — 10 manifests (destino: Mekano-Infra, D-14)
- `docker-compose*.yml`, `Makefile`, `scripts/`, `newman/` — dev local + E2E (Mekano-API)
- `.planning/codebase/STACK.md`, `ARCHITECTURE.md`, `INTEGRATIONS.md` — mapas do codebase

### Decisões de fases anteriores relevantes
- `.planning/phases/08-documentation-polish/08-CONTEXT.md` — DOC-07 CI/CD Mermaid depende de INF-04 (afetado pelo split)
- `.planning/STATE.md` §Blockers — OIDC GitHub↔AWS pode estar bloqueado por permissões (risco do D-15)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ci.yml` — copiar quase como está para Mekano-API (sem deps cross-repo)
- `cd.yml` — decompor: jobs build/docker → API; bootstrap/TF/apply VPC+EKS → Infra; apply RDS → DB; kubectl/helm → Infra (D-14)
- `terraform/backend.tf` + `bootstrap/` — S3+DynamoDB já existem; keys `mekano/infra/...` (mantida) e `mekano/db/...` (nova)
- `k8s/` (10 manifests) + `README-INFRA.md` → Mekano-Infra
- Auth `AuthResource` (login/refresh/logout, Ed25519, issuer `mekano-api`) permanece na API

### Established Patterns
- GitHub Actions como CI/CD único (não GitLab)
- Flyway `migrate-at-start=true` (app owns schema)
- Environment `production` já usado no job `infra-apply` da cd.yml
- Hybrid ID + Clean Architecture 4 módulos — todo o código vai para Mekano-API

### Integration Points
- DB endpoint: hoje `needs.infra-apply.outputs.db_endpoint` intra-workflow → após split: SSM (D-16)
- GHCR pull secret recriado a cada deploy job
- `mp.jwt.*` + `publicKey.pem` na API — Lambda fora do fluxo de auth até DQ2

### Fact corrections (não confiar em docs stale)
- Auth EXISTE (AGENTS.md "Missing Items" está desatualizado)
- Terraform EXISTE monolítico (não é greenfield)
- `User` NÃO tem CPF (CPF só em `Cliente`) — auth CPF do brief antigo fora do requirement
- `k8s/` = 10 manifests (não 11); `develop` = 166 à frente de `main` (não 50)

</code_context>

<specifics>
## Specific Ideas

- Assignment: 4 repos × CI/CD × **deploy automático comprovado** (push main → pós-cheque: rollout Ready / LastUpdate / plan exit 0)
- Estado do split: RDS ARN/CreateTime estáveis antes/depois; Infra plan nunca mostra destroy do RDS
- Secret hygiene: varredura de histórico + `.gitignore` case-robust para `privatekey.pem` antes de qualquer cópia
- Source ref divergente: re-medir `develop` vs `main` no preflight antes do filter-repo

</specifics>

<deferred>
## Deferred Ideas

- **DQ2 Lambda scope/runtime/IaC** — não discutido; planner deve abrir como ponto de decisão no plan (não assumir CPF/JWT)
- Módulo Terraform compartilhado de VPC (5º repo) — rejeitado implicitamente por D-07
- Migração GHCR→ECR — rejeitada (D-02: manter FIAP/GHCR)
- Escopo de auth CPF do brief anterior — fora do requirement da disciplina

</deferred>

---

*Phase: 10-Repository Split — 4 repos with CI/CD*
*Context gathered: 2026-09-22*
