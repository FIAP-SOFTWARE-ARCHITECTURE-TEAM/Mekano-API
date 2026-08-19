package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dados da peça")
public record PecaResponse(
                @Schema(description = "Identificador único da peça", examples = "550e8400-e29b-41d4-a716-446655440000") UUID id,
                @Schema(description = "Código identificador", examples = "PEA-001") String codigo,
                @Schema(description = "Descrição da peça", examples = "Óleo do Motor 5W30") String descricao,
                @Schema(description = "Valor unitário em reais", examples = "45.90") BigDecimal valorUnitario,
                @Schema(description = "Saldo atual em estoque", examples = "50") Long saldoAtual,
                @Schema(description = "Estoque mínimo para alerta", examples = "10") Long estoqueMinimo,
                @Schema(description = "Data e hora de criação (ISO-8601)", examples = "2026-05-29T14:30:00") LocalDateTime createdAt,
                @Schema(description = "Indica se o registro está ativo") Boolean isActive) {
}
