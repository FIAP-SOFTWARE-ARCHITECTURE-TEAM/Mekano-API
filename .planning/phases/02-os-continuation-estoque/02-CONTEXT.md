# Phase 2: OS Continuation & Estoque — Context

**Gathered:** 2026-06-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Dois subdomínios: (1) continuação do ciclo de vida da OS — orçamento automático, aprovação/reprovação pelo cliente, execução, finalização e métricas; (2) gestão de estoque — CRUD de peças, reserva atômica, requisições de compra, entrada NF, alerta de mínimo. Também inclui CRUD admin de usuários (AUTH-04) e documentação OpenAPI (DOC-02).

Requisitos: AUTH-04, OS-09 a OS-11, OS-13, OS-14, OS-16 a OS-18, EST-01 a EST-09, DOC-02 (20 requisitos).

**Depende de:** Phase 1 (OS Aggregate Root, Cliente, Veiculo, Servico endpoints, state machine, auth com roles).

**Time:** Dias 5-7, 3 devs OS + 2 devs Estoque.
</domain>

<decisions>
## Implementation Decisions

### Modelo de Precificação do Orçamento
- **D-01:** Orçamento = soma simples dos valores unitários dos serviços + valor das peças. Sem markup, sem mão de obra separada.
- **D-02:** Sem descontos no orçamento.
- **D-03:** Geração automática ao finalizar diagnóstico (sem edição manual).
- **D-04:** Valor da peça no estoque = valor usado no orçamento (preço único, sem custo vs venda).
- **D-05:** Valor unitário do serviço é fixo por tipo (TipoServico.valor). Orçamento = quantidade × valor.
- **D-06:** Peças fornecidas pelo cliente não entram no orçamento. Só peças do estoque ou via requisição de compra.
- **D-07:** Diagnóstico pode adicionar serviços/peças além dos solicitados na criação da OS.
- **D-08:** Reserva de peças acontece SOMENTE ao cliente aprovar o orçamento. Diagnóstico sugere mas não reserva.

### Autenticação do Cliente (Aprovação de Orçamento)
- **D-09:** Cliente loga no sistema com JWT para ver orçamento, aprovar ou reprovar. Não é link mágico/token na URL.
- **D-10:** Conta de cliente é criada automaticamente ao cadastrar o cliente no sistema. Login = CPF.
- **D-11:** Cliente = User com nova role `cliente`. Reaproveita todo o sistema de auth existente (JWT Ed25519, roles, etc.).
- **D-12:** Senha padrão gerada pelo sistema e exibida ao admin no momento do cadastro.
- **D-13:** Cliente vê apenas OS próprias (filtro por CPF).
- **D-14:** Endpoints: GET /orcamento/{id} (autenticado) para consultar orçamento; POST /orcamento/{id}/aprovar e /reprovar para ações.
- **D-15:** Admin NÃO pode aprovar/reprovar pelo cliente. Só o cliente.
- **D-16:** Envio do link é simulado (sistema loga o link). Sem integração real de notificação.
- **D-17:** Sem rate limit nos endpoints de aprovação.

### SLA do Orçamento
- **D-18:** Prazo padrão: 72 horas. Fixo global (não configurável por tipo de serviço). Configurável via application.properties.
- **D-19:** Ao expirar, sistema cancela a OS automaticamente com motivo "SLA expirado".
- **D-20:** Verificação via scheduled job a cada 12h (Quarkus Scheduler).

### CNPJ / Pessoa Jurídica
- **D-21:** Adiado. Escopo atual só PF (CPF). Sem CNPJ na Fase 2.

### Gestão de Estoque
- **D-22:** Alerta de estoque mínimo disparado ao atualizar saldo (evento síncrono).
- **D-23:** Ao atingir mínimo, sistema gera Requisição de Compra automaticamente.
- **D-24:** Quantidade da requisição = lote fixo configurado na peça (não "até o mínimo").
- **D-25:** Requisição automática por mínimo nasce PENDENTE. Financeiro precisa aprovar.
- **D-26:** Requisição gerada por orçamento (peça indisponível) nasce auto-aprovada (urgência da OS).
- **D-27:** Estados da Requisição: ABERTA, CANCELADA, COMPRADA, RECEBIDA.
- **D-28:** Cancelamento de requisição: só admin.
- **D-29:** NF de entrada OBRIGATORIAMENTE referencia uma Requisição de Compra. Sem compra avulsa.
- **D-30:** Dados da NF: completa (número, série, fornecedor, data emissão, CFOP).
- **D-31:** Unidades: enum fixo (UN, KG, L, CX, M, PC).
- **D-32:** Estoque nunca negativo (`UPDATE saldo = saldo - qtd WHERE saldo >= qtd`).
- **D-33:** Saída de peças do estoque acontece ao INICIAR execução (não ao finalizar).
- **D-34:** Alerta exibido em: endpoint GET /alertas + destaque na listagem de peças.
- **D-35:** Erro de saldo insuficiente: HTTP 409, mensagem genérica, RFC 7807 via ApiExceptionMapper. Peça inexistente: HTTP 404.

