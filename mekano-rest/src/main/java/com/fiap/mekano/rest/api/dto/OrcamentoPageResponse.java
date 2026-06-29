package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resposta paginada de orçamentos")
public record OrcamentoPageResponse(
        @Schema(description = "Lista de orçamentos da página atual") List<OrcamentoResponse> content,
        @Schema(description = "Número da página (0-based)", example = "0") int page,
        @Schema(description = "Tamanho da página", example = "10") int size,
        @Schema(description = "Total de orçamentos", example = "15") long totalElements,
        @Schema(description = "Total de páginas", example = "2") int totalPages
) {}