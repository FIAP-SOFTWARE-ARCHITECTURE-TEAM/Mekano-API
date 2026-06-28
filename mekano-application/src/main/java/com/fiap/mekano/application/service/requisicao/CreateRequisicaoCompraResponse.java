package com.fiap.mekano.application.service.requisicao;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateRequisicaoCompraResponse(
    UUID id,
    UUID pecaId,
    Long quantidade,
    String status,
    String motivo,
    LocalDateTime createdAt
) {}
