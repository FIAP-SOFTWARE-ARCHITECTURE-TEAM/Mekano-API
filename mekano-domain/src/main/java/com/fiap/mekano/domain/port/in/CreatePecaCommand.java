package com.fiap.mekano.domain.port.in;

import java.math.BigDecimal;

public record CreatePecaCommand(
    String codigo,
    String descricao,
    BigDecimal valorUnitario,
    Long estoqueMinimo
) {}
