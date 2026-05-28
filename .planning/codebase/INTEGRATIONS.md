# External Integrations

**Analysis Date:** 2025-01-31

## APIs & External Services

**None detected.**
- The current codebase (`src/main/java/com/fiap/GreetingResource.java`) contains only a single stub REST endpoint returning a static string.
- No outbound HTTP clients, third-party SDKs, or external API calls are present.

## Data Storage

**Databases:**
- None — No database extension or datasource is configured. No `quarkus-hibernate-orm`, `quarkus-jdbc-*`, `quarkus-mongodb`, `quarkus-redis`, or equivalent dependency is present in `pom.xml`.

**File Storage:**
- Local filesystem only (no object storage SDK detected)

**Caching:**
- None — No `quarkus-cache`, Redis, or in-memory cache extension present

## Authentication & Identity

**Auth Provider:**
- None — No `quarkus-oidc`, `quarkus-security`, `quarkus-smallrye-jwt`, or any auth library is declared in `pom.xml`.
- All endpoints are currently unauthenticated.

## Messaging & Events

**Message Brokers:**
- None — No `quarkus-messaging`, `quarkus-smallrye-reactive-messaging`, Kafka, AMQP, or JMS extension present.

## Monitoring & Observability

**Health Checks:**
- Not configured — No `quarkus-smallrye-health` extension present.

**Metrics:**
- Not configured — No `quarkus-micrometer` or `quarkus-smallrye-metrics` extension present.

**Error Tracking:**
- None

**Logs:**
- JBoss LogManager via `org.jboss.logmanager.LogManager` (set in surefire/failsafe configuration in `pom.xml` and via `JAVA_OPTS_APPEND` in `src/main/docker/Dockerfile.jvm`)
- Log output goes to stdout; no external log aggregation configured.

## API Documentation (Exposed)

**OpenAPI / Swagger UI:**
- SmallRye OpenAPI (`quarkus-smallrye-openapi`) auto-generates spec from JAX-RS annotations.
- Endpoints exposed at runtime:
  - `GET /q/openapi` — OpenAPI 3.x JSON/YAML spec
  - `GET /q/swagger-ui` — Interactive Swagger UI (dev mode default)

## CI/CD & Deployment

**Hosting:**
- Docker / container-based deployment (Dockerfiles provided in `src/main/docker/`)
- No cloud-provider-specific deployment config (no `app.yaml`, `serverless.yml`, `fly.toml`, Helm charts, etc.)

**Container Registry:**
- Base images pulled from `registry.access.redhat.com` (Red Hat Universal Base Image)
  - JVM: `registry.access.redhat.com/ubi9/openjdk-17-runtime:1.24`
  - Native: `registry.access.redhat.com/ubi9/ubi-minimal:9.7`

**CI Pipeline:**
- None detected — No `.github/workflows/`, `.gitlab-ci.yml`, `Jenkinsfile`, or similar CI configuration found.

## Environment Configuration

**Required env vars:**
- None currently required — `src/main/resources/application.properties` is empty; all Quarkus defaults apply.

**Optional Docker env vars (JVM mode):**
- `JAVA_OPTS` — Override JVM options
- `JAVA_OPTS_APPEND` — Append JVM options (default: `-Dquarkus.http.host=0.0.0.0 -Djava.util.logging.manager=org.jboss.logmanager.LogManager`)
- `JAVA_MAX_MEM_RATIO` — Heap size ratio relative to container memory limit (default: 50%)
- `JAVA_DEBUG` — Enable remote debugging (default: false)
- `JAVA_DEBUG_PORT` — Remote debug port (default: 5005)

**Secrets location:**
- No secrets management in place. No `.env` files, Vault integration, or secrets provider configured.

## Webhooks & Callbacks

**Incoming:**
- None configured beyond the stub `GET /hello` endpoint in `src/main/java/com/fiap/GreetingResource.java`

**Outgoing:**
- None

---

*Integration audit: 2025-01-31*
