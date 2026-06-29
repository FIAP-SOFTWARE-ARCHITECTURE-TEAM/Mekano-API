package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resposta paginada de notas fiscais de entrada")
public record NfEntradaPageResponse(
                @Schema(description = "Lista de NF's da página atual") List<NfEntradaResponse> content,
                @Schema(description = "Número da página (0-based)", examples = "0") int page,
                @Schema(description = "Tamanho da página", examples = "10") int size,
                @Schema(description = "Total de NF's no sistema", examples = "5") long totalElements,
                @Schema(description = "Total de páginas", examples = "1") int totalPages) {
}
