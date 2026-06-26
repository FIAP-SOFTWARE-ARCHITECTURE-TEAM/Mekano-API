package com.fiap.mekano.rest.api.dto;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Resposta paginada de veículos")
public record VeiculoPageResponse(
        List<VeiculoResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {
}
