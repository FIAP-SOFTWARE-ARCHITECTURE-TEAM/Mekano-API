package com.fiap.mekano.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record OSFinalizadaEvent(
        UUID ordemServicoId,
        LocalDateTime occurredAt
) {
    public static OSFinalizadaEvent of(UUID ordemServicoId) {
        return new OSFinalizadaEvent(ordemServicoId, LocalDateTime.now());
    }
}
