---
phase: 02-m-dulo-domain
plan: "02"
subsystem: domain
tags: [value-object, email, validation, immutability, domain, clean-architecture, java]
dependency_graph:
  requires:
    - InvalidEmailException (02-01)
    - DomainException (02-01)
  provides:
    - Email (Value Object imutável com validação RFC 5322 e normalização lowercase)
  affects:
    - mekano-domain (pacote valueobject)
    - Plano 02-05 (testes unitários do Email VO)
    - Plano 02-06 (entidade User usará Email ao invés de String)
    - Fase 3 (application — CreateUserUseCase recebe Email ou String e constrói Email)
tech_stack:
  added: []
  patterns:
    - Value Object imutável com validação no construtor
    - Null-check antes de regex (evita NPE)
    - Normalização de dados de entrada via Locale.ROOT
    - Pattern compilado como constante estática (thread-safe)
key_files:
  created:
    - mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/Email.java
  modified: []
  deleted: []
decisions:
  - "Sem método fábrica Email.of() — construtor público é o único ponto de entrada, por DDD convention"
  - "Null check explícito ANTES do regex evita NullPointerException silencioso no matcher"
  - "Locale.ROOT na normalização garante comportamento determinístico independente de locale do servidor"
  - "Pattern como constante estática (não local) — compilado uma vez, thread-safe por spec Java"
metrics:
  duration: "~5 minutos"
  completed_date: "2025-07-15"
  tasks_completed: 1
  tasks_total: 1
  files_created: 1
  files_deleted: 0
---

# Phase 2 Plan 02: Email Value Object — Summary

**One-liner:** Value Object `Email` imutável com validação RFC 5322 simplificada, null-check antes de regex e normalização para lowercase via `Locale.ROOT`.

---

## O que foi criado

### `Email.java` — pacote `com.fiap.mekano.domain.valueobject`

| Aspecto | Detalhe |
|---------|---------|
| Tipo | `public final class` |
| Campo | `private final String value` |
| Anotações | `@Getter`, `@EqualsAndHashCode`, `@ToString` |
| Anotações proibidas | Nenhuma (`@Data`, `@Setter`, `@Builder`, `@Value` ausentes) |
| Validação | null check → isBlank check → regex RFC 5322 → lowercase |
| Regex | `^[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}$` |
| Normalização | `trimmed.toLowerCase(Locale.ROOT)` |
| Exceção lançada | `InvalidEmailException` (herdada do Plano 02-01) |
| Imports proibidos | Zero (`jakarta.*`, `io.quarkus.*`, `org.hibernate.*` ausentes) |

### Contrato do construtor

```
new Email(null)        → lança InvalidEmailException("null")
new Email("")          → lança InvalidEmailException("")
new Email("   ")       → lança InvalidEmailException("")
new Email("semArroba") → lança InvalidEmailException("semArroba")
new Email("@dom.com")  → lança InvalidEmailException("@dom.com")
new Email("user@example.com")    → Email{value="user@example.com"}
new Email("USER@FIAP.BR")        → Email{value="user@fiap.br"}
new Email("a@b.co").getValue()   → "a@b.co"
new Email("x@y.io").equals(new Email("x@y.io")) → true
new Email("x@y.io").equals(new Email("z@y.io")) → false
```

---

## Verificação

### Compilação

```
cd C:\Users\victo\Desktop\fiap-software-architecture\mekano
.\mvnw.cmd compile -pl mekano-domain -q
Exit code: 0  →  BUILD SUCCESS
```

### Zero imports proibidos

```
Select-String -Path Email.java -Pattern "jakarta|quarkus|hibernate|javax\.ws"
→ zero resultados ✅
```

### Zero anotações proibidas

```
Select-String -Path Email.java -Pattern "@Data|@Setter|@Builder|@Value"
→ zero resultados ✅
```

### Estrutura de arquivos

```
mekano-domain\src\main\java\com\fiap\mekano\domain\valueobject\
└── Email.java
```

---

## Commits

| Tarefa | Hash | Mensagem |
|--------|------|----------|
| Tarefa 1 | `8590064` | `feat(domain): adiciona Email Value Object com validacao e normalizacao` |

---

## Desvios do Plano

Nenhum — plano executado exatamente como escrito.

---

## Known Stubs

Nenhum — `Email.java` é uma classe completa e funcional. Validação, normalização e imutabilidade implementadas integralmente.

---

## Threat Flags

Nenhuma superfície nova além do mapeado no `<threat_model>` do plano:

- `T-02-02-01`: Null check antes do regex implementado (`value == null` checado ANTES de `EMAIL_PATTERN.matcher()`) ✅
- `T-02-02-02`: Regex simplificado — emails Unicode são rejeitados; aceitável para escopo atual ✅
- `T-02-02-03`: `@ToString` expõe email em logs (PII) — adapter deve logar com cautela (responsabilidade da Fase 5) ✅

---

## Status: DONE ✅

---

## Self-Check: PASSED

- ✅ `Email.java` existe em `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/`
- ✅ Classe é `public final`
- ✅ Campo `private final String value` presente
- ✅ `@Getter`, `@EqualsAndHashCode`, `@ToString` presentes
- ✅ Zero anotações proibidas (`@Data`, `@Setter`, `@Builder`, `@Value`)
- ✅ Zero imports proibidos (`jakarta.*`, `io.quarkus.*`, `org.hibernate.*`)
- ✅ Null check antes do regex (T-02-02-01 mitigado)
- ✅ Normalização `Locale.ROOT` aplicada
- ✅ `mvn compile -pl mekano-domain` → exit code 0 (BUILD SUCCESS)
- ✅ Commit `8590064` existe
