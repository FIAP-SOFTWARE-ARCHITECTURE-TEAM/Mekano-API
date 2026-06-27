package com.fiap.mekano.domain.event;

import java.util.UUID;

public record EstoqueMinimoAtingidoEvent(
    UUID pecaId,
    Integer saldoAtual,
    Integer estoqueMinimo
) {}
