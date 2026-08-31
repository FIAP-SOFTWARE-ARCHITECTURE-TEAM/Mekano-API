package com.fiap.mekano.application.service.requisicao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record CreateRequisicaoCompraResponse(
    UUID id,
    List<ItemRequisicaoCompraItemResponse> itens,
    String status,
    String motivo,
    LocalDateTime createdAt
) {
    public record ItemRequisicaoCompraItemResponse(
        UUID pecaId,
        Long quantidade
    ) {}
}
