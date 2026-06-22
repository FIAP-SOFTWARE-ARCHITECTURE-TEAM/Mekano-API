# Walking Skeleton — Mekano

**Phase:** 1 — Auth & OS Foundation
**Generated:** 2026-06-22

## Capability Proven End-to-End

> A user with role "admin" can log in, receive a JWT pair, create a client, create a vehicle for that client, and the public can query the client's service order status.

## Architectural Decisions

| Decision | Choice | Rationale |
|---|---|---|
| Framework | Quarkus 3.36.0 (JAX-RS + Panache) | Existing codebase; mature JWT/EdDSA support via SmallRye |
| Data layer | PostgreSQL 16 + Flyway migrations | Existing; H2 in tests for speed |
| Auth | Ed25519/EdDSA JWT + BCrypt + refresh token rotation | EdDSA smaller than RSA; rotation prevents replay (D-01/D-03) |
| API prefix | `/api/v1` via `quarkus.rest.path` | Existing standard (D-08) |
| Exception format | RFC 7807 Problem Details (`application/problem+json`) | Existing standard (D-09) |
| MapStruct | `componentModel = "cdi"` | Existing standard (G9) |
| Hybrid ID | `Long id` PK (auto-increment) + `UUID uuid` (unique, API-facing) | Existing standard; prevents enumeration (D-19) |
| Soft delete | `is_active` + `deleted_at` columns | Existing pattern; queries filter `isActive = true` |
| State machine | Enum `StatusOS` with `Map<StatusOS, Set<StatusOS>>` transition matrix | Single source of truth; testable with parameterized test (D-25) |
| OS transactions | `@Transactional` on use case only (never resource/repository) | Existing standard (D-01) |
| Cache | Caffeine `@CacheResult` on reads, `@CacheInvalidate` on writes | Existing pattern (D-13) |
| Inter-context events | CDI events (`CdiEventPublisher`) | Decoupled without Kafka overengineering |
| Key management | Ed25519 keys: public committed, private at `~/.mekano/secrets/` | Security; public key needed for verification at runtime |

## Stack Touched in Phase 1

- [x] Project scaffold (Maven multi-module, Quarkus 3.36.0, Lombok, MapStruct)
- [x] Routing — existing `/api/v1/users`, `/api/v1/auth`
- [x] Database — PostgreSQL DevServices + Flyway V1-V5; V6-V10 added
- [x] UI — API-only (no frontend)
- [x] Deployment — `docker-compose up -d` (PostgreSQL) + `./mvnw quarkus:dev`
- [ ] Auth — Ed25519 JWT with 5 roles + refresh token rotation (V6 + plan 01-01)
- [ ] Cliente CRUD (plan 01-02)
- [ ] Veiculo CRUD (plan 01-03)
- [ ] Servico CRUD (plan 01-04)
- [ ] OrdemDeServico + public status (plan 01-05)
- [ ] Sequence diagrams (plan 01-06)

## Out of Scope (Deferred to Later Slices)

- Client type CNPJ (pessoa jurídica) — Phase 2
- Orçamento generation, approval workflows — Phase 2
- OS execution, finalization, delivery — Phases 2-3
- Estoque (stock management) — Phase 2
- Payment / billing / simulated bank — Phase 3
- SLA timer for automatic cancellation — Phase 3
- Notification (WhatsApp/email) — v2 (not in v1 requirements)
- Front-end / GUI — API-first
- OAuth / 2FA — v2

## Subsequent Slice Plan

- Phase 2: OS Continuation & Estoque — orçamento/approval, execution/finalization, full stock CRUD
- Phase 3: Pagamento & Delivery — billing, payment mock, delivery, SLA, docs
