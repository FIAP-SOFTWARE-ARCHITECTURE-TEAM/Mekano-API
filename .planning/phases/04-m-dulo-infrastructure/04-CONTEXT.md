# Fase 4 — Módulo Infrastructure: Contexto de Decisões

**Data:** 2025-07-12  
**Fase:** 4 — Módulo Infrastructure (`mekano-infrastructure`)  
**Status:** Pronto para planejamento

---

<domain>
## Phase Boundary

Esta fase implementa a camada de persistência: entidade JPA `UserEntity` com Panache, `UserRepositoryImpl` que implementa `UserRepositoryPort`, migrations Flyway e mappers MapStruct `entity↔domain`. É aqui que `@Transactional` aparece pela primeira vez no projeto.

**Fora do escopo desta fase:**
- Camada REST / HTTP (`UserResource` — Fase 5)
- Outros casos de uso além de `CreateUser`
- Autenticação JWT
</domain>

<decisions>
## Implementation Decisions

### D-01 — UUID assignment em UserEntity

**Decisão:** `UserEntity.id` declarado como `@Id UUID id` **sem** `@GeneratedValue`.

**Raciocínio:** O domain já gera o UUID em `User.create()` via `UUID.randomUUID()`. O infrastructure apenas persiste o UUID proveniente do domain — não gera um próprio. Adicionar `@GeneratedValue` sobrescreveria o ID do domain, quebrando a invariância de identidade estabelecida na Fase 2.

```java
@Entity
@Table(name = "users")
public class UserEntity extends PanacheEntityBase {
    @Id
    UUID id;
    // sem @GeneratedValue
    ...
}
```

---

### D-02 — persist() + flush() em UserRepositoryImpl.save()

**Decisão:** O método `save(User user)` usa `persist(entity)` seguido de `flush()`.

**Raciocínio:** `flush()` garante que o SQL `INSERT` foi executado antes de retornar — o ID não é perdido e qualquer constraint violation (e.g., email duplicado) é capturada imediatamente dentro da transação, não silenciosamente.

```java
@Transactional
public void save(User user) {
    UserEntity entity = mapper.toEntity(user);
    persist(entity);
    flush();
}
```

---

### D-03 — User.reconstitute() para reconstruction do banco

**Decisão:** Adicionar `User.reconstitute(UUID id, String name, String emailValue, String passwordHash, LocalDateTime createdAt)` ao módulo `mekano-domain` como factory method para reconstrução a partir de dados persistidos.

**Raciocínio:** `User.create()` sempre chama `UUID.randomUUID()` e `LocalDateTime.now()` — não pode ser reusado para reconstruir um User do banco sem corromper o `id` e `createdAt` reais. Um método separado preserva os valores exatos vindos da entidade JPA.

```java
// mekano-domain — adicionado na Fase 4
public static User reconstitute(UUID id, String name, String emailValue,
                                 String passwordHash, LocalDateTime createdAt) {
    return User.builder()
        .id(id)
        .name(name)
        .email(new Email(emailValue))
        .passwordHash(passwordHash)
        .createdAt(createdAt)
        .build();
}
```

> **Nota:** `@Builder(access = PRIVATE)` impede chamada direta ao builder de fora do próprio `User`. O método `reconstitute` fica dentro de `User`, então tem acesso ao builder privado.

---

### D-04 — MapStruct Email VO: default methods

**Decisão:** `UserEntityMapper` usa `default` methods para conversão `Email ↔ String`:

```java
@Mapper(componentModel = "cdi")
public interface UserEntityMapper {
    UserEntity toEntity(User user);
    User toDomain(UserEntity entity);

    default String emailToString(Email email) {
        return email.getValue();
    }

    default Email emailFromString(String value) {
        return new Email(value);
    }
}
```

**Raciocínio:** MapStruct detecta automaticamente a conversão por tipo (`Email` → `String` e vice-versa) sem `@Mapping` explícito. É mais limpo e robusto a refactorings que `@Mapping(expression = "java(...)")`. A chamada `new Email(value)` também revalida o formato ao reconstituir do banco.

---

