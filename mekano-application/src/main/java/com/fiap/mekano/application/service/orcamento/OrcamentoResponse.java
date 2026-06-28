package com.fiap.mekano.application.service.orcamento;

import com.fiap.mekano.domain.model.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.StatusOrcamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrcamentoResponse(
        UUID id,
        String descricao,
        List<ItemOrcamento> itens,
        BigDecimal valorTotal,
        StatusOrcamento status,
        UUID ordemServicoUuid,
        LocalDateTime dataExpiracao,
        LocalDateTime createdAt
) {
    public static OrcamentoResponse from(Orcamento orcamento) {
        return new OrcamentoResponse(
                orcamento.getId(),
                orcamento.getDescricao(),
                orcamento.getItens(),
                orcamento.getValorTotal(),
                orcamento.getStatus(),
                orcamento.getOrdemServicoUuid(),
                orcamento.getDataExpiracao(),
                orcamento.getCreatedAt()
        );
    }
}
