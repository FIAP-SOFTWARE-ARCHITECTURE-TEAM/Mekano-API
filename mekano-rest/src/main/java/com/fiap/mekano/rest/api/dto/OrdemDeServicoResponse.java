package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Dados completos da Ordem de Serviço")
public record OrdemDeServicoResponse(
        @Schema(description = "UUID da OS") UUID id,
        @Schema(description = "UUID do cliente") UUID clienteId,
        @Schema(description = "UUID do veículo") UUID veiculoId,
        @Schema(description = "Descrição do problema") String descricaoProblema,
        @Schema(description = "Status atual") String status,
        @Schema(description = "Motivo do cancelamento (se aplicável)") String motivoCancelamento,
        @Schema(description = "UUID do orçamento gerado") UUID orcamentoUuid,
        @Schema(description = "Data de criação") LocalDateTime createdAt
) {}
