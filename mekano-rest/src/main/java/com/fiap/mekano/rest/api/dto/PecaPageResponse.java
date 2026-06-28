package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resposta paginada de peças")
public record PecaPageResponse(
                @Schema(description = "Lista de peças da página atual") List<PecaResponse> content,
                @Schema(description = "Número da página (0-based)", examples = "0") int page,
                @Schema(description = "Tamanho da página", examples = "10") int size,
                @Schema(description = "Total de peças ativas no sistema", examples = "25") long totalElements,
                @Schema(description = "Total de páginas", examples = "3") int totalPages) {
}
