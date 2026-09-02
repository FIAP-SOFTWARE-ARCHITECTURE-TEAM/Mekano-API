# Phase 7: API Improvements — Discussion Log

**Date:** 2026-08-08
**Status:** Completed

## Areas Discussed

### API-02: Endpoint de Abertura de OS
- **Verificado:** `POST /api/v1/os` existe em OrdemDeServicoResource.java:72
- **Decisão:** Documentar como confirmado, não precisa implementar

### API-03: Endpoint de Consulta de Status
- **Verificado:** `GET /api/v1/os/{id}/status` existe em OrdemDeServicoResource.java:101-102 (@PermitAll)
- **Decisão:** Documentar como confirmado, não precisa implementar

### API-01: "APIs" — Endpoints ou Múltiplas APIs?
- **Decisão:** Pendente de esclarecimento com o professor. Task deve documentar ambos os cenários.

### API-04: Ordenação da Listagem
- **Ordem de prioridade:** EM_EXECUCAO > AGUARDANDO_APROVACAO > EM_DIAGNOSTICO > RECEBIDA > AGUARDANDO_EXECUCAO
- **Desempate:** createdAt ASC (mais antigas primeiro)
- **Excluir:** FINALIZADA, ENTREGUE, CANCELADA
- **Implementar em:** findAllWithFilters no OrdemDeServicoRepositoryImpl

---

*Discussion completed: 2026-08-08*