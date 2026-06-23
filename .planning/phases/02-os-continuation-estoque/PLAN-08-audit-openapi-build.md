# PLAN-08: Audit Log, OpenAPI, JaCoCo, OWASP DC, README

## Goal
Implement OsAuditLog (immutable transition history), add comprehensive OpenAPI annotations to all Phase 2 endpoints, configure JaCoCo 80% LINE coverage gate, configure OWASP Dependency Check, and write project README.

## Dependencies
- All other plans complete (entities, services, REST endpoints exist for annotation)
- Phase 2 endpoints exist for OpenAPI annotation pass

## Requirements Covered
DOC-02 (OpenAPI specification), D-66..D-69 (Audit log), D-73 (JaCoCo 80% gate), D-74 (OWASP DC), D-75 (README)

---

## Tasks

### Task 1: OsAuditLog Entity, Repository, and REST Endpoint

**Files created:**
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/repository/OsAuditLogRepositoryImpl.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/os/OsAuditLogResource.java`
- `mekano-rest/src/main/java/com/fiap/mekano/rest/api/dto/os/HistoricoResponse.java`

**Action:**

**OsAuditLogRepositoryImpl:**
- Implements `OsAuditLogRepositoryPort` (port created in PLAN-01).
- `@ApplicationScoped`, inject `OsAuditLogPanacheRepository`.
- **`save(OsAuditLog)`:** Persist without cache (immutable, append-only per D-69).
- **`findByOrdemDeServicoUuid(UUID osUuid)`:** HQL `"ordemDeServicoUuid = ?1 ORDER BY dataCriacao ASC"`. Returns list.

**OsAuditLog creation trigger:**
- Add audit logging to OS state transitions via CDI observer. Create `OsAuditLogObserver.java` in `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/service/` that observes all OS state transition events (`OrcamentoGeradoEvent`, `OrcamentoAprovadoEvent`, `OrcamentoReprovadoEvent`, `OSFinalizadaEvent`, plus cancellation events).
- The `@Observes` method calls `osAuditLogRepository.save()` for each event.
- Per D-67: log includes user ("sistema" for automatic transitions like SLA expiry).
- The snapshot JSON captures: OS uuid, status, services list, parts list, cliente, veiculo at the moment of transition. Use Jackson `ObjectMapper.writeValueAsString()` on the OS domain object.
- No explicit calls from service layer — purely event-driven.

**OsAuditLogResource:**
- `@Path("/os/{osUuid}/historico)`, `@RequestScoped`, `@RolesAllowed({"admin", "mecanico"})` per D-68.
- **`GET /`** — Returns `List<HistoricoResponse>` with transition history.
  ```java
  public record HistoricoResponse(
      UUID id,
      String statusOrigem,
      String statusDestino,
      String usuario,
      String snapshotJson,  // or parsed JSON object
      LocalDateTime dataCriacao
  ) {}
  ```

**Verification:**
```bash
./mvnw test -pl mekano-infrastructure -am -Dtest="OsAuditLogRepositoryImplTest"
./mvnw test -pl mekano-rest -am -Dtest="OsAuditLogResourceTest"
```

---

### Task 2: OpenAPI Annotation Pass on All Phase 2 Endpoints

**Files modified:**
All REST resources, DTOs, and mappers created in PLAN-06 and PLAN-07.

**Action:**
Per D-70 and D-71: Add/modify OpenAPI annotations on every Phase 2 endpoint.

**Resource-level annotations:**
- `@Tag(name = "...", description = "...")` on each resource class
- Verify `quarkus.smallrye-openapi.auto-add-security-requirement=true` is in `application.properties`

**Method-level annotations:**
Each endpoint must have:
- `@Operation(summary = "...", description = "...")` — Portuguese descriptions for domain concepts
- `@APIResponse(responseCode = "200", description = "...")`
- `@APIResponse(responseCode = "400", description = "Dados inválidos")`
- `@APIResponse(responseCode = "401", description = "Não autenticado")`
- `@APIResponse(responseCode = "403", description = "Acesso negado")`
- `@APIResponse(responseCode = "404", description = "Recurso não encontrado")`
- `@APIResponse(responseCode = "409", description = "Conflito")` for stock/duplicate errors

**DTO annotations:**
- `@Schema(description = "...", example = "...")` on each field
- Realistic examples: UUIDs, filenames, prices, dates

Example completeness check (per PLAN-06, PLAN-07):
| Resource | Endpoints | OpenAPI Status |
|----------|-----------|----------------|
| PecaResource | POST, GET /, GET /{uuid}, PUT /{uuid}, DELETE /{uuid}, PATCH /restore | ✅ Add annotations |
| RequisicaoCompraResource | GET /, GET /{uuid}, POST cancelar, POST comprar, POST receber | ✅ Add annotations |
| NfEntradaResource | POST /, GET /, GET /{uuid} | ✅ Add annotations |
| AlertaResource | GET / | ✅ Add annotations |
| OrcamentoResource | GET /{uuid}, POST aprovar, POST reprovar | ✅ Add annotations |
| OrdemDeServicoExecutionResource | POST iniciar, POST finalizar | ✅ Add annotations |
| OrdemDeServicoQueryResource | GET /, GET /{uuid} | ✅ Add annotations |
| OsMetricasResource | GET /tempo-medio | ✅ Add annotations |
| AdminUserResource | POST, GET, GET/{uuid}, PUT, DELETE, PATCH restore, POST reset-password | ✅ Add annotations |
| OsAuditLogResource | GET / | ✅ Add annotations |

**Verification:**
```bash
# Verify OpenAPI spec generates without errors
./mvnw compile -pl mekano-rest -am
# Start dev mode and check Swagger UI
# docker-compose up -d && ./mvnw quarkus:dev
# curl http://localhost:8080/q/openapi | grep -q "openapi" && echo "OpenAPI OK"
```

---

### Task 3: JaCoCo 80% LINE Coverage Gate

**Files modified:**
- `mekano-rest/pom.xml` (add JaCoCo plugin)
- Create test suite for coverage (ensure tests cover all new code paths)

**Action:**
Per Pattern 6 (Research Pitfall 6): Use stand-alone `jacoco-maven-plugin` NOT `quarkus-jacoco` extension.

Add to `mekano-rest/pom.xml`:
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
            <configuration>
                <exclClassLoaders>*QuarkusClassLoader</exclClassLoaders>
                <destFile>${project.build.directory}/jacoco-quarkus.exec</destFile>
                <append>true</append>
            </configuration>
        </execution>
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <dataFile>${project.build.directory}/jacoco-quarkus.exec</dataFile>
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
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
            <configuration>
                <dataFile>${project.build.directory}/jacoco-quarkus.exec</dataFile>
                <outputDirectory>${project.build.directory}/jacoco-report</outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Configure Surefire for JaCoCo multi-module (per Research):
```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <systemPropertyVariables>
            <quarkus.jacoco.data-file>${maven.multiModuleProjectDirectory}/target/jacoco.exec</quarkus.jacoco.data-file>
            <quarkus.jacoco.reuse-data-file>true</quarkus.jacoco.reuse-data-file>
            <quarkus.jacoco.report-location>${maven.multiModuleProjectDirectory}/target/coverage</quarkus.jacoco.report-location>
        </systemPropertyVariables>
        <argLine>@{argLine}</argLine>
    </configuration>
</plugin>
```

**Coverage gaps to address:**
- Ensure all domain model classes have pure JUnit 5 tests (constructors, factory methods, validation)
- Ensure all services have Mockito tests (success + error paths)
- Ensure all repositories have @QuarkusTest integration tests
- Ensure all REST endpoints have REST Assured tests

**Verification:**
```bash
./mvnw verify -pl mekano-rest -am  # Should build with 80%+ coverage
# Check report: open target/jacoco-report/index.html
```

---

### Task 4: OWASP Dependency Check

**Files modified:**
- `mekano-rest/pom.xml` (add OWASP DC plugin)
- `.github/workflows/ci.yml` (add OWASP step)

**Action:**
Per Research Pattern and D-74. Add to `mekano-rest/pom.xml`:
```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>12.2.2</version>
    <configuration>
        <format>ALL</format>
        <failBuildOnCVSS>11</failBuildOnCVSS>
        <nvdApiKey>${env.NVD_API_KEY}</nvdApiKey>
        <skipProvidedScope>false</skipProvidedScope>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

Add to CI pipeline `.github/workflows/ci.yml`:
```yaml
- name: OWASP Dependency Check
  run: ./mvnw dependency-check:check -pl mekano-rest -am --no-transfer-progress
  env:
    NVD_API_KEY: ${{ secrets.NVD_API_KEY }}
```

Note: `failBuildOnCVSS=11` means the build NEVER fails due to OWASP (CVSS max is 10). This is intentional per Research — OWASP report is generated for review but doesn't block CI. If a real vulnerability is found, it's surfaced in the HTML report.

**Verification:**
```bash
# Run OWASP check (requires NVD_API_KEY or works with rate limits)
./mvnw dependency-check:check -pl mekano-rest -am --no-transfer-progress
# Check report: open target/dependency-check-report.html
```

---

### Task 5: README.md Update

**Files modified:**
- `README.md` (project root — create if not exists, or update)

**Action:**
Per D-75: Write comprehensive README covering:

```markdown
# Mekano — API de Oficina Mecânica

## Visão Geral
[Project description, Clean Architecture, tech stack]

## Stack Tecnológica
- Java 17, Quarkus 3.36.0, Maven multi-módulo
- PostgreSQL 16, H2 (testes)
- JWT Ed25519, BCrypt
- MapStruct, Lombok, Jandex
- JUnit 5, Mockito, REST Assured, AssertJ

## Pré-requisitos
- JDK 17
- Docker (para PostgreSQL dev)
- Maven 3.9+ (wrapper incluso)

## Setup

### 1. Banco de Dados
```bash
docker-compose up -d
```

### 2. Chaves JWT
```bash
# Gerar par de chaves Ed25519
openssl genpkey -algorithm ed25519 -out ~/.mekano/secrets/privatekey.pem
openssl pkey -in ~/.mekano/secrets/privatekey.pem -pubout > mekano-rest/src/main/resources/publicKey.pem
```

### 3. Executar
```bash
./mvnw quarkus:dev
```

## Arquitetura
[Clean Architecture diagram, module dependencies, layer descriptions]

## Estrutura do Projeto
```
mekano-domain/     # Entidades, VOs, ports, eventos
mekano-application/  # Casos de uso, serviços
mekano-infrastructure/  # JPA, Flyway, repositórios, segurança
mekano-rest/       # Recursos REST, DTOs, mappers
```

## Módulos e Responsabilidades
| Módulo | Responsabilidade |
|--------|-----------------|
| Auth/Users | Autenticação JWT, roles, CRUD de usuários |
| Cliente/Veículo/Serviço | Cadastros básicos |
| Ordem de Serviço | Ciclo de vida completo da OS |
| Orçamento | Geração, aprovação, SLA |
| Estoque | Peças, requisições, NF entrada |

## API
- Base: `/api/v1`
- Docs: `/q/swagger-ui` (dev) | `/q/openapi` (spec JSON/YAML)
- Health: `/q/health`

## Fluxos Principais
[Brief description of key flows: OS creation, budget approval, stock reservation]

## Padrões de Código
- Clean Architecture com inversão de dependência
- `@Transactional` APENAS no service layer
- `@RequestScoped` em resources REST
- Reserva atômica de estoque via SQL nativo
- Eventos CDI síncronos para comunicação entre agregados
- Soft delete em todas as entidades

## Testes
```bash
./mvnw verify -pl mekano-rest -am   # Full suite + coverage
./mvnw test -pl mekano-domain              # Domain (puro, < 3s)
./mvnw test -pl mekano-application -am    # Application (Mockito)
./mvnw test -pl mekano-infrastructure -am # Integration (DevServices)
./mvnw test -pl mekano-rest -am           # REST (REST Assured)
```

## OWASP Dependency Check
```bash
export NVD_API_KEY=your_key
./mvnw dependency-check:check -pl mekano-rest -am
```
Reports generated in `target/dependency-check-report.html`.

## Variáveis de Ambiente
| Variável | Default | Descrição |
|----------|---------|-----------|
| DB_URL | jdbc:postgresql://localhost:5432/mekano | URL do banco |
| DB_USER | mekano | Usuário do banco |
| DB_PASSWORD | mekano | Senha do banco |
| MP_JWT_ISSUER | https://mekano.fiap.com.br/auth | Emissor JWT |
| NVD_API_KEY | — | Chave NVD para OWASP DC |

## Decisões Técnicas
[D-01 through D-75 summary or reference to STATE.md]
```

**Verification:**
```bash
# README exists and is well-formed
cat README.md | head -50
```

---

### Task 6: CI Pipeline Update + Build Configuration Finalization

**Files modified:**
- `.github/workflows/ci.yml` (add JaCoCo + OWASP steps)
- `mekano-rest/src/main/resources/application.properties` (add SLA + OpenAPI config)

**Action:**

**CI Pipeline (.github/workflows/ci.yml):**
```yaml
# After existing build step:
- name: Build & Test with Coverage
  run: ./mvnw verify -pl mekano-rest -am --no-transfer-progress

- name: OWASP Dependency Check
  run: ./mvnw dependency-check:check -pl mekano-rest -am --no-transfer-progress
  env:
    NVD_API_KEY: ${{ secrets.NVD_API_KEY }}
  continue-on-error: true  # Don't fail CI on OWASP warnings

- name: Upload JaCoCo Report
  uses: actions/upload-artifact@v4
  with:
    name: jacoco-report
    path: mekano-rest/target/jacoco-report/

- name: Upload OWASP Report
  uses: actions/upload-artifact@v4
  with:
    name: dependency-check-report
    path: mekano-rest/target/dependency-check-report.*
```

**application.properties additions:**
```properties
# SLA expiry (Phase 2)
sla.expiry.cron=0 0 */12 * * ?
sla.expiry.hours=72

# OpenAPI auto-add security
quarkus.smallrye-openapi.auto-add-security-requirement=true
```

**Verification:**
```bash
# Verify compile
./mvnw compile -pl mekano-rest -am

# Full build with coverage
./mvnw verify -pl mekano-rest -am
```

---

## Verification (Plan-Level)

```bash
# Full compile + test + coverage + OWASP
./mvnw verify -pl mekano-rest -am

# Check JaCoCo report
if [ -f "mekano-rest/target/jacoco-report/index.html" ]; then echo "Coverage report OK"; fi

# Check OWASP report
if [ -f "mekano-rest/target/dependency-check-report.html" ]; then echo "OWASP report OK"; fi

# Verify OpenAPI spec
curl http://localhost:8080/q/openapi | jq '.openapi' | head -5
```

## Risk Mitigation
- **JaCoCo double-instrumentation:** Use stand-alone `jacoco-maven-plugin` (not `quarkus-jacoco` extension). Pitfall 6 in Research.
- **OWASP DC NVD API Key:** `failBuildOnCVSS=11` ensures build never fails. NVD API key is optional but recommended for faster data downloads. Documented in README.
- **OpenAPI annotation completeness:** Systematic pass over all endpoint methods. Use grep to verify all `@Path` methods have `@Operation`:
  ```bash
  grep -r "@Path" mekano-rest/src/main/java/com/fiap/mekano/rest/api/ | wc -l
  grep -r "@Operation" mekano-rest/src/main/java/com/fiap/mekano/rest/api/ | wc -l
  # Should match (approximately)
  ```
- **Audit log immutability:** `OsAuditLogEntity` extends `PanacheEntityBase` directly (no BaseEntity). No setters for mutable fields. No `isActive`/`deletedAt`. Enforced by entity design.
