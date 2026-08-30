package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Resposta de item de Ordem de Serviço")
public record ItemOsResponse(
        @Schema(description = "UUID do item") UUID id,
        @Schema(description = "UUID da peça ou serviço") UUID referenciaUuid,
        @Schema(description = "Tipo: PECA ou SERVICO") String tipo,
        @Schema(description = "Descrição") String descricao,
        @Schema(description = "Quantidade") Long quantidade
) {}
