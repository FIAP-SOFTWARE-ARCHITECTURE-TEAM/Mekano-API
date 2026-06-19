-- V5__add_sequential_id.sql
-- Migration: adiciona PK sequencial (BIGSERIAL) às tabelas users e refresh_tokens
-- O UUID existente vira coluna uuid (unique) para exposição segura em APIs
-- A PK sequencial é uso interno do banco (joins, FK performance)

-- 0. refresh_tokens: dropar FK antes das PKs (dependência)
ALTER TABLE refresh_tokens DROP CONSTRAINT IF EXISTS fk_refresh_tokens_user;

-- 1. Dropar PKs antes de renomear (a constraint segue a coluna renomeada)
-- Nomes definidos em V1 (pk_users) e V2 (pk_refresh_tokens)
ALTER TABLE users DROP CONSTRAINT IF EXISTS pk_users;
ALTER TABLE refresh_tokens DROP CONSTRAINT IF EXISTS pk_refresh_tokens;

-- 2. users: renomear id → uuid
ALTER TABLE users RENAME COLUMN id TO uuid;

-- 3. refresh_tokens: renomear colunas
ALTER TABLE refresh_tokens RENAME COLUMN id TO uuid;
ALTER TABLE refresh_tokens RENAME COLUMN user_id TO user_uuid;

-- 4. users: adicionar nova PK sequencial
ALTER TABLE users ADD COLUMN id BIGSERIAL;
ALTER TABLE users ADD PRIMARY KEY (id);

-- 5. refresh_tokens: adicionar nova PK sequencial
ALTER TABLE refresh_tokens ADD COLUMN id BIGSERIAL;
ALTER TABLE refresh_tokens ADD PRIMARY KEY (id);

-- 6. uuid columns: NOT NULL + UNIQUE
ALTER TABLE users ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_uuid UNIQUE (uuid);

ALTER TABLE refresh_tokens ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE refresh_tokens ADD CONSTRAINT uq_refresh_tokens_uuid UNIQUE (uuid);

-- 7. Reconstruir FK: refresh_tokens.user_uuid → users.uuid
ALTER TABLE refresh_tokens ADD CONSTRAINT fk_refresh_tokens_user
    FOREIGN KEY (user_uuid) REFERENCES users(uuid);
