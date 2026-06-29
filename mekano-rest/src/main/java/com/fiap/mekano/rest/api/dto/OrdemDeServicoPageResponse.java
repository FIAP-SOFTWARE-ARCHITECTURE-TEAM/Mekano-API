package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resposta paginada de Ordens de Serviço")
public record OrdemDeServicoPageResponse(
        @Schema(description = "Lista de OS da página atual") List<OrdemDeServicoResponse> content,
        @Schema(description = "Número da página") int page,
        @Schema(description = "Tamanho da página") int size,
        @Schema(description = "Total de OS") long totalElements,
        @Schema(description = "Total de páginas") int totalPages
) {}
