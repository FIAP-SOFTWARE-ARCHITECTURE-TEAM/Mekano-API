package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de saída para dados do usuário criado.
 *
 * Implementado como Java record para imutabilidade.
 * CRÍTICO: passwordHash é EXCLUÍDO intencionalmente — nunca expor hash de senha na API.
 *
 * MapStruct 1.6.x suporta records como TARGET usando o canonical constructor:
 * o mapper chama UserResponse(UUID, String, String, LocalDateTime) diretamente.
 *
 * Campos: id, name, email (String — não o VO Email), createdAt.
 */
@Schema(description = "Dados do usuário criado")
public record UserResponse(
        @Schema(description = "Identificador único do usuário", example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
        @Schema(description = "Nome do usuário", example = "Ana Lima") String name,
        @Schema(description = "Email do usuário", example = "ana@fiap.br") String email,
        @Schema(description = "Data e hora de criação (ISO-8601)", example = "2026-05-29T14:30:00") LocalDateTime createdAt
) {}
