package com.fiap.mekano.adapter.in.rest.dto;

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
        @Schema(description = "Identificador único do usuário") UUID id,
        @Schema(description = "Nome do usuário") String name,
        @Schema(description = "Email do usuário") String email,
        @Schema(description = "Data e hora de criação") LocalDateTime createdAt
) {}
