package com.fiap.mekano.rest.api.dto;

import com.fiap.mekano.domain.valueobject.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.StatusOrcamento;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RegisterForReflection
@Schema(description = "Dados do orçamento")
public record OrcamentoResponse(
        @Schema(description = "Identificador único do orçamento") UUID id,
        @Schema(description = "Descrição do orçamento") String descricao,
        @Schema(description = "Itens do orçamento") List<ItemOrcamento> itens,
        @Schema(description = "Valor total calculado") BigDecimal valorTotal,
        @Schema(description = "Status do orçamento") StatusOrcamento status,
        @Schema(description = "UUID da Ordem de Serviço vinculada") UUID ordemServicoUuid,
        @Schema(description = "Data de expiração do SLA (72h)") LocalDateTime dataExpiracao,
        @Schema(description = "Data de criação") LocalDateTime createdAt
) {
    public static OrcamentoResponse from(Orcamento o) {
        return new OrcamentoResponse(
                o.getId(), o.getDescricao(), o.getItens(), o.getValorTotal(),
                o.getStatus(), o.getOrdemServicoUuid(), o.getDataExpiracao(), o.getCreatedAt());
    }
}