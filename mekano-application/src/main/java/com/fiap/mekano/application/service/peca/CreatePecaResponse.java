package com.fiap.mekano.application.service.peca;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CreatePecaResponse(
    UUID id,
    String codigo,
    String descricao,
    BigDecimal valorUnitario,
    Long saldoAtual,
    Long estoqueMinimo,
    LocalDateTime createdAt
) {}
