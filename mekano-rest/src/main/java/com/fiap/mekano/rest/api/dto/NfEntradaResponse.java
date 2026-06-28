package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dados da nota fiscal de entrada")
public record NfEntradaResponse(
                @Schema(description = "Identificador único da NF", examples = "550e8400-e29b-41d4-a716-446655440000") UUID id,
                @Schema(description = "Chave de acesso NFe (44 dígitos)", examples = "35200612345678000190550000001234567890123456") String chaveAcesso,
                @Schema(description = "Valor total", examples = "1875.00") BigDecimal valorTotal,
                @Schema(description = "UUID da peça", examples = "550e8400-e29b-41d4-a716-446655440000") UUID pecaId,
                @Schema(description = "UUID da requisição de compra", examples = "550e8400-e29b-41d4-a716-446655440000") UUID requisicaoCompraId,
                @Schema(description = "Data e hora de criação (ISO-8601)", examples = "2026-05-29T14:30:00") LocalDateTime createdAt) {
}
