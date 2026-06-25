package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resposta paginada de clientes")
public record ClientePageResponse(
        @Schema(description = "Lista de clientes da pagina atual") List<ClienteResponse> content,
        @Schema(description = "Numero da pagina (0-based)") int page,
        @Schema(description = "Tamanho da pagina") int size,
        @Schema(description = "Total de clientes ativos") long totalElements,
        @Schema(description = "Total de paginas") int totalPages
) {}
