# Technology Stack

**Analysis Date:** 2025-01-31

## Languages

**Primary:**
- Java 17 - All application and test source code (`src/main/java/`, `src/test/java/`)

## Runtime

**Environment:**
- JVM (standard mode) via Red Hat UBI9 OpenJDK 17 runtime (`registry.access.redhat.com/ubi9/openjdk-17-runtime:1.24`)
- Native binary (optional) via GraalVM native-image, packaged with Red Hat UBI9 minimal (`registry.access.redhat.com/ubi9/ubi-minimal:9.7`)

**Package Manager:**
- Maven 3.x (via Maven Wrapper `mvnw` / `mvnw.cmd`)
- Lockfile: Not present (Maven uses `pom.xml` for dependency management)

## Frameworks

**Core:**
- Quarkus 3.36.0 — Full application framework (REST, CDI, OpenAPI)
  - BOM: `io.quarkus.platform:quarkus-bom:3.36.0`

**Dependency Injection:**
- Quarkus ArC (CDI 4.x) — `quarkus-arc` — Context and Dependency Injection, zero-runtime-overhead IoC container

**REST API:**
- Quarkus REST — `quarkus-rest` — Jakarta REST (JAX-RS) server-side implementation
  - Jakarta WS RS annotations (`jakarta.ws.rs.*`) used in `src/main/java/com/fiap/GreetingResource.java`

**API Documentation:**
- SmallRye OpenAPI — `quarkus-smallrye-openapi` — Auto-generates OpenAPI 3.x spec and Swagger UI from JAX-RS annotations

**Testing:**
- Quarkus JUnit 5 — `quarkus-junit` (test scope) — `@QuarkusTest` and `@QuarkusIntegrationTest` runner
- REST Assured — `io.rest-assured:rest-assured` (test scope) — HTTP integration test DSL

## Key Dependencies

**Critical:**
- `io.quarkus:quarkus-arc` — CDI container; required for all bean injection
- `io.quarkus:quarkus-rest` — JAX-RS REST layer; all HTTP endpoints depend on this
- `io.quarkus:quarkus-smallrye-openapi` — OpenAPI/Swagger UI; exposes `/q/openapi` and `/q/swagger-ui`

**Testing:**
- `io.quarkus:quarkus-junit` — JUnit 5 integration with Quarkus lifecycle
- `io.rest-assured:rest-assured` — HTTP-level assertion library for endpoint tests

## Build Tools

**Build System:** Maven 3.x via wrapper scripts `mvnw` / `mvnw.cmd`

**Plugins:**
- `io.quarkus.platform:quarkus-maven-plugin:3.36.0` — Quarkus build lifecycle (compile, package, dev mode)
- `org.apache.maven.plugins:maven-compiler-plugin:3.15.0` — Java 17 compilation with `-parameters` flag
- `org.apache.maven.plugins:maven-surefire-plugin:3.5.4` — Unit test execution (`@QuarkusTest`)
- `org.apache.maven.plugins:maven-failsafe-plugin:3.5.4` — Integration test execution (`@QuarkusIntegrationTest`)

**Build Profiles:**
- `native` — Activates GraalVM native compilation (`quarkus.native.enabled=true`), disables JAR output, enables integration tests

**Common Build Commands:**
```bash
./mvnw quarkus:dev              # Start dev mode (hot reload)
./mvnw package                  # Build JVM fat JAR → target/quarkus-app/
./mvnw package -Dnative         # Build native binary → target/*-runner
./mvnw test                     # Run unit tests (@QuarkusTest)
./mvnw verify -Pnative          # Run integration tests against native binary
```

## Configuration

**Environment:**
- Configuration file: `src/main/resources/application.properties` (currently empty — Quarkus defaults apply)
- No custom properties defined; default HTTP port is `8080`

**Build:**
- `pom.xml` — Maven build descriptor with all dependency and plugin configuration
- `src/main/docker/Dockerfile.jvm` — JVM-mode container image build
- `src/main/docker/Dockerfile.legacy-jar` — Legacy JAR container image build
- `src/main/docker/Dockerfile.native` — Native binary container image (UBI9 minimal)
- `src/main/docker/Dockerfile.native-micro` — Native binary micro-container image

## Platform Requirements

**Development:**
- Java 17 JDK
- Maven 3.x (or use included `mvnw` wrapper)
- Docker (optional, for containerized runs)
- GraalVM with `native-image` tool (only for native profile)

**Production:**
- JVM mode: Red Hat UBI9 OpenJDK 17 runtime container (`openjdk-17-runtime:1.24`)
- Native mode: Red Hat UBI9 minimal container (`ubi-minimal:9.7`), no JVM required
- Exposed port: `8080`
- HTTP host bound to `0.0.0.0` (set via `JAVA_OPTS_APPEND` in JVM Dockerfile or via entrypoint arg in native Dockerfile)

---

*Stack analysis: 2025-01-31*
