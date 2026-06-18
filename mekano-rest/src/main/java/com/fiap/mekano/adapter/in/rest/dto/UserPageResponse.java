package com.fiap.mekano.adapter.in.rest.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * DTO de resposta paginada para listagem de usuários.
 *
 * <p>Implementado como Java record para imutabilidade.
 * Contém a lista de usuários da página atual e metadados de paginação.
 *
 * @param content     lista de usuários da página atual
 * @param page        número da página (0-based)
 * @param size        tamanho da página
 * @param totalElements total de usuários ativos no sistema
 * @param totalPages  total de páginas
 */
@Schema(description = "Resposta paginada de usuários")
public record UserPageResponse(
        @Schema(description = "Lista de usuários da página atual") List<UserResponse> content,
        @Schema(description = "Número da página (0-based)", example = "0") int page,
        @Schema(description = "Tamanho da página", example = "10") int size,
        @Schema(description = "Total de usuários ativos no sistema", example = "42") long totalElements,
        @Schema(description = "Total de páginas", example = "5") int totalPages
) {}
