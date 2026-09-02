# Requirements: Mekano

**Defined:** 2026-06-20
**Core Value:** Gerenciar o ciclo de vida completo das Ordens de Serviço — do recebimento do veículo à entrega — com rastreabilidade, controle de estoque e cobrança integrados.

## v1 Requirements

### Autenticação e Autorização

- [x] **AUTH-01**: Sistema oferece roles para cada perfil (admin, atendente, mecânico, almoxarife, financeiro)
- [x] **AUTH-02**: Endpoints administrativos são protegidos por `@RolesAllowed` com base no perfil
- [x] **AUTH-03**: Cliente pode consultar status da OS via endpoint público sem autenticação
- [x] **AUTH-04**: Admin pode gerenciar usuários do sistema (CRUD)

### Ordem de Serviço

- [x] **OS-01**: Admin/atendente pode cadastrar cliente com nome, CPF/CNPJ (único, validado), e-mail e telefone
- [x] **OS-02**: Admin/atendente pode editar, consultar e excluir clientes
- [x] **OS-03**: Admin/atendente pode cadastrar veículo com placa (única, formatos Mercosul e antigo), marca, modelo e ano vinculado a um cliente
- [x] **OS-04**: Admin/atendente pode editar, consultar e excluir veículos
- [x] **OS-05**: Admin pode cadastrar tipos de serviço com nome, descrição e valor unitário (valor > 0)
- [x] **OS-06**: Admin pode editar, consultar e excluir tipos de serviço
- [x] **OS-07**: Atendente pode criar OS identificando cliente (CPF/CNPJ) e veículo (placa), registrando entrada e serviços solicitados — status inicial Recebida
- [x] **OS-08**: Mecânico pode iniciar diagnóstico da OS (status → Em Diagnóstico) e incluir serviços e peças identificados
- [x] **OS-09**: Sistema gera orçamento automaticamente ao finalizar diagnóstico e envia para aprovação do cliente (OS → Aguardando Aprovação)
- [x] **OS-10**: Cliente pode aprovar orçamento via API pública (OS → Em Execução)
- [x] **OS-11**: Cliente pode reprovar orçamento via API pública (OS → Cancelada)
- [x] **OS-12**: Sistema cancela OS automaticamente se orçamento expirar por SLA
- [x] **OS-13**: Mecânico pode registrar início da execução (status → Em Execução)
- [x] **OS-14**: Mecânico pode finalizar execução (status → Finalizada)
- [x] **OS-15**: Cliente pode consultar status público da OS via API sem autenticação
- [x] **OS-16**: Admin/atendente pode listar OS com filtros por data, status e cliente (paginado)
- [x] **OS-17**: Admin/atendente pode ver detalhes completos de uma OS
- [x] **OS-18**: Admin pode consultar tempo médio de execução por tipo de serviço em um período

### Gestão de Estoque

- [x] **EST-01**: Admin/almoxarife pode cadastrar peça/insumo com código, descrição, unidade, saldo inicial, estoque mínimo e valor
- [x] **EST-02**: Admin/almoxarife pode editar, consultar e excluir peças/insumos (saldo não pode ficar negativo)
- [x] **EST-03**: Sistema reserva automaticamente peças disponíveis ao aprovar orçamento (reserva = flag)
- [x] **EST-04**: Sistema gera Requisição de Compra para peças indisponíveis ao aprovar orçamento
- [x] **EST-05**: Admin/almoxarife pode listar, visualizar e cancelar Requisições de Compra
- [x] **EST-06**: Almoxarife/financeiro pode registrar NF de entrada referenciando Requisição de Compra, atualizando saldo
- [x] **EST-07**: Ao atualizar saldo, sistema verifica itens abaixo do estoque mínimo e gera nova requisição se necessário
- [x] **EST-08**: Almoxarife registra saída de peças reservadas ao iniciar execução (saldo debitado, reserva encerrada)
- [x] **EST-09**: Sistema alerta quando estoque mínimo é atingido (calculado: tempo de reposição × consumo médio diário)

### Ordem de Pagamento

- [x] **PAG-01**: Sistema emite cobrança automaticamente ao finalizar execução (pagamento → Pendente)
- [x] **PAG-02**: Sistema registra confirmação de pagamento via serviço bancário simulado (pagamento → Confirmado)
- [x] **PAG-03**: Admin registra entrega do veículo após pagamento confirmado (OS → Entregue)

### Documentação

- [x] **DOC-01**: Diagramas de sequência dos fluxos principais (criar OS, aprovar orçamento, fluxo estoque, fluxo pagamento)
- [x] **DOC-02**: Especificação OpenAPI/Swagger da API documentada
- [x] **DOC-03**: Guia de contribuição (CONTRIBUTING.md) com setup, padrões e workflow do time

## v2 Requirements

### Documentação e Meta

- [ ] **DOC-04**: Gravar e disponibilizar vídeo demonstrativo do ambiente em execução (até 15 minutos)
- [ ] **DOC-05**: Atualizar README.md com descrição da solução, objetivos da fase 2, instruções de execução local, deploy K8s e Terraform
- [ ] **DOC-06**: Adicionar diagrama de sequência do fluxo de consumo de endpoints no README
- [ ] **DOC-07**: Adicionar Mermaid do fluxo de CI/CD no README
- [ ] **DOC-08**: Ajustar collection e disponibilizar link para collection completa das APIs (Postman/Swagger)
- [ ] **DOC-09**: Ajustar Miro em relação à API
- [ ] **DOC-10**: Documentar componentes da aplicação, infraestrutura provisionada e fluxo de deploy
- [ ] **DOC-11**: Explicar escalabilidade automática (HPA) e como simular aumento de carga no README

