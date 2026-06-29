package com.fiap.mekano.application.service.nfentrada;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreateNfEntradaResponse(
    UUID id,
    String chaveAcesso,
    BigDecimal valorTotal,
    UUID pecaId,
    UUID requisicaoCompraId,
    LocalDateTime createdAt
) {}
