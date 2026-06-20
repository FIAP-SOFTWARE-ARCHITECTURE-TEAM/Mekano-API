-- V1__create_users_table.sql
-- Migration: criação da tabela users
-- Corresponde ao mapeamento JPA de UserEntity (INF-01, INF-04)

CREATE TABLE users
(
    id            UUID         NOT NULL,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMP    NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_email UNIQUE (email)
);
