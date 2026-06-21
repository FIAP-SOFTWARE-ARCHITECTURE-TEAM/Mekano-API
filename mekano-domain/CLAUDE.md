# mekano-domain — Núcleo Puro do Clean Architecture

## Overview

Camada de domínio — zero dependências de framework. Contém as entidades de negócio, value objects, interfaces de port (contratos) e exceções de domínio.

**Regra fundamental**: nenhum import de `jakarta.*`, `io.quarkus.*`, `org.hibernate.*` neste módulo. Apenas Java SE + Lombok (`scope=provided`).

## Package Structure

```
com.fiap.mekano.domain
├── model/             # Entidades de domínio (POJO puro)
│   └── User.java
├── valueobject/       # Value Objects imutáveis
│   └── Email.java
├── port/
│   ├── in/            # Input ports (driving side — chamados por application)
│   │   ├── CreateUserInputPort.java
│   │   ├── AuthenticateUserInputPort.java
│   │   ├── PasswordHasher.java          # Abstração de hash (impl em infrastructure)
│   │   ├── CreateUserCommand.java       # Record — dados de entrada
│   │   └── AuthenticateUserCommand.java # Record — dados de entrada
│   └── out/            # Output ports (driven side — implementados por infrastructure)
│       ├── UserRepositoryPort.java
│       ├── RefreshTokenRepositoryPort.java
│       ├── RefreshTokenData.java        # Record — dados de saída
│       └── EventPublisher.java          # Abstração de eventos de domínio
├── exception/         # Exceções de domínio
│   ├── DomainException.java             # Unchecked (RuntimeException) — base
│   ├── BusinessException.java           # Checked (Exception) — regras recuperáveis
│   ├── InvalidEmailException.java
│   ├── InvalidUserDataException.java
│   ├── UserAlreadyExistsException.java  # extends BusinessException (checked)
│   ├── UserNotFoundException.java       # extends DomainException
│   ├── InvalidCredentialsException.java
│   ├── InvalidRefreshTokenException.java
│   └── RateLimitExceededException.java
└── event/             # Eventos de domínio
    └── UserCreatedEvent.java            # Record — imutável
```

## Key Files & Patterns

### User.java (`model/User.java:27-86`)
- `@Builder(access = PRIVATE)` — força uso dos factory methods
- `@ToString.Exclude` em `passwordHash` — nunca expor hash em logs
- Factory `User.create(name, emailValue, passwordHash)` — gera UUID + timestamp
- Factory `User.reconstitute(id, name, emailValue, passwordHash, createdAt)` — preserva valores existentes (usado por mappers JPA)

### Email.java (`valueobject/Email.java:24-51`)
- Construtor valida: null → `InvalidEmailException`, blank → `InvalidEmailException`, regex → `InvalidEmailException`
- Normaliza: `trimmed.toLowerCase(Locale.ROOT)` — evita duplicatas por casing
- Pattern compilado como `static final` — thread-safe, sem overhead por instância
- `@EqualsAndHashCode` — igualdade por valor (dois Emails com mesmo endereço são iguais)

### Ports (`port/`)
- **Input ports**: interfaces que o *mundo externo* chama para executar ações no sistema. Ex: `CreateUserInputPort`, `AuthenticateUserInputPort`
- **Output ports**: interfaces que o *domínio* chama para acessar recursos externos. Ex: `UserRepositoryPort`, `EventPublisher`
- **Commands**: records que carregam dados de entrada para os use cases. Ex: `CreateUserCommand`
- **Data objects**: records que carregam dados de saída dos repositórios. Ex: `RefreshTokenData`

### Exceptions
- `DomainException extends RuntimeException` — unchecked, para erros de validação internos (email inválido, usuário não encontrado)
- `BusinessException extends Exception` — checked, para regras de negócio recuperáveis (email duplicado)
- Todas as subclasses têm construtor que aceita `String message` e opcionalmente `Throwable cause`

## Dependencies

- **compile**: nenhuma (só Java SE)
- **provided**: `lombok` (boilerplate)
- **test**: `junit-jupiter` (JUnit 5 puro)

## How to Add New Entity

1. Criar `model/NovaEntidade.java` — POJO com `@Builder(access = PRIVATE)`, factory methods `create()` e `reconstitute()`
2. Criar `valueobject/NovoVO.java` — imutável, valida no construtor, `@EqualsAndHashCode`
3. Criar `port/in/NovoInputPort.java` — interface do use case
4. Criar `port/out/NovoRepositoryPort.java` — interface do repository
5. Criar `exception/NovaExcecao.java` — estende `DomainException` ou `BusinessException`
6. Criar `exception/NovaExcecaoNotFoundException.java` se aplicável

## Testing

- **JUnit 5 puro** — sem Quarkus, sem container, sem Mockito
- Testes unitários verificam: validação de VO, factory methods, exceções
- `@ParameterizedTest` para múltiplos cenários de validação
- `assertThrows` para verificar exceções

Exemplos: `EmailTest.java`, `UserTest.java`
