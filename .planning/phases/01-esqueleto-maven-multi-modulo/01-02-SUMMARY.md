---
phase: 01-esqueleto-maven-multi-modulo
plan: "02"
subsystem: maven-build
tags: [maven, multi-module, quarkus, clean-architecture, jandex, pom]
dependency_graph:
  requires:
    - "01-01-SUMMARY.md — root POM com packaging=pom e módulos declarados"
  provides:
    - "4 sub-módulos pom.xml com regras Clean Architecture enforced"
    - "Estrutura de diretórios Java completa com .gitkeep"
  affects:
    - "mekano-domain — domínio puro, packaging jar, somente lombok"
    - "mekano-application — casos de uso, CDI via quarkus-arc, jandex"
    - "mekano-infrastructure — adaptadores de persistência, jandex"
    - "mekano-adapter — entry point Quarkus, packaging quarkus, todas as extensões"
tech_stack:
  added:
    - "io.smallrye:jandex-maven-plugin:3.5.3 — CDI bean discovery em application, infrastructure, adapter"
    - "io.quarkus:quarkus-arc — CDI container para @ApplicationScoped nos use cases"
    - "io.quarkus:quarkus-rest-jackson — REST reativo com serialização Jackson"
    - "io.quarkus:quarkus-hibernate-orm-panache — ORM + Active Record pattern"
    - "io.quarkus:quarkus-flyway — migrations de banco"
    - "io.quarkus:quarkus-smallrye-openapi — documentação OpenAPI/Swagger"
    - "io.quarkus:quarkus-hibernate-validator — validação @Valid/@NotBlank"
    - "io.quarkus:quarkus-jdbc-postgresql — driver PostgreSQL"
  patterns:
    - "Clean Architecture enforced via dependências Maven: domain ← application, domain ← infrastructure, (domain + application + infrastructure:runtime) ← adapter"
    - "Jandex index gerado em compile-time em módulos com CDI beans (application, infrastructure, adapter)"
    - "packaging=quarkus exclusivo no adapter — define entry point do build Quarkus"
    - "scope=runtime para mekano-infrastructure no adapter — adapter não importa infra diretamente (Clean Architecture)"
key_files:
  created:
    - path: "mekano-domain/pom.xml"
      description: "Módulo domain — packaging jar, apenas lombok provided+optional, zero CDI/Quarkus"
    - path: "mekano-application/pom.xml"
      description: "Módulo application — packaging jar, domain dep, quarkus-arc, jandex make-index"
    - path: "mekano-infrastructure/pom.xml"
      description: "Módulo infrastructure — packaging jar, domain dep, jandex make-index, NÃO depende de application"
    - path: "mekano-adapter/pom.xml"
      description: "Módulo adapter — packaging quarkus, ÚNICO com quarkus-maven-plugin, jandex, 6 extensões, infra como runtime"
    - path: "mekano-domain/src/main/java/com/fiap/mekano/domain/.gitkeep"
      description: "Raiz do pacote domain para código Java"
    - path: "mekano-domain/src/test/java/com/fiap/mekano/domain/.gitkeep"
      description: "Raiz do pacote domain para testes"
    - path: "mekano-application/src/main/java/com/fiap/mekano/application/.gitkeep"
      description: "Raiz do pacote application para código Java"
    - path: "mekano-application/src/test/java/com/fiap/mekano/application/.gitkeep"
      description: "Raiz do pacote application para testes"
    - path: "mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/.gitkeep"
      description: "Raiz do pacote infrastructure para código Java"
    - path: "mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/.gitkeep"
      description: "Raiz do pacote infrastructure para testes"
    - path: "mekano-adapter/src/main/java/com/fiap/mekano/adapter/.gitkeep"
      description: "Raiz do pacote adapter para código Java"
    - path: "mekano-adapter/src/test/java/com/fiap/mekano/adapter/.gitkeep"
      description: "Raiz do pacote adapter para testes"
    - path: "mekano-adapter/src/main/resources/.gitkeep"
      description: "Diretório resources do adapter (será preenchido em 01-03 com application.properties)"
  modified: []
decisions:
  - "Jandex via goal 'jandex' (não 'make-index' como nome do goal, mas sim como id da execution) — goal real é 'jandex', id é 'make-index' conforme padrão do plugin io.smallrye"
  - "scope=runtime para mekano-infrastructure no adapter — enforça que o adapter não possa fazer import direto de classes da infra em compile-time"
  - "mekano-infrastructure NÃO depende de mekano-application — Clean Architecture: infrastructure só conhece o domain, não a camada de application"
  - "quarkus-maven-plugin declarado SOMENTE em mekano-adapter — evita conflito de empacotamento e garante único entry point de build"
  - "Build final (mvnw clean install -DskipTests) adiado para Plan 01-03 — requer application.properties para compilar sem erros de configuração Quarkus"
metrics:
  duration: "~5 minutos"
  completed: "2026-05-27"
  tasks_completed: 2
  tasks_total: 2
  files_created: 13
  files_modified: 0
---

# Phase 01 Plan 02: Sub-módulos Maven com POMs e Estrutura de Diretórios — Summary

**One-liner:** 4 sub-módulos Maven criados com regras de dependência Clean Architecture enforced nos pom.xml, jandex configurado nos módulos CDI, e 9 diretórios de código-fonte com .gitkeep.

## O que foi feito

### Task 1 — POMs dos módulos domain, application e infrastructure (commit `4e25ae4`)

Criados os três primeiros módulos do projeto multi-módulo Maven:

