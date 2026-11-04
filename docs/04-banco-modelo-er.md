# 04 — Banco de Dados, Modelo Relacional e Diagrama ER

## 1. Justificativa formal do banco

O domínio Mekano possui operações que exigem consistência forte:

- Ordem de Serviço;
- cliente;
- veículo;
- orçamento;
- estoque;
- reserva de peças;
- requisição de compra;
- nota fiscal;
- pagamento;
- auditoria.

Essas características tornam o modelo relacional adequado.

## 2. Por que PostgreSQL

PostgreSQL foi escolhido pelos seguintes motivos:

- transações ACID;
- integridade;
- constraints;
- índices;
- joins;
- UUID;
- JSONB;
- controle concorrente;
- integração com Hibernate/Panache;
- Flyway;
- Testcontainers;
- amplo suporte no Amazon RDS.

## 3. Por que Amazon RDS

O RDS reduz o esforço de operação.

Benefícios:

- banco gerenciado;
- criptografia;
- snapshots;
- backups conforme configuração;
- Security Groups;
- subnets privadas;
- possibilidade futura de Multi-AZ.

Configuração importante:

```text
publicly_accessible = false
```

## 4. Diagrama Entidade-Relacionamento

```mermaid
erDiagram

    USERS ||--o{ USER_ROLES : possui
    USERS ||--o{ REFRESH_TOKENS : possui

    CLIENTES ||--o{ VEICULOS : possui
    CLIENTES ||--o{ ORDENS_DE_SERVICO : solicita

    VEICULOS ||--o{ ORDENS_DE_SERVICO : possui

    ORDENS_DE_SERVICO ||--o{ OS_ITENS : possui
    ORDENS_DE_SERVICO ||--o| ORCAMENTOS : gera
    ORDENS_DE_SERVICO ||--o{ OS_AUDIT_LOGS : registra

    OS_ITENS }o--|| PECAS : referencia
    OS_ITENS }o--|| SERVICOS : referencia

    PECAS ||--o{ ITENS_REQUISICAO_COMPRA : solicitada_em

    REQUISICOES_COMPRA ||--o{ ITENS_REQUISICAO_COMPRA : possui
    REQUISICOES_COMPRA ||--o| NF_ENTRADAS : recebe

    USERS {
        bigint id PK
        uuid uuid UK
        varchar username UK
        varchar password_hash
    }

    CLIENTES {
        bigint id PK
        uuid uuid UK
        varchar cpf UK
        varchar nome
        varchar email
        boolean ativo
    }

    VEICULOS {
        bigint id PK
        uuid uuid UK
        varchar placa UK
        uuid cliente_uuid
    }

    ORDENS_DE_SERVICO {
        bigint id PK
        uuid uuid UK
        uuid cliente_uuid
        uuid veiculo_uuid
        varchar status
        uuid orcamento_uuid
        bigint version
        boolean is_active
    }

    OS_ITENS {
        bigint id PK
        uuid uuid UK
        uuid os_uuid
        uuid referencia_uuid
        varchar tipo
        bigint quantidade
    }

    ORCAMENTOS {
        bigint id PK
        uuid uuid UK
        varchar status
        numeric valor_total
    }

    PECAS {
        bigint id PK
        uuid uuid UK
        varchar nome
        numeric preco
        int saldo
        int saldo_reservado
        int estoque_minimo
    }

    SERVICOS {
        bigint id PK
        uuid uuid UK
        varchar nome
        numeric preco
    }

    REQUISICOES_COMPRA {
        bigint id PK
        uuid uuid UK
        varchar status
        varchar motivo
    }

    ITENS_REQUISICAO_COMPRA {
        bigint id PK
        uuid peca_uuid
        bigint quantidade
    }

    NF_ENTRADAS {
        bigint id PK
        uuid uuid UK
        varchar chave_acesso
        numeric valor_total
        uuid requisicao_uuid
    }

    OS_AUDIT_LOGS {
        bigint id PK
        uuid os_uuid
        varchar status_anterior
        varchar status_novo
        varchar autor
        timestamp data
    }

    USER_ROLES {
        bigint user_id
        varchar role
    }

    REFRESH_TOKENS {
        bigint id PK
        uuid user_uuid
        varchar token
    }
```

## 5. Relacionamentos

| Relacionamento | Cardinalidade | Explicação |
|---|---:|---|
| Cliente → Veículo | 1:N | cliente possui vários veículos |
| Cliente → OS | 1:N | cliente pode abrir várias OS |
| Veículo → OS | 1:N | veículo pode ter vários atendimentos |
| OS → Itens | 1:N | peças e serviços compõem a OS |
| OS → Orçamento | 1:0..1 | orçamento associado à OS |
| OS → Auditoria | 1:N | histórico de transições |
| Requisição → Itens | 1:N | várias peças na compra |
| Requisição → NFe | 1:0..1 | entrada vinculada à compra |

## 6. Ajustes no modelo relacional

### BIGINT + UUID

O modelo utiliza:

```text
BIGINT IDENTITY
+
UUID UNIQUE
```

O BIGINT é utilizado como chave física e o UUID como identidade pública.

### Soft Delete

Preserva histórico ao invés de remover fisicamente registros de negócio.

### Optimistic Locking

A Ordem de Serviço possui:

```text
version
```

para evitar conflitos de atualização concorrente.

### Reserva de estoque

A peça possui:

```text
saldo
saldo_reservado
estoque_minimo
```

Isso ajuda a impedir overselling.

### Auditoria

`os_audit_logs` registra:

- status anterior;
- novo status;
- autor;
- data da alteração.

### Migrações

Flyway é a fonte de verdade do schema.

## 7. Fluxo de estados da OS

```mermaid
stateDiagram-v2

    [*] --> RECEBIDA

    RECEBIDA --> EM_DIAGNOSTICO
    EM_DIAGNOSTICO --> AGUARDANDO_APROVACAO

    AGUARDANDO_APROVACAO --> EM_EXECUCAO: aprovado
    AGUARDANDO_APROVACAO --> CANCELADA: rejeitado

    EM_EXECUCAO --> FINALIZADA
    FINALIZADA --> ENTREGUE

    RECEBIDA --> CANCELADA
    EM_DIAGNOSTICO --> CANCELADA

    ENTREGUE --> [*]
    CANCELADA --> [*]
```
