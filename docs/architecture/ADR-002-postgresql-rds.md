# ADR-002 — PostgreSQL 16 gerenciado (RDS) como banco de dados

- Status: Aceito
- Data: 2026-10

## Contexto

O domínio da oficina (ordens de serviço, estoque, orçamentos, pagamentos) é fortemente transacional: movimentação de peças com reserva atômica de saldo, máquina de estados da OS e auditoria. O desafio exige banco **gerenciado** com justificativa formal.

## Opções consideradas

| Opção | Avaliação |
|-------|-----------|
| **PostgreSQL (RDS)** | ✅ ACID, integridade referencial, ampla adoção, suporte nativo no RDS, já é o banco do projeto desde a Fase 1 (Flyway + Hibernate/Panache) |
| MySQL (RDS) | ACID, mas sem JSONB comparável e com histórico de divergências de comportamento entre versões |
| SQL Server (RDS) | Licenciamento caro para o contexto acadêmico |
| DynamoDB (NoSQL) | Ótimo para escala horizontal, mas o domínio é relacional (relacionamentos fortes entre OS ↔ itens ↔ peças ↔ estoque); sem transações multi-item ricas |
| MongoDB (DocumentDB) | Modelo de documento dificulta constraints de integridade do estoque |

## Decisão

**PostgreSQL 16 no Amazon RDS**, em VPC dedicada, com:

- Migrações versionadas por **Flyway** (fonte de verdade do schema — 36+ migrations);
- `storage_encrypted = true`, `publicly_accessible = false`, `deletion_protection` em prod;
- Backup automático com janela `01:00–02:00`, retention de 7 dias;
- Instância `db.t4g.micro` (dev) — adequada ao Free Tier, ver [RFC-003](RFC-003-free-tier-single-node.md).

A justificativa detalhada do **modelo relacional** (diagrama ER, relacionamentos e ajustes) está em [modelo-de-dados-er.md](modelo-de-dados-er.md).

## Consequências

- ✅ Consistência transacional garantida para reserva de estoque (`UPDATE ... WHERE saldo >= qtd`);
- ✅ Integridade referencial e constraints no banco, não só na aplicação;
- ✅ Conhecimento já consolidado pela equipe (zero curva);
- ⚠️ Escala vertical limitada pela instância — aceitável para o estágio atual;
- ⚠️ Acesso restrito por VPC peering + security group (porta 5432 apenas da VPC da aplicação).
