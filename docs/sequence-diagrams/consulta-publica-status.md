# Consulta publica de status da OS

Fluxo publico de consulta sem autenticacao para acompanhar andamento da OS.

```mermaid
sequenceDiagram
    actor Cliente
    participant API as OrdemDeServicoResource
    participant App as OrdemDeServicoService
    participant Repo as OrdemDeServicoRepositoryImpl
    participant DB as PostgreSQL

    Cliente->>API: GET /api/v1/os/{uuid}/status
    Note over API: @PermitAll (sem JWT obrigatorio)

    API->>App: consultarStatus(uuid)
    App->>Repo: findById(uuid)
    Repo->>DB: SELECT status, data_entrada, uuid FROM ordens_de_servico WHERE uuid=?
    DB-->>Repo: registro encontrado
    Repo-->>App: OrdemDeServico

    App-->>API: OrdemDeServicoStatusResponse
    Note over API: resposta minima: { id, status, dataEntrada }
    API-->>Cliente: 200 OK

    alt OS nao encontrada
        App-->>API: AppException(404)
        API-->>Cliente: 404 Problem Details
    end
```
