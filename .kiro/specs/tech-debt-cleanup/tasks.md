# Implementation Plan: Tech Debt Cleanup

## Overview

This plan addresses four independent tech debt items: replacing `@Data` with `@Getter`/`@Setter` on JPA entities, removing empty mapper classes, removing a duplicate enum, and removing a duplicate value object. The annotation change is done first (highest risk), followed by pure deletions, then full verification.

## Tasks

- [x] 1. Convert JPA entity annotations from @Data to @Getter/@Setter
  - [x] 1.1 Modify PecaEntity.java: replace @Data with @Getter/@Setter, change public fields to private
    - Replace `import lombok.Data;` with `import lombok.Getter;` and `import lombok.Setter;`
    - Replace `@Data` annotation with `@Getter` and `@Setter`
    - Change all `public` field declarations to `private`
    - Keep `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` unchanged
    - _Requirements: 3.1, 3.4, 3.5, 3.6_

  - [x] 1.2 Modify RequisicaoCompraEntity.java: replace @Data with @Getter/@Setter, change public fields to private
    - Replace `import lombok.Data;` with `import lombok.Getter;` and `import lombok.Setter;`
    - Replace `@Data` annotation with `@Getter` and `@Setter`
    - Change all `public` field declarations to `private`
    - Keep `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` unchanged
    - _Requirements: 3.2, 3.4, 3.5, 3.6_

  - [x] 1.3 Modify NfEntradaEntity.java: replace @Data with @Getter/@Setter, change public fields to private
    - Replace `import lombok.Data;` with `import lombok.Getter;` and `import lombok.Setter;`
    - Replace `@Data` annotation with `@Getter` and `@Setter`
    - Change all `public` field declarations to `private`
    - Keep `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` unchanged
    - _Requirements: 3.3, 3.4, 3.5, 3.6_

  - [x] 1.4 Verify compilation after annotation changes
    - Run `./mvnw compile` and confirm zero errors
    - _Requirements: 3.7_

- [x] 2. Remove empty mapper classes
  - [x] 2.1 Delete PecaEntityMapper.java, RequisicaoCompraEntityMapper.java, and NfEntradaEntityMapper.java
    - Delete `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/PecaEntityMapper.java`
    - Delete `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/RequisicaoCompraEntityMapper.java`
    - Delete `mekano-infrastructure/src/main/java/com/fiap/mekano/infrastructure/mapper/NfEntradaEntityMapper.java`
    - Grep to confirm zero remaining imports or references to these classes
    - _Requirements: 1.1, 1.2, 1.3, 1.4_

- [x] 3. Remove duplicate StatusPagamento enum
  - [x] 3.1 Delete mekano-domain/src/main/java/com/fiap/mekano/domain/model/StatusPagamento.java
    - Delete the file at `mekano-domain/src/main/java/com/fiap/mekano/domain/model/StatusPagamento.java`
    - Grep to confirm zero remaining imports of `com.fiap.mekano.domain.model.StatusPagamento`
    - Confirm canonical enum at `com.fiap.mekano.domain.os.StatusPagamento` is untouched
    - _Requirements: 2.1, 2.2, 2.3_

- [x] 4. Remove duplicate Placa value object
  - [x] 4.1 Delete Placa.java and PlacaTest.java
    - Delete `mekano-domain/src/main/java/com/fiap/mekano/domain/valueobject/Placa.java`
    - Delete `mekano-domain/src/test/java/com/fiap/mekano/domain/valueobject/PlacaTest.java`
    - Grep to confirm zero remaining imports of `com.fiap.mekano.domain.valueobject.Placa`
    - Confirm `PlacaVeiculo.java` is untouched
    - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 5. Checkpoint - Compile and run tests after all deletions
  - Ensure all tests pass, ask the user if questions arise.
  - Run `./mvnw compile` — confirm zero errors
  - Run `./mvnw test -pl mekano-domain` — confirm domain tests pass
  - _Requirements: 1.5, 1.6, 2.4, 2.5, 4.5, 4.6, 5.1, 5.2_

- [x] 6. Full integration verification
  - [x] 6.1 Run infrastructure and REST module tests
    - Run `./mvnw test -pl mekano-infrastructure -am` — confirm zero failures
    - Run `./mvnw verify -pl mekano-rest -am` — confirm full integration suite passes
    - Confirm total test count is >= baseline
    - _Requirements: 3.8, 5.3, 5.4, 5.5_

## Notes

- No property-based tests are needed — changes are deletions and annotation corrections with no new logic
- All four changes are independent at the source level; the annotation swap is done first because it modifies files (higher merge-conflict risk)
- Changes 2, 3, and 4 are pure file deletions of confirmed dead code (zero references verified by grep)
- Hibernate uses reflection-based field access via `@Column`, so `private` fields work identically to `public`
- `@Builder` works with `@Getter`/`@Setter` — it relies on `@AllArgsConstructor`, not `@Data`

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2", "1.3"] },
    { "id": 1, "tasks": ["1.4"] },
    { "id": 2, "tasks": ["2.1", "3.1", "4.1"] },
    { "id": 3, "tasks": ["6.1"] }
  ]
}
```