### Qualidade e Refatoração

- [ ] **QLD-01**: Garantir 80% de cobertura de testes (JaCoCo line coverage)
- [ ] **QLD-02**: Revisar e refatorar código para princípios Clean Code e SOLID

### Infraestrutura

- [ ] **INF-01**: Revisar Dockerfile e docker-compose para produção
- [ ] **INF-02**: Criar manifestos K8s (Deployments, Services, ConfigMaps, Secrets, HPA)
- [ ] **INF-03**: Criar scripts Terraform para provisionamento de cluster K8s e banco de dados
- [ ] **INF-04**: Configurar CD na pipeline (GitHub Actions)
- [ ] **INF-05**: Documentar pipeline CI/CD com Mermaid no README

### Integração WhatsApp

- [ ] **WPP-01**: Enviar notificação via WhatsApp para aprovação/recusa de orçamento
- [ ] **WPP-02**: Notificar cliente via WhatsApp quando OS for finalizada (retirada)

### Melhorias na API

- [ ] **API-01**: Verificar e esclarecer se "APIs" refere-se a endpoints ou múltiplas APIs
- [ ] **API-02**: Verificar se já existe endpoint para abertura de OS
- [ ] **API-03**: Verificar se já existe endpoint para consulta de status da OS
- [ ] **API-04**: Modificar listagem de OS para ordenar por prioridade de status (Em Execução > Aguardando Aprovação > Diagnóstico > Recebida), mais antigas primeiro, omitindo finalizadas/entregues
- [ ] **API-05**: Verificar escopo de atualização de status via ferramenta externa (WhatsApp/e-mail) — aplicar somente a aprovar/recusar orçamento?

## v2 Deferred

Deferred to future release. Tracked but not in current roadmap.

### Monitoramento

- **MON-01**: Configurar Prometheus/Grafana para métricas da aplicação
- **MON-02**: Alertas baseados em métricas de negócio (OS acumuladas, estoque baixo)

### Integrações Adicionais

- **INT-01**: Notificação push para mobile
- **INT-02**: Integração com gateway de pagamento real (não simulado)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Front-end / interface gráfica | API-first, time focado no backend |
| Aplicativo mobile do cliente | Não contemplado |
| Módulo financeiro/contábil completo | Apenas cobrança essencial implementada |
| Chat em tempo real | Fora do domínio |
| Notificações push/email real | Substituído por WhatsApp |
| Monitoramento/Prometheus/Grafana | Deferido para v2.x |
| Serviço de mensageria externo (Redis/RabbitMQ) | CDI events suficiente

## Traceability

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | Phase 1 | Complete |
| AUTH-02 | Phase 1 | Complete |
| AUTH-03 | Phase 3 | Complete |
| AUTH-04 | Phase 1 | Complete |
| OS-01 | Phase 1 | Complete |
| OS-02 | Phase 1 | Complete |
| OS-03 | Phase 1 | Complete |
| OS-04 | Phase 1 | Complete |
| OS-05 | Phase 2 | Complete |
| OS-06 | Phase 2 | Complete |
| OS-07 | Phase 2 | Complete |
| OS-08 | Phase 2 | Complete |
| OS-09 | Phase 2 | Complete |
| OS-10 | Phase 2 | Complete |
| OS-11 | Phase 2 | Complete |
| OS-12 | Phase 3 | Complete |
| OS-13 | Phase 2 | Complete |
| OS-14 | Phase 2 | Complete |
| OS-15 | Phase 3 | Complete |
| OS-16 | Phase 3 | Complete |
| OS-17 | Phase 3 | Complete |
| OS-18 | Phase 3 | Complete |
| EST-01 | Phase 2 | Complete |
| EST-02 | Phase 2 | Complete |
| EST-03 | Phase 3 | Complete |
| EST-04 | Phase 3 | Complete |
| EST-05 | Phase 2 | Complete |
| EST-06 | Phase 2 | Complete |
| EST-07 | Phase 3 | Complete |
| EST-08 | Phase 3 | Complete |
| EST-09 | Phase 3 | Complete |
| PAG-01 | Phase 2 | Complete |
| PAG-02 | Phase 3 | Complete |
| PAG-03 | Phase 2 | Complete |
| DOC-01 | Phase 3 | Complete |
| DOC-02 | Phase 3 | Complete |
| DOC-03 | Phase 3 | Complete |
| DOC-04 | Phase 8 | Pending |
| DOC-05 | Phase 8 | Pending |
| DOC-06 | Phase 8 | Pending |
| DOC-07 | Phase 8 | Pending |
| DOC-08 | Phase 8 | Pending |
| DOC-09 | Phase 8 | Pending |
| DOC-10 | Phase 8 | Pending |
| DOC-11 | Phase 8 | Pending |
| QLD-01 | Phase 6 | Pending |
| QLD-02 | Phase 6 | Pending |
| INF-01 | Phase 4 | Pending |
| INF-02 | Phase 4 | Pending |
| INF-03 | Phase 4 | Pending |
| INF-04 | Phase 4 | Pending |
| INF-05 | Phase 4 | Pending |
| WPP-01 | Phase 5 | Pending |
| WPP-02 | Phase 5 | Pending |
| API-01 | Phase 7 | Pending |
| API-02 | Phase 7 | Pending |
| API-03 | Phase 7 | Pending |
| API-04 | Phase 7 | Pending |
| API-05 | Phase 5 | Pending |

**Coverage:**
- v1 requirements: 37 total — All Complete ✓
- v2 requirements: 22 total
- Mapped to phases: 22 ✓
- Unmapped: 0 ✓

---
*Requirements defined: 2026-06-20*
*Last updated: 2026-08-08 after milestone v2.0 initialization*
