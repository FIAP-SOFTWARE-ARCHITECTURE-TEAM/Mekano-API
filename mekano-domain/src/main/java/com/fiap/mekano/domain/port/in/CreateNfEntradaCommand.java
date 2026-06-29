package com.fiap.mekano.domain.port.in;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateNfEntradaCommand(
    String chaveAcesso,
    BigDecimal valorTotal,
    UUID requisicaoCompraId
) {}
