package com.fiap.mekano.rest.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Dados do veículo")
public record VeiculoResponse(
        UUID id,
        UUID clienteUuid,
        String placa,
        String marca,
        String modelo,
        Integer ano,
        LocalDateTime createdAt,
        @Schema(description = "Indica se o registro está ativo") Boolean isActive) {
}
