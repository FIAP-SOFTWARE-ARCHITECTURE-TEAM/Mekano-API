# Mekano — Sistema Integrado de Atendimento e Execução de Serviços para Oficina Mecânica

## What This Is

API REST monolítica em Java 17 com Quarkus 3.36.0 seguindo Clean Architecture e DDD, para gestão de oficinas mecânicas de médio porte. O sistema gerencia o ciclo completo de ordens de serviço (desde recebimento até entrega), controle de estoque de peças/insumos, e processamento de pagamentos. Atualmente implementa apenas o subsistema de autenticação/usuários.

## Core Value

Gerenciar o ciclo de vida completo das Ordens de Serviço — do recebimento do veículo à entrega — com rastreabilidade, controle de estoque e cobrança integrados.

## Requirements

### Validated

<!-- Shipped and confirmed valuable. -->

- ✓ Autenticação JWT (Ed25519/EdDSA)
- ✓ CRUD de usuários com soft delete
- ✓ Cache Caffeine em consultas de usuário
- ✓ Flyway migrations (V1-V4)
- ✓ Arquitetura multi-módulo (domain, application, infrastructure, rest)
- ✓ Cobertura de testes com JUnit 5, Mockito, REST Assured, AssertJ
- ✓ Pipeline CI via GitHub Actions
- ✓ Dockerfiles (JVM e Native)

### Active

<!-- Current scope. Building toward these. -->

#### Contexto de Ordem de Serviço
- [ ] **OS-01**: Cadastrar cliente com CPF/CNPJ (validação e unicidade)
- [ ] **OS-02**: Cadastrar veículo vinculado a cliente (placa única)
- [ ] **OS-03**: Cadastrar tipos de serviço disponíveis
- [ ] **OS-04**: Criar Ordem de Serviço com status Recebida
- [ ] **OS-05**: Iniciar diagnóstico e incluir serviços/peças na OS
- [ ] **OS-06**: Gerar e enviar orçamento automaticamente
- [ ] **OS-07**: Cliente aprovar/reprovar orçamento via API pública
- [ ] **OS-08**: Executar, finalizar e registrar entrega do veículo
- [ ] **OS-09**: Consulta pública de status da OS pelo cliente
- [ ] **OS-10**: Listar/detalhar OS com filtros (admin)
- [ ] **OS-11**: Monitorar tempo médio de execução
- [ ] **OS-12**: SLA com expiração automática de orçamento

#### Contexto de Gestão de Estoque
- [ ] **EST-01**: CRUD de peças/insumos com saldo e estoque mínimo
- [ ] **EST-02**: Reservar peças ao aprovar orçamento
- [ ] **EST-03**: Retirar peças do estoque ao iniciar execução
- [ ] **EST-04**: Gerir requisições de compra (automáticas por OS ou estoque mínimo)
- [ ] **EST-05**: Registrar nota fiscal de entrada e atualizar saldo
- [ ] **EST-06**: Alerta de estoque mínimo

#### Contexto de Ordem de Pagamento
- [ ] **PAG-01**: Emitir cobrança ao finalizar execução
- [ ] **PAG-02**: Registrar confirmação de pagamento (integração bancária simulada)
- [ ] **PAG-03**: Liberar entrega do veículo após pagamento

#### Documentação
- [ ] **DOC-01**: Diagramas de sequência dos fluxos principais
- [ ] **DOC-02**: Swagger/OpenAPI da API
- [ ] **DOC-03**: Guia de contribuição para desenvolvedores

### Out of Scope

- Front-end / interface gráfica — API-first, time focado no backend
- Aplicativo mobile do cliente — não contemplado na Fase 1
- Módulo financeiro/contábil completo — apenas cobrança essencial
- Integração real com múltiplos gateways de pagamento — serviço bancário simulado
- Chat em tempo real — fora do domínio
- Notificações push/email real — envio simulado de orçamento

## Context

Projeto acadêmico da FIAP, equipe de 5 desenvolvedores. O código atual (auth/users) foi implementado como atividade anterior e servirá de base. O Event Storming foi realizado no Miro e documentado em Mermaid, e a documentação do sistema está em `docs/`. O prazo de entrega é de 10 dias a partir do início da Fase 1.

O código segue Clean Architecture com 4 módulos Maven:
- `mekano-domain`: entidades puras, interfaces de porta, value objects, exceções
- `mekano-application`: casos de uso com @Transactional
- `mekano-infrastructure`: JPA Panache, repositórios, mappers MapStruct, security
- `mekano-rest`: endpoints REST, DTOs, exception mapper RFC 7807

## Constraints

- **Prazo**: 10 dias para entrega completa
- **Stack**: Java 17, Quarkus 3.36.0, PostgreSQL 16, Maven multi-módulo
- **Time**: 5 desenvolvedores trabalhando em paralelo
- **Qualidade**: Testes unitários + integração, Swagger, pipeline CI
- **Base existente**: Código atual precisa ser revisado para aderência DDD

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Manter estrutura multi-módulo existente | Clean Architecture já estabelecida e testada | — Pending |
| Revisar base auth antes de expandir | Garantir aderência ao DDD e padrões definidos no Event Storming | — Pending |
| API-first, sem frontend | Foco no backend, time reduzido, prazo curto | — Pending |
| 3 bounded contexts (OS, Estoque, Pagamento) | Definição do Event Storming | — Pending |

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
*Last updated: 2026-06-20 after initialization*
