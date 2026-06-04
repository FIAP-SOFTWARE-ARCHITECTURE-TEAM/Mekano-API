---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: Executing Phase 09
last_updated: "2026-06-04T02:12:58.386Z"
progress:
  total_phases: 10
  completed_phases: 8
  total_plans: 40
  completed_plans: 37
  percent: 92
---

# Mekano — State

**Milestone ativo:** v1 — Clean Architecture Quarkus API  
**Fase atual:** Fase 9 — Segurança e Completude da API (próxima)  
**Última atualização:** 2026-06-04  
**Último plano concluído:** 09-02 (Rate Limiting no Login)  
**Sessão parada em:** Fase 9 — 3 planos restantes (Secrets, CRUD, MapStruct)

---

## Status das Fases

| Fase | Status | Notas |
|------|--------|-------|
| 1 — Esqueleto Maven | ✅ Concluída | BUILD SUCCESS — 5 módulos compilando |
| 2 — Domain | ✅ Concluída | 6/6 critérios, 22 testes, BUILD SUCCESS, zero imports proibidos |
| 3 — Application | ✅ Concluída | 3/3 planos, 4 testes, BUILD SUCCESS, zero imports proibidos |
| 4 — Infrastructure | ✅ Concluída | 5/5 planos, 7/7 requisitos, BUILD SUCCESS, desvio two-class pattern aceito |
| 5 — Adapter | ✅ Concluída | 7/7 planos + HI-01 fix, 4 testes REST Assured, BUILD SUCCESS, gsd-verifier PASS 8/8 |
| 6 — Observabilidade | ✅ Concluída | 4/4 planos + 1 fix, 9/9 testes integração, PASS 4/4 UATs |
| 7 — Fault Tolerance | 🟡 Estática-PASS | 3/3 planos; UAT-1 + UAT-4 + compile ✅; UAT-2/UAT-3 (test runtime) deferidos ao usuário |
| 8 — JWT | ✅ Concluída | 5/5 planos, @TestSecurity bypass, mp.jwt.* namespace |
| 9 — Segurança e Completude | 🟡 Em andamento | 2/5 planos (Refresh Token, Rate Limiting), 3 restantes |

---

## Artefatos de Planejamento

- [x] `.planning/PROJECT.md` — contexto do projeto
- [x] `.planning/REQUIREMENTS.md` — 48 requirements aprovados
- [x] `.planning/ROADMAP.md` — 8 fases mapeadas
- [x] `.planning/research/SUMMARY.md` — síntese de pesquisa
- [x] `.planning/config.json` — preferências do workflow

---

---

## Accumulated Context

### Pending Todos

**16 pending** — remaining after promoting 5 HIGH to Phase 9.

| # | Title | Area | Priority |
|---|-------|------|----------|
| 1 | Escrever testes de integração para Fault Tolerance | testing | 🟡 medium |
| 2 | Configurar logging estruturado JSON | observability | 🟡 medium |
| 3 | Adicionar cache em leituras de usuário (Caffeine) | database | 🟡 medium |
| 4 | Configurar CORS para frontend web | api | 🟡 medium |
| 5 | Implementar eventos de domínio (UserCreatedEvent) | domain | 🟡 medium |
| 6 | Implementar paginação e listagem de usuários | api | 🟡 medium |
| 7 | Substituir ExceptionMappers múltiplos por um genérico | api | 🟢 low |
| 8 | Adicionar prefixo de versão na API (/api/v1) | api | 🟢 low |
| 9 | Adicionar campos de auditoria na tabela users | database | 🟢 low |
| 10 | Garantir timezone explícito em respostas de data | api | 🟢 low |
| 11 | Configurar CI/CD pipeline (GitHub Actions) | tooling | 🟡 medium |
| 12 | Fazer UserRepositoryPort.save() retornar User em vez de void | domain | 🟡 medium |
| 13 | Extrair PasswordHasher do application para interface no domínio | domain | 🟡 medium |
| 14 | Mover @Transactional para o use case (unidade de trabalho) | domain | 🟡 medium |
| 15 | Criar UseCaseResponse objects para não expor entidades de domínio | domain | 🟢 low |
| 16 | Avaliar DomainException checked vs unchecked para contratos de negócio | domain | 🟢 low |

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
18. RefreshTokenService em mekano-infrastructure com quarkus-smallrye-jwt-build — JWT signing adicionado como dependência compile-time no módulo infrastructure, já presente no adapter em runtime
19. Refresh token validation via SHA-256 hash comparison (não JWT signature) — o hash armazenado é o mecanismo de segurança
20. Test key generation in-memory via RefreshTokenServiceTestProfile (não arquivo PEM commitado) — respeita gitignore privateKey*.pem
