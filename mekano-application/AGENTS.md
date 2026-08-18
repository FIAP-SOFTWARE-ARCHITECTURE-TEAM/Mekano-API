# mekano-application — Service Orchestration Layer

## Constraint
Minimal framework deps: only `quarkus-arc` for `@ApplicationScoped`. No `jakarta.persistence`, `jakarta.ws.rs`, `org.hibernate`.

## Package Map (Verified Against Source)

```
com.fiap.mekano.application
└── service/
    ├── user/                    ## FULLY IMPLEMENTED
    │   ├── UserService.java           — @ApplicationScoped, implements UserServicePort
    │   │                                constructor injection, @Transactional on execute/deleteUser
    │   │                                creates User, hashes password, publishes UserCreatedEvent
    │   └── CreateUserResponse.java    — record(UUID id, String name, String email, LocalDateTime createdAt)
    ├── cliente/                 ## FULLY IMPLEMENTED
    │   ├── ClienteService.java        — @ApplicationScoped, implements ClienteServicePort
    │   │                                constructor injection, @Transactional on execute/update/delete
    │   │                                validates required fields, preserves cpf/createdAt on update
    │   └── CreateClienteResponse.java — record
    ├── vehicle/                 ## FULLY IMPLEMENTED
    │   ├── VeiculoService.java        — @ApplicationScoped, implements VeiculoServicePort
    │   │                                constructor injection, @Transactional on execute/update/delete
    │   │                                validates client exists, checks plate uniqueness, publishes event
    │   └── (no response record — returns domain entity directly)
    ├── servico/                 ## FULLY IMPLEMENTED
    │   ├── ServicoService.java        — @ApplicationScoped, implements ServicoServicePort
    │   │                                constructor injection, @Transactional on create/update/delete
    │   │                                nome normalization, uniqueness check with existsByNomeAndIdNot
    │   └── (no response record — returns domain entity directly)
    ├── peca/                    ## STUB (incomplete)
    │   ├── PecaService.java           — @ApplicationScoped, does NOT implement any port
    │   │                                ⚠ field injection (@Inject) — violates constructor injection convention
    │   │                                ⚠ criar() returns hardcoded values — not real implementation
    │   └── CreatePecaResponse.java    — record
    ├── nfentrada/               ## STUB (incomplete)
    │   ├── NfEntradaService.java      — @ApplicationScoped, does NOT implement any port
    │   │                                ⚠ field injection, hardcoded response
    │   └── CreateNfEntradaResponse.java — record
    └── requisicao/              ## STUB (incomplete)
        ├── RequisicaoCompraService.java — @ApplicationScoped, does NOT implement any port
        │                                  ⚠ field injection, hardcoded response
        └── CreateRequisicaoCompraResponse.java — record
```

## Service Pattern (For IMPLEMENTED services)
1. `@ApplicationScoped` — CDI bean
2. Implements domain `port/in/*Port.java` interface
3. **Constructor injection** (required for `@InjectMocks` in Mockito tests)
4. `@Transactional` on ALL write methods (execute, create, update, delete)
5. Flow: validate → check duplicates → hash (if password) → create entity → save → publish event
6. Response records never expose `passwordHash` or domain entities directly

## Stub Services — What NOT to Do
The 3 stub services (`PecaService`, `NfEntradaService`, `RequisicaoCompraService`) violate conventions:
- No port implementation
- Field injection instead of constructor injection
- No `@Transactional`
- Return hardcoded values

These are placeholders awaiting real implementation.

## Dependencies
- **compile**: mekano-domain, quarkus-arc
- **provided**: lombok
- **test**: junit-jupiter, mockito-junit-jupiter

## Testing
- JUnit 5 + Mockito `@ExtendWith(MockitoExtension.class)` — no Quarkus container
- `@Mock` for ports, `@InjectMocks` for service
- 3 test files: `UserServiceTest`, `VeiculoServiceTest`, `ServicoServiceTest`
- No tests for stub services (Peca, NfEntrada, RequisicaoCompra)
