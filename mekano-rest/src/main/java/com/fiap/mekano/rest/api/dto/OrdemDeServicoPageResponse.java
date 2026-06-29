package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resposta paginada de ordens de serviço")
public record OrdemDeServicoPageResponse(
        @Schema(description = "Lista de OS da página atual") List<OrdemDeServicoResponse> content,
        @Schema(description = "Número da página (0-based)", example = "0") int page,
        @Schema(description = "Tamanho da página", example = "10") int size,
        @Schema(description = "Total de OS ativas", example = "15") long totalElements,
        @Schema(description = "Total de páginas", example = "2") int totalPages
) {}