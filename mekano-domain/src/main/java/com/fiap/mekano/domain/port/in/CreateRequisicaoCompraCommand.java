package com.fiap.mekano.domain.port.in;

import java.util.UUID;

public record CreateRequisicaoCompraCommand(
    UUID pecaId,
    Integer quantidade
) {}
