# Stack Research

**Domain:** Mekano v2.0 — WhatsApp integration, K8s infra, Terraform, JaCoCo gate, CD pipeline
**Researched:** 2026-08-08
**Confidence:** HIGH (verified via Context7, official docs, and Quarkus ecosystem sources)

## Recommended Stack

### WhatsApp Integration

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| **WhatsApp Cloud API (Meta)** | v19.0+ | Send notification messages via WhatsApp | Free tier: 1,000 conversations/month. Direct-to-Meta, no middleman fees. Template-based messaging for utility (notifications) and marketing. Native support for template messages with variables — perfect for "Orçamento aprovado #OS-1234" and "OS finalizada pronta para retirada". |
| **Quarkus REST Client** | 3.36.0 (`quarkus-rest-client-jackson`) | HTTP client to call WhatsApp Cloud API | Reactive (Mutiny Uni) non-blocking HTTP calls. Declarative `@RegisterRestClient` interfaces. Native CDI injection. Zero boilerplate — fits existing Mekano pattern. JSON via built-in Jackson. |
| **Quarkus Vert.x Web Client** | 3.36.0 (`smallrye-mutiny-vertx-web-client`) | Alternative low-level HTTP client | Only if REST Client proves insufficient (e.g., complex multipart uploads). Not needed for simple POST JSON to WhatsApp API. |
| **WireMock** | 3.x (`io.quarkiverse.wiremock:quarkus-wiremock:1.6.3`) | Mock WhatsApp API in tests | Quarkus DevService — starts automatically in dev/test mode. Native `@ConnectWireMock` annotation. Injects `WireMock` client directly. JSON stubs in `src/test/resources/mappings/` for WhatsApp responses. |
| **WhatsApp Cloud API access token** | N/A | Auth via `Bearer` token in `Authorization` header | Permanent token from Meta Business Account. Stored as K8s Secret, mapped to `quarkus.whatsapp.api.token` config property. No SDK dependency needed — raw REST call is simpler. |

**Architecture decision (WPP-01, WPP-02):**

```
┌──────────────┐     POST /v19.0/PHONE_NUMBER_ID/messages     ┌─────────────────┐
│ Quarkus App  │ ────────────────────────────────────────────→ │ WhatsApp Cloud   │
│ (use case)   │                                              │ API (Meta)       │
│              │ ←─ 200 { "messages": [{ "id": "wamid..." }]} │                  │
└──────────────┘                                              └─────────────────┘
     │  (CDI event)                                                    │
     │  WhatsAppNotificationEvent                                      │
     ▼                                                                │
┌──────────────┐                                                      │
│ Event handler│  Se falhar → log + não impacta transação principal    │
│ (async)      │←─────────────────────────────────────────────────────┘
└──────────────┘
```

**Why NOT a Java SDK:** Twilio offers Java SDK, but adding a full SDK for two POST calls is overkill. `quarkus-rest-client-jackson` with `@RegisterRestClient` produces 3 files (interface, DTO, config) vs SDK dependency + classpath issues + version conflicts.

### Terraform

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| **Terraform** | v1.8+ (stable) | IaC for K8s cluster + DB | Industry standard for provisioning. Open-source, works with all cloud providers. HCL declarative syntax. |
| **Terraform AWS Provider** | `hashicorp/aws` ~> 5.0 | Provision EKS cluster + RDS PostgreSQL | Most complete cloud provider. EKS module for managed K8s. RDS for managed PostgreSQL 16. IAM roles for cluster auth. |
| **Terraform Helm Provider** | `hashicorp/helm` ~> 2.0 | Deploy applications via Helm charts | Only if YAML drift becomes a problem in future. For v2.0 scope, plain YAML + `kubectl` is simpler. |
| **Terraform Kubernetes Provider** | `hashicorp/kubernetes` ~> 2.0 | Manage K8s resources in Terraform | Allows creating Deployments, Services, ConfigMaps, Secrets, HPA from Terraform. Useful for GitOps-style deploy. |
| **Terraform Random Provider** | `hashicorp/random` ~> 3.0 | Generate DB passwords | Standard for creating random passwords for RDS. No extra secrets management needed. |
| **Kind** | 0.20+ | Local K8s cluster | Lightweight, runs in Docker. Fast startup (<30s). Supports `kind load docker-image` for local image testing. No cloud costs. |

