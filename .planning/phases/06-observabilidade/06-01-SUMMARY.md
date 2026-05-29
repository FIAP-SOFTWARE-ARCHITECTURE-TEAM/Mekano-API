---
phase: 06-observabilidade
plan: 01
subsystem: adapter/build
tags: [quarkus, observability, health, metrics, prometheus, dependencies]
requirements: [EXT-06, EXT-07, DEV-04, DEV-05]
dependency_graph:
  requires:
    - "mekano-adapter/pom.xml (Phase 5 baseline)"
    - "quarkus-bom 3.36.0 (root pom)"
  provides:
    - "io.quarkus:quarkus-smallrye-health:3.36.0"
    - "io.quarkus:quarkus-micrometer-registry-prometheus:3.36.0"
    - "endpoints: /q/health, /q/health/live, /q/health/ready, /q/health/started, /q/metrics"
  affects:
    - "06-02 (OpenAPI polish)"
    - "06-03 (custom ApplicationLivenessCheck)"
    - "06-04 (ObservabilityEndpointsTest)"
tech-stack:
  added:
    - "SmallRye Health 4.3.0 (transitivo via quarkus-smallrye-health)"
    - "Micrometer Core 1.16.5 + registry-prometheus-simpleclient 1.16.5"
  patterns:
    - "BOM-managed versions (sem <version> nas dependencies)"
    - "Convention-over-configuration: zero properties novas, defaults Quarkus"
key-files:
  modified:
    - "mekano-adapter/pom.xml"
  created: []
decisions:
  - "Versões 100% gerenciadas pelo quarkus-bom 3.36.0 (research §Standard Stack)"
  - "Nenhuma alteração em application.properties (D-03 / Pattern 4 do research)"
  - "quarkus-micrometer-registry-prometheus preferido sobre quarkus-smallrye-metrics (deprecated)"
metrics:
  duration: "~3 min"
  completed: "2026-05-29"
  tasks: 1
  files_changed: 1
---

# Phase 6 Plan 1: Habilitar Extensões de Health e Metrics — Summary

Adicionadas `quarkus-smallrye-health` e `quarkus-micrometer-registry-prometheus` ao `mekano-adapter/pom.xml`, sem versão (BOM 3.36.0 gerencia). Build full reactor compila com SUCCESS e Quarkus passa a auto-expor `/q/health/*` (DataSourceHealthCheck auto-registrado para o pool Agroal) e `/q/metrics` (JVM, HTTP server, Agroal binders auto-ativados).

## Tasks Executadas

| Task | Descrição | Commit |
|------|-----------|--------|
| 1 | Adicionar EXT-06 e EXT-07 ao pom.xml do adapter | `3c6baf2` |

## Versões Resolvidas (via `mvn dependency:tree`)

| Artifact | Versão | Origem |
|----------|--------|--------|
| `io.quarkus:quarkus-smallrye-health` | 3.36.0 | quarkus-bom |
| `io.smallrye:smallrye-health` (transitivo) | 4.3.0 | quarkus-smallrye-health |
| `io.smallrye:smallrye-health-provided-checks` | 4.3.0 | quarkus-smallrye-health |
| `io.quarkus:quarkus-micrometer-registry-prometheus` | 3.36.0 | quarkus-bom |
| `io.quarkus:quarkus-micrometer` (transitivo) | 3.36.0 | quarkus-micrometer-registry-prometheus |
| `io.micrometer:micrometer-core` | 1.16.5 | quarkus-micrometer |
| `io.micrometer:micrometer-registry-prometheus-simpleclient` | 1.16.5 | quarkus-micrometer-registry-prometheus |

## Verificação

- `./mvnw clean package -pl mekano-adapter -am -DskipTests` → **BUILD SUCCESS** ✅
- `./mvnw dependency:tree -pl mekano-adapter` → ambas extensões resolvidas em 3.36.0 ✅
- `git diff` mostra exatamente +13 linhas (2 blocos `<dependency>` + comentários), zero modificações em outras dependências ✅
- Nenhum warning sobre versões conflitantes ✅

## Deviations from Plan

Nenhuma — plano executado exatamente como escrito. As duas dependências foram inseridas no bloco indicado (logo após EXT-10), sem `<version>`, sem properties novas, sem código adicional.

## Hand-off para Próximos Planos

- **06-02 (OpenAPI polish):** Vai adicionar `@APIResponse(content + schema)` e `@RequestBody examples` em `UserResource` (D-05). Não depende deste plano, mas pode coexistir.
- **06-03 (Custom ApplicationLivenessCheck):** Vai criar `ApplicationLivenessCheck implements HealthCheck` em `mekano-adapter/.../observability/` (D-01). **Depende de EXT-06 (este plano)** para que `org.eclipse.microprofile.health.*` esteja no classpath.
- **06-04 (ObservabilityEndpointsTest):** Vai validar via `@QuarkusTest` + REST Assured que `/q/health`, `/q/health/live`, `/q/health/ready` retornam 200/UP e que `/q/metrics` contém `# HELP` / `# TYPE`. **Depende de EXT-06 e EXT-07 (este plano)**.

## Self-Check: PASSED

- FOUND: `mekano-adapter/pom.xml` contém `<artifactId>quarkus-smallrye-health</artifactId>` (sem `<version>`)
- FOUND: `mekano-adapter/pom.xml` contém `<artifactId>quarkus-micrometer-registry-prometheus</artifactId>` (sem `<version>`)
- FOUND: commit `3c6baf2` em `git log`
- FOUND: BUILD SUCCESS no `./mvnw clean package -pl mekano-adapter -am -DskipTests`
- FOUND: dependency:tree lista as duas extensões resolvidas em 3.36.0