**`mekano-domain/pom.xml`** (packaging=jar):
- Única dependência: `lombok` com `scope=provided` + `optional=true`
- Sem jandex (domain não tem beans CDI — por design)
- Sem quarkus-arc, sem extensões Quarkus
- Representa o núcleo da Clean Architecture: zero dependências externas de runtime

**`mekano-application/pom.xml`** (packaging=jar):
- Depende de `mekano-domain` (compile scope)
- `quarkus-arc` para `@ApplicationScoped` nos use cases
- `jandex-maven-plugin` com execution `make-index` — CDI bean discovery
- `lombok` provided+optional

**`mekano-infrastructure/pom.xml`** (packaging=jar):
- Depende de `mekano-domain` (compile scope)
- **NÃO depende** de `mekano-application` — regra Clean Architecture
- `jandex-maven-plugin` com execution `make-index`
- `lombok` provided+optional

### Task 2 — mekano-adapter/pom.xml + estrutura de diretórios (commit `9a08eea`)

**`mekano-adapter/pom.xml`** (packaging=quarkus):
- **ÚNICO** módulo com `quarkus-maven-plugin` (Gotcha 1 — evita conflito de build)
- `<extensions>true</extensions>` + goals `build`, `generate-code`, `generate-code-tests`
- `jandex-maven-plugin` com execution `make-index`
- Dependências internas:
  - `mekano-domain` — compile
  - `mekano-application` — compile
  - `mekano-infrastructure` — **runtime** (Clean Architecture: adapter não importa infra diretamente em código)
- 6 extensões Quarkus declaradas:
  - `quarkus-rest-jackson` (EXT-01)
  - `quarkus-hibernate-orm-panache` (EXT-02)
  - `quarkus-flyway` (EXT-03)
  - `quarkus-smallrye-openapi` (EXT-04)
  - `quarkus-hibernate-validator` (EXT-05)
  - `quarkus-jdbc-postgresql` (EXT-10)
- `quarkus-junit5` + `rest-assured` para testes

**9 arquivos `.gitkeep`** criados para rastrear os diretórios de código-fonte no Git:
```
mekano-domain/src/main/java/com/fiap/mekano/domain/
mekano-domain/src/test/java/com/fiap/mekano/domain/
mekano-application/src/main/java/com/fiap/mekano/application/
mekano-application/src/test/java/com/fiap/mekano/application/
mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/
mekano-infrastructure/src/test/java/com/fiap/mekano/infrastructure/
mekano-adapter/src/main/java/com/fiap/mekano/adapter/
mekano-adapter/src/test/java/com/fiap/mekano/adapter/
mekano-adapter/src/main/resources/
```

## Regras Clean Architecture Enforced nos POMs

| Módulo | Pode importar | NÃO pode importar |
|--------|--------------|-------------------|
| `domain` | (nada) | tudo |
| `application` | `domain` | `infrastructure`, `adapter` |
| `infrastructure` | `domain` | `application`, `adapter` |
| `adapter` | `domain` (compile), `application` (compile) | `infrastructure` diretamente (runtime only) |

## Jandex — CDI Discovery por Módulo

| Módulo | Jandex configurado? | Motivo |
|--------|--------------------|----|
| `domain` | ❌ Não | Sem beans CDI — design intencional |
| `application` | ✅ Sim | `@ApplicationScoped` nos use cases |
| `infrastructure` | ✅ Sim | `@ApplicationScoped` nas implementações (Fase 4) |
| `adapter` | ✅ Sim | `@Path`, `@ApplicationScoped` nos resources |

## Decisões Tomadas

1. **scope=runtime para infrastructure no adapter** — garante que o código do adapter não faça `import` direto de classes da infrastructure. Se um desenvolvedor tentar, o compilador falha.

2. **Build adiado para Plan 01-03** — `./mvnw clean install -DskipTests` requer `application.properties` com datasource configurado para o Hibernate/Flyway não falhar. Executar o build agora causaria `BUILD FAILURE` esperado.

3. **Versões não declaradas nos módulos filhos** — todas as versões são gerenciadas pelo root BOM (Quarkus BOM + `dependencyManagement` do root pom.xml). Módulos filhos herdam via `<parent>`.

## Deviações do Plano

Nenhuma — plano executado exatamente como especificado, seguindo os Padrões 2 a 5 do RESEARCH.md.

## Known Stubs

Nenhum — este plano cria apenas estrutura de módulos (pom.xml + diretórios). Sem código Java ainda.

## Self-Check: PASSED

### Arquivos verificados como existentes:
- ✅ `mekano-domain/pom.xml`
- ✅ `mekano-application/pom.xml`
- ✅ `mekano-infrastructure/pom.xml`
- ✅ `mekano-adapter/pom.xml`
- ✅ 9/9 arquivos `.gitkeep`

### Commits verificados:
- ✅ `4e25ae4` — `feat(01-02): criar POMs dos modulos domain, application e infrastructure`
- ✅ `9a08eea` — `feat(01-02): criar adapter/pom.xml e estrutura de diretorios com .gitkeep`

### Critérios de sucesso:
- ✅ `mekano-domain/pom.xml` tem `<packaging>jar</packaging>`, lombok only, sem jandex
- ✅ `mekano-application/pom.xml` tem jandex + quarkus-arc + domain dependency
- ✅ `mekano-infrastructure/pom.xml` tem jandex + domain dependency
- ✅ `mekano-adapter/pom.xml` tem packaging=quarkus, quarkus-maven-plugin, jandex, 6 extensões, infrastructure como runtime
- ✅ 9 arquivos `.gitkeep` criados nos diretórios de código-fonte
- ✅ Tasks individualmente commitadas
- ⏳ `./mvnw clean install -DskipTests` — adiado para Plan 01-03 (requer application.properties)
