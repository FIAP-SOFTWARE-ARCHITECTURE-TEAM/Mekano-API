# Mekano — Sistema Integrado de Atendimento e Execução de Serviços para Oficina Mecânica

## What This Is

API REST monolítica em Java 17 com Quarkus 3.36.0 seguindo Clean Architecture e DDD, para gestão de oficinas mecânicas de médio porte. O sistema gerencia o ciclo completo de ordens de serviço (desde recebimento até entrega), controle de estoque de peças/insumos, processamento de pagamentos, trilha de auditoria e SLA. O core do sistema (OS lifecycle, estoque, pagamento, audit) está implementado e funcional.

## Current Milestone: v2.0 infra-docs-quality-whatsapp

**Goal:** Evoluir o projeto de uma API funcional para um produto entregável com infraestrutura de produção, documentação completa, qualidade assegurada e integração externa com WhatsApp.

**Target features:**
- Infraestrutura: Docker revisado, K8s manifests, Terraform, HPA, CI/CD pipeline
- Documentação: README completo, diagramas de sequência, Swagger, Miro, vídeo demonstrativo
- Qualidade: 80% cobertura de testes, refactoring Clean Code + SOLID
- Integração WhatsApp: notificação de aprovação/recusa de orçamento e OS finalizada
- Melhorias: listagem ordenada de OS por prioridade de status, verificação de endpoints

## Core Value

Gerenciar o ciclo de vida completo das Ordens de Serviço — do recebimento do veículo à entrega — com rastreabilidade, controle de estoque e cobrança integrados.

## Requirements

### Validated

<!-- Shipped and confirmed valuable. -->

- ✓ Autenticação JWT (Ed25519/EdDSA)
- ✓ CRUD de usuários com soft delete
- ✓ Cache Caffeine em consultas de usuário
- ✓ Flyway migrations (V1-V34)
- ✓ Arquitetura multi-módulo (domain, application, infrastructure, rest)
- ✓ Cobertura de testes com JUnit 5, Mockito, REST Assured, AssertJ
- ✓ Pipeline CI via GitHub Actions
- ✓ Dockerfiles (JVM e Native)
- ✓ Ciclo completo de OS (recebimento → entrega) com máquina de estados
- ✓ Pipeline de estoque (reserva atômica, débito, liberação, requisição de compra, NF entrada)
- ✓ Pagamento idempotente com ProcessedEventRepositoryImpl
- ✓ Auditoria completa de transições de OS
- ✓ SLA com cancelamento automático de OS
- ✓ Endpoints públicos de status e aprovação de orçamento
- ✓ 517 testes, 0 falhas (v1.0)
- ✓ Verificação formal com VERIFICATION.md e traceability 37/37

### Active

<!-- Current scope. Building toward these. -->

#### Documentação e Meta
- [ ] **DOC-04**: Gravar e disponibilizar vídeo demonstrativo do ambiente (até 15 min)
- [ ] **DOC-05**: README.md com descrição da solução, objetivos da fase 2, instruções de execução local, deploy K8s e Terraform
- [ ] **DOC-06**: Diagrama de sequência do fluxo de consumo de endpoints no README
- [ ] **DOC-07**: Mermaid do fluxo de CI/CD no README
- [ ] **DOC-08**: Especificar API (collection Postman/Swagger) revisada e link disponível
- [ ] **DOC-09**: Miro atualizado em relação à API
- [ ] **DOC-10**: Documentação da API com componentes, infraestrutura provisionada e fluxo de deploy
- [ ] **DOC-11**: Explicação de escalabilidade automática (HPA) e simulação de carga no README

#### Qualidade e Refatoração
- [ ] **QLD-01**: Garantir 80% de cobertura de testes
- [ ] **QLD-02**: Revisar e refatorar código para Clean Code e SOLID

#### Infraestrutura
- [ ] **INF-01**: Dockerfile e docker-compose revisados
- [ ] **INF-02**: Manifestos K8s (Deployments, Services, ConfigMaps, Secrets, HPA)
- [ ] **INF-03**: Scripts Terraform para provisionamento de cluster K8s e banco de dados
- [ ] **INF-04**: Configurar CD na pipeline (GitHub Actions)
- [ ] **INF-05**: Pipeline CI/CD documentada com Mermaid

#### Integração WhatsApp
- [ ] **WPP-01**: Enviar notificação via WhatsApp para aprovação/recusa de orçamento
- [ ] **WPP-02**: Notificar cliente via WhatsApp quando OS for finalizada (retirada)

#### Melhorias na API
- [ ] **API-01**: Verificar e esclarecer se "APIs" refere-se a endpoints ou múltiplas APIs
- [ ] **API-02**: Verificar existência de endpoint de abertura de OS
- [ ] **API-03**: Verificar existência de endpoint de consulta de status da OS
- [ ] **API-04**: Listagem de OS ordenada por prioridade de status (Em Execução > Aguardando Aprovação > Diagnóstico > Recebida), mais antigas primeiro, omitindo finalizadas/entregues
- [ ] **API-05**: Verificar escopo de atualização de status via ferramenta externa (e-mail, WhatsApp) — aplicar só a aprovar/recusar orçamento?

### Out of Scope

- Front-end / interface gráfica — API-first, time focado no backend
- Aplicativo mobile do cliente — não contemplado
- Módulo financeiro/contábil completo — apenas cobrança essencial implementada
- Chat em tempo real — fora do domínio
- Notificações push/email real — substituído por WhatsApp
- Monitoramento/Prometheus/Grafana — além do escopo atual
- Serviço de mensageria externo (Redis/RabbitMQ) — CDI events suficiente

## Context

Projeto acadêmico da FIAP, equipe de 5 desenvolvedores. O core do sistema (OS lifecycle, estoque, pagamento, audit, SLA) foi implementado e validado na v1.0 com 517 testes. A v2.0 foca em preparar o sistema para um ambiente de produção real: infraestrutura em nuvem (K8s + Terraform), documentação completa para novos desenvolvedores, qualidade de código assegurada (80% coverage + Clean Code), e uma integração externa com WhatsApp para comunicação com clientes.

O código segue Clean Architecture com 4 módulos Maven:
- `mekano-domain`: entidades puras, interfaces de porta, value objects, exceções
- `mekano-application`: casos de uso com @Transactional
- `mekano-infrastructure`: JPA Panache, repositórios, mappers MapStruct, security
- `mekano-rest`: endpoints REST, DTOs, exception mapper RFC 7807

## Constraints

- **Prazo**: 10 dias para entrega completa (restante do prazo acadêmico)
- **Stack**: Java 17, Quarkus 3.36.0, PostgreSQL 16, Maven multi-módulo
- **Time**: 5 desenvolvedores trabalhando em paralelo
- **Qualidade**: 80% cobertura de testes (JaCoCo), Swagger, pipeline CI/CD
- **Infraestrutura**: Docker, Kubernetes (local), Terraform
- **Integração**: WhatsApp (provedor free-tier ou simulado)

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Manter estrutura multi-módulo existente | Clean Architecture já estabelecida e testada | ✓ Good |
| WhatsApp como canal único de notificação | Simples, abrangente, sem dependência de e-mail | — Pending |
| Docker + K8s + Terraform | Stack padrão de mercado para deploy | — Pending |
| JaCoCo 80% LINE + OWASP CVSS≥7 | Gates de qualidade definidos desde a v1.0 | ✓ Good |

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-08-08 after milestone v2.0 initialization*
