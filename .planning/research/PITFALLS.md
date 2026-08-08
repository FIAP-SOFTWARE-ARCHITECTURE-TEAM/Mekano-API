# Pitfalls Research

**Domain:** Quarkus Clean Architecture API + WhatsApp/K8s/Terraform infra
**Researched:** 2026-08-08
**Confidence:** HIGH (verified against context7 docs, official Quarkus guides, JaCoCo docs, Terraform docs)

## Critical Pitfalls

### Pitfall 1: WhatsApp Token Expiration — 24h Short-Lived Token

**What goes wrong:**
The WhatsApp Cloud API access token expires **every 24 hours**. If you hardcode the token or store it without a rotation mechanism, notifications stop silently after 24h. The system appears to work in dev (where you refresh manually) but breaks in production after day one.

**Why it happens:**
WhatsApp Cloud API uses Facebook access tokens with a maximum lifetime of 24 hours. Developers test by copying a fresh token from the Meta Developer dashboard, forget about it, and deploy. The integration fails silently because REST clients typically receive a 401 or 403 but most teams don't have monitoring on WhatsApp responses.

**How to avoid:**
1. Implement a **token refresh service** that uses `app_id` + `app_secret` to exchange for a new token before expiry (Meta's long-lived token endpoint supports 60-day tokens — use that instead).
2. Store the token in **Kubernetes Secret** (not ConfigMap) and reference via environment variable — NOT in application.properties.
3. Add a **health check** (`@Readiness`) that pings WhatsApp API's test endpoint to verify the token is still valid. If the health check fails, the pod is taken out of rotation.
4. Monitor HTTP 401 responses from WhatsApp API with a metric + alert.

**Warning signs:**
- Notifications work on day 1, stop on day 2
- No WhatsApp API responses are logged or monitored
- Token is stored in application.properties or hardcoded

**Phase to address:**
WhatsApp Integration Phase — make the TokenManager service the first component built, not an afterthought.

---

### Pitfall 2: WhatsApp Rate Limits — 1K Messages/Day Per Phone Number

**What goes wrong:**
WhatsApp Cloud API limits marketing messages to **1,000 per day per phone number** in the free tier (Tier 1). If the notification system sends bulk messages (e.g., "OS finalizada" to all customers at once), it hits the limit and messages are silently rejected.

**Why it happens:**
Meta enforces rate limits based on business account quality rating. The limit is not documented in the "quick start" guides — you only discover it when the API starts returning code `130429` (rate overlimit). The limit scales with account quality but starts very low.

**How to avoid:**
1. Implement a **rate limiter** in the WhatsApp service that tracks daily send count per phone number.
2. Use **message templates** for all notifications — template messages have higher throughput than free-form. Pre-approve templates in Meta Business Manager.
3. Queue notifications via CDI events (`Event<NotificacaoWhatsAppEvent>`) with a **background processor** that respects rate limits, rather than sending synchronously.
4. Log API error code `130429` and retry with exponential backoff — do not discard.
5. If approaching the limit, fall back gracefully (log + alert admin) rather than crashing.

**Warning signs:**
- No rate limit tracking logic in the WhatsApp service
- All notifications are sent synchronously (blocking the request thread)
- No error handling for code `130429`
- No daily count reset logic

**Phase to address:**
WhatsApp Integration Phase — build the rate limiter BEFORE the sender. It's harder to retroactively add throttling than to have it from the start.

---

### Pitfall 3: K8s Liveness vs Readiness Probe Confusion (Pod Restart Loops)

**What goes wrong:**
Confusing liveness (`/q/health/live`) and readiness (`/q/health/ready`) probes causes pod restart loops. If you set the **liveness check to depend on database connectivity**, a temporary DB blip restarts the pod — restarting the pod doesn't fix the DB. The pod enters CrashLoopBackoff unnecessarily.

**Why it happens:**
Developers default to checking "everything is OK" in both probes. The Quarkus guide warns: liveness = "is the app running?"; readiness = "can the app handle traffic?" Many teams copy-paste health check code without understanding the distinction.

**How to avoid:**
1. **Liveness probe** (`/q/health/live`): Only verify the JVM/Quarkus is alive (the existing `ApplicationLivenessCheck` is correct — it always returns `up`).
2. **Readiness probe** (`/q/health/ready`): Check database connectivity, WhatsApp API connectivity, and other external dependencies.
3. **Startup probe** (`/q/health/started`): Use `@Startup` for slow-starting containers (Quarkus starts in <1s so this is less critical, but useful if Flyway migrations take time).
4. Set `initialDelaySeconds` to at least 10s for readiness — Quarkus starts fast but Flyway migrations + database warmup can take 5-10s.
5. **Never** make liveness dependent on external services.

**Warning signs:**
- `@Liveness` annotated beans check database connections
- Both probes have the same checks (copy-paste)
- Pods restart during routine DB maintenance

**Phase to address:**
K8s Manifests Phase — define probe paths and thresholds in application.properties BEFORE generating manifests.

---

### Pitfall 4: JaCoCo Multi-Module Coverage — Per-Module 80% Is Not 80% Total

**What goes wrong:**
The current JaCoCo config uses `<element>BUNDLE</element>` which checks coverage **per Maven module**. Module `mekano-domain` might have 95%, `mekano-rest` 70%. The build passes each module independently, but **overall project coverage could be below 80%** because small modules with low coverage don't fail the build.

**Why it happens:**
The `jacoco:check` goal with `BUNDLE` element runs once per module that has the plugin configured. Each module's check sees only its own classes. There's no **cross-module aggregation** that produces a single project-wide number. Developers optimize coverage in the wrong modules.

**How to avoid:**
1. Configure a **dedicated aggregation module** or use `jacoco:report-aggregate` in the parent POM to produce a single report across ALL modules. This requires the `report-aggregate` goal (available since JaCoCo 0.7.7) which collects `.exec` files from dependent projects.
2. OR move the `jacoco:check` execution to the **parent POM** with `<element>BUNDLE</element>` pointing at the aggregated report, not individual modules.
3. Set `<dataFileIncludes>` to collect all `jacoco.exec` files from child modules.
4. Run coverage check at the **verify phase** of `mekano-rest` (which depends on all other modules) so it sees the full picture.

**Exclude generated code:**
- MapStruct implementation classes (suffixed `Impl`, generated in `target/generated-sources/`)
- Lombok-generated methods (JaCoCo sees these as covered/uncovered lines)
- Add `**/*MapperImpl.class` and `**/generated/**` to exclusions

**Warning signs:**
- Each module reports >80% individually but `mvn verify` passes despite obvious test gaps in some modules
- MapStruct `*MapperImpl` classes appear as uncovered in the report
- No aggregated report is generated during builds
- The existing exclusions (`*Dto.class`, `*Entity.class`, `*Resource.class`) are correct but need to add MapStruct Impl classes

**Phase to address:**
JaCoCo Coverage Gate Phase — reconfigure the aggregation strategy FIRST, then measure. Measuring without aggregation gives false confidence.

---

### Pitfall 5: Clean Code Refactoring Scope Creep (Rewriting Working Code)

**What goes wrong:**
The "Clean Code and SOLID" task (QLD-02) devolves into rewriting working code for stylistic preferences. Developers rename variables, extract methods, reorder imports, add interfaces "just in case" — introducing bugs in code that had 517 passing tests.

**Why it happens:**
Clean Code is subjective. Without a defined scope (what patterns to fix, what to leave alone), each developer applies their own standards. The existing code is already Clean Architecture — the remaining issues are minor naming inconsistencies and documentation gaps, not architectural problems.

**How to avoid:**
1. Define a **fixed list of refactoring targets** before starting: no more than 5-10 specific issues (e.g., "fix Placa/PlacaVeiculo VO duplication", "rename salvar→save in PecaRepositoryPort", "remove dead mapper classes").
2. **Ban stylistic changes** — no renaming, reformatting, or method extraction unless it's on the list.
3. Run the full test suite (`./mvnw verify -pl mekano-rest -am`) after EVERY change. A clean code change that breaks tests is not clean code.
4. Use a linter (Checkstyle or ErrorProne) instead of manual review for code style — automate it.
5. **Accept good enough.** The project has 10 days left. Perfect code is the enemy of done code.

**Warning signs:**
- PRs that touch 20+ files for "clean code" with test changes
- Discussion shifting from "does it work" to "is it elegant"
- Refactoring identified issues that weren't on the original backlog

**Phase to address:**
QLD-02 Refactoring Phase — create a strict refactoring scope document FIRST, get team sign-off, then execute.

---

### Pitfall 6: Tech Debt Shortcut — Mocking WhatsApp Instead of Building Real Integration

**What goes wrong:**
To save time, the team builds a `WhatsAppNotifierMock` implementation that logs to console and calls it "done". The real WhatsApp API has different error codes, rate limits, token behavior, and message format requirements. The integration works in dev but fails in production.

**Why it happens:**
The 10-day deadline pressures the team to cut corners. Mocking is tempting because the real API requires a Meta Business Account, template approval, and webhook verification. The mock hides all the complexity that WILL bite in production.

**How to avoid:**
1. Use **WhatsApp Cloud API's sandbox** (free, no real phone numbers needed) for testing — it behaves like the real API with rate limits, token checks, and error codes.
2. If sandbox is not available, use **WireMock** to simulate the real API responses, including error scenarios (401, 429, 500) — not a no-op mock.
3. Build the `WhatsAppApiClient` as a **Quarkus REST Client** (`@RegisterRestClient`) against the real endpoint URL, configurable via `application.properties`. Swap the base URI for sandbox vs production.
4. Write **at least one integration test** that calls the sandbox API and verifies the full send + webhook callback flow.

**Warning signs:**
- The WhatsApp client has no HTTP calls to external services
- No WhatsApp API dependency is declared in `pom.xml`
- The test for WhatsApp is `assertDoesNotThrow()` on a mock that does nothing

**Phase to address:**
WhatsApp Integration Phase — build against the sandbox API from day 1. A mock-only strategy will fail at production integration testing.

---

### Pitfall 7: Terraform State Management — Local State Checked Into Git

**What goes wrong:**
Team uses local `terraform.tfstate` files that get committed to git or, worse, ignored by `.gitignore` and lost. When two devs run `terraform apply` simultaneously, the state corrupts. When the CI/CD pipeline runs, it has a stale local state or no state at all, causing resource duplication.

**Why it happens:**
Terraform works fine locally with local state for the first few applies. The project has no cloud account (K8s via Minikube), so the team doesn't see the need for remote state. By the time the CI/CD pipeline needs to update infrastructure, the local state is outdated or lost.

**How to avoid:**
1. Use a **remote backend** from the start — even for local development. Options:
   - **S3 backend** (AWS): `terraform { backend "s3" { bucket = "...", key = "mekano/terraform.tfstate", region = "...", use_lockfile = true } }`
   - **Local file backend** with consistent path: Shared network drive or WSL-mounted volume if no cloud is available.
2. Enable **state locking** to prevent concurrent modifications. The S3 backend supports `use_lockfile = true` for S3-based locking (DynamoDB is deprecated).
3. Add **`terraform.tfstate*` to `.gitignore`** (CRITICAL — do NOT commit state files, they contain secrets).
4. Pin the **Terraform provider versions** with `required_providers`:
   ```hcl
   terraform {
     required_providers {
       kubernetes = {
         source  = "hashicorp/kubernetes"
         version = "~> 2.35.0"
       }
     }
     required_version = "~> 1.9.0"
   }
   ```
5. Run `terraform plan` in CI/CD with `-out=tfplan` and `terraform apply tfplan` to prevent drift.

**Warning signs:**
- `terraform.tfstate` appears in git commits
- No `.gitignore` entry for `*.tfstate*`
- Two developers can run `terraform apply` simultaneously without errors (means no locking)
- No `required_providers` block in `.tf` files

**Phase to address:**
Terraform Provisioning Phase — configure backend and locking BEFORE writing the first resource. Migrating from local to remote state mid-project is painful.

---

### Pitfall 8: Test Coverage Glut — Writing Low-Value Tests to Hit 80%

**What goes wrong:**
To meet the 80% coverage gate, developers write getter/setter tests, no-op tests, and "test framework initialized" tests. The coverage number goes up but the test suite doesn't catch regressions. The build passes with 80% coverage but bugs slip through because the wrong code is tested.

**Why it happens:**
JaCoCo LINE coverage counts every executed line equally. A test that calls `entity.getId()` and asserts it's non-null "covers" the line but provides zero regression protection. Teams optimize for the metric instead of the outcome.

**How to avoid:**
1. Use **BRANCH coverage instead of LINE** as the primary metric (change `<counter>BRANCH</counter>`) — it's harder to game and correlates better with test quality. Set the minimum to 65-70% for BRANCH.
2. Exclude generated code, DTOs, and trivial getters/setters from the coverage check (already done for `*Dto.class`, `*Entity.class` — verify the exclusions work).
3. Add **mutations testing** (Pitest) as a quality signal without a hard gate — it catches tests that assert without verifying.
4. Focus new tests on:
   - **Domain logic** (value objects validation, entity state machines, business rules) — currently the HIGHEST value per line of test
   - **Edge cases** in application services (null inputs, duplicates, concurrent modifications)
   - **Error paths** in resources (validation errors, 404, 409)
5. Do NOT write tests for:
   - MapStruct mappers (test the behavior, not the mapping)
   - JPA repository methods (unless custom query)
   - Generated code

**Warning signs:**
- Tests are titled "testGettersAndSetters" or "testEntityCreation"
- Coverage report shows 100% on entity classes
- New tests don't assert behavior, only that "no exception was thrown"
- Coverage went up but no new business logic is tested

**Phase to address:**
JaCoCo Coverage Gate Phase — define test quality criteria alongside coverage targets. Use branch coverage for the gate.

---

### Pitfall 9: HPA Misconfiguration — Autoscaling on Wrong Metrics

**What goes wrong:**
The HPA is configured to scale on CPU utilization, but Quarkus on Java 17 with a JVM heap of 512MB typically sits at 30-50% CPU under normal load. The HPA never triggers scaling, so during peak traffic the application melts down. OR: the HPA scales based on memory, but the JVM heap pre-allocates and never releases memory, so it scales up infinitely.

**Why it happens:**
Kubernetes HPA defaults are CPU-based. Java applications have a different resource profile than Go/Node.js apps — the JVM uses steady CPU and memory even at low request rates. Default thresholds (CPU 80%) may never be reached for a Quarkus app serving 100 requests/second on a single core.

**How to avoid:**
1. **Benchmark before configuring HPA**: Run a load test (`hey` or `k6`) against the app to measure baseline CPU/memory at various request rates.
2. Set **realistic thresholds**:
   - CPU target: 70% (not 80%) — Quarkus uses more CPU than memory
   - Add memory target if the app allocates per-request objects (likely with JPA + MapStruct)
3. **Test the HPA** with `kubectl run -i --tty load-generator --image=busybox -- /bin/sh -c "while true; do wget -q -O- http://mekano-api:8080/api/v1/servicos; done"` and verify `kubectl get hpa` shows scaling.
4. Consider using **KEDA** if the app's load pattern is event-driven (CDI events for WhatsApp notifications + OS status transitions), which scales on event queue depth rather than CPU.
5. Set `minReplicas` to at least 2 and `maxReplicas` to 5-10 for reasonable cost vs performance.

**Warning signs:**
- HPA configured before any load testing was done
- No resource `requests`/`limits` set on the container (HPA can't scale without resource metrics)
- CPU target is the default 80% without understanding the app's profile
- The app runs on Minikube (single node) where HPA is less meaningful

**Phase to address:**
K8s Manifests Phase — run load tests to determine baseline metrics, then configure HPA. Don't guess the thresholds.

---

### Pitfall 10: Documentation Drift — README Goes Stale Within Days

**What goes wrong:**
The team invests time creating beautiful README, Mermaid diagrams, Swagger docs, and a Miro board. Two days later, someone changes an endpoint path or adds a new migration, and the docs are outdated. By week 2, nobody trusts the docs, so nobody reads them, so nobody updates them — dead documents.

**Why it happens:**
Documentation has no **update trigger** tied to code changes. Code is reviewed in PRs; docs are not. There's no CI check that verifies documentation matches the current state of the code.

**How to avoid:**
1. **Link docs to code** — Swagger/OpenAPI should be auto-generated by Quarkus (`quarkus-swagger-ui` and `quarkus-smallrye-openapi`), never manually maintained.
2. Put **Mermaid diagrams in the README** that are generated from code or verified in CI. Use a tool like `mermaid-cli` to snapshot-check diagram changes.
3. Add a **CI step** that warns if README hasn't been updated when API changes are detected (`git diff --name-only HEAD~1 | grep -q "src/main/java"`).
4. Keep the **Miro board** as a high-level architecture overview only — don't put implementation details there. Accept that it will be slightly out of sync and document the date of last update.
5. For the **video demo** (DOC-04): record it LAST (on day 9-10), not first. Record fresh after all code changes are done.
6. Write documentation as **HOWTO guides** (task-oriented) rather than reference docs — they're more useful and more frequently updated.

**Warning signs:**
- The README endpoint list doesn't match `mekano-rest/src/main/java/com/fiap/mekano/rest/api/`
- Swagger UI shows different contracts than the README
- Miro board has no "last updated" date
- PRs don't include documentation updates

**Phase to address:**
Documentation Phase — generate API docs from code, keep architecture docs high-level, and add a CI warning for doc drift.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Mock WhatsApp instead of real API | Saves 2-3 days on Meta Business setup | Integration fails in production, 5+ days to fix | NEVER — use sandbox or WireMock |
| JaCoCo per-module (no aggregation) | Simple config, works now | False 80% pass, real coverage may be 60% | NEVER for a release — fix before coverage gate |
| Terraform local state | Works immediately, no cloud setup | State loss, concurrent corruption, CI fails | Only for first 2 applies, migrate ASAP |
| Manual token refresh (WhatsApp) | Quick implementation | 24h expiry == silent failure in production | NEVER — build the refresh service |
| Stylistic code refactoring | Feels productive | Introduces bugs, breaks tests, burns time | NEVER in a 10-day milestone |
| Resource resources without limits | Easy, no tuning needed | No HPA metric data, pods can OOM | Temporary during initial deployment, fix before HPA |
| README with hand-typed endpoints | Looks complete | Drifts immediately, loses trust | NEVER — use auto-generated Swagger for API docs |
| Single replica K8s deployment | Simple, uses less resources | HPA can't scale, single point of failure | Only for dev/Minikube, never for staging/prod |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| **WhatsApp Cloud API** | Storing token in `application.properties` (committed to git) | Store in K8s Secret, inject via env var, rotate programmatically |
| **WhatsApp Cloud API** | Sending sync HTTP request on the Quarkus REST thread | Use `@Asynchronous` or CDI event + background processor |
| **WhatsApp Cloud API** | Ignoring webhook signature validation | Validate `X-Hub-Signature-256` HMAC-SHA256 on every webhook callback |
| **WhatsApp Cloud API** | Using free-form messages instead of templates | Templates: pre-approved, higher throughput, required for business-initiated messages |
| **K8s ConfigMap** | Storing DB passwords in ConfigMap (plaintext) | Use K8s Secret with `opaque` type, reference via `env.valueFrom.secretKeyRef` |
| **K8s HPA** | Forgetting `requests` in container spec | HPA requires resource requests to calculate target utilization — limits alone are insufficient |
| **K8s health probes** | Using same path for liveness and readiness | Liveness = `/q/health/live`, Readiness = `/q/health/ready` — they have different purposes |
| **Terraform S3 backend** | Not enabling `use_lockfile` | Without locking, concurrent `apply` destroys state |
| **Terraform providers** | No version constraint (`version = "~> X.Y"`) | Unpinned providers can upgrade automatically and break infrastructure |
| **JaCoCo + MapStruct** | Not excluding `*MapperImpl.class` | Generated mapper implementations show as 0% coverage and drag down the aggregate number |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| **Sync WhatsApp API call** | REST endpoint latency spikes to 500ms-2s per notification | Use `@Asynchronous` or event queue + batch sender | At 10+ concurrent notifications (during OS batch finalization) |
| **Database-read in every health probe** | Readiness probe queries DB every 10s, adds 6 queries/min per pod | Cache probe status or use a simple connection check (not a full query) | At 5+ pods, the extra probe load is visible in DB metrics |
| **JVM heap > container limit** | Pod gets OOMKilled because JVM heap exceeds container memory limit | Set `-Xmx` to 70% of container memory limit (e.g., `-Xmx300m` for 512Mi limit) | At any scale — JVM doesn't respect cgroup limits by default on Java 17 (use `-XX:+UseContainerSupport`, which is on by default in Java 17, but DOUBLE CHECK your `-Xmx` doesn't exceed the limit) |
| **No connection pooling tuning** | DB connections exhausted during HPA scale-up | Tune `quarkus.datasource.jdbc.max-size` relative to `maxReplicas` | At 3+ pods with default 20 connections each = 60 connections to a small PostgreSQL |
| **Vanity refactoring (renaming)** | Renames cascade through 15+ files, test changes for the same logic | Ban stylistic changes, measure refactoring value in "bugs caught" not "lines changed" | Immediately — wastes 1-2 days of a 10-day milestone |
| **Full test suite on every commit** | PR CI takes 15+ minutes for 517 tests | Split CI: fast unit tests on commit, full suite on merge to main | When CI pipeline blocks merges repeatedly |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| **WhatsApp token in application.properties** | Token leaked in git history; any developer can send messages as the business | Store in K8s Secret or env var; NEVER commit tokens |
| **No webhook signature validation** | Attacker can send fake status updates ("pagamento recusado") to the webhook endpoint | Validate `X-Hub-Signature-256` with HMAC-SHA256 using the app secret |
| **Terraform state file in git** | State contains DB connection strings, possibly passwords | Add `*.tfstate*` to `.gitignore`; use remote backend |
| **Checkstyle/linter bypass during refactoring** | Security-relevant warnings (injection, null safety) are hidden in unrelated formatting changes | Run linter AFTER refactoring, not before — or skip formatting-only commits |
| **No rate limiting on webhook endpoint** | Attacker floods webhook endpoint, causing resource exhaustion or triggering fake notifications | Apply `@RateLimit` (or filter) on the webhook POST endpoint |
| **Exposing internal UUIDs in WhatsApp messages** | UUIDs are guessable; attacker can infer system structure | Send only display-friendly IDs or short codes in WhatsApp notifications |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| **Generic WhatsApp message** | Customer doesn't know what "OS #42" refers to | Include vehicle model + plate and a readable status description: "Seu Fiat Uno (ABC-1234) está pronto para retirada" |
| **No opt-out from WhatsApp notifications** | Customer receives unwanted messages | Add a `notificationPrefs` field to Cliente — none/OFF by default |
| **Portuguese text with proper encoding issues** | Accented characters display as garbage | Ensure WhatsApp message sender uses UTF-8 and the template is submitted with proper charset |
| **Unclear document phase in audio/video** | Viewers can't tell which code version the video demonstrates | Include a prominent "Recorded for v2.0" caption in the video, and update the README with the recording date |
| **Mermaid diagram with stale endpoint paths** | Developer tries a documented endpoint that 404s | Autogenerate from Quarkus OpenAPI spec; if manual, add a CI check for drift |

## "Looks Done But Isn't" Checklist

- **[ ] WhatsApp Integration:** Often missing token refresh logic — verify the token is rotated programmatically before 24h expiry
- **[ ] WhatsApp Integration:** Often missing `X-Hub-Signature-256` validation — verify webhook endpoint validates every incoming request
- **[ ] K8s Manifests:** Often missing resource requests/limits — verify every container has both `requests` and `limits` (HPA needs them)
- **[ ] K8s Manifests:** Often missing ConfigMap/Secret names for WhatsApp tokens — verify secrets are referenced, not hardcoded
- **[ ] K8s HPA:** Often missing `minReplicas`/`maxReplicas` or set to `minReplicas: 1` (no HA) — verify at least 2 replicas
- **[ ] Terraform:** Often missing `required_providers` version constraints — verify every provider has a version pin
- **[ ] Terraform:** Often missing backend config — verify `terraform init` asks for a backend or shows remote state
- **[ ] JaCoCo:** Often missing MapStruct `*MapperImpl` exclusions — verify the coverage report doesn't include generated mapper classes
- **[ ] JaCoCo:** Often missing aggregation for multi-module — verify the coverage number covers ALL modules, not just one
- **[ ] Clean Code refactoring:** Often missing a defined scope — verify there's a written list of exactly what to refactor before starting
- **[ ] README:** Often missing Swagger link — verify the README links to `/q/swagger-ui/` (auto-generated, always up-to-date)
- **[ ] README:** Often missing HPA load testing instructions — verify there's a "how to test autoscaling" section with `kubectl run load-generator` command
- **[ ] Video demo:** Often recorded too early — verify the video is recorded AFTER all code changes are merged
- **[ ] CD pipeline:** Often missing `terraform plan` — verify the pipeline runs `terraform plan` before `terraform apply`

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| WhatsApp token expired | LOW | Refresh via `app_id` + `app_secret` endpoint; add health check monitoring |
| WhatsApp rate limit hit | MEDIUM | Wait 24h for quota reset; implement queuing; request tier upgrade in Meta dashboard |
| K8s pod crash loop from wrong probes | LOW | `kubectl edit deployment` to fix probe config; no redeploy needed |
| HPA not scaling | MEDIUM | Run load test to find real resource usage; adjust target metrics; verify `requests` are set |
| Terraform state corrupt | HIGH | Restore from S3 versioned backups or last known-good state; manual resource reconciliation |
| Terraform local state lost | HIGH | `terraform import` every resource; manual reconciliation; takes hours |
| JaCoCo false 80% pass | MEDIUM | Add `report-aggregate` goal; add MapStruct exclusions; re-run and find real number |
| Refactoring broke tests | LOW | `git checkout .` on the refactored files; re-run tests; restrict refactoring scope |
| README out of sync with API | LOW | Point readers to Swagger UI as source of truth; remove endpoint listing from README |
| CD pipeline failed on Terraform | MEDIUM | Check backend state; `terraform init -reconfigure`; ensure service principal has correct permissions |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| WhatsApp token expiration | WhatsApp Integration | TokenManager integration test that verifies token refresh before expiry |
| WhatsApp rate limits | WhatsApp Integration | Rate limiter unit test with daily quota boundary conditions |
| Liveness vs readiness confusion | K8s Manifests | `kubectl describe pod` shows different probe paths; health check tests pass |
| JaCoCo multi-module aggregation | JaCoCo Coverage Gate | `./mvnw verify -pl mekano-rest -am` produces ONE aggregated report with MapStruct excluded |
| Clean Code scope creep | QLD-02 Refactoring | Refactoring PR touches only files on the pre-approved scope list |
| Mock-only WhatsApp (no real integration) | WhatsApp Integration | WireMock integration test verifies HTTP call signatures and error handling |
| Terraform local state | Terraform Provisioning | `terraform init` prompts for backend; `terraform.tfstate*` in `.gitignore` |
| Low-value tests for coverage | JaCoCo Coverage Gate | New tests don't test getters/setters; branch coverage >65% |
| HPA wrong metrics | K8s Manifests | Load test produces HPA scaling event within 2 minutes |
| Documentation drift | Documentation | CI warns on README mismatch; Swagger is auto-generated |
| WhatsApp token in application.properties | WhatsApp Integration | No token string found in any `.properties` or `.yaml` file (only env var refs) |
| No webhook signature validation | WhatsApp Integration | Exists: test verifies 401 response when signature is missing/invalid |
| No container resource requests | K8s Manifests | Every Deployment container has both `requests.cpu` and `requests.memory` |

## Sources

- **WhatsApp Cloud API:** Meta official docs via Context7 (`/fbsamples/whatsapp-api-examples`) — rate limits, token types, webhook signature validation, message templates
- **Quarkus K8s:** Official Quarkus guides — "Deploying to Kubernetes" (health probes, ConfigMap/Secret, env vars, HPA), "SmallRye Health" (liveness/readiness/startup distinction), "Kubernetes Config" (external configuration from ConfigMaps/Secrets)
- **JaCoCo:** Official JaCoCo docs via Context7 (`/websites/jacoco_jacoco_trunk_doc`) — `report-aggregate` goal, excludes for coverage checks, FAQ on exclude behavior (agent vs report)
- **Terraform:** HashiCorp official docs via Context7 (`/websites/developer_hashicorp_terraform`) — S3 backend, state locking, `required_providers`, `use_lockfile` for S3 locking
- **Project AGENTS.md:** Verified codebase analysis — existing JaCoCo config (BUNDLE element, per-module), existing health check (ApplicationLivenessCheck with `@Liveness`), existing exclusion patterns
- **PROJECT.md:** Milestone requirements — 10-day deadline, 5 developers, WhatsApp notifications, K8s manifests, Terraform, 80% coverage, Clean Code refactoring
- **Personal experience:** Common Quarkus K8s deployment issues, WhatsApp integration patterns, JaCoCo multi-module aggregation failings, Clean Code refactoring scope creep

---

*Pitfalls research for: Mekano v2.0 infra-docs-quality-whatsapp milestone*
*Researched: 2026-08-08*