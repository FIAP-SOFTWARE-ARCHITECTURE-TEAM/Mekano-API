# Mekano — State

**Milestone ativo:** v1 — Clean Architecture Quarkus API  
**Fase atual:** Fase 4 — Infrastructure  
**Última atualização:** 2026-05-28  
**Último plano concluído:** 03-03 (Testes Mockito puro — CreateUserUseCaseTest)  
**Sessão parada em:** Fase 3 CONCLUÍDA (3/3 planos, 26 testes totais, BUILD SUCCESS)

---

## Status das Fases

| Fase | Status | Notas |
|------|--------|-------|
| 1 — Esqueleto Maven | ✅ Concluída | BUILD SUCCESS — 5 módulos compilando |
| 2 — Domain | ✅ Concluída | 6/6 critérios, 22 testes, BUILD SUCCESS, zero imports proibidos |
| 3 — Application | ✅ Concluída | 3/3 planos, 4 testes, BUILD SUCCESS, zero imports proibidos |
| 4 — Infrastructure | 📋 Planejada (5 planos, 4 waves) | Pronto para `/gsd-execute-phase 4` |
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
9. Sem método fábrica Email.of() — construtor público é o único ponto de entrada do VO
10. Null-check antes do regex no Email evita NPE silencioso no matcher
11. Locale.ROOT na normalização garante comportamento determinístico no servidor
12. @Builder(access = PRIVATE) em User força toda construção via factory method User.create()
13. Campo email de User é tipo Email (VO), não String — validação garantida em build time
15. Interfaces UserRepositoryPort/CreateUserInputPort sem anotações — domínio agnóstico de framework
16. findById/findByEmail retornam Optional<User> — responsabilidade do use case tratar ausência sem exceção
17. CreateUserInputPort.execute() usa String primitivos (não CreateUserCommand) — evita dependência cíclica domain→application