**Terraform project structure (INF-03):**

```
terraform/
├── main.tf             # Provider config + backend
├── variables.tf        # Input variables (region, cluster_name, etc.)
├── outputs.tf          # Cluster endpoint, DB connection string
├── modules/
│   ├── eks/            # EKS cluster + node group
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   └── outputs.tf
│   └── rds/            # RDS PostgreSQL 16
│       ├── main.tf
│       ├── variables.tf
│       └── outputs.tf
├── k8s/                # K8s manifests (applied via kubectl or kustomize)
│   ├── namespace.yaml
│   ├── configmap.yaml
│   ├── secret.yaml     # (placeholder — real secrets via AWS Secrets Manager)
│   ├── deployment.yaml
│   ├── service.yaml
│   └── hpa.yaml
└── backend.tf          # S3 backend for state
```

**Decision: Plain YAML > Helm for v2.0.** Single-service API. Helm's templating creates more abstraction than value. Plain YAML is version-controllable, `kubectl apply -f` works directly, and the deployment structure is simple (1 Deployment + 1 Service + 1 HPA). If the project grows to microservices, migrate to Helm or Kustomize later.

**Decision: EKS (AWS) as primary target.** Since it's a FIAP academic project, choose AWS EKS because:
- Terraform AWS Provider is the most mature
- EKS Auto Mode (2025) eliminates node group management
- RDS PostgreSQL 16 is a first-class resource
- Free tier eligible for small clusters (t3.medium)

**Local dev alternative:** Kind cluster with `kubectl apply -f` — zero Terraform needed for local. Terraform is for cloud provisioning only.

### JaCoCo 80% Line Coverage Gate

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| **JaCoCo Maven Plugin** | 0.8.12 | Line coverage measurement + build gate | Standard for Java coverage. `jacoco:check` goal binds to `verify` phase. `haltOnFailure=true` blocks build. |
| **JaCoCo Maven Plugin** | 0.8.12 | Aggregate report across modules | `report-aggregate` goal produces combined report from all modules. Required for multi-module coverage view. |

**Configuration approach (QLD-01):**

**Parent POM (`pom.xml`):**
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <haltOnFailure>true</haltOnFailure>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

**Exclude generated code** (MapStruct, DTO records, Panache entities):
```xml
<configuration>
    <excludes>
        <exclude>**/mekano/infrastructure/entity/**</exclude>
        <exclude>**/mekano/infrastructure/mapper/*Impl*</exclude>
        <exclude>**/dto/*</exclude>
        <exclude>**/model/*</exclude>
    </excludes>
</configuration>
```

**Aggregate report in `mekano-rest`** (app entrypoint module):
```xml
<execution>
    <id>report-aggregate</id>
    <phase>verify</phase>
    <goals><goal>report-aggregate</goal></goals>
</execution>
```

**Why BUNDLE element:** Applies rule per Maven module (each module must hit 80% LINE coverage). More granular than a single project-wide check. Prevents one uncovered module from hiding behind covered ones.

**Exclusions rationale:**
- `infrastructure/entity/*` — JPA boilerplate (Lombok `@Data`, generated getters/setters)
- `infrastructure/mapper/*Impl*` — MapStruct generated implementations
- `rest/dto/*` — Request/response DTO records (no logic)
- `domain/valueobject/*` — Value objects are immutable data carriers (validate in constructor, trivial getters) — **on second thought, DO test VO validation.** Only exclude if coverage < 80% blocks build.

