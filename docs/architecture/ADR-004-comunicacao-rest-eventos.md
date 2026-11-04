# ADR-004 — Comunicação REST síncrona + eventos de domínio internos

- Status: Aceito
- Data: 2026-10

## Contexto

O domínio possui processos que ultrapassam um contexto: abertura de OS dispara verificação de estoque, NF de entrada dispara eventos de reposição, transições de OS geram auditoria e mensagens WhatsApp. Era preciso decidir o padrão de comunicação entre componentes.

## Opções consideradas

| Opção | Avaliação |
|-------|-----------|
| **REST síncrono + eventos de domínio internos (CDI)** | ✅ Simples, observável, adequado ao volume; eventos desacoplam side-effects dentro do processo |
| Fila (SQS/Kafka) entre serviços | Assíncrono robusto — mas há **um único deploy** da API; fila entre módulos do mesmo processo é indireção desnecessária (YAGNI) |
| gRPC | Ganho irrelevante para chamadas internas ao mesmo JVM |
| Microserviços separados | Custo operacional alto para o tamanho da equipe (5 devs) e do domínio |

## Decisão

- **APIs REST síncronas** como contrato externo (padrão Quarkus, `/api/v1`, RFC 7807 para erros);
- **Eventos de domínio internos** (records publicados via `EventPublisher`/CDI, observados com `AFTER_SUCCESS`) para side-effects: auditoria de OS (`OsAuditEventPublisher`), eventos de estoque mínimo (NF de entrada), integração WhatsApp;
- **Limites de módulo dentro do processo**: Clean Architecture (domain → application → infrastructure → rest) mantém os bounded contexts separados, permitindo extrair serviços depois sem reescrita.

A única comunicação síncrona **entre deploys** é a autenticação (Lambda → RDS, ver [RFC-001](RFC-001-autenticacao-cpf-lambda.md)) e o webhook da Evolution (HTTP POST assinado por token).

## Consequências

- ✅ Zero infraestrutura extra (sem broker) — alinhado ao Free Tier;
- ✅ Transações locais do PostgreSQL cobrem consistência (sem saga distribuída);
- ✅ Eventos desacoplam side-effects do caminho principal (HTTP fora de `@Transactional` — decisão D-04/D-15 do projeto);
- ⚠️ Escala futura para serviços independentes exigirá introduzir fila/mensageria — decisão registrada aqui para referência.
