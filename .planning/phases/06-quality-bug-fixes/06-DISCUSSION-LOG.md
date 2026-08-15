# Phase 6: Quality & Bug Fixes — Discussion Log

**Date:** 2026-08-08
**Status:** Completed

## Areas Discussed

### JaCoCo 80% (QLD-01)
- **Decision:** Aggregated report via `report-aggregate` em mekano-rest
- **Exclusions:** Adicionar `*MapperImpl`, `*PanacheRepository*` aos excludes existentes
- **Gate:** JaCoCo check já falha no `verify` abaixo de 80%

### Bug NfEntradaRepositoryImpl
- **Decision:** Corrigir na Fase 6 (não criar issue separada)
- **Problema:** `pecaId = nfEntrada.getId()` em vez de `requisicao.getPecaId()`
- **Fix:** `pecaId = requisicao.getPecaId()`, `requisicaoCompraId = requisicao.getId()`

### Tech Debt (QLD-02)
- **Todos os 8 itens selecionados:**
  1. Naming PT-BR → EN nos ports
  2. Field injection → constructor injection
  3. Entity style (@Data vs @Getter/@Setter)
  4. Mappers vazios (dead code)
  5. VOs duplicados (Placa.java vs PlacaVeiculo.java)
  6. ItemOrcamento package (model → valueobject)
  7. StatusPagamento duplicado (domain/model/ + domain/os/)
  8. FT/Cache inconsistente (adicionar @Retry+@Timeout+@CacheResult nos faltantes)

### Code Review Scope
- **Decision:** Open-ended — executor pode identificar mais itens durante execução
- **Base:** 8 itens do AGENTS.md + bug NfEntradaRepositoryImpl

---

*Discussion completed: 2026-08-08*