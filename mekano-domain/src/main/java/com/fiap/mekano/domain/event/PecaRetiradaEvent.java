package com.fiap.mekano.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record PecaRetiradaEvent(
    UUID pecaId,
    Long quantidade,
    String motivo,
    LocalDateTime occurredAt
) {
    public static PecaRetiradaEvent of(UUID pecaId, Long quantidade, String motivo) {
        return new PecaRetiradaEvent(pecaId, quantidade, motivo, LocalDateTime.now());
    }
}
