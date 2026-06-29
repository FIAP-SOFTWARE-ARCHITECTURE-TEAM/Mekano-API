package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Dados resumidos da peça")
public record PecaResumidaResponse(
    @Schema(description = "UUID da peça", examples = "550e8400-e29b-41d4-a716-446655440000") UUID pecaId,
    @Schema(description = "Código da peça", examples = "PEA-001") String codigo,
    @Schema(description = "Descrição da peça", examples = "Óleo do Motor 5W30") String descricao
) {}
