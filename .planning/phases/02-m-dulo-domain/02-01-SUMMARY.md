---
phase: 02-m-dulo-domain
plan: "01"
subsystem: domain
tags: [exceptions, domain, clean-architecture, java]
dependency_graph:
  requires: []
  provides:
    - DomainException (classe base abstrata)
    - InvalidEmailException
    - UserAlreadyExistsException
    - UserNotFoundException
  affects:
    - mekano-domain (pacote exception)
    - Fase 3 (application — casos de uso lançam exceções)
    - Fase 5 (adapter — ExceptionMapper traduz para HTTP)
tech_stack:
  added: []
  patterns:
    - Hierarquia de exceções de domínio sem acoplamento de framework
    - Exceções concretas com mensagem contextualizada no construtor
key_files:
  created:
    - mekano-domain/src/main/java/com/fiap/mekano/domain/exception/DomainException.java
    - mekano-domain/src/main/java/com/fiap/mekano/domain/exception/InvalidEmailException.java
    - mekano-domain/src/main/java/com/fiap/mekano/domain/exception/UserAlreadyExistsException.java
    - mekano-domain/src/main/java/com/fiap/mekano/domain/exception/UserNotFoundException.java
  modified: []
  deleted:
    - mekano-domain/src/main/java/com/fiap/mekano/domain/.gitkeep
decisions:
  - "Exceções de domínio estendem RuntimeException via DomainException — sem checked exceptions"
  - "Zero imports de framework — isolamento total da camada de domínio"
  - "Mensagens incluem contexto (valor inválido, email, identificador) para facilitar diagnóstico"
metrics:
  duration: "~5 minutos"
  completed_date: "2025-07-15"
  tasks_completed: 2
  tasks_total: 2
  files_created: 4
  files_deleted: 1
---

# Phase 2 Plan 01: Exceções de Domínio — Summary

**One-liner:** Hierarquia de exceções de domínio puras (`DomainException` + 3 subclasses) sem nenhum import de framework, compilando com BUILD SUCCESS.

---

## O que foi criado

### 4 classes Java no pacote `com.fiap.mekano.domain.exception`

| Classe | Tipo | Descrição |
|--------|------|-----------|
| `DomainException` | `abstract class` | Classe base que estende `RuntimeException`. Dois construtores `protected`: `(String message)` e `(String message, Throwable cause)`. Nenhum import. |
| `InvalidEmailException` | `class` | Lançada pelo Value Object `Email` quando o formato é inválido. Mensagem: `"Formato de email inválido: <valor>"` |
| `UserAlreadyExistsException` | `class` | Lançada pelo caso de uso `CreateUser` quando email já está cadastrado. Mensagem: `"Usuário já existe com o email: <email>"` |
| `UserNotFoundException` | `class` | Lançada em lookups de usuário por id ou email. Mensagem: `"Usuário não encontrado: <identifier>"` |

### Arquivo removido

- `mekano-domain/src/main/java/com/fiap/mekano/domain/.gitkeep` — placeholder removido após inserção de código Java real no pacote.

---

## Verificação

### Compilação

```
cd C:\Users\victo\Desktop\fiap-software-architecture\mekano
.\mvnw.cmd compile -pl mekano-domain -q
Exit code: 0  →  BUILD SUCCESS
```

### Estrutura de arquivos

```
mekano-domain\src\main\java\com\fiap\mekano\domain\exception\
├── DomainException.java
├── InvalidEmailException.java
├── UserAlreadyExistsException.java
└── UserNotFoundException.java
```

### Zero imports proibidos

Verificação com `Select-String -Pattern "^import (jakarta|io\.quarkus|org\.hibernate|javax\.ws)"` → **zero resultados** em todos os 4 arquivos.

---

## Commits

| Tarefa | Hash | Mensagem |
|--------|------|----------|
| Tarefa 1 | `3957dbe` | `feat(domain): adiciona DomainException base e limpa .gitkeep` |
| Tarefa 2 | `71cf4a7` | `feat(domain): adiciona excecoes de dominio concretas` |

---

## Desvios do Plano

Nenhum — plano executado exatamente como escrito.

---

## Known Stubs

Nenhum — as 4 classes são completas e funcionais. As mensagens de erro já estão com contexto real.

---

## Threat Flags

Nenhuma superfície nova além do mapeado no `<threat_model>` do plano:

- `T-02-01-01`: `DomainException.getMessage()` — mensagens contêm apenas dados não-sensíveis (email formatado, identificador). `passwordHash` nunca aparece.
- `T-02-01-03`: `UserNotFoundException` expõe o identificador buscado — o adapter deverá sanitizar antes de retornar ao cliente HTTP (responsabilidade da Fase 5).

---

## Status: DONE ✅

---

## Self-Check: PASSED

- ✅ `DomainException.java` existe
- ✅ `InvalidEmailException.java` existe
- ✅ `UserAlreadyExistsException.java` existe
- ✅ `UserNotFoundException.java` existe
- ✅ `.gitkeep` removido
- ✅ Commit `3957dbe` existe
- ✅ Commit `71cf4a7` existe
- ✅ `mvn compile -pl mekano-domain` → exit code 0 (BUILD SUCCESS)
