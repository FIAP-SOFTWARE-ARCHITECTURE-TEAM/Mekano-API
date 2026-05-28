# Mekano — State

**Milestone ativo:** v1 — Clean Architecture Quarkus API  
**Fase atual:** Fase 2 — Domain  
**Última atualização:** 2025-07-15  
**Último plano concluído:** 02-01 (Exceções de Domínio)  
**Sessão parada em:** Completed 02-01-PLAN.md

---

## Status das Fases

| Fase | Status | Notas |
|------|--------|-------|
| 1 — Esqueleto Maven | ✅ Concluída | BUILD SUCCESS — 5 módulos compilando |
| 2 — Domain | 🔄 Em progresso | Plano 02-01 concluído (exceções de domínio) |
| 3 — Application | ⬜ Não iniciada | Depende da Fase 2 |
| 4 — Infrastructure | ⬜ Não iniciada | Depende da Fase 2 |
| 5 — Adapter | ⬜ Não iniciada | Depende das Fases 3 e 4 |
| 6 — Observabilidade | ⬜ Não iniciada | Depende da Fase 5 |
| 7 — Fault Tolerance | ⬜ Não iniciada | Depende da Fase 5 |
| 8 — JWT | ⬜ Não iniciada | Depende da Fase 5 |

---

## Artefatos de Planejamento

- [x] `.planning/PROJECT.md` — contexto do projeto
- [x] `.planning/REQUIREMENTS.md` — 48 requirements aprovados
- [x] `.planning/ROADMAP.md` — 8 fases mapeadas
- [x] `.planning/research/SUMMARY.md` — síntese de pesquisa
- [x] `.planning/config.json` — preferências do workflow

---

## Decisões Chave

1. `quarkus-maven-plugin` SOMENTE no módulo `adapter`
2. `jandex-maven-plugin` em todos os módulos não-root
3. Ordem no annotationProcessorPaths: `lombok` → `lombok-mapstruct-binding` → `mapstruct-processor`
4. MapStruct `componentModel = "cdi"` (não "spring")
5. `@Transactional` SOMENTE na camada infrastructure
6. JWT: namespace `mp.jwt.*` (não `quarkus.smallrye-jwt.*`)
7. PostgreSQL via docker-compose; DevServices para dev/test
8. Exceções de domínio estendem RuntimeException — sem checked exceptions, sem imports de framework
