package com.fiap.mekano.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record OrcamentoRecusadoEvent(
    UUID orcamentoId,
    String motivo,
    LocalDateTime occurredAt
) {
    public static OrcamentoRecusadoEvent of(UUID orcamentoId, String motivo) {
        return new OrcamentoRecusadoEvent(orcamentoId, motivo, LocalDateTime.now());
    }
}
