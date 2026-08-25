# Design Document: Tech Debt Cleanup

## Overview

This design addresses four tech debt items in the Mekano API multi-module Quarkus project. All changes are safe deletions or annotation corrections with no behavioral impact on production logic. The cleanup removes dead code (empty mapper classes, unused enum, unused value object) and corrects JPA entity annotations to avoid known Lombok pitfalls with `@Data` on Hibernate-managed entities.

**Key Constraint:** All four changes are independent at the source level — no change depends on another. However, the implementation order is chosen to minimize intermediate compilation risk and enable incremental verification.

## Architecture

The Mekano API follows a layered Clean Architecture:

```
mekano-rest → mekano-application → mekano-domain
                                  ↗
mekano-infrastructure ───────────┘
```

Module dependency direction (compile-time):
- `mekano-domain`: zero dependencies on other modules (pure domain model)
- `mekano-application`: depends on `mekano-domain`
- `mekano-infrastructure`: depends on `mekano-domain` and `mekano-application`
- `mekano-rest`: depends on all three

All four changes target either `mekano-domain` or `mekano-infrastructure` — the two innermost layers. No API contracts, REST endpoints, or application-level service interfaces are affected.

## Components and Interfaces

### Change 1: Remove Empty Mapper Classes (mekano-infrastructure)

**Files to delete:**
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/PecaEntityMapper.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/RequisicaoCompraEntityMapper.java`
- `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/NfEntradaEntityMapper.java`

**Rationale:** These three classes are annotated `@ApplicationScoped` but contain zero methods. Grep confirms no imports, field declarations, or constructor injections reference them anywhere in the codebase. They were likely created as stubs during initial scaffolding and never implemented.

**Impact:** None. CDI will simply not register these beans. No injection points reference them.

### Change 2: Remove Duplicate StatusPagamento Enum (mekano-domain)

**File to delete:**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/model/StatusPagamento.java`

**File to keep (canonical):**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/os/StatusPagamento.java`

**Rationale:** The duplicate enum in `domain.model` has values `PENDENTE` and `CONFIRMADO` with description strings, while the canonical enum in `domain.os` has `NAO_COBRADO`, `AGUARDANDO_PAGAMENTO`, `CONFIRMADO`, `CANCELADO` with a state-transition map. Grep confirms zero imports of `com.fiap.mekano.domain.model.StatusPagamento` anywhere in the project.

**Impact:** None. The file is unreferenced dead code.

### Change 3: Convert Entity Annotations (mekano-infrastructure)

**Files to modify:**
- `PecaEntity.java`
- `RequisicaoCompraEntity.java`
- `NfEntradaEntity.java`

**Changes per file:**
1. Replace `import lombok.Data;` with `import lombok.Getter;` and `import lombok.Setter;`
2. Replace `@Data` annotation with `@Getter` and `@Setter`
3. Change all `public` field declarations to `private`

**Annotations to keep unchanged:** `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Entity`, `@Table`, `@Column`

**Rationale:** Lombok's `@Data` generates `equals()`, `hashCode()`, and `toString()` methods. On JPA entities these cause:
- **Lazy-loading issues:** `toString()` may trigger unintended fetches of lazy associations
- **Proxy equality bugs:** `equals()`/`hashCode()` fail when comparing a Hibernate proxy with the real entity
- **Infinite recursion:** Bidirectional relationships can cause stack overflow in `toString()`

`BaseEntity` (the superclass) already uses `@Getter`/`@Setter` with package-private fields, establishing the project convention.

**Compatibility:** Lombok's `@Builder` works with `@Getter`/`@Setter` — it generates the builder based on the `@AllArgsConstructor` and does not depend on `@Data`. Field access throughout the project already goes through getter/setter methods (Panache queries use field names directly for JPQL, not Java field access).

### Change 4: Remove Duplicate Placa Value Object (mekano-domain)

**Files to delete:**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/Placa.java`
- `mekano-domain/src/test/java/com/fiap/mekano/domain/valueobject/PlacaTest.java`

**File to keep (canonical):**
- `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/PlacaVeiculo.java`

**Rationale:** Both validate Brazilian vehicle license plates but with different patterns:
- `Placa.java`: 7-char old format (`[A-Z]{3}[0-9]{4}`) + 8-char Mercosul (`[A-Z]{2}[0-9]{4}[A-Z]{2}`)
- `PlacaVeiculo.java`: 7-char old format (`[A-Z]{3}[0-9]{4}`) + 7-char Mercosul (`[A-Z]{3}[0-9][A-Z][0-9]{2}`)

`PlacaVeiculo` is the one actually imported and used (by the `Veiculo` domain model). `Placa` has zero imports. The Mercosul format in `PlacaVeiculo` (`ABC1D23`) is the correct official format adopted by Brazil.

**Impact:** None on production code. The associated test file (`PlacaTest.java`) tests the dead `Placa` class and must also be removed to keep the test suite compiling.

## Data Models

No data model changes. This cleanup does not alter:
- Database schema
- Entity field definitions (only access modifiers change, Hibernate accesses fields reflectively)
- API DTOs or response structures
- Domain model classes

The `private` field modifier on entities is transparent to Hibernate ORM, which uses reflection-based field access configured by `@Column` annotations.

## Error Handling

No error handling changes. All removed code is dead (unreachable), and the annotation swap produces identical runtime behavior for field access.

## Testing Strategy

**PBT is not applicable** for this feature. The changes are:
- Deletion of dead code (no logic to test)
- Annotation replacement (no new behavior to validate)
- Field visibility change (transparent to callers using getters/setters)

There are no pure functions, parsers, serializers, or algorithmic logic introduced or modified.

**Verification approach:**

| Step | Command | Validates |
|------|---------|-----------|
| 1 | `./mvnw compile` | All modules compile after changes |
| 2 | `./mvnw test -pl mekano-domain` | Domain tests pass (Placa test removed, no other impact) |
| 3 | `./mvnw test -pl mekano-infrastructure -am` | Infrastructure tests pass with entity annotation changes |
| 4 | `./mvnw verify -pl mekano-rest -am` | Full integration tests pass end-to-end |

**Static verification (grep):**
- Confirm zero remaining imports of deleted classes across all `.java` files
- Confirm no `@Data` annotation on the three modified entities
- Confirm no `public` field declarations on the three modified entities

**Implementation order (recommended):**

1. **Change 3 first** (annotation swap) — This is the only change that modifies files rather than deleting them, so it carries the most risk of merge conflicts. Doing it first allows immediate compile verification.
2. **Changes 1, 2, 4 in any order** (deletions) — These are pure deletions of unreferenced code. They cannot cause compilation errors and are order-independent.

This order ensures that if any step fails compilation, the problematic change is immediately identifiable.
