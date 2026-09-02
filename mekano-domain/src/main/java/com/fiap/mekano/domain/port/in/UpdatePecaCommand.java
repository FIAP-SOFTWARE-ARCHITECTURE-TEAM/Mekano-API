package com.fiap.mekano.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdatePecaCommand(
    UUID id,
    String codigo,
    String descricao,
    BigDecimal valorUnitario,
    Long estoqueMinimo
) {}