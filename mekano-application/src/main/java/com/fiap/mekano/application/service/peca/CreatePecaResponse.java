package com.fiap.mekano.application.service.peca;

import java.util.UUID;

public record CreatePecaResponse(
    UUID id,
    String descricao,
    Integer saldo,
    Integer estoqueMinimo
) {}
