# Modelo de dados (ER) e justificativa do banco de dados

- Fonte de verdade: migrations Flyway (`mekano-infrastructure/src/main/resources/db/migration/`) — 36+ migrações versionadas
- Banco: PostgreSQL 16 (RDS) — ver [ADR-002](ADR-002-postgresql-rds.md)

## Diagrama Entidade-Relacionamento

```mermaid
erDiagram
    USERS ||--o{ USER_ROLES : "tem"
    USERS ||--o{ REFRESH_TOKENS : "possui"

    CLIENTES ||--o{ VEICULOS : "possui"
    CLIENTES ||--o{ ORDENS_DE_SERVICO : "abre"
    VEICULOS ||--o{ ORDENS_DE_SERVICO : "e atendido em"
    ORDENS_DE_SERVICO ||--o{ OS_ITENS : "compoe"
    ORDENS_DE_SERVICO ||--o| ORCAMENTOS : "gera"
    ORDENS_DE_SERVICO ||--o{ OS_AUDIT_LOGS : "auditada em"

    OS_ITENS }o--|| PECAS : "referencia (tipo=PECA)"
    OS_ITENS }o--|| SERVICOS : "referencia (tipo=SERVICO)"

    PECAS ||--o{ REQUISICOES_COMPRA : "dispara (estoque minimo)"
    REQUISICOES_COMPRA ||--o{ ITENS_REQUISICAO_COMPRA : "possui"
    REQUISICOES_COMPRA ||--o| NF_ENTRADAS : "recebe"

    USERS {
        bigint id PK
        uuid uuid UK
        string username UK
        string password_hash
    }
    CLIENTES {
        bigint id PK
        uuid uuid UK
        string cpf UK
        string nome
        string email
        boolean ativo
    }
    VEICULOS {
        bigint id PK
        uuid uuid UK
        string placa UK
        uuid cliente_uuid FK-logico
    }
    ORDENS_DE_SERVICO {
        bigint id PK
        uuid uuid UK
        uuid cliente_uuid FK-logico
        uuid veiculo_uuid FK-logico
        string status
        uuid orcamento_uuid
        uuid mecanico_uuid
        timestamp execucao_iniciada_em
        timestamp execucao_finalizada_em
        bigint version
        boolean is_active
    }
    OS_ITENS {
        bigint id PK
        uuid uuid UK
        uuid os_uuid FK-logico
        uuid referencia_uuid FK-logico
        string tipo "PECA|SERVICO"
        bigint quantidade
    }
    ORCAMENTOS {
        bigint id PK
        uuid uuid UK
        string status
        numeric valor_total
    }
    PECAS {
        bigint id PK
        uuid uuid UK
        string nome
        numeric preco
        int saldo
        int saldo_reservado
        int estoque_minimo
    }
    SERVICOS {
        bigint id PK
        uuid uuid UK
        string nome
        numeric preco
    }
    REQUISICOES_COMPRA {
        bigint id PK
        uuid uuid UK
        string status
        string motivo
    }
    ITENS_REQUISICAO_COMPRA {
        bigint id PK
        uuid peca_uuid FK-logico
        bigint quantidade
    }
    NF_ENTRADAS {
        bigint id PK
        uuid uuid UK
        string chave_acesso
        numeric valor_total
        uuid requisicao_uuid FK-logico
    }
    OS_AUDIT_LOGS {
        bigint id PK
        uuid os_uuid FK-logico
        string status_anterior
        string status_novo
        string autor
        timestamp data
    }
    USER_ROLES {
        bigint user_id FK
        string role
    }
    REFRESH_TOKENS {
        bigint id PK
        uuid user_uuid FK
        string token
    }
    PROCESSED_EVENTS {
        bigint id PK
        string event_id UK
        timestamp processed_at
    }
```

## Explicação dos relacionamentos

