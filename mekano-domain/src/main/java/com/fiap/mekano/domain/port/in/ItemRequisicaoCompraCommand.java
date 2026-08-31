package com.fiap.mekano.domain.port.in;

import java.util.UUID;

public record ItemRequisicaoCompraCommand(
    UUID pecaId,
    Integer quantidade
) {}
