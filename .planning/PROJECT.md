# Mekano — Clean Architecture Quarkus API

## What This Is

API REST em Java com Quarkus 3.36.0 estruturada em múltiplos módulos Maven seguindo Clean Architecture. O projeto transforma o boilerplate gerado pelo Quarkus em uma base arquitetural robusta, com separação clara entre domínio, casos de uso, infraestrutura e adaptadores REST.

## Core Value

Demonstrar Clean Architecture aplicada ao ecossistema Quarkus de forma didática e funcional — com separação de responsabilidades real entre módulos Maven, sem acoplamento entre camadas.

## Requirements

### Validated

- ✓ Scaffold Quarkus 3.36.0 com Java 17 e Maven — existente
- ✓ Suporte a empacotamento JVM e nativo (Dockerfiles presentes) — existente

### Active

- [ ] Estrutura multi-módulos Maven: `domain`, `application`, `infrastructure`, `adapter`
- [ ] Regras de dependência entre módulos refletidas nos `pom.xml` (domain sem deps, application → domain, infrastructure → domain, adapter → domain + application)
- [ ] Módulo `domain`: entidades puras sem anotações JPA, Value Objects, interfaces de ports (in/out), exceções de domínio — apenas Lombok como dependência opcional
- [ ] Módulo `application`: casos de uso (`CreateUserUseCase` etc.), orquestração via ports, `@ApplicationScoped` permitido
- [ ] Módulo `infrastructure`: entidades JPA com Panache, `UserRepositoryImpl`, migrations Flyway, MapStruct entity↔domain, configuração técnica Quarkus
- [ ] Módulo `adapter`: `UserResource` (JAX-RS), DTOs separados Request/Response, validação `@Valid`/`@NotNull`, MapStruct domain↔DTO, ExceptionMappers, OpenAPI/Swagger
- [ ] Exemplo `User` cobrindo fluxo completo: `UserResource` → `CreateUserUseCase` → `UserRepositoryPort` → `UserRepositoryImpl` → banco
- [ ] PostgreSQL via `docker-compose.yml` com Quarkus conectado via `application.properties`
- [ ] Extensões Quarkus: `quarkus-rest-jackson`, `quarkus-hibernate-orm-panache`, `quarkus-flyway`, `quarkus-smallrye-openapi`, `quarkus-hibernate-validator`, `quarkus-smallrye-health`, `quarkus-micrometer`, `quarkus-smallrye-fault-tolerance`, `quarkus-smallrye-jwt`

### Out of Scope

- Domínio de negócio real além do exemplo `User` — será definido em milestone futuro
- Frontend / UI — API only
- Deploy em cloud / Kubernetes — fora do escopo desta fase
- Autenticação JWT funcional end-to-end — extensão incluída mas fluxo completo de auth fora do escopo

## Context

- Projeto acadêmico FIAP — disciplina de Arquitetura de Software
- Base: scaffold Quarkus gerado, apenas `GreetingResource.java` existe hoje
- `application.properties` vazio — toda configuração será criada do zero
- Docker presente (Dockerfiles existentes para JVM e nativo)
- PostgreSQL como banco de dados, provisionado via `docker-compose.yml`
- Tradeoffs conscientemente adotados: `@ApplicationScoped` no `application`, anotações JPA apenas no `infrastructure`, MapStruct para todos os mapeamentos entre camadas

## Constraints

- **Tech Stack**: Java 17 + Quarkus 3.36.0 — não negociável
- **Build**: Maven multi-módulo — estrutura de módulos é o objetivo central
- **Banco**: PostgreSQL — via docker-compose, não H2 em produção
- **Arquitetura**: Clean Architecture — regras de dependência entre módulos devem ser cumpridas nos `pom.xml`

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Entidades JPA apenas no `infrastructure` | Mantém domínio puro, sem vazamento de concerns de persistência | — Pending |
| MapStruct para todos os mapeamentos | Elimina mapeamento manual sujeito a erro; gera código em compile-time | — Pending |
| DTOs Request e Response separados | Request carrega validações; Response controla exposição — evita over-posting e over-fetching | — Pending |
| `@ApplicationScoped` permitido no `application` | CDI é neutro o suficiente; benefício de container supera o tradeoff | — Pending |
| PostgreSQL via docker-compose | Ambiente de dev reproduzível sem instalação local | — Pending |

## Evolution

Este documento evolui a cada transição de fase e marco de milestone.

**Após cada fase** (via `/gsd-transition`):
1. Requirements invalidados? → Mover para Out of Scope com motivo
2. Requirements validados? → Mover para Validated com referência de fase
3. Novos requirements surgiram? → Adicionar em Active
4. Decisões a registrar? → Adicionar em Key Decisions
5. "What This Is" ainda preciso? → Atualizar se mudou

**Após cada milestone** (via `/gsd-complete-milestone`):
1. Revisão completa de todas as seções
2. Core Value check — ainda é a prioridade certa?
3. Auditoria de Out of Scope — motivos ainda válidos?
4. Atualizar Context com estado atual

---
*Last updated: 2026-05-27 after initialization*
