# mekano-application — Casos de Uso

## Overview

Camada de aplicação — orquestra regras de negócio invocando ports do domínio. Não contém lógica de negócio (essa fica no domain), apenas coordenação: validação → duplicidade → hash → persistência → evento.

**Dependência mínima de framework**: apenas `quarkus-arc` para `@ApplicationScoped` + `quarkus-elytron-security-common` para BCrypt.

## Package Structure

```
com.fiap.mekano.application
└── usecase/
    └── user/
        ├── CreateUserUseCase.java       # Criação de usuário
        ├── CreateUserResponse.java      # Record de resposta (não expõe User entity)
        └── AuthenticateUserUseCase.java # Autenticação
```

## Key Conventions

### Use Case Structure (`CreateUserUseCase.java:32-122`)
1. `@ApplicationScoped` — CDI permite injeção
2. **Constructor injection** — necessário para `@InjectMocks` do Mockito
3. Implementa `CreateUserInputPort` (interface no domain)
4. Injeta ports: `UserRepositoryPort`, `PasswordHasher`, `EventPublisher`
5. `@Transactional` no método `execute()` — unidade de trabalho do use case
6. Fluxo do `execute()`:
   - Valida dados (nome não-nulo)
   - Verifica duplicidade (`existsByEmail`)
   - Gera hash da senha (`PasswordHasher.hash()`)
   - Cria entidade (`User.create()`)
   - Persiste (`userRepository.save()`)
   - Publica evento (`eventPublisher.publish()`)

### CreateUserResponse (`CreateUserResponse.java`)
- Record Java: `id`, `name`, `email`, `createdAt`
- **NUNCA expõe** `passwordHash` ou a entidade `User` — D-04

### AuthenticateUserUseCase
- `@ApplicationScoped`, SEM `@Transactional` (leitura pura)
- Injeta `UserRepositoryPort` + `PasswordHasher`
- Fluxo: busca por email → verifica hash → retorna User

## Dependencies

- **compile**: `mekano-domain`, `quarkus-arc`, `quarkus-elytron-security-common`
- **provided**: `lombok`
- **test**: `junit-jupiter`, `mockito-junit-jupiter`

## How to Add New Use Case

1. Criar interface no domain (`port/in/NovoInputPort.java`)
2. Criar command record no domain (`port/in/NovoCommand.java`) se necessário
3. Criar use case em `application/usecase/subdominio/NovoUseCase.java`:
   - `@ApplicationScoped`, implementa `NovoInputPort`
   - Injeta ports necessários via construtor
   - `@Transactional` se for operação de escrita
4. Criar response record em `application/usecase/subdominio/NovoResponse.java` se necessário

## Auditing

```bash
# Verificar que não há imports de framework proibidos no código fonte
grep -r "jakarta.persistence\|jakarta.ws.rs\|org.hibernate\|io.quarkus" mekano-application/src/main/java
# Deve retornar zero resultados

# Verificar que @Transactional é usado corretamente
grep -n "@Transactional" mekano-application/src/main/java
# Deve aparecer apenas em métodos execute()
```

## Testing

- **JUnit 5 + Mockito puro** — sem Quarkus
- `@ExtendWith(MockitoExtension.class)`
- `@Mock` para ports, `@InjectMocks` para use case
- Cenários: sucesso, duplicidade, validação, exceções

Exemplo: `CreateUserUseCaseTest.java`
