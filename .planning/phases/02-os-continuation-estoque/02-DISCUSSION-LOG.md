# Phase 2: OS Continuation & Estoque - Discussion Log

> **Audit trail only.** Do not use as input to planning, research, or execution agents.
> Decisions are captured in CONTEXT.md — this log preserves the alternatives considered.

**Date:** 2026-06-22
**Phase:** 2-OS Continuation & Estoque
**Areas discussed:** Modelo de Precificacao, Seguranca da Aprovacao, SLA do Orcamento, CNPJ, Alertas de Estoque, Metadados da Execucao, Admin User CRUD, Soft Delete, Cancelamento de OS, Eventos CDI, Paginacao, Audit de Transicoes, Erro de Saldo, OS Detail, Nomes Entidades, DOC-02 Swagger, Requisitos Academicos

---

## Modelo de Precificacao

| Option | Selected |
|--------|----------|
| Calculo: Soma simples / Com markup / Mao de obra separada | Soma simples |
| Desconto: Sim / Nao | Nao |
| Itens editaveis: Manual / Automatico | Automatico |
| Preco pecas: Unico / Separado | Unico |
| Valor servico: Fixo por tipo / Variavel | Fixo |
| Peca cliente: Sim / Nao | Nao |
| Reserva: Diagnostico / Aprovacao | Aprovacao |
| Estorno: Nao precisa / Sim | Nao precisa |
| Diagnostico adiciona: Sim / So solicitados | Sim |
| Geracao: Automatica / Manual | Automatica |

**Notas:** Orcamento = soma simples sem markup. Preco unico estoque=orcamento.

---

## Seguranca da Aprovacao

| Option | Selected |
|--------|----------|
| Auth cliente: Token URL / Codigo | Login JWT |
| Criacao conta: Automatica / Cliente define | Automatica (CPF) |
| Cliente=User: Separado / User | User com role 'cliente' |
| Nova role: Sim / Reusa | Nova role |
| Senha inicial: Link definicao / Senha padrao | Senha padrao |
| OS visiveis: Proprias / Todas | Proprias |
| Admin aprova: Sim / Nao | Nao |
| Token: Hash / Raw / Nao precisa | Nao precisa (JWT) |
| Envio link: Armazena / Simulado | Simulado |

---

## SLA do Orcamento

| Option | Selected |
|--------|----------|
| Prazo: 48h / 72h / 7 dias | 72h |
| Configuravel: Global / Por servico | Fixo global |
| Expiracao: Cancela OS / Marca expirado | Cancela OS |
| Deteccao: Job / Sob demanda | Scheduled job |
| Frequencia job: 1h / 15min | 12h |

**Notas:** Job de SLA a cada 12h. Motivo automatico "SLA expirado".

---

## CNPJ / Cliente PJ

**Decisao:** Adiado. So PF (CPF) no escopo da Fase 2.

---

## Alertas de Estoque

| Option | Selected |
|--------|----------|
| Disparo: Ao atualizar / Job / Hibrido | Ao atualizar saldo |
| Req automatica: Sim / So notifica | Sim |
| Quantidade: Ate minimo / Lote fixo / Ate maximo | Lote fixo |
| Aprovacao: Pendente / Auto | Financeiro aprova |
| Req orcamento: Financeiro / Auto / Cancela | Auto (urgencia) |
| NF referencia: Obrigatorio / Opcional | Obrigatorio |
| Saida: Iniciar / Finalizar | Iniciar execucao |
| Exibicao: Endpoint / Listagem / Ambos | Ambos |
| Estados req: 3 / 4 | ABERTA, CANCELADA, COMPRADA, RECEBIDA |
| Cancela req: Admin / Criador | So admin |
| Estoque negativo: Nunca / Permite | Nunca |
| Unidades: Enum / Texto | Enum (UN, KG, L, CX, M, PC) |
| Dados NF: Completa / So qtd | Completa (num, serie, fornecedor, CFOP) |

---

## Metadados da Execucao

| Option | Selected |
|--------|----------|
| Quem executa: Mecanico+admin / So mecanico | So mecanico |
| Inicio: Timestamp+mecanico+obs / So timestamp | Timestamp+mecanico+obs |
| Finalizacao: Obs obrigatoria / Opcional | Opcional |
| Multiplos mecanicos: Sim / Nao | Nao |
| Tempo medio: Fim-inicio / Por servico | Fim-inicio |
| Servicos vs orcados: Comparativo / Mesma lista | Comparativo |
| Filtros listagem: Data,status,cliente / +placa | +placa veiculo |

---

## Admin User CRUD