### Execução da OS
- **D-36:** Só mecânico pode iniciar e finalizar execução.
- **D-37:** Dados capturados no início: timestamp + ID do mecânico + observação opcional.
- **D-38:** Dados na finalização: timestamp + observação opcional.
- **D-39:** Apenas um mecânico por OS.
- **D-40:** Tempo médio de execução = fim - início. Endpoint dedicado GET /api/v1/os/metricas/tempo-medio?tipo=&inicio=&fim=.
- **D-41:** OS Detail retorna comparativo: serviços executados vs orçados.
- **D-42:** Filtros na listagem de OS: data, status, cliente, placa (OS-16).
- **D-43:** OS Detail retorna dados completos (OS, cliente, veículo, serviços, peças, histórico, orçamento, pagamento se houver). Cliente e veículo embutidos.

### Admin User CRUD (AUTH-04)
- **D-44:** Endpoint separado: /admin/users.
- **D-45:** Admin pode criar, listar (com filtro ativos/inativos), editar (nome, email, role), resetar senha e soft-deletar qualquer usuário.
- **D-46:** Sistema gera senha na criação (exibida uma vez ao admin).
- **D-47:** Admin pode alterar role do usuário.

### Soft Delete
- **D-48:** Todas as novas entidades (Cliente, Veiculo, ServicoTipo, Peca, Orcamento e demais) extendem BaseEntity com soft delete.
- **D-49:** Bloqueia soft delete de cliente com OS em aberto (status != ENTREGUE e != CANCELADA).
- **D-50:** Bloqueia soft delete de peça referenciada em OS pendente.
- **D-51:** Endpoint de restore: PATCH /{entidade}/{uuid}/restore.
- **D-52:** Restore de cliente NÃO restaura veículos vinculados automaticamente.
- **D-53:** Soft delete publica evento CDI.

### Cancelamento de OS
- **D-54:** Admin e cliente podem cancelar. Só no estado AGUARDANDO_APROVACAO.
- **D-55:** Motivo do cancelamento obrigatório (inclusive reprovação do orçamento).
- **D-56:** Cancelamento libera reservas de peças automaticamente.
- **D-57:** OS cancelada mantém todo o histórico (itens, diagnóstico, orçamento).

### Eventos CDI
- **D-58:** Eventos: OrcamentoGeradoEvent, OrcamentoAprovadoEvent, OrcamentoReprovadoEvent, OSFinalizadaEvent, EstoqueMinimoAtingidoEvent, RequisicaoCompraCriadaEvent.
- **D-59:** Payload com dados principais (não só UUID). OrcamentoAprovadoEvent inclui lista de peças + quantidades.
- **D-60:** Nomenclatura: EntidadeAcaoEvent.

### Paginação
- **D-61:** Default: 10 itens/página. Máximo: 50.
- **D-62:** Ordenação padrão: dataCriacao desc.
- **D-63:** Parâmetros: page, size, sort, order. Formato: sort=campo&order=asc|desc.
- **D-64:** Response: totalPages + totalElements + page + size + content.
- **D-65:** Mesma config para OS, peças e requisições.

### Audit de Transições
- **D-66:** Tabela específica `os_audit_log`. Snapshot JSON dos itens da OS no momento da transição.
- **D-67:** Toda transição logada (inclusive automáticas, com usuário = "sistema").
- **D-68:** Endpoint GET /os/{id}/historico. Acesso: admin + mecânico.
- **D-69:** Dados imutáveis. Sem soft delete na tabela de audit.

### Documentação OpenAPI (DOC-02)
- **D-70:** Descrições completas: @Operation com summary/description em cada endpoint, @Schema nos DTOs.
- **D-71:** Exemplos realistas via @ExampleAnnotation.

### Nomes das Entidades
- **D-72:** Português: Cliente, Veiculo, ServicoTipo, Peca, OrdemDeServico, Orcamento, RequisicaoCompra.

### Requisitos Acadêmicos
- **D-73:** JaCoCo no verify com gate de 80% de cobertura nos domínios OS e Estoque.
- **D-74:** OWASP Dependency Check no pipeline CI.
- **D-75:** README.md completo com setup, padrões e instruções de uso.

