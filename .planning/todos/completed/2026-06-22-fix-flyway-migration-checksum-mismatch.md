---
created: 2026-06-22 12:37
title: Fix Flyway migration checksum mismatch
area: database
files:
  - mekano-infrastructure/src/main/resources/db/migration/V3__add_soft_delete_to_users.sql
  - mekano-infrastructure/src/main/resources/db/migration/V4__add_audit_columns_to_users.sql
  - mekano-infrastructure/src/main/resources/db/migration/V5__add_sequential_id.sql
---

## Problem

Flyway validation fails on startup with checksum mismatch for migrations V3, V4, V5:

- V3: applied DB=1935591631, local=624857703
- V4: applied DB=410362889, local=-1965227799
- V5: applied DB=-304057180, local=-1816389007

Stack trace: `FlywayValidateException: Validate failed: Migrations have failed validation`

This occurs because the migration files were modified after Flyway already applied them to the database, causing checksums to diverge.

## Solution

Run `./mvnw quarkus:dev -Dquarkus.flyway.repair=true` to update the schema history table with current checksums. Or, if using dev database only, drop and recreate:
1. `docker-compose down -v` (removes volumes)
2. `docker-compose up -d`
3. `./mvnw quarkus:dev`
