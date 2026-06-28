package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resposta paginada de requisições de compra")
public record RequisicaoCompraPageResponse(
                @Schema(description = "Lista de requisições da página atual") List<RequisicaoCompraResponse> content,
                @Schema(description = "Número da página (0-based)", examples = "0") int page,
                @Schema(description = "Tamanho da página", examples = "10") int size,
                @Schema(description = "Total de requisições no sistema", examples = "10") long totalElements,
                @Schema(description = "Total de páginas", examples = "1") int totalPages) {
}
