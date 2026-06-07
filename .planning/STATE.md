---
gsd_state_version: 1.0
milestone: v1.0
milestone_name: milestone
status: Milestone complete
last_updated: "2026-06-04T15:02:58.136Z"
progress:
  total_phases: 11
  completed_phases: 10
  total_plans: 44
  completed_plans: 44
  percent: 91
---

# Mekano — State

**Milestone ativo:** v1 — Clean Architecture Quarkus API  
**Fase atual:** Fase 10 — Melhorias Pós-v1  
**Última atualização:** 2026-06-04  
**Último plano concluído:** 10-04 (Observabilidade & Eventos)  
**Sessão parada em:** Fase 10 — Completa (4/4 planos)

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
| 9 — Segurança e Completude | ✅ Concluída | 5/5 planos (Refresh Token, Rate Limiting, Secrets, Soft Delete, MapStruct) |
| 10 — Melhorias Pós-v1 | ✅ Concluída | 4/4 planos |

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

---

## Decisões Chave

1. `quarkus-maven-plugin` SOMENTE no módulo `adapter`
2. `jandex-maven-plugin` em todos os módulos não-root
3. Ordem no annotationProcessorPaths: `lombok` → `lombok-mapstruct-binding` → `mapstruct-processor`
4. MapStruct `componentModel = "cdi"` (não "spring")
5. `@Transactional` na camada application (use case) — unidade de trabalho no use case, não no repositório (D-03, revisado em 10-01)
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
21. `PasswordHasher` é interface pura no domain — sem imports de framework; use cases injetam a abstração, não `BcryptUtil` concreto (D-02, 10-01)
22. `CreateUserResponse` record no application — `executeResponse()` retorna id/name/email/createdAt, não expõe entidade `User` (D-04, 10-01)
23. `BusinessException extends Exception` — checked para regras de negócio recuperáveis; `DomainException extends RuntimeException` mantido para validações (D-05, 10-01)
24. Paginação pragmática: `UserResource` chama `UserRepositoryPort` diretamente — sem use case para listagem puramente leitura (D-06, 10-02)
25. `quarkus.rest.path=/api/v1` sobre `@ApplicationPath("/api/v1")` — endpoints /q/health e /q/metrics inalterados (D-08, 10-02)
26. GenericExceptionMapper com ConcurrentHashMap — dispatching por tipo, fallback 500 com stacktrace; mapeadores antigos preservados com @Deprecated para rollback (D-09, 10-02)
27. Jackson timezone America/Sao_Paulo faz LocalDateTime serializar com offset ISO-8601 (D-10, 10-02)
28. CI/CD via GitHub Actions: `mvn verify -pl mekano-adapter -am` sem Docker explícito — DevServices do Quarkus auto-gerencia PostgreSQL (D-11, 10-03)
29. Logging JSON: `quarkus.log.console.json=true` com pretty-print=false — uma linha por JSON para Loki/Datadog; perfil `%dev` = DEBUG, `%prod` = INFO, `%test` = WARN (D-12, 10-03)
30. Cache Caffeine: `@CacheResult(cacheName="users")` em findById/findByEmail, `@CacheInvalidate(cacheName="users")` em save/markAsDeleted; expire-after-write=60s, max-size=100 (D-13, 10-03)
31. FT Tests: @Retry/@Timeout testados via integração com UserRepositoryImpl; @CircuitBreaker omitido (PostgreSQL local não requer CB); assertj-core adicionado como test dep no adapter (D-14, 10-04)
32. UserCreatedEvent é record no pacote domain/event; EventPublisher interface pura no domain/port/out; CdiEventPublisher usa Event<Object> genérico no infrastructure/event (D-15, 10-04)
33. Audit fields (created_by, updated_by, updated_at) são exclusivos de infrastructure — domain User NÃO tem esses campos; @PreUpdate no UserEntity para updated_at; MapStruct auto-ignora (D-16, 10-04)
