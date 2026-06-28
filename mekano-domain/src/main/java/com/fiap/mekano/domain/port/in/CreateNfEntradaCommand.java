package com.fiap.mekano.domain.port.in;

import java.util.UUID;

public record CreateNfEntradaCommand(
    UUID pecaId,
    UUID requisicaoCompraId,
    Integer quantidade
) {}
