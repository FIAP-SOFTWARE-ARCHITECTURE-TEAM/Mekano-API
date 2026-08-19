package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO de saída para dados do serviço.
 *
 * <p>Java record para imutabilidade.
 */
@Schema(description = "Dados do serviço")
public record ServicoResponse(
        @Schema(description = "Identificador único do serviço", example = "550e8400-e29b-41d4-a716-446655440000") UUID id,
        @Schema(description = "Nome do serviço", example = "Troca de óleo") String nome,
        @Schema(description = "Descrição do serviço", example = "Troca de óleo do motor com filtro incluso") String descricao,
        @Schema(description = "Valor do serviço em reais", example = "89.90") BigDecimal valor,
        @Schema(description = "Data e hora de criação (ISO-8601)", example = "2026-05-29T14:30:00") LocalDateTime createdAt,
        @Schema(description = "Indica se o registro está ativo") Boolean isActive
) {}
