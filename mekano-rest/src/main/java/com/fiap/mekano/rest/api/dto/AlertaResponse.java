package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Alerta de estoque mínimo")
public record AlertaResponse(
                @Schema(description = "UUID da peça", examples = "550e8400-e29b-41d4-a716-446655440000") UUID pecaId,
                @Schema(description = "Código da peça", examples = "PEA-001") String codigo,
                @Schema(description = "Descrição da peça", examples = "Óleo do Motor 5W30") String descricao,
                @Schema(description = "Saldo atual", examples = "3") Long saldoAtual,
                @Schema(description = "Estoque mínimo configurado", examples = "10") Long estoqueMinimo) {
}