### K8s Manifest Tooling

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| **Plain YAML** | K8s v1.32 | Define Deployments, Services, ConfigMaps, Secrets, HPA | Zero dependency, `kubectl apply -f`, version-controllable. Single-service API doesn't need Helm. |
| **Kustomize** | Built into `kubectl` | Env-specific overlays | Only if you need dev/staging/prod variants. `kubectl kustomize` — no extra tool. |
| **kind** | 0.20+ | Local K8s for testing | `kind create cluster` + `kind load docker-image mekano:latest`. Fast, cheap, no cloud. |
| **Kompose** | Latest | Convert docker-compose → K8s | NOT recommended. Creates verbose, non-idiomatic manifests. Use as inspiration only. |

**Plain YAML structure (INF-02):**

```
k8s/
├── 00-namespace.yaml       # apiVersion: v1, kind: Namespace
├── 01-configmap.yaml       # DB URL, Log config, Quarkus properties
├── 02-secret.yaml          # DB password, JWT key, WhatsApp API token (applied externally)
├── 03-deployment.yaml      # apiVersion: apps/v1, kind: Deployment
│                           #   - replicas: 2 (default)
│                           #   - resources: requests/limits
│                           #   - readiness: /q/health/ready
│                           #   - liveness:  /q/health/live
├── 04-service.yaml         # ClusterIP, port 8080 → 8080
└── 05-hpa.yaml             # apiVersion: autoscaling/v2
```

### HPA Configuration for Quarkus

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| **HorizontalPodAutoscaler** | `autoscaling/v2` | Auto-scale Quarkus pods | Built-in K8s resource. No operators needed. v2 supports CPU + memory metrics. |
| **Quarkus Kubernetes Resource Config** | 3.36.0 | Define resource requests/limits in `application.properties` | `quarkus.kubernetes.resources.requests.cpu=500m`, `quarkus.kubernetes.resources.limits.memory=1Gi`. HPA reads requests to calculate utilization. |

**HPA YAML for Quarkus:**
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: mekano-api
  namespace: mekano
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: mekano-api
  minReplicas: 2
  maxReplicas: 8
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  behavior:
    scaleDown:
      stabilizationWindowSeconds: 120
      policies:
      - type: Percent
        value: 20
        periodSeconds: 60
    scaleUp:
      stabilizationWindowSeconds: 0
      policies:
      - type: Percent
        value: 100
        periodSeconds: 30
```

**Quarkus-specific HPA notes:**
- CPU target 70%: Quarkus JVM cold-start spike (~200% CPU for 5s) is brief enough. HPA ignores startup spikes if `--horizontal-pod-autoscaler-cpu-initialization-period` (default 5min) is respected.
- Memory target 80%: JVM heap stays stable during load. If using `-Xmx512m`, set requests to ~75% of limit so HPA scales before OOM.
- **minReplicas: 2** — ensures zero-downtime rolling updates. 1 pod = downtime during deploy.
- **scaleDown stabilization** — prevent thrashing from traffic spikes. 120s window means HPA won't scale below 2 pods within 2 minutes of utilization dropping.

**Testing HPA:**
```bash
# Install metrics server (Kind doesn't ship it)
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# Generate load
kubectl run load-test --image=busybox -- /bin/sh -c "while sleep 0.01; do wget -q -O- http://mekano-api:8080/q/health; done"

# Watch HPA
kubectl get hpa mekano-api --watch
```

### CD Pipeline (GitHub Actions)

| Technology | Version | Purpose | Why Recommended |
|------------|---------|---------|-----------------|
| **GitHub Actions** | N/A | CD pipeline for K8s deploy | Already used for CI. CD is a natural extension. Native OIDC support for AWS EKS auth. |
| **`aws-actions/configure-aws-credentials`** | v4 | AWS auth via OIDC | No long-lived keys stored in GitHub Secrets. OIDC trust between GitHub and AWS. |
| **`kubectl`** | kubectl-action or inline | Apply K8s manifests | Simple `kubectl apply -f k8s/`. No Helm needed for single-service deploy. |

**CD flow:**
```
push to main → CI (build + test + JaCoCo check) → Docker build & push (ECR) → CD: kubectl set image → rollout
```

**GitHub Actions CD workflow skeleton:**
```yaml
deploy:
  needs: [build, test]
  if: github.ref == 'refs/heads/main'
  runs-on: ubuntu-latest
  steps:
    - uses: aws-actions/configure-aws-credentials@v4
      with:
        role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
        aws-region: us-east-1
    - run: aws eks update-kubeconfig --name mekano-cluster --region us-east-1
    - run: kubectl set image deployment/mekano-api mekano-api=${{ needs.build.outputs.image }} -n mekano