| Option | Selected |
|--------|----------|
| Altera role: Sim / Nao | Sim |
| Reseta senha: Sim / Nao | Sim, sistema gera |
| Lista inativos: So ativos / Todos c/ filtro | Todos c/ filtro |
| Exclui usuario: Sim / Nao | Sim (soft delete) |
| Cria: Admin define senha / Sistema gera | Sistema gera |
| Edita: So role / Tudo | Tudo |
| Endpoint: /users existente / /admin/users separado | /admin/users |

---

## Soft Delete

| Option | Selected |
|--------|----------|
| Entidades: Todas / Parcial | Todas (Cliente, Veiculo, Servico, Peca, Orcamento, Req) |
| Cliente c/ OS: Bloqueia / Permite | Bloqueia |
| Peca em OS: Bloqueia / Permite | Bloqueia |
| Restore: Endpoint / DBA | Sim, PATCH endpoint |
| BaseEntity: Sim, todas / So soft delete | Sim, todas |
| Restore cliente-veiculo: Sim / Nao | Nao |
| Evento ao deletar: Sim / Nao | Sim |

---

## Cancelamento de OS

| Option | Selected |
|--------|----------|
| Quem: Admin+cliente / So admin | Admin+cliente |
| Estados: AGUARDANDO / Qualquer | So AGUARDANDO_APROVACAO |
| Motivo: Obrigatorio / Opcional | Obrigatorio |
| Libera reserva: Auto / Manual | Auto |
| Motivo SLA: Automatico / Sem | Automatico "SLA expirado" |
| Reprovacao motivo: Sim / Nao | Sim |
| Historico: Mantido / Limpa | Mantido |

---

## Eventos CDI

| Option | Selected |
|--------|----------|
| Lista: 3 / 4 / Todos | Todos |
| Payload: So UUID / Dados principais | Dados principais |
| OrcamentoAprovado: Lista pecas / So ID | Lista pecas+qtd |
| Nomenclatura: EntidadeAcaoEvent / Outro | EntidadeAcaoEvent |

---

## Paginacao

| Option | Selected |
|--------|----------|
| Default: 10 / 20 | 10 |
| Maximo: 50 / 100 | 50 |
| Ordenacao: Data desc / Status+data | dataCriacao desc |
| Sort param: Sim / Nao | Sim (sort=campo&order=asc) |
| Unificada: Sim / Pecas diferente | Sim, mesma config |
| Response: Ambos / So total | totalPages+totalElements+page+size+content |

---

## Audit de Transicoes

| Option | Selected |
|--------|----------|
| Forma: Tabela especifica / So OS | Tabela os_audit_log |
| Transicoes auto: Sim (sistema) / So manuais | Sim, usuario="sistema" |
| Endpoint: Sim / So banco | GET /os/{id}/historico |
| Snapshot: Sim / So refs | Sim (JSON itens) |
| Soft delete: Nao / Sim | Nao (imutavel) |
| Quem ve: Admin+mecanico / So admin | Admin+mecanico |

---

## Erro de Saldo

| Option | Selected |
|--------|----------|
| HTTP status: 409 / 422 | 409 Conflict |
| Mensagem: Generica / Saldo | Generica |
| Peca inexistente: 404 separado / Mesmo | 404 separado |
| Formato: RFC 7807 / Custom | RFC 7807 |

---

## OS Detail

| Option | Selected |
|--------|----------|
| Conteudo: Completo / Basico | Completo (OS, cliente, veiculo, itens, historico, orcamento, pagamento) |
| Pagamento: Sim / Nao | Sim fe houver |
| Dados: Embutidos / So UUID | Embutidos |

---

## Nomes Entidades

| Option | Selected |
|--------|----------|
| Idioma: Portugues / Ingles | Portugues |

---

## DOC-02 Swagger

| Option | Selected |
|--------|----------|
| Detalhe: Completo / Minimo | Completo (@Operation, @Schema) |
| Exemplos: Sim / Nao | Sim (@ExampleAnnotation) |

---

## Requisitos Academicos

| Option | Selected |
|--------|----------|
| Cobertura: JaCoCo | Gate 80% OS+Estoque |
| Scan: OWASP DC / Sonar / Ambos | OWASP Dependency Check |
| README: Completo | Setup, padroes, instrucoes |

---

## the agent's Discretion

- Detalhes de implementacao nao cobertos seguem padroes do codebase
- Estrutura de testes segue padrao existente (JUnit 5 + Mockito + REST Assured + AssertJ)

## Deferred Ideas

- **CNPJ (Pessoa Juridica):** Adiado para fase futura
- **Notificacoes reais (WhatsApp/email):** Envio simulado por enquanto
- **Rate limit:** Nao necessario por enquanto
