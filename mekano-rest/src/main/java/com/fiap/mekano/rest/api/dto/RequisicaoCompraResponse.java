package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Dados da requisição de compra")
public record RequisicaoCompraResponse(
                @Schema(description = "Identificador único da requisição", examples = "550e8400-e29b-41d4-a716-446655440000") UUID id,
                @Schema(description = "Lista de itens da requisição") List<ItemRequisicaoCompraResponse> itens,
                @Schema(description = "Status da requisição", examples = "ABERTA") String status,
                @Schema(description = "Motivo da requisição", examples = "ESTOQUE_MINIMO") String motivo,
                @Schema(description = "Data e hora de criação (ISO-8601)", examples = "2026-05-29T14:30:00") LocalDateTime createdAt) {
}