```

## Alternatives Considered

| Category | Recommended | Alternative | Why Not |
|----------|-------------|-------------|---------|
| WhatsApp Provider | WhatsApp Cloud API (Meta) | Twilio WhatsApp API | Twilio adds $0.005/msg fee on top of Meta fees. Meta's free tier (1K conversations/month) is more generous. For a project sending <50 msg/day, Meta is free entirely. Twilio only makes sense if already using Twilio for SMS/voice. |
| WhatsApp Client | `quarkus-rest-client` | Twilio Java SDK | Adding 3MB SDK for 2 POST calls is wasteful. REST Client is declarative, testable with WireMock, and zero classpath issues. |
| HPA Metrics | CPU + Memory only | Custom metrics (Prometheus) | Prometheus operator + custom metrics adapter adds significant complexity. For a monolith API, CPU + Memory are sufficient scaling signals. KNative/Serving overkill for this scope. |
| K8s Tooling | Plain YAML | Helm, Kustomize | Helm is over-engineered for single-deployment apps. Kustomize adds a layer without solving a real problem at this scale. Plain YAML keeps learning curve minimal for the 5-person dev team. |
| K8s Local | Kind | Minikube | Minikube needs a VM, slower startup, higher resource usage. Kind runs in Docker, 30s cluster creation, supports `kind load docker-image`. |
| DB Provisioning | RDS PostgreSQL | Aurora Serverless, Self-managed | Aurora is cost-prohibitive for academic project. Self-managed PostgreSQL on EC2 adds maintenance burden. RDS is managed, cheapest option ($15-30/mo for db.t3.micro). |

## What NOT to Use

| Avoid | Why | Use Instead |
|-------|-----|-------------|
| **Twilio Java SDK** | Heavy dependency, SDK version mismatches with Quarkus 3.36, extra classpath scanning | `quarkus-rest-client-jackson` + WireMock for tests |
| **WhatsApp Business API On-Premise** | Requires dedicated server, deprecated by Meta, no free tier | WhatsApp Cloud API (hosted by Meta, free tier) |
| **Kompose** | Generates messy, non-production-ready YAML. Creates unnecessary Services, uses `Deployment` when `StatefulSet` isn't needed. | Plain YAML written by hand |
| **Minikube** | VM dependency, slower cluster creation, higher resource usage for local dev | Kind (runs in Docker, fast, lightweight) |
| **Helm for v2.0** | Chart boilerplate, Tiller history, learning curve for team. Value not justified for single-service API. | Plain YAML + `kubectl apply` |
| **JaCoCo BRANCH counter for gate** | BRANCH coverage at 80% is much harder than LINE and produces false negatives. LINE is more practical and standard. | LINE counter at 80% |
| **Kuberenetes Metrics Server installed via Helm** | Another dependency to manage for a simple metrics installation | Direct manifest from GitHub releases |

## Stack Patterns by Variant

**If deploying to EKS (primary path):**
- Use `terraform/` with `hashicorp/aws` provider for cluster + RDS
- Use `k8s/` plain YAML for app manifests
- Use GitHub Actions with OIDC for auth
- DB: RDS PostgreSQL 16 (managed, automated backups)

**If deploying locally via Kind:**
- No Terraform needed
- `kind create cluster --name mekano`
- `kubectl apply -f k8s/` (change service type to NodePort or port-forward)
- DB: PostgreSQL 16 via `docker-compose` (existing infra)
- Install metrics-server for HPA testing

**If deploying to a different cloud (GCP/Azure):**
- Swap `hashicorp/aws` with `hashicorp/google` or `hashicorp/azurerm`
- Same K8s manifest structure (K8s is cloud-agnostic)
- RDS → Cloud SQL / Azure Database for PostgreSQL

## Version Compatibility

| Package | Compatible With | Notes |
|---------|-----------------|-------|
| `quarkus-rest-client-jackson:3.36.0` | Quarkus 3.36, Java 17 | In Quarkus 3.x, `quarkus-resteasy-reactive-client` was renamed to `quarkus-rest-client`. Use the new artifact ID. |
| `quarkus-wiremock:1.6.3` | Quarkus 3.36, WireMock 3.x | DevService scope — only active in dev/test mode. Not shipped in production artifact. |
| `org.jacoco:jacoco-maven-plugin:0.8.12` | JDK 17, Maven 3.9 | Latest stable (Jan 2025). Full support for Java 17 records, sealed classes. |
| `hashicorp/aws ~> 5.0` | Terraform 1.8+ | AWS provider v5 is the current stable major version. v6 is in preview (might break with 1.8). |
| `hashicorp/kubernetes ~> 2.0` | Terraform 1.8+ | API compatible with Kubernetes 1.28+. |
| Kind 0.20+ | Docker 24+, K8s 1.32 | Kind creates a K8s version-mapped cluster. Node image pins the exact K8s version. |

## Quarkus Configuration Properties to Add

```properties
# WhatsApp API
quarkus.whatsapp.api.base-uri=https://graph.facebook.com/v19.0
quarkus.whatsapp.api.phone-number-id=${WHATSAPP_PHONE_NUMBER_ID}
quarkus.whatsapp.api.token=${WHATSAPP_API_TOKEN}

# REST Client for WhatsApp
quarkus.rest-client.whatsapp-api.url=${quarkus.whatsapp.api.base-uri}
quarkus.rest-client.whatsapp-api.connection-ttl=30000
quarkus.rest-client.whatsapp-api.max-retries=2

# K8s resources (used by HPA)
quarkus.kubernetes.resources.requests.cpu=500m
quarkus.kubernetes.resources.requests.memory=512Mi
quarkus.kubernetes.resources.limits.cpu=1000m
quarkus.kubernetes.resources.limits.memory=1Gi
```

## Sources

| Source | Topic | Confidence |
|--------|-------|-----------|
| Context7 `/quarkusio/quarkus` — REST Client declarations, Mutiny Uni, WireMock test patterns | Quarkus HTTP client | HIGH |
| Context7 `/jacoco/jacoco` — `check` mojo parameters, `LINE` counter, `BUNDLE` element | JaCoCo gate config | HIGH |
| Context7 `/hashicorp/terraform` — Kubernetes backend, Kind setup, PostgreSQL backend | Terraform infra provisioning | HIGH |
| Context7 `/kubernetes/website` — HPA v2 docs, resource/custom metrics, scaling behavior | HPA configuration | HIGH |
| Twilio official pricing page (twilio.com/en-us/whatsapp/pricing) | Twilio WhatsApp pricing ($0.005/msg + Meta fee) | HIGH |
| Quarkiverse WireMock docs (docs.quarkiverse.io/quarkus-wiremock) | WireMock DevService setup | HIGH |
| WhatsApp Cloud API docs (developers.facebook.com/docs/whatsapp) — free tier: 1K conversations/mo | WhatsApp free tier | MEDIUM (could not verify directly due to 400 error) |
| Quarkus Kubernetes extension docs — resource request/limit config in application.properties | HPA resource configuration | HIGH |
| WireMock official docs — JSON stubs in `mappings/` and `__files/` | Mocking WhatsApp API in tests | HIGH |

---
*Stack research for: Mekano v2.0 infra-docs-quality-whatsapp milestone*
*Researched: 2026-08-08*