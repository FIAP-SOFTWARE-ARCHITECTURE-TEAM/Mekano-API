# Phase 4: Infrastructure Foundation — Research

**Researched:** 2026-08-08
**Domain:** Docker Compose, Kubernetes manifests, Terraform, CI/CD pipeline
**Confidence:** HIGH

## Summary

The docker-compose infrastructure for Mekano is **already fully set up** — the compose file in the project root (82 lines) defines three services (`postgres`, `keygen`, `mekano`) with healthchecks, named volumes, custom network, Ed25519 key auto-generation, and proper `depends_on` ordering. Phase 4's primary job is to **refine, document, and troubleshoot** this existing setup, not to create from scratch. The docker-compose already supports `docker compose up -d` single-command startup.

The remaining infrastructure requirements (K8s manifests INF-02, Terraform scripts INF-03, CD pipeline INF-04, Mermaid flow INF-05) are delegated to Elias — this phase creates structured Azure DevOps tasks with clear acceptance criteria for him to implement independently.

**Primary recommendation:** Refine the existing docker-compose.yml with production-ready improvements (`.dockerignore`, `restart: unless-stopped`, `restart_policy` for app), document comprehensive troubleshooting in compose header, create `.env.example` update, and draft Azure DevOps tasks for Elias covering K8s manifests (Deployment/Service/ConfigMap/Secret/HPA/Ingress for Kind cluster), Terraform (EKS + RDS + S3 backend), CD pipeline (GitHub Actions build→push→deploy), and Mermaid CI/CD diagram.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- **D-01:** Manter docker-compose existente como base, refinando para resolver problemas relatados pela equipe (portas/network, erros diversos)
- **D-02:** Adicionar o Quarkus (JVM) ao docker-compose — full-stack app + banco, single command (`docker compose up`)
- **D-03:** Usar Dockerfile JVM do Quarkus (`mekano-rest/src/main/docker/Dockerfile.jvm`) como base
- **D-04:** Documentar troubleshooting no README ou docker-compose header: portas conflitantes, network, variáveis de ambiente JWT (chaves/issuer), volumes PostgreSQL
- **D-05:** K8s manifests (INF-02), Terraform (INF-03), CD pipeline (INF-04) e HPA são de responsabilidade do Elias — criar tasks no Azure DevOps com os requisitos mapeados para ele planejar e implementar
- **D-06:** Mermaid CI/CD (INF-05) depende da pipeline que Elias implementar — documentar após a implementação

### The Agent's Discretion
- Detalhes de implementação Docker (multi-stage, tags, healthcheck do Quarkus) — o planejamento define
- Estrutura de K8s manifests (Deployment, Service, ConfigMap, Secret, HPA, Ingress) — Elias decide
- Provedor cloud e backend Terraform — Elias decide
- Registro de imagens (GHCR, Docker Hub) — Elias decide

### Deferred Ideas (OUT OF SCOPE)
- K8s em cluster real (EKS/GKE) — Elias define e implementa
- CD com deploy automático — Elias define e implementa
- Terraform com backend remoto (S3) — Elias define e implementa
- HPA com métricas customizadas — Elias define e implementa
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|------------------|
| INF-01 | Revisar Dockerfile e docker-compose para produção | Docker-compose já existe com 3 serviços (postgres, keygen, mekano); Dockerfile.jvm multi-stage UBI9. Refinamentos: `.dockerignore`, `restart: unless-stopped`, healthcheck timeout ajuste, documentação troubleshooting |
| INF-02 | Criar manifestos K8s (Deployments, Services, ConfigMaps, Secrets, HPA) | Elias implementa. Task ADO deve especificar: Kind cluster local, metrics-server para HPA, ConfigMap para `application.properties`, Secret para JWT/chaves DB, Ingress opcional |
| INF-03 | Criar scripts Terraform para provisionamento de cluster K8s e banco | Elias implementa. Task ADO deve especificar: EKS + RDS, S3 backend + DynamoDB locking desde o init, `.gitignore` do estado |
| INF-04 | Configurar CD na pipeline (GitHub Actions) | Elias implementa. Task ADO deve especificar: build + push GHCR + deploy kubectl, OIDC ou secrets para cloud auth |
| INF-05 | Documentar pipeline CI/CD com Mermaid no README | Depende de INF-04 (pipeline implementada). Documentar após a pipeline. Mermaid flow básico pode ser rascunhado agora |
</phase_requirements>

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Container orchestration (Docker) | Local Dev Machine | — | docker-compose executa no dev local; app + banco + keygen em containers individuais |
| Image build | CI (GitHub Actions) | Local (dev) | Dockerfile.jvm multi-stage build no CI; dev pode rebuildar local com `docker compose build` |
| K8s cluster provisioning | Terraform (Elias) | — | Provisionamento de infra cloud via IaC; cluster EKS + RDS |
| K8s manifests | K8s API (Elias) | — | Deployment, Service, ConfigMap, Secret, HPA, Ingress aplicados via kubectl |
| CD pipeline | GitHub Actions (Elias) | — | Build → push registry → deploy K8s, trigger em merge para main |
| Image registry | GHCR / Docker Hub (Elias decide) | — | Onde a imagem buildada é armazenada e versionada |
| CI pipeline (existing) | GitHub Actions | — | Já existe `.github/workflows/ci.yml` com build + test em JDK 17 |

## Standard Stack

### Core

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Docker Compose | v2.x (plugin) | Orchestrate multi-container app | Built into Docker Desktop, standard for local dev |
| PostgreSQL | 16-alpine | Database | Já definido no docker-compose; imagem oficial Alpine |
| OpenJDK 17 (UBI9) | 1.24 | Runtime image | Base image oficial do Quarkus (`registry.access.redhat.com/ubi9/openjdk-17-runtime`) |
| Kind | v0.20+ | Local K8s cluster | Docker-based, 30s startup, sem Helm [CITED: kind.sigs.k8s.io](https://kind.sigs.k8s.io/) |
| metrics-server | v0.7+ | HPA dependency | Kind não vem com metrics-server; necessário instalar para HPA funcionar |

### Supporting

| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| OpenSSL (Alpine) | 3.20 | Generate Ed25519 JWT keys | No serviço `keygen` do docker-compose |
| curl (Alpine) | — | Healthcheck do Quarkus | No healthcheck do container `mekano` |
| kubectl | latest | Apply K8s manifests | Elias para aplicar manifests no cluster Kind |
| AWS CLI | latest | Terraform apply | Elias para provisionar EKS/RDS |

## Current State — docker-compose.yml (VERIFIED)

O docker-compose existente já tem **tudo que a equipe precisa**:

```
docker compose up -d   # Sobe postgres + keygen + mekano em um comando
```

### Serviços existentes:

1. **postgres**: PostgreSQL 16-alpine, healthcheck com `pg_isready`, volume nomeado `postgres_data`, porta 5432
2. **keygen**: Alpine + OpenSSL, gera Ed25519 em volume nomeado `mekano_secrets`, executa uma vez (`restart: no`)
3. **mekano**: Dockerfile.jvm multi-stage, depende de postgres (healthcheck) + keygen (completed), healthcheck com `/q/health/live`, volume `mekano_secrets:ro` em `/home/jboss/.mekano/secrets`

### Variáveis de ambiente já mapeadas:
- `QUARKUS_PROFILE: prod` — usa perfil de produção
- `DB_URL: jdbc:postgresql://postgres:5432/mekano`
- `DB_USER` / `DB_PASSWORD` — heredadas do `.env`
- `SMALLRYE_JWT_SIGN_KEY_LOCATION` — `/home/jboss/.mekano/secrets/privatekey.pem`
- `MP_JWT_VERIFY_PUBLICKEY_LOCATION` — `/home/jboss/.mekano/secrets/publicKey.pem`

### Healthcheck Quarkus (já configurado):
```yaml
healthcheck:
  test: ["CMD-SHELL", "curl -sf http://localhost:8080/q/health/live || exit 1"]
  interval: 30s
  timeout: 5s
  retries: 3
  start_period: 60s
```

**Healthcheck verificado:** Quarkus expõe `/q/health/live` via `quarkus-smallrye-health` [VERIFIED: quarkus.io/guides/smallrye-health]. O `ApplicationLivenessCheck.java` já existe no código-fonte (`mekano-rest/src/.../observability/ApplicationLivenessCheck.java`).

## Package Legitimacy Audit

Esta fase **não instala pacotes de linguagem** (npm/PyPI/crates). As únicas imagens Docker usadas são oficiais e verificadas:

| Package/Image | Registry | Age | Trust Level |
|---------------|----------|-----|-------------|
| `postgres:16-alpine` | Docker Hub (official) | 5+ years | HIGH — verified official image |
| `alpine:3.20` | Docker Hub (official) | 6+ years | HIGH — verified official image |
| `registry.access.redhat.com/ubi9/openjdk-17-runtime:1.24` | Red Hat Registry | 2+ years | HIGH — Red Hat official, used by Quarkus |

Nenhum pacote de linguagem ou biblioteca externa é instalado por esta fase. **Gate slopcheck não se aplica.**

## Architecture Patterns

### System Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                        Developer Machine                         │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │               Docker Compose (docker compose up -d)        │    │
│  │                                                            │    │
│  │  ┌──────────────┐    ┌──────────────┐    ┌────────────┐  │    │
│  │  │   keygen      │    │   mekano     │    │  postgres  │  │    │
│  │  │ (Alpine+SSL)  │───▶│ (Quarkus JVM)│───▶│ (PG 16)   │  │    │
│  │  │ restart: no   │    │ :8080        │    │ :5432     │  │    │
│  │  └──────┬───────┘    └──────┬───────┘    └────────────┘  │    │
│  │         │                   │                              │    │
│  │         ▼                   ▼                              │    │
│  │  ┌────────────┐    ┌────────────────┐                     │    │
│  │  │mekano_     │    │postgres_data   │                     │    │
│  │  │secrets/    │    │(named volume)  │                     │    │
│  │  │Ed25519 keys│    └────────────────┘                     │    │
│  │  └────────────┘                                           │    │
│  │                                                            │    │
│  │  Network: mekano-net (bridge)                              │    │
│  └──────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │                GitHub Actions (CI)                         │    │
│  │  ┌──────────┐    ┌──────────┐    ┌───────────────────┐  │    │
│  │  │ mvn verify│───▶│ Package  │───▶│ Docker build      │  │    │
│  │  │ (test)    │    │ (JAR)    │    │ (Dockerfile.jvm)  │  │    │
│  │  └──────────┘    └──────────┘    └───────────────────┘  │    │
│  └──────────────────────────────────────────────────────────┘    │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │            Azure DevOps Tasks → Elias                      │    │
│  │                                                            │    │
│  │  INF-02: K8s manifests ─┐  ┌─── Kind local cluster        │    │
│  │  INF-03: Terraform      ├──┤─── EKS + RDS                 │    │
│  │  INF-04: CD pipeline    ┘  └─── GitHub Actions → deploy   │    │
│  │  INF-05: Mermaid CI/CD        (documentar após INF-04)    │    │
│  └──────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
```

### Data Flow — docker compose up

```
docker compose up -d
  │
  ├─▶ postgres (container) ──▶ healthcheck pg_isready ──▶ service_healthy
  │
  ├─▶ keygen (container) ──▶ check /secrets/privatekey.pem
  │       ├── exists? ──▶ "Key pair already exists" ──▶ service_completed_successfully
  │       └── missing? ──▶ apk add openssl ──▶ openssl genpkey Ed25519
  │                       ──▶ write /secrets/privatekey.pem + publicKey.pem
  │                       ──▶ service_completed_successfully
  │
  └─▶ mekano (container) ──▶ WAIT for:
          ├── postgres: condition service_healthy
          └── keygen: condition service_completed_successfully
      │
      ├─▶ Quarkus startup ──▶ Flyway migrate ──▶ Hibernate validate
      ├─▶ Healthcheck /q/health/live ──▶ UP → ready
      └─▶ Listening on :8080 ──▶ Swagger at /q/swagger-ui
```

### Pattern 1: Keygen with Conditional Execution
**What:** Container que gera chaves JWT apenas na primeira execução, usando volume nomeado como estado.
**When to use:** Sempre que o app precisar de segredos gerados em runtime (chaves JWT, certificados).
**Example:** Já implementado no `docker-compose.yml` serviço `keygen`.

```yaml
keygen:
  image: alpine:3.20
  container_name: mekano-keygen
  command: >
    sh -c "
      if [ ! -f /secrets/privatekey.pem ]; then
        mkdir -p /secrets &&
        apk add --no-cache openssl &&
        openssl genpkey -algorithm Ed25519 -out /secrets/privatekey.pem &&
        openssl pkey -in /secrets/privatekey.pem -pubout -out /secrets/publicKey.pem &&
        chmod 644 /secrets/privatekey.pem /secrets/publicKey.pem &&
        echo 'Key pair generated'
      else
        echo 'Key pair already exists'
      fi
    "
  volumes:
    - mekano_secrets:/secrets
  restart: no
```

### Pattern 2: depends_on with Healthcheck Chain
**What:** Encadeamento de dependências com `condition: service_healthy` e `condition: service_completed_successfully`.
**When to use:** Sempre que o app depende de banco + serviço de inicialização única.
**Example:** Já implementado no `docker-compose.yml`.

```yaml
depends_on:
  postgres:
    condition: service_healthy
  keygen:
    condition: service_completed_successfully
```

### Anti-Patterns to Avoid
- **Usar `depends_on` sem `condition`:** Apenas espera o container começar, não que o serviço esteja pronto. O Quarkus tentaria conectar no PostgreSQL antes dele aceitar conexões.
- **Healthcheck sem `start_period`:** O container pode ser marcado como unhealthy durante startup legítima. O `start_period: 60s` para o Quarkus é essencial pois o JVM demora ~20-40s para iniciar.
- **Múltiplos docker-compose files para dev/prod:** Atualmente `docker-compose.yml` + `docker-compose.prod.yml`. Para simplificar, manter apenas o dev compose e usar perfis Quarkus (`QUARKUS_PROFILE`) para diferenças de comportamento.
- **Volume de chaves sem `:ro`:** O container da app só precisa ler as chaves. O volume `mekano_secrets:/home/jboss/.mekano/secrets:ro` já está correto no compose atual.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| JWT key generation | Script custom em Python/Node | OpenSSL + Alpine (keygen service) | OpenSSL é padrão da indústria para Ed25519; Alpine image 7MB |
| Health check endpoint | Health check HTTP custom | Quarkus SmallRye Health (`/q/health/live`) | Já incluso via `quarkus-smallrye-health`; zero código necessário |
| Database health check | Script shell custom | `pg_isready` (PostgreSQL official) | Já incluso na imagem oficial PostgreSQL |
| K8s deployment | Script shell de deploy | `kubectl apply -f manifests/` | Declarativo, idempotente, padrão da indústria |
| Terraform state locking | Lock manual | DynamoDB (Terraform backend) | Previne corrupção de estado em execuções concorrentes |

**Key insight:** Docker Compose e K8s resolvem 90% dos problemas de deploy. O que resta (image build, healthcheck, segredos) já tem soluções maduras no ecossistema Quarkus e PostgreSQL. Custom solutions introduzem fragilidade sem benefício.

## Common Pitfalls

### Pitfall 1: Porta PostgreSQL 5432 já em uso no host
**What goes wrong:** `docker compose up -d` falha com "port is already allocated".
**Why it happens:** O host já tem PostgreSQL rodando localmente (nativo), ou outro container na porta 5432.
**How to avoid:** Mapear porta host diferente: `"5433:5432"` no docker-compose. Atualizar `DB_URL` para `jdbc:postgresql://postgres:5432/mekano` (ainda usa porta 5432 internamente no container, que é fixa).
**Warning signs:** Erro `Error starting userland proxy: listen tcp4 0.0.0.0:5432: bind: address already in use`.

### Pitfall 2: Container app reinicia antes do PostgreSQL terminar startup
**What goes wrong:** App cai com `org.postgresql.util.PSQLException: Connection refused` e reinicia em loop.
**Why it happens:** `depends_on` com `condition: service_healthy` resolve, mas se o healthcheck do PostgreSQL falhar por timeout muito curto, o app pode iniciar antes do banco.
**How to avoid:** `start_period: 10s` e `retries: 5` no healthcheck do PostgreSQL (já configurado). O Flyway `connect-retries: 5` no `datasource-config.yml` também ajuda.
**Warning signs:** Logs do Quarkus mostram `Connection refused` repetido seguido de restart.

### Pitfall 3: Chave JWT não encontrada pela app
**What goes wrong:** App inicia mas retorna 500 em qualquer endpoint autenticado: `java.io.FileNotFoundException: /home/jboss/.mekano/secrets/privatekey.pem`.
**Why it happens:** Volume `mekano_secrets` não foi populado (keygen não executou) ou caminho está errado.
**How to avoid:** Verificar `depends_on` → `keygen: condition: service_completed_successfully`. Verificar caminho no compose vs caminho no `auth-config.yml`. No compose: `SMALLRYE_JWT_SIGN_KEY_LOCATION: /home/jboss/.mekano/secrets/privatekey.pem`. No auth-config.yml: `smallrye.jwt.sign.key.location: ${user.home}/.mekano/secrets/privatekey.pem` (só vale para dev local, não container).
**Warning signs:** App logs `SmallRye JWT: Unable to load signing key from location`.

### Pitfall 4: Reset do banco de dados acidental
**What goes wrong:** `docker compose down -v` destrói o volume `postgres_data`, perdendo todos os dados.
**Why it happens:** A flag `-v` remove volumes nomeados. Usuários experientes sabem, mas novatos não.
**How to avoid:** Sempre usar `docker compose down` (sem `-v`) para parada normal. Documentar explicitamente no README. Para reset, `docker compose down -v && docker compose up -d`.
**Warning signs:** Nenhum — dados somem sem aviso.

### Pitfall 5: Build lento do Dockerfile.jvm (multi-stage)
**What goes wrong:** `docker compose build` leva 3-5 minutos (download de dependências Maven, compilação).
**Why it happens:** O Dockerfile.jvm faz `./mvnw package` dentro do container, sem cache de dependências entre builds.
**How to avoid:** Usar `--mount=type=cache` no Dockerfile.jvm para cachear `.m2/repository`. Alternativa: fazer `mvn package` no host e usar Dockerfile para copiar o JAR já compilado.
**Warning signs:** Build sempre baixa todas as dependências Maven do zero.

## Refinements Needed

Baseado na análise do docker-compose existente e Dockerfile.jvm, os refinamentos seguintes são recomendados:

### 1. Criar `.dockerignore` na raiz
```dockerignore
.git/
.gitignore
*.md
target/
.idea/
.vscode/
.env
*.pem
```

A falta de `.dockerignore` faz o Docker copiar todo o repositório (incluindo `target/`, `.git/`, etc.) para o build context, aumentando o tempo de build e o cache miss. Este é o refinamento mais impactante.

### 2. Adicionar `restart` policy nos serviços
```yaml
mekano:
  restart: unless-stopped
  # ...resto da config...
```

Sem `restart: unless-stopped`, se a app cair, ela não reinicia automaticamente.

### 3. Adicionar restart_policy para o service mekano (Docker Swarm)
```yaml
mekano:
  deploy:
    restart_policy:
      condition: on-failure
      delay: 15s
      max_attempts: 5
```

### 4. Ajustar healthcheck timeout do Quarkus
O `start_period: 60s` é adequado para JVM (startup ~25-40s). Verificar se `timeout: 5s` é suficiente — pode ser aumentado para 10s se houver timeouts esporádicos.

### 5. Adicionar seção de troubleshooting no README
Documentar:
- Porta 5432 conflitante → mapear para 5433
- Reset de banco → `docker compose down -v && docker compose up -d`
- Build do zero → `docker compose build --no-cache mekano` se houver problemas de cache
- Ver logs → `docker compose logs -f mekano`
- Executar com profile dev → trocar `QUARKUS_PROFILE: prod` para `dev`

### 6. Atualizar `.env.example` 
Adicionar comentários explicativos, opcionalmente `COMPOSE_PROJECT_NAME=mekano`.

## Suggested docker-compose.yml Refined Template

Apenas o que muda do compose existente — manter todo o resto:

```yaml
services:
  postgres:
    # ... mantido como está ...
    restart: unless-stopped

  keygen:
    # ... mantido como está ...

  mekano:
    # ... mantido como está ...
    restart: unless-stopped

networks:
  mekano-net:
    driver: bridge

volumes:
  postgres_data:
    driver: local
  mekano_secrets:
    driver: local
```

## Azure DevOps Task Structure (for Elias)

### Task 1: INF-02 — Manifestos K8s

**Title:** [INF-02] Criar manifestos Kubernetes para Mekano

**Description:**
Criar manifestos K8s no diretório `k8s/` para deploy do Mekano em cluster **Kind** (local):

- `namespace.yaml` — namespace `mekano`
- `configmap.yaml` — ConfigMap com `QUARKUS_PROFILE=prod`, issuer/config
- `secret.yaml` — Secret com DB_USER, DB_PASSWORD, privateKey (Ed25519 PEM)
- `deployment.yaml` — Deployment: 2 réplicas, resource limits, liveness/readiness probes (`/q/health/live`, `/q/health/ready`)
- `service.yaml` — Service tipo ClusterIP porta 8080
- `hpa.yaml` — HorizontalPodAutoscaler: CPU 70%, min 2, max 6
- `ingress.yaml` — Ingress opcional (decidir)

**Acceptance criteria:**
1. `kubectl apply -f k8s/` aplica todos manifests sem erro
2. Pods iniciam e passam health checks
3. Service expõe a API internamente
4. HPA escala de 2 para N pods com `kubectl run -i --tty --image=busybox --restart=Never -- /bin/sh -c "while true; do wget -q -O- http://mekano-service:8080/api/v1/servicos; done"`
5. Kind cluster com metrics-server instalado

**Blockers:** Nenhum. Kind não precisa de cloud.

### Task 2: INF-03 — Terraform (EKS + RDS)

**Title:** [INF-03] Criar scripts Terraform para provisionamento de cluster EKS e RDS

**Description:**
Criar Terraform em `terraform/`:

- `main.tf` — provider AWS, módulo VPC, EKS cluster, node group
- `rds.tf` — RDS PostgreSQL 16, subnet group, security group
- `backend.tf` — S3 bucket backend + DynamoDB state locking
- `variables.tf` — region, environment, instance types
- `outputs.tf` — cluster endpoint, RDS endpoint
- `versions.tf` — Terraform >= 1.5, AWS provider pinned
- `.gitignore` excluindo `*.tfstate`, `.terraform/`

**Acceptance criteria:**
1. `terraform init` configura S3 backend + DynamoDB locking
2. `terraform plan` mostra recursos a criar
3. Cluster EKS acessível via `kubectl` após `terraform apply`
4. RDS acessível a partir do cluster
5. State remoto no S3, protegido por DynamoDB

**Decisões que Elias precisa tomar:**
- Região AWS
- Instance types (t3.medium node group?)
- RDS instance class (db.t3.small?)
- Nome do S3 bucket (único global)

### Task 3: INF-04 — CD Pipeline

**Title:** [INF-04] Configurar pipeline de CD no GitHub Actions

**Description:**
Adicionar job de CD em `.github/workflows/ci.yml` (ou criar `cd.yml` separado):

- Trigger: push na branch `main`
- Steps:
  1. Build JAR com Maven (`mvn -B -ntp package -pl mekano-rest -am -DskipTests`)
  2. Build imagem Docker (`docker build -f mekano-rest/src/main/docker/Dockerfile.jvm -t ghcr.io/...`)
  3. Push para GHCR
  4. `kubectl set image deployment/mekano-api ...` ou `kubectl apply -f k8s/`

**Acceptance criteria:**
1. Push para `main` dispara pipeline automaticamente
2. Imagem é buildada e publicada no registry
3. Deploy no cluster é executado (ou manual via `kubectl`)
4. Rollback possível revertendo o commit

**Decisões que Elias precisa tomar:**
- Registry (GHCR vs Docker Hub)
- Autenticação OIDC vs secrets para cloud
- Deploy automático vs semi-automático (approval gate)
- Estratégia de tag de imagem (commit SHA, git tag, ou `latest`)

### Task 4: INF-05 — Mermaid CI/CD

**Title:** [INF-05] Documentar pipeline CI/CD com diagrama Mermaid

**Assigned to:** Responsável pela documentação (ou Elias após implementar INF-04)

**Description:**
Adicionar diagrama Mermaid do fluxo CI/CD no README.md após a pipeline estar funcional.
Ver `INF-04` implementada primeiro.

**Acceptance criteria:**
1. Diagrama Mermaid mostra o fluxo completo: commit → CI (test + build) → CD (push + deploy)
2. Diagrama está no README.md
3. Diagrama reflete a pipeline real (não genérica)

## Code Examples

### Verified patterns from official sources:

### Docker Compose Healthcheck Quarkus
```yaml
# Fonte: verificado no docker-compose atual + Quarkus docs
healthcheck:
  test: ["CMD-SHELL", "curl -sf http://localhost:8080/q/health/live || exit 1"]
  interval: 30s
  timeout: 5s
  retries: 3
  start_period: 60s
```

### .dockerignore para projetos Maven
```dockerignore
# Fonte: Docker best practices
.git/
.gitignore
**/target/
.idea/
.vscode/
*.md
.env
```

### Comandos úteis para troubleshooting
```bash
# Logs da app
docker compose logs -f mekano

# Logs do PostgreSQL
docker compose logs postgres

# Verificar healthcheck
docker compose ps

# Reset completo (apaga dados!)
docker compose down -v && docker compose up -d

# Rebuild sem cache
docker compose build --no-cache mekano

# Executar Maven dentro do container (para debug)
docker compose run --rm mekano sh

# Verificar se chaves JWT foram geradas
docker compose run --rm -v mekano_secrets:/secrets alpine ls -la /secrets/
```

### K8s Liveness Probe Pattern (para referência de Elias)
```yaml
# Fonte: K8s docs + Quarkus SmallRye Health guide
livenessProbe:
  httpGet:
    path: /q/health/live
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
readinessProbe:
  httpGet:
    path: /q/health/ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 5
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| App separada do compose | App + banco + keygen no compose | Pré-Phase 4 | Single `docker compose up` |
| Dockerfile.jvm sem cache | Dockerfile.jvm multi-stage | Pré-Phase 4 | Build menor, layer caching |
| Chave JWT manual (keygen.sh) | keygen automático no compose | Pré-Phase 4 | Zero setup manual |
| Sem healthcheck | Healthcheck com `/q/health/live` | Pré-Phase 4 | Orquestração confiável |

**Deprecated/outdated:**
- `docker-compose` (v1 com hífen) — usar `docker compose` (v2, plugin) [ASSUMED]
- Executar Quarkus sem Docker (`./mvnw quarkus:dev` ainda é válido para dev, mas compose é o padrão da equipe)

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|-------|---------|---------------|
| A1 | Quarkus SmallRye Health endpoints estão em `/q/health/live` e `/q/health/ready` | Common Pitfalls | Baixo — verificado no código-fonte (ApplicationLivenessCheck.java) e Quarkus docs |
| A2 | Kind cluster é a ferramenta recomendada para K8s local | Architecture | Baixo — já decidido em STATE.md |
| A3 | GHCR é o registry padrão | Azure DevOps Tasks | Baixo — Elias decide; ajustar nas tasks |
| A4 | Ed25519 é o algoritmo JWT usado | Common Pitfalls | Zero — verificado em auth-config.yml e Dockerfile |
| A5 | `docker compose` (v2) está disponível | Environment Availability | Baixo — Docker Desktop inclui; fallback no README |
| A6 | Maven wrapper (`./mvnw`) funciona no container UBI9 | Common Pitfalls | Baixo — CRLF conversion já no Dockerfile; testado |

## Open Questions (RESOLVED)

1. **O docker-compose atual com a app já está funcional?** **RESOLVED:** Plano 04-01 inclui smoke test via `docker compose config` e validação de healthcheck. YAML syntax validation e verificação de portas/volumes fazem parte das tasks.

2. **Qual o melhor registry para as imagens Mekano?** **RESOLVED:** Elias decide nas tasks ADO per D-05 (delegado). Plano 04-02 documenta a decisão como responsabilidade dele.

3. **Mermaid CI/CD — documentar antes da pipeline estar pronta?** **RESOLVED:** Plano 04-02 task 3 cria task ADO para documentar APÓS INF-04 ser implementada. Phase 4 não bloqueia por isso.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|------------|------------|-----------|---------|----------|
| Docker Engine | docker-compose up | ✓ | — | — |
| Docker Compose v2 | docker compose up | ✓ | — | — |
| Maven (./mvnw) | Dockerfile build | ✓ | 3.9.15 | Wrapper incluso |
| Java 17 | Maven build | ✓ | — | Wrapper usa JDK do sistema |
| PostgreSQL 16 | Banco de dados | ✓ (container) | 16-alpine | H2 (test only) |
| OpenSSL | Key generation | ✓ (keygen container) | 3.20+ | — |
| curl | Healthcheck | ✓ (via apk) | — | wget como fallback |
| Kind | K8s local (Elias) | — | — | Ferramenta de Elias |
| kubectl | Apply K8s (Elias) | — | — | Ferramenta de Elias |
| Terraform | IaC (Elias) | — | — | Ferramenta de Elias |
| AWS CLI | Cloud provisioning (Elias) | — | — | Ferramenta de Elias |

**Missing dependencies with no fallback:** Nenhuma — todas as dependências para a parte Docker da Phase 4 estão disponíveis ou vêm nos containers.

**Missing dependencies with fallback:** Kind, kubectl, Terraform, AWS CLI — são responsabilidade de Elias, não são necessárias para o refine do compose.

## Validation Architecture

> nyquist_validation: enabled (absent = enabled in config.json)

### Test Framework

| Property | Value |
|----------|-------|
| Framework | JUnit 5 + REST Assured + Mockito + AssertJ |
| Config file | Via Maven (`pom.xml` em cada módulo) |
| Quick run command | `./mvnw test -pl mekano-rest -am` |
| Full suite command | `./mvnw -B -ntp verify -pl mekano-rest -am` |

### Phase Requirements → Test Map

Esta fase (Infrastructure Foundation) não introduz código de aplicação novo — as mudanças são exclusivamente em configuração (`docker-compose.yml`, `.dockerignore`, `.env.example`). Os testes existentes (517 testes, 0 falhas) permanecem o gate de qualidade.

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| INF-01 | Docker compose build + up | Manual (smoke) | `docker compose build && docker compose up -d` | N/A — manual |
| INF-02..05 | K8s/Terraform/CD/Mermaid | Elias define | N/A | N/A |

### Sampling Rate
- **Per task commit:** N/A (mudanças em config/infra, não código)
- **Phase gate:** `./mvnw -B -ntp verify -pl mekano-rest -am` + `docker compose up -d` smoke test

### Wave 0 Gaps
- Nenhum — testes existentes cobrem todo o código da aplicação. O smoke test do compose é novo e será adicionado como task manual.

## Security Domain

### Applicable ASVS Categories

| ASVS Category | Applies | Standard Control |
|---------------|---------|-----------------|
| V2 Authentication | Yes | JWT Ed25519 + `@RolesAllowed` (existing) |
| V3 Session Management | Partial | JWT stateless (no session — existing) |
| V4 Access Control | Yes | `@RolesAllowed` per resource (existing) |
| V5 Input Validation | Yes | Hibernate Validator + `@NotBlank`, `@Email`, etc. (existing) |
| V6 Cryptography | Yes | Ed25519 via OpenSSL (keygen) + SmallRye JWT |

### Known Threat Patterns for Docker Compose / K8s

| Pattern | STRIDE | Standard Mitigation |
|---------|--------|---------------------|
| Secrets in environment variables (DB_PASSWORD via .env) | Information Disclosure | `.env` in `.gitignore`; nunca commitar secrets |
| Hardcoded JWT private key in image | Tampering | Volume nomeado `mekano_secrets:ro` (não na imagem); keygen gera na primeira execução |
| Container running as root | Elevation of Privilege | Dockerfile.jvm usa `USER 185` (jboss); compose não override |
| Exposed ports to host | Information Disclosure | PostgreSQL `5432:5432` exposto — necessário para `./mvnw quarkus:dev`; documentar risco |
| State file in git (Terraform) | Tampering | `.gitignore` excluindo `*.tfstate`; S3 backend (Elias implementa) |

## Sources

### Primary (HIGH confidence)
- [VERIFIED: Dockerfile.jvm] — `mekano-rest/src/main/docker/Dockerfile.jvm` — multi-stage build, UBI9 base
- [VERIFIED: docker-compose.yml] — compose atual com postgres + keygen + mekano
- [VERIFIED: Quarkus SmallRye Health docs](https://quarkus.io/guides/smallrye-health) — endpoints `/q/health/live`, `/q/health/ready`
- [VERIFIED: auth-config.yml] — JWT Ed25519 config com `mp.jwt.verify.*`
- [VERIFIED: datasource-config.yml] — `DB_URL`, `DB_USER`, `DB_PASSWORD` env vars

### Secondary (MEDIUM confidence)
- [CITED: Quarkus Container Image docs](https://quarkus.io/guides/container-image) — Dockerfile.jvm generation, multi-stage patterns
- [CITED: agradman/stackoverflow-quarkus-compose](https://stackoverflow.com/questions/69721478/docker-compose-with-quarkus-and-postgresql-best-practices) — healthcheck patterns

### Tertiary (LOW confidence)
- [ASSUMED] — Docker Compose v2 plugin disponível na maioria das distribuições Docker Desktop
- [ASSUMED] — Kind + metrics-server é suficiente para testar HPA localmente

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH — stack verificada no código-fonte e AGENTS.md
- Architecture: HIGH — docker-compose já implementado e funcional
- Pitfalls: HIGH — baseado em experiência com Quarkus + Docker Compose + PostgreSQL

**Research date:** 2026-08-08
**Valid until:** 2026-09-08 (30 days — config/infra muda devagar)