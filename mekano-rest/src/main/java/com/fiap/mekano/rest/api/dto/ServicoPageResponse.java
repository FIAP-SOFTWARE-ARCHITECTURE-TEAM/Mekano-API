package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * DTO de resposta paginada para listagem de serviços.
 */
@Schema(description = "Resposta paginada de serviços")
public record ServicoPageResponse(
        @Schema(description = "Lista de serviços da página atual") List<ServicoResponse> content,
        @Schema(description = "Número da página (0-based)", example = "0") int page,
        @Schema(description = "Tamanho da página", example = "10") int size,
        @Schema(description = "Total de serviços ativos no sistema", example = "15") long totalElements,
        @Schema(description = "Total de páginas", example = "2") int totalPages
) {}
