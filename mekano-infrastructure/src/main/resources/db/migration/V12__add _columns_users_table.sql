-- V1__create_users_table.sql
-- Migration: criação da tabela users
-- Corresponde ao mapeamento JPA de UserEntity (INF-01, INF-04)

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;