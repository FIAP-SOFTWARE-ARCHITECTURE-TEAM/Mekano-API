package com.fiap.mekano.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PagamentoConfirmadoEvent(
        UUID osUuid,
        UUID transacaoId,
        BigDecimal valor,
        LocalDateTime dataConfirmacao
) {
    public static PagamentoConfirmadoEvent of(UUID osUuid, UUID transacaoId, BigDecimal valor) {
        return new PagamentoConfirmadoEvent(osUuid, transacaoId, valor, LocalDateTime.now());
    }

    public static PagamentoConfirmadoEvent of(UUID osUuid, String referenciaPagamento) {
        return new PagamentoConfirmadoEvent(osUuid, UUID.nameUUIDFromBytes(referenciaPagamento.getBytes()),
                BigDecimal.ZERO, LocalDateTime.now());
    }
}