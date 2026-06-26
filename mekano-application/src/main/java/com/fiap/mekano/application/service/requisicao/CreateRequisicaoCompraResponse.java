package com.fiap.mekano.application.service.requisicao;

import java.util.UUID;

public record CreateRequisicaoCompraResponse(
    UUID id,
    String status,
    Integer quantidade
) {}
