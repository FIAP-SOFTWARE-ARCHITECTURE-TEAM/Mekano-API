# Requirements Document

## Introduction

This feature addresses four tech debt issues identified in the Mekano API multi-module project (Quarkus/Java). The cleanup targets dead code removal (empty mappers, unused enum, unused value object) and annotation corrections on JPA entities to align with project conventions and avoid known pitfalls with Lombok's `@Data` on entities.

## Glossary

- **Mekano_System**: The Mekano API application composed of four Maven modules (mekano-domain, mekano-infrastructure, mekano-application, mekano-rest).
- **Empty_Mapper**: A mapper class annotated with `@ApplicationScoped` that contains zero methods and is not referenced by any production or test code.
- **Dead_Code**: Source code that is unreachable or unused by any other part of the system.
- **JPA_Entity**: A Java class annotated with `@Entity` that maps to a database table via Jakarta Persistence.
- **Value_Object**: An immutable domain object identified by its value rather than an identity field.
- **Canonical_Enum**: The single authoritative version of an enumeration used across the codebase.

## Requirements

### Requirement 1: Remove Empty Mapper Classes

**User Story:** As a developer, I want unused empty mapper classes removed from the codebase, so that the project contains no dead code that misleads future maintainers.

#### Acceptance Criteria

1. WHEN the Mekano_System source tree is inspected, THE Mekano_System SHALL NOT contain the file `PecaEntityMapper.java` in the package `com.fiap.mekano.infrastructure.mapper`.
2. WHEN the Mekano_System source tree is inspected, THE Mekano_System SHALL NOT contain the file `RequisicaoCompraEntityMapper.java` in the package `com.fiap.mekano.infrastructure.mapper`.
3. WHEN the Mekano_System source tree is inspected, THE Mekano_System SHALL NOT contain the file `NfEntradaEntityMapper.java` in the package `com.fiap.mekano.infrastructure.mapper`.
4. WHEN the empty mapper files are removed, THE Mekano_System SHALL contain no remaining compile-time references (imports, field declarations, or type usages) to `PecaEntityMapper`, `RequisicaoCompraEntityMapper`, or `NfEntradaEntityMapper` in any module.
5. WHEN the empty mapper files are removed, THE Mekano_System SHALL compile successfully with zero errors when the project build command is executed across all modules.
6. WHEN the empty mapper files are removed, THE Mekano_System SHALL pass all existing automated tests with zero failures and zero errors.

### Requirement 2: Remove Duplicate StatusPagamento Enum

**User Story:** As a developer, I want only one authoritative StatusPagamento enum in the project, so that there is no ambiguity about which enum to import and no risk of using the wrong one.

#### Acceptance Criteria

1. WHEN the Mekano_System source tree is inspected, THE Mekano_System SHALL NOT contain the file `mekano-domain/src/main/java/com/fiap/mekano/domain/model/StatusPagamento.java`.
2. THE Mekano_System SHALL retain the Canonical_Enum at `com/fiap/mekano/domain/os/StatusPagamento.java` with exactly four values: NAO_COBRADO, AGUARDANDO_PAGAMENTO, CONFIRMADO, and CANCELADO.
3. WHEN the duplicate enum is removed, THE Mekano_System SHALL contain zero import statements referencing `com.fiap.mekano.domain.model.StatusPagamento` across all source and test files.
4. WHEN the duplicate enum is removed, THE Mekano_System SHALL compile successfully with zero errors across all modules.
5. WHEN the duplicate enum is removed, THE Mekano_System SHALL pass all existing automated tests with zero failures and zero errors.

### Requirement 3: Convert Entity Annotations from @Data to @Getter/@Setter

**User Story:** As a developer, I want JPA entities to use `@Getter`/`@Setter` instead of `@Data`, so that entities avoid Lombok-generated `equals`/`hashCode`/`toString` methods that cause issues with JPA lazy-loading and proxy objects.

#### Acceptance Criteria

1. WHEN `PecaEntity.java` is inspected, THE Mekano_System SHALL use `@Getter` and `@Setter` annotations instead of `@Data`, and SHALL NOT contain the `@Data` annotation or its import.
2. WHEN `RequisicaoCompraEntity.java` is inspected, THE Mekano_System SHALL use `@Getter` and `@Setter` annotations instead of `@Data`, and SHALL NOT contain the `@Data` annotation or its import.
3. WHEN `NfEntradaEntity.java` is inspected, THE Mekano_System SHALL use `@Getter` and `@Setter` annotations instead of `@Data`, and SHALL NOT contain the `@Data` annotation or its import.
4. THE Mekano_System SHALL NOT contain `@EqualsAndHashCode` or `@ToString` annotations on `PecaEntity`, `RequisicaoCompraEntity`, or `NfEntradaEntity`.
5. THE Mekano_System SHALL declare all fields in `PecaEntity`, `RequisicaoCompraEntity`, and `NfEntradaEntity` with `private` access modifier instead of `public`.
6. THE Mekano_System SHALL retain the `@Builder`, `@NoArgsConstructor`, and `@AllArgsConstructor` annotations on all three entities.
7. WHEN the annotation changes are applied, THE Mekano_System SHALL compile successfully with zero errors across all modules.
8. WHEN the annotation changes are applied, THE Mekano_System SHALL pass all existing automated tests with zero failures and zero errors.

### Requirement 4: Remove Duplicate Placa Value Object

**User Story:** As a developer, I want a single Placa value object in the domain, so that there is no confusion about which validation logic is canonical and no dead code in the project.

#### Acceptance Criteria

1. WHEN the Mekano_System source tree is inspected, THE Mekano_System SHALL NOT contain the file `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/Placa.java`.
2. WHEN the Mekano_System source tree is inspected, THE Mekano_System SHALL NOT contain the file `mekano-domain/src/test/java/com/fiap/mekano/domain/valueobject/PlacaTest.java`.
3. THE Mekano_System SHALL retain `PlacaVeiculo.java` at `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/PlacaVeiculo.java` as the sole placa Value_Object in the `com.fiap.mekano.domain.valueobject` package.
4. WHEN the duplicate Placa file is removed, THE Mekano_System SHALL contain zero import statements or qualified references to `com.fiap.mekano.domain.valueobject.Placa` across all source and test files.
5. WHEN the duplicate Placa file is removed, THE Mekano_System SHALL compile successfully with zero errors across all modules.
6. WHEN the duplicate Placa file is removed, THE Mekano_System SHALL pass all existing automated tests with zero failures and zero errors.

### Requirement 5: Full Build Verification

**User Story:** As a developer, I want confidence that all four cleanup changes together do not break the system, so that the tech debt removal is safe to merge.

#### Acceptance Criteria

1. WHEN all cleanup changes from Requirements 1 through 4 are applied together on a single branch, THE Mekano_System SHALL pass `./mvnw compile` without errors.
2. WHEN all four cleanup changes are applied together, THE Mekano_System SHALL pass `./mvnw test -pl mekano-domain` without failures.
3. WHEN all four cleanup changes are applied together, THE Mekano_System SHALL pass `./mvnw test -pl mekano-infrastructure -am` without failures.
4. WHEN all four cleanup changes are applied together, THE Mekano_System SHALL pass `./mvnw verify -pl mekano-rest -am` without failures.
5. WHEN all four cleanup changes are applied together, THE Mekano_System SHALL report a total number of tests executed that is greater than or equal to the total number of tests executed on the baseline branch without the changes.