### the agent's Discretion
- Detalhes de implementação não cobertos acima seguem os padrões existentes no codebase (BaseEntity, two-class repository, MapStruct CDI, etc.)
- Estrutura de testes segue padrão já estabelecido (JUnit 5 + Mockito + REST Assured + AssertJ)
- Nomes de método, Javadoc e comentários seguem convenções do codebase
</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Roadmap & Requirements
- `.planning/ROADMAP.md` §Phase 2 — Goal, success criteria, requirements, risk mitigation (Pitfall 3: inventory race condition)
- `.planning/REQUIREMENTS.md` — Full requirement specs (AUTH-04, OS-09..18, EST-01..09, DOC-02)
- `.planning/PROJECT.md` — Project constraints (10 days, 5 devs, clean architecture)
- `.planning/STATE.md` — Accumulated decisions (vertical slices, state machine, atomic stock, Orcamento as separate AR)

### Codebase Patterns
- `.planning/codebase/ARCHITECTURE.md` — Module structure, data flow, patterns, two-class repository, hybrid ID
- `.planning/codebase/CONVENTIONS.md` — Naming, error handling, MapStruct, VO conventions
- `.planning/codebase/STACK.md` — Technology stack, dependencies, configuration
- `.planning/codebase/INTEGRATIONS.md` — Database, auth, caching, resilience, CDI events

### Phase 1 Context (decisions carried forward)
- `.planning/phases/01-auth-os-foundation/01-CONTEXT.md` — D-17 (CPF-only, CNPJ deferred), D-22/23 (placa validation), D-25/26 (state machine), D-27/28 (ServicoExecutado + PecaUsada)

### Domain Docs
- `docs/` — Event Storming documentation, Mermaid diagrams
- `CLAUDE.md` — Project conventions, gotchas (G1-G10), build commands

### Academic Requirements
- Nenhum spec externo — requisitos acadêmicos capturados nas decisões D-73 a D-75

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `BaseEntity` — PK Long + UUID público + created_at + is_active + deleted_at + audit fields. Todas as novas entidades estendem.
- `AppException` — Exceção unificada com HTTP status. Usar para saldo insuficiente (HTTP 409) e demais erros.
- `ApiExceptionMapper` — Mapeia AppException para RFC 7807. Reutilizar, não criar novo mapper.
- `CdiEventPublisher` — Publicação de eventos CDI. Usar para todos os eventos de domínio da Phase 2.
- `BcryptPasswordHasher` — BCrypt para hash de senha de cliente (role 'cliente').
- `CacheNames` + `cache-config.yml` — Adicionar caches para Peca, Orcamento, etc.
- Two-class repository pattern — `PanacheRepository` + `RepositoryImpl` para todos os novos aggregates.
- MapStruct `componentModel = "cdi"` — Padrão para mapeamento JPA ↔ domain.

### Established Patterns
- Clean Architecture: domain puro → application (@Transactional) → infrastructure (JPA) → rest (JAX-RS)
- Hybrid ID: Long PK + UUID público exposto em APIs
- Factory methods: `static create()` + `static reconstitute()`
- `@Transactional` APENAS no service layer
- Soft delete: `isActive = true` em queries
- `@RequestScoped` em resources JAX-RS (obrigatório para JWT)
- `@Retry(maxRetries=3)` em reads, `@Timeout(5s)` em writes
- Paginação via query params page, size, sort

### Integration Points
- `mekano-infrastructure/src/main/resources/db/migration/` — Migrations Flyway V11-V17 para novas tabelas (Peca, Orcamento, OrcamentoItem, RequisicaoCompra, NfEntrada, OsAuditLog) + seed role cliente
- `mekano-rest/pom.xml` — Adicionar quarkus-scheduler para job de SLA, jacoco-maven-plugin, owasp-dependency-check
- `.github/workflows/ci.yml` — Adicionar etapas de cobertura JaCoCo e scan OWASP
- `mekano-rest/src/main/resources/cache-config.yml` — Adicionar caches para novas entidades
</code_context>

<specifics>
## Specific Ideas

Nenhuma referência específica além dos padrões já estabelecidos. Seguir convenções do codebase.
</specifics>

<deferred>
## Deferred Ideas

- **CNPJ (Pessoa Jurídica):** Adiado. Escopo atual apenas PF com CPF. Modelagem futura quando necessário.
- **Notificações reais (WhatsApp/email):** V2 ou fase futura. Envio simulado por enquanto.
- **Rate limit em endpoints públicos:** Não necessário por enquanto (token UUID + JWT é suficiente).

None — discussion stayed within phase scope.
</deferred>

---

*Phase: 2-OS Continuation & Estoque*
*Context gathered: 2026-06-22*
