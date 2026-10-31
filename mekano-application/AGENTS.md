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
    ├── ordemdeservico/          ## FULLY IMPLEMENTED
    │   ├── OrdemDeServicoService.java — @ApplicationScoped, implements OrdemDeServicoServicePort
    │   │                                constructor injection, @Transactional on all write methods
    │   │                                manages OS lifecycle (create → diagnose → execute → deliver)
    │   │                                validates items via ItemOsRepositoryPort + PecaRepository/ServicoRepository
    │   │                                create(): persists items to os_itens junction table
    │   │                                update(): deletes old items, persists new ones
    │   │                                finalizarDiagnostico(): adds mechanic items, creates Orcamento from ALL items
    │   └── (no response record — returns domain entity directly)
    ├── orcamento/               ## FULLY IMPLEMENTED
    │   └── OrcamentoService.java      — @ApplicationScoped, implements OrcamentoServicePort
    │                                    aprovacao/reprovacao with status validation
    ├── pagamento/               ## FULLY IMPLEMENTED
    │   └── PagamentoService.java      — @ApplicationScoped
    │                                    confirmacao with status validation
    ├── peca/                    ## FULLY IMPLEMENTED (re-verified 2026-10-17)
    │   ├── PecaService.java           — @ApplicationScoped, constructor injection
    │   │                                (PecaRepositoryPort + EventPublisher + ...); usado por
    │   │                                PecaResource, AlertaResource, RequisicaoCompraResource e PecaOrcamentoObserver
    │   └── CreatePecaResponse.java    — record
    ├── nfentrada/               ## FULLY IMPLEMENTED (re-verified 2026-10-17)
    │   ├── NfEntradaService.java      — @ApplicationScoped, constructor injection, @Transactional
    │   │                                em registrar(); valida status PRODUTO_RECEBIDO da requisição (409),
    │   │                                bloqueia chave de acesso duplicada (409), credita saldo por item da
    │   │                                requisição e publica EstoqueMinimoAtingidoEvent
    │   └── CreateNfEntradaResponse.java — record
    └── requisicao/              ## FULLY IMPLEMENTED
        ├── RequisicaoCompraService.java — @ApplicationScoped, does NOT implement any port
        │                                  constructor injection (RequisicaoCompraRepositoryPort + PecaRepositoryPort)
        │                                  @Transactional on criar/cancelar/enviar/marcarComoComprada/marcarComoRecebida
        │                                  criar(): validates peca existence via PecaRepositoryPort
        │                                  cancelar(): blocks cancellation when motivo=ORDEM_SERVICO
        └── CreateRequisicaoCompraResponse.java — record
```

## Service Pattern (For IMPLEMENTED services)
1. `@ApplicationScoped` — CDI bean
2. Implements domain `port/in/*Port.java` interface (exceção documentada: Peca, NfEntrada e RequisicaoCompra não têm port de entrada — resources chamam a service direto; extrair ports é melhoria planejada)
3. **Constructor injection** (required for `@InjectMocks` in Mockito tests)
4. `@Transactional` on ALL write methods (execute, create, update, delete)
5. Flow: validate → check duplicates → hash (if password) → create entity → save → publish event
6. Response records never expose `passwordHash` or domain entities directly

## Logging (in Services) — develop, D-19
- `io.quarkus.logging.Log` estático — NUNCA SLF4J `LoggerFactory`
- INFO=fluxo normal de escrita, WARN=erro de cliente/negócio (4xx, incl. transições 422), ERROR=NUNCA (só no `ApiExceptionMapper`)
- Com parâmetros usar `Log.infof`/`Log.warnf` com `%s`; formato `chave=valor` em PT-BR; nunca logar PII
- Detalhes completos em `AGENTS.md` raiz → "Logging Conventions"

## Known Deviations (re-verified 2026-10-17)
- `PecaService`/`NfEntradaService`/`RequisicaoCompraService` não implementam port de entrada — dívida documentada em docs/ARCHITECTURE-REVIEW-2026-10-17.md (sprint 1 extraiu `PecaServicePort`/`NfEntradaServicePort`/`RequisicaoCompraServicePort`)
- ~~Alguns erros hardcoded fora do `Messages` bundle~~ **RESOLVIDO** — todas as mensagens via `Messages.get` (sprint 2)

## Dependencies
- **compile**: mekano-domain, quarkus-arc
- **provided**: lombok
- **test**: junit-jupiter, mockito-junit-jupiter

## Testing
- JUnit 5 + Mockito `@ExtendWith(MockitoExtension.class)` — no Quarkus container
- `@Mock` for ports, `@InjectMocks` for service
- 17 test files: `UserServiceTest`, `VeiculoServiceTest`, `ServicoServiceTest`, `ClienteServiceTest`, `OrdemDeServicoServiceTest`, `OrcamentoServiceTest`, `PecaServiceTest`, `NfEntradaServiceTest`, `RequisicaoCompraServiceTest`, `AdminUserServiceTest`, `AuthServiceJwtTest`, `PasswordGeneratorTest`, `RefreshTokenServiceTest`, `MockPaymentServiceTest`, `CobrancaEmitidaListenerTest`, `OsAuditEventPublisherTest`, `OsAuditQueryServiceTest`, `WhatsAppOrcamentoRespostaServiceTest`
- No tests for stub services (Peca, NfEntrada)
