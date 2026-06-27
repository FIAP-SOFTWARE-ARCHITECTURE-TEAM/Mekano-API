---
created: 2026-06-27T23:30:00.190Z
title: Criar AGENTS.md para aplicar engenharia de contexto no projeto
area: planning
files:
  - CLAUDE.md
  - pom.xml
  - mekano-rest/pom.xml
  - mekano-application/pom.xml
  - mekano-infrastructure/pom.xml
  - mekano-domain/pom.xml
  - .planning/ROADMAP.md
  - .planning/STATE.md
---

## Problem

O projeto Mekano possui diversas convenções de arquitetura (Clean Architecture multi-módulo), stack tecnológica (Quarkus 3.36, MapStruct, Lombok, JWT Ed25519, Flyway, H2 compatibilidade) e decisões de design (hybrid ID, soft delete, @Transactional em use case, etc.) que não estão documentadas em formato estruturado para consumo por IA. A ausência de um AGENTS.md faz com que a IA alucine sobre arquitetura, convenções e padrões do projeto, gerando código inconsistente e perdendo tempo com correções.

## Solution

Criar um arquivo AGENTS.md na raiz do projeto que consolide:
1. Stack tecnológica e versões
2. Estrutura de módulos e dependências
3. Convenções de código (entidades, VOs, ports, services, resources, DTOs, mappers)
4. Regras de build (quarkus-maven-plugin, jandex, annotation processor order)
5. Configurações críticas (JWT, datasource, Flyway, CORS)
6. Padrões de teste por camada
7. Gotchas (armadilhas conhecidas)
8. Decision Records resumidos

Basear no conteúdo existente do CLAUDE.md e expandir com referências a arquivos-fonte.
