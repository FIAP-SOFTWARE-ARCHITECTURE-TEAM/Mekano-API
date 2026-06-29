package com.fiap.mekano.domain.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record CobrancaEmitidaEvent(
        UUID osUuid,
        UUID cobrancaId,
        BigDecimal valor,
        LocalDateTime dataEmissao
) {
    public static CobrancaEmitidaEvent of(UUID osUuid, BigDecimal valor) {
        return new CobrancaEmitidaEvent(osUuid, UUID.randomUUID(), valor, LocalDateTime.now());
    }
}