package com.fiap.mekano.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record OSEntregueEvent(
        UUID osUuid,
        LocalDateTime dataEntrega,
        String observacao
) {
    public static OSEntregueEvent of(UUID osUuid, String observacao) {
        return new OSEntregueEvent(osUuid, LocalDateTime.now(), observacao);
    }
}