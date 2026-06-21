# mekano-application — Serviços de Aplicação

## Overview

Camada de aplicação — orquestra regras de negócio invocando ports do domínio. Não contém lógica de negócio (essa fica no domain), apenas coordenação: validação → duplicidade → hash → persistência → evento.

**Dependência mínima de framework**: apenas `quarkus-arc` para `@ApplicationScoped`. O BCrypt fica exclusivamente no módulo `mekano-infrastructure` (via `PasswordHasher` interface no domain).

## Package Structure

```
com.fiap.mekano.application
└── service/
    └── user/
        ├── UserService.java              # Serviço de usuário (criação, busca, deleção)
        └── CreateUserResponse.java       # Record de resposta (não expõe User entity)
```

## Key Conventions

### Service Structure (`UserService.java`)
1. `@ApplicationScoped` — CDI permite injeção
2. **Constructor injection** — necessário para `@InjectMocks` do Mockito
3. Implementa `UserServicePort` (interface no domain)
4. Injeta ports: `UserRepositoryPort`, `PasswordHasher`, `EventPublisher`
5. `@Transactional` no método `execute()` — unidade de trabalho do service
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

## Dependencies

- **compile**: `mekano-domain`, `quarkus-arc`
- **provided**: `lombok`
- **test**: `junit-jupiter`, `mockito-junit-jupiter`

## How to Add New Service

1. Criar interface no domain (`port/in/NovoServicePort.java`)
2. Criar command record no domain (`port/in/NovoCommand.java`) se necessário
3. Criar service em `application/service/subdominio/NovoService.java`:
   - `@ApplicationScoped`, implementa `NovoServicePort`
   - Injeta ports necessários via construtor
   - `@Transactional` se for operação de escrita
4. Criar response record em `application/service/subdominio/NovoResponse.java` se necessário

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
- `@Mock` para ports, `@InjectMocks` para service
- Cenários: sucesso, duplicidade, validação, exceções

Exemplo: `UserServiceTest.java`
