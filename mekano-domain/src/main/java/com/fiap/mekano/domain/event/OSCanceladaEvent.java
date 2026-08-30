package com.fiap.mekano.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

public record OSCanceladaEvent(
        UUID osUuid,
        String motivo,
        LocalDateTime occurredAt
) {
    public static OSCanceladaEvent of(UUID osUuid, String motivo) {
        return new OSCanceladaEvent(osUuid, motivo, LocalDateTime.now());
    }
}