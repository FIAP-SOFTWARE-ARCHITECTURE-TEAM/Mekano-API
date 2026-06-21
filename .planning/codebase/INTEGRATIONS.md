# External Integrations

**Analysis Date:** 2026-06-20

## APIs & External Services

**Currently integrated:**
None. The application is self-contained with no external API calls outbound.

**Planned but not yet integrated:**
- JWT signing/verification (EdDSA/Ed25519) — referenced in `CLAUDE.md` docs but no `quarkus-smallrye-jwt` or `quarkus-smallrye-jwt-build` dependency in `pom.xml`; public key file `publicKey.pem` not found on disk.

## Data Storage

**Databases:**
- **PostgreSQL 16-alpine**
  - Production: `docker.io/library/postgres:16-alpine` via `docker-compose.yml`
  - Dev: Same docker-compose instance on `localhost:5432`
  - Test: DevServices auto-provisions PostgreSQL container (Quarkus test container support)
  - Config profile: `%dev` uses JDBC URL, `%test` uses DevServices, `%prod` uses env vars
  - Connection: `jdbc:postgresql://localhost:5432/mekano`
  - Credentials: `mekano`/`mekano` (default), overridable via `DB_USER`/`DB_PASSWORD`/`DB_URL` env vars
  - Client: JDBC via `quarkus-jdbc-postgresql` (managed by Quarkus BOM)
  - ORM: Hibernate ORM + Panache (`quarkus-hibernate-orm-panache`)
  - Migrations: Flyway (`quarkus-flyway`) with `migrate-at-start=true`
  - Schema management strategy: `validate` in both `%dev` and `%prod` (Flyway manages schema)
  - Flyway migration files: `mekano-infrastructure/src/main/resources/db/migration/`

**Flyway migrations (`mekano-infrastructure/src/main/resources/db/migration/`):**
| File | Description |
|------|-------------|
| `V1__create_users_table.sql` | Creates `users` table with UUID PK |
| `V2__create_refresh_tokens_table.sql` | Creates `refresh_tokens` table with FK to users |
| `V3__add_soft_delete_to_users.sql` | Adds `is_active`, `deleted_at` columns |
| `V4__add_audit_columns_to_users.sql` | Adds `created_by`, `updated_by`, `updated_at` columns |
| `V5__add_sequential_id.sql` | Hybrid ID: BIGSERIAL PK + UUID unique column |

**File Storage:**
- Local filesystem only — No object storage (S3, GCS, etc.) integrated

**Caching:**
- **Caffeine** (in-memory, via `quarkus-cache`)
  - Cache name: `users`
  - Configuration: `initial-capacity=10`, `maximum-size=100`, `expire-after-write=60s`
  - Annotated on: `findById` / `findByEmail` (`@CacheResult`), `save` / `markAsDeleted` (`@CacheInvalidate`)
  - Configured at `mekano-rest/src/main/resources/application.properties:72-75`

## Authentication & Identity

**Auth Provider:**
- **Custom JWT-based** (in progress — EdDSA/Ed25519 algorithm planned)
  - Implementation approach: SmallRye JWT with `mp.jwt.*` configuration namespace
  - Algorithm: Ed25519/EdDSA (not RSA)
  - Public key location: `mekano-rest/src/main/resources/publicKey.pem` (planned)
  - Private key location: `~/.mekano/secrets/privatekey.pem` (planned, gitignored)
  - Issuer: Configurable via `MP_JWT_ISSUER` env var
  - Test profile: `JwtTestProfile` generates an in-memory Ed25519 key pair
  - Note: `@RolesAllowed("user")` is used on `UserResource` (`mekano-rest/src/main/java/com/fiap/mekano/rest/api/UserResource.java:58`)
  - `@PermitAll` is used on AuthResource (planned but file not yet created)
  - `quarkus.http.auth.proactive=false` planned (not yet in config)

**Password Hashing:**
- **BCrypt** via `io.quarkus.security.runtime.BcryptUtil`
  - Implementation: `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/security/BcryptPasswordHasher.java`
  - Interface: `mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/PasswordHasher.java`
  - Used by: `UserService` (`mekano-application/src/main/java/com/fiap/mekano/application/service/user/UserService.java`)

## Monitoring & Observability

**Health Checks:**
- **SmallRye Health** (`quarkus-smallrye-health`)
  - Endpoints: `/q/health`, `/q/health/live`, `/q/health/ready`, `/q/health/started`
  - Auto-registers `DataSourceHealthCheck` via Agroal datasource pool
  - Custom check: `ApplicationLivenessCheck` (`mekano-rest/src/main/java/com/fiap/mekano/rest/observability/ApplicationLivenessCheck.java`)

**Metrics:**
- **Micrometer + Prometheus registry** (`quarkus-micrometer-registry-prometheus`)
  - Endpoint: `/q/metrics` (Prometheus format)
  - Auto-binds: JVM metrics, HTTP server metrics, Agroal connection pool metrics

**Logs:**
- **Structured JSON logging** (`quarkus-logging-json`)
  - Console output in JSON format (non-pretty-printed)
  - Configurable levels: DEBUG (dev), INFO (prod), WARN (test)
  - Exception mapper logs unhandled exceptions via `io.quarkus.logging.Log` at `mekano-rest/src/main/java/com/fiap/mekano/rest/api/exception/ApiExceptionMapper.java:42`

**Error Tracking:**
- None integrated

## CI/CD & Deployment

**Hosting:**
- Docker container (Quarkus-based images)
- Multiple Dockerfile options at `src/main/docker/`:
  - `Dockerfile.jvm` — JVM mode on UBI 9 OpenJDK 17
  - `Dockerfile.native` — Native binary on UBI 9 minimal
  - `Dockerfile.native-micro` — Native binary on micro image (smallest)
  - `Dockerfile.legacy-jar` — Legacy JAR deployment

**CI Pipeline:**
- **GitHub Actions** (`.github/workflows/ci.yml`)
  - Trigger: pushes and PRs to `main` branch
  - Runner: `ubuntu-latest`
  - Steps:
    1. Checkout (`actions/checkout@v4`)
    2. JDK 17 setup with Eclipse Temurin (`actions/setup-java@v4`)
    3. Maven cache (`actions/cache@v4`)
    4. Build & test: `mvn verify -pl mekano-rest -am --no-transfer-progress`
  - PostgreSQL is auto-provisioned via DevServices (no separate Docker step)

## Environment Configuration

**Required env vars (production):**
| Variable | Default | Purpose |
|----------|---------|---------|
| `DB_USER` | `mekano` | PostgreSQL username |
| `DB_PASSWORD` | `mekano` | PostgreSQL password |
| `DB_URL` | `jdbc:postgresql://localhost:5432/mekano` | JDBC connection URL |
| `MP_JWT_ISSUER` | `https://mekano.fiap.com.br/auth` | JWT issuer claim (planned) |

**Secrets location:**
- Private key: `~/.mekano/secrets/privatekey.pem` (gitignored, not committed)
- Database credentials: `docker-compose.yml` uses `POSTGRES_USER`/`POSTGRES_PASSWORD` env vars with defaults
- `%prod` profile reads DB credentials from environment variables
- No `.env` file committed to repository

## Webhooks & Callbacks

**Incoming:**
- None

**Outgoing:**
- None

**Events:**
- Internal domain events via CDI (`CdiEventPublisher` at `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/event/CdiEventPublisher.java`)
  - Interface: `EventPublisher` at `mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/EventPublisher.java`
  - Event type: `UserCreatedEvent` at `mekano-domain/src/main/java/com/fiap/mekano/domain/event/UserCreatedEvent.java`
  - Implementation: CDI `javax.enterprise.event.Event.fire()` (synchronous, in-process)
  - No external message broker (RabbitMQ, Kafka, etc.)

## Resilience & Reliability

**Fault Tolerance (SmallRye MP Fault Tolerance):**
- `@Retry(maxRetries = 3)` on `UserRepositoryImpl.findById()` / `findByEmail()` (read operations only)
- `@Timeout(value = 5, unit = ChronoUnit.SECONDS)` on `UserRepositoryImpl.save()` (write operations)
- No `@CircuitBreaker` (intentionally omitted — PostgreSQL local does not justify it)
- No `@Bulkhead` or `@Fallback` implemented

**Rate Limiting:**
- `TokenBucketRateLimiter` referenced in `Mekano-infrastructure` module docs (planned for login endpoint, not yet on disk)

---

*Integration audit: 2026-06-20*
