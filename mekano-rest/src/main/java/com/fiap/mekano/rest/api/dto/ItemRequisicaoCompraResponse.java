package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Item de uma requisição de compra")
public record ItemRequisicaoCompraResponse(
    @Schema(description = "UUID da peça", examples = "550e8400-e29b-41d4-a716-446655440000") UUID pecaId,
    @Schema(description = "Dados resumidos da peça") PecaResumidaResponse peca,
    @Schema(description = "Quantidade solicitada", examples = "10") Long quantidade
) {}