### D-05 — application.properties para testes de infrastructure

**Decisão:** Criar `mekano-infrastructure/src/test/resources/application.properties` com configuração mínima para DevServices ativar automaticamente.

**Conteúdo:**
```properties
# Sem quarkus.datasource.jdbc.url — DevServices ativa automaticamente
quarkus.flyway.migrate-at-start=true
quarkus.hibernate-orm.schema-management.strategy=validate
%test.quarkus.flyway.clean-at-start=true
```

**Raciocínio:** O `mekano-adapter/src/main/resources/application.properties` não está no classpath quando `./mvnw test -pl mekano-infrastructure -am` roda. Cada módulo deve ser autossuficiente para seus próprios `@QuarkusTest`. Sem URL configurada, o Quarkus ativa DevServices (PostgreSQL via Docker) automaticamente.

---

### Decisões Herdadas de Fases Anteriores (Confirmadas)

| Decisão | Origem |
|---|---|
| `@Transactional` SOMENTE em infrastructure | Fase 1 / STATE.md Decision 5 |
| `@Mapper(componentModel = "cdi")` — nunca "spring" | STATE.md Decision 4 |
| Ordem annotationProcessorPaths: `lombok` → `lombok-mapstruct-binding` → `mapstruct-processor` | STATE.md Decision 3 |
| PostgreSQL via docker-compose; DevServices para dev/test | STATE.md Decision 7 |
| `quarkus-maven-plugin` SOMENTE no módulo `adapter` | STATE.md Decision 1 |
| `jandex-maven-plugin` em todos os módulos não-root | STATE.md Decision 2 |
| Campo `email` em `User` é tipo `Email` (VO), não String | STATE.md Decision 13 |
| `UserRepositoryPort` retorna `Optional<User>` | Fase 2 |

</decisions>

## Escopo da Fase 4

### O que ENTRA nesta fase

1. **`mekano-infrastructure/pom.xml`** — adicionar MapStruct, quarkus-hibernate-orm-panache, quarkus-flyway, quarkus-datasource deps; configurar `annotationProcessorPaths`
2. **`UserEntity.java`** — entidade JPA com Panache, sem referência ao domain
3. **`V1__create_users_table.sql`** — migration Flyway com naming correto (`V1__`)
4. **`UserEntityMapper.java`** — interface MapStruct com `default` methods para Email VO
5. **`UserRepositoryImpl.java`** — `@ApplicationScoped`, `@Transactional` em `save()`, `persist()+flush()`
6. **`User.reconstitute()`** — adição ao módulo `mekano-domain` para reconstrução
7. **`mekano-infrastructure/src/test/resources/application.properties`** — DevServices config
8. **`mekano-adapter/src/main/resources/application.properties`** — perfis dev/prod com Flyway e Hibernate
9. **`UserRepositoryImplTest.java`** — `@QuarkusTest` com `@TestTransaction`

### O que NÃO entra nesta fase

- `UserResource` (Fase 5)
- ExceptionMappers HTTP (Fase 5)
- Outros repositórios além de `UserRepositoryImpl`
- Autenticação / JWT

---

## Restrições de Compilação

- `mekano-infrastructure/src/main/**` — proibido importar `jakarta.ws.rs`, classes do `mekano-application`
- `mekano-infrastructure/src/main/**` — `@Transactional` PERMITIDO (somente aqui)
- `mekano-domain/src/main/**` — proibido importar qualquer módulo interno (ainda válido)

---

## Canonical Refs

- `.planning/ROADMAP.md` — Phase 4 spec (planos 1–7, critérios UAT)
- `.planning/STATE.md` — Decisões 1–7 que se aplicam a esta fase
- `.planning/PROJECT.md` — Constraints e Key Decisions do projeto
- `.planning/phases/02-m-dulo-domain/02-CONTEXT.md` — Contratos do domain (User, Email, UserRepositoryPort)
- `.planning/phases/03-m-dulo-application/03-CONTEXT.md` — D2 (BCrypt no use case), D3 (testes Mockito)

---

## Próximo Passo

Executar: `/gsd-plan-phase 4`