| Relacionamento | Cardinalidade | Como é implementado |
|----------------|---------------|---------------------|
| Cliente → Veículos | 1:N | `veiculos.cliente_uuid` (associação lógica por UUID) |
| Cliente → OS | 1:N | `ordens_de_servico.cliente_uuid` |
| Veículo → OS | 1:N | `ordens_de_servico.veiculo_uuid` |
| OS ↔ Itens (peças **ou** serviços) | 1:N — tabela pivô `os_itens` | `os_itens(os_uuid, referencia_uuid, tipo)` — **many-to-many polimórfico** (decisão D-17): um item referencia `pecas` **ou** `servicos` por `tipo` |
| OS → Orçamento | 1:0..1 | `ordens_de_servico.orcamento_uuid`; orçamento é gerado automaticamente de todos os itens no `finalizarDiagnostico` |
| Peça → Requisição de compra | 1:N | Disparada automática quando saldo disponível ≤ estoque mínimo (evento de estoque) |
| Requisição → Itens | 1:N | `itens_requisicao_compra` (multi-item, V36) |
| Requisição → NF de entrada | 1:0..1 | `nf_entradas.requisicao_uuid` — recebimento credita o saldo da peça |
| OS → Auditoria | 1:N | `os_audit_logs` — toda transição de status registrada (autor, antes/depois) via `OsAuditEventPublisher` |
| User → Roles/Tokens | 1:N | FK física (`user_roles.user_id`, `refresh_tokens.user_id` — auth interna de operadores) |
| Idempotência de integrações | — | `processed_events` garante que eventos (webhooks WhatsApp, NF) não sejam processados duas vezes |

## Padrões estruturais do modelo

1. **Híbrido de ID**: `id BIGINT IDENTITY` (PK física, performance de join/índice) + `uuid UUID UNIQUE` (identidade exposta nas APIs e usada nas associações lógicas entre agregados). FKs físicas são usadas onde a integridade é intrínseca (auth); associações entre **agregados** usam UUID sem FK física para manter os bounded contexts independentes na aplicação.
2. **Soft delete**: `is_active` + `deleted_at` em todas as entidades de negócio — histórico de OS/peças nunca é perdido.
3. **Auditoria automática**: `created_at/updated_at/created_by/updated_by` preenchidos por `AuditoriaListener` (`@EntityListeners`), usuário resolvido do JWT (`PUBLICO`/`SISTEMA` como fallback).
4. **Reserva atômica de estoque (V34)**: coluna `saldo_reservado` em `pecas` + `UPDATE ... WHERE saldo - saldo_reservado >= :qtd` — evita oversell sob concorrência; liberação no cancelamento/SLA.
5. **Máquina de estados da OS**: `status` versionado com `version` (optimistic locking) e 81 testes de transição.

## Justificativa formal do PostgreSQL (modelo relacional)

**Por que relacional:** o domínio é intensivo em **transações de consistência forte** — venda/reserva de peças não pode vender o mesmo estoque duas vezes; OS, itens, orçamento e pagamento precisam de invariantes no banco (constraints, FK, unique por negócio: `uk_os_itens`, CPF/placa únicos). O modelo de entidades e relacionamentos é naturalmente normalizado (3FN), com cardinalidades bem definidas — exatamente o terreno onde SQL supera NoSQL. Consultas operacionais da oficina (OS por cliente/status, histórico de veículo, giro de peça) são joins simples com índices focados.

**Por que PostgreSQL entre os relacionais:** 100% open source (sem licença por instância), suporte maduro no RDS, recursos usados no projeto (`JSONB` para payload de integrações, `IDENTITY`, indexes parciais em `is_active`), e equipe/experiência prévia — o mesmo banco desde a Fase 1, com 36 migrations acumuladas e 510+ testes de integração contra PostgreSQL real (Testcontainers/DevServices), ou seja, **o schema está exercitado por testes desde o dia 1**.

**Por que gerenciado (RDS):** backup automático com PITR, encryption at rest, patching, Multi-AZ como evolução, `deletion_protection` em produção — elimina a operação de banco da equipe sem abrir mão de controle (VPC dedicada + SG restrito a 5432).
