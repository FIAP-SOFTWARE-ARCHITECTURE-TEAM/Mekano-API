---
phase: 02-m-dulo-domain
plan: "04"
subsystem: domain
tags: [ports, hexagonal-architecture, output-port, input-port, clean-architecture, java, domain, interfaces]
dependency_graph:
  requires:
    - User (02-03) — tipo de retorno em todos os métodos de UserRepositoryPort e CreateUserInputPort
    - Email (02-02) — usado indiretamente via User.create() no use case que implementará CreateUserInputPort
    - DomainException (02-01) — UserNotFoundException/UserAlreadyExistsException lançadas pelos use cases via ports
  provides:
    - UserRepositoryPort (output port — contrato de persistência para infrastructure)
    - CreateUserInputPort (input port — contrato do caso de uso para application)
  affects:
    - Fase 3 (application — CreateUserUseCase implementa CreateUserInputPort, chama UserRepositoryPort)
    - Fase 4 (infrastructure — UserRepositoryImpl implementa UserRepositoryPort via Panache)
    - Fase 5 (adapter — UserResource injeta CreateUserInputPort via CDI)
tech_stack:
  added: []
  patterns:
    - Hexagonal Architecture (Ports and Adapters)
    - Output Port (driven port — contrato para infra)
    - Input Port (driving port — contrato para adapter)
    - Optional<T> como tipo de retorno para buscas nullable (sem exceção se não encontrado)
    - Zero anotações de framework em interfaces de domínio
key_files:
  created:
    - mekano-domain/src/main/java/com/fiap/mekano/domain/port/out/UserRepositoryPort.java
    - mekano-domain/src/main/java/com/fiap/mekano/domain/port/in/CreateUserInputPort.java
  modified: []
  deleted: []
decisions:
  - "findById/findByEmail retornam Optional<User> — responsabilidade do chamador (use case) decidir o que fazer se ausente, evitando exceções desnecessárias em consultas"
  - "CreateUserInputPort.execute() usa String primitivos em vez de CreateUserCommand — evita dependência cíclica domain → application; será refatorado na Fase 3"
  - "existsByEmail() separado de findByEmail() — mais eficiente para verificação de duplicatas pois não precisa carregar o objeto User completo"
  - "Interfaces sem nenhuma anotação de CDI/JPA/REST — domínio permanece agnóstico de framework conforme Clean Architecture"
metrics:
  duration: "~5 minutos"
  completed_date: "2026-05-27"
  tasks_completed: 1
  tasks_total: 1
  files_created: 2
  files_deleted: 0
---

# Phase 2 Plan 04: Interfaces de Porta (Ports) — Summary

**One-liner:** Duas interfaces de porta `UserRepositoryPort` (output port, 4 métodos com `Optional<User>`) e `CreateUserInputPort` (input port, `execute(String,String,String)`) definem as fronteiras da arquitetura hexagonal no módulo domain, sem qualquer anotação de framework.

---

## O que foi criado

### `UserRepositoryPort.java` — pacote `com.fiap.mekano.domain.port.out`

| Aspecto | Detalhe |
|---------|---------|
| Tipo | `public interface` |
| Papel arquitetural | Output Port (Driven Port) |
| Implementado por | `UserRepositoryImpl` em mekano-infrastructure (Fase 4) |
| Métodos | `save(User)`, `findById(UUID)`, `findByEmail(String)`, `existsByEmail(String)` |
| Retorno findById | `Optional<User>` — nunca lança exceção se não encontrado |
| Retorno findByEmail | `Optional<User>` — nunca lança exceção se não encontrado |
| Retorno existsByEmail | `boolean` |
| Anotações proibidas | Zero (`@ApplicationScoped`, `@Transactional`, `@Entity` ausentes) |
| Imports proibidos | Zero (`jakarta.*`, `io.quarkus.*`, `org.hibernate.*` ausentes) |

### `CreateUserInputPort.java` — pacote `com.fiap.mekano.domain.port.in`

| Aspecto | Detalhe |
|---------|---------|
| Tipo | `public interface` |
| Papel arquitetural | Input Port (Driving Port) |
| Implementado por | `CreateUserUseCase` em mekano-application (Fase 3) |
| Chamado por | `UserResource` em mekano-adapter (Fase 5) |
| Método | `execute(String name, String email, String passwordHash): User` |
| Parâmetros | Primitivos String (não `CreateUserCommand` — evita dependência cíclica) |
| Anotações proibidas | Zero |

### Estrutura de pacotes após este plano

```
mekano-domain\src\main\java\com\fiap\mekano\domain\
├── exception\
│   ├── DomainException.java
│   ├── InvalidEmailException.java
│   ├── UserAlreadyExistsException.java
│   └── UserNotFoundException.java
├── model\
│   └── User.java
├── port\
│   ├── in\
│   │   └── CreateUserInputPort.java    ← NOVO
│   └── out\
│       └── UserRepositoryPort.java     ← NOVO
└── valueobject\
    └── Email.java
```

---

## Verificação

### Compilação

```
cd C:\Users\victo\Desktop\fiap-software-architecture\mekano
.\mvnw.cmd compile -pl mekano-domain -q
Exit code: 0  →  BUILD SUCCESS ✅
```

### Estrutura de arquivos

```
Get-ChildItem mekano-domain\...\domain\port\ -Recurse -Filter "*.java"
→ CreateUserInputPort.java (port/in/) ✅
→ UserRepositoryPort.java (port/out/) ✅
```

### Zero anotações de framework

```
Verificação manual nos arquivos:
- @ApplicationScoped → ausente ✅
- @Transactional → ausente ✅
- @Path → ausente ✅
- jakarta.* imports → ausentes ✅
- io.quarkus.* imports → ausentes ✅
```

### Assinatura CreateUserInputPort

```
execute(String name, String email, String passwordHash): User
→ Parâmetros primitivos ✅
→ NÃO usa CreateUserCommand como parâmetro de método ✅
```

---

## Commits

| Tarefa | Hash | Mensagem |
|--------|------|----------|
| Tarefa 1 | `feef6de` | `feat(domain): adiciona interfaces de porta (ports) do dominio` |

---

## Desvios do Plano

### Notas de observação

**1. [Nota - Verificação] Javadoc referencia `CreateUserCommand` em comentário**
- **Encontrado durante:** Verificação pós-criação
- **Observação:** O arquivo `CreateUserInputPort.java` contém `CreateUserCommand` no Javadoc (`@{code CreateUserCommand}`) para documentar a evolução futura do método. Isso é **parte do conteúdo exato especificado no plano**.
- **Impacto:** Zero — é apenas um comentário. Não há `import`, não há uso real do tipo. O arquivo compila com sucesso (exit code 0).
- **Verificação cruzada:** A assinatura do método usa `(String name, String email, String passwordHash)` — primitivos corretos. A restrição crítica está satisfeita.

---

## Known Stubs

Nenhum — ambas as interfaces são contratos completos e funcionais. Nenhuma implementação de método existe (são interfaces), portanto não há stubs de comportamento.

---

## Threat Flags

Nenhuma superfície nova além do mapeado no `<threat_model>` do plano:

- `T-02-04-01` (Tampering nos parâmetros de `execute()`): parâmetros primitivos aceitos — validação delegada ao `User.create()` via Email VO na implementação do use case (Fase 3) ✅
- `T-02-04-02` (EoP via `save()` polimórfico): mitigação por design aceita — os use cases (Fase 3) controlam quando `save()` é chamado ✅
- `T-02-04-03` (Information Disclosure via `findByEmail()`): `existsByEmail()` criado como alternativa mais segura para verificação de duplicatas ✅

---

## Status: DONE ✅

---

## Self-Check: PASSED

- ✅ `UserRepositoryPort.java` existe em `mekano-domain/.../domain/port/out/`
- ✅ `CreateUserInputPort.java` existe em `mekano-domain/.../domain/port/in/`
- ✅ `UserRepositoryPort` tem 4 métodos: `save(User)`, `findById(UUID)`, `findByEmail(String)`, `existsByEmail(String)`
- ✅ `findById(UUID)` retorna `Optional<User>`
- ✅ `findByEmail(String)` retorna `Optional<User>`
- ✅ `existsByEmail(String)` retorna `boolean`
- ✅ `CreateUserInputPort.execute()` aceita `(String, String, String)` e retorna `User`
- ✅ Zero anotações CDI/JPA/REST em ambas as interfaces
- ✅ `CreateUserInputPort` NÃO usa `CreateUserCommand` como tipo de parâmetro no método
- ✅ `mvn compile -pl mekano-domain` → exit code 0 (BUILD SUCCESS)
- ✅ Commit `feef6de` existe
