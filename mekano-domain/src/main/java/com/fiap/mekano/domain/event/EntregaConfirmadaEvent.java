package com.fiap.mekano.domain.event;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record EntregaConfirmadaEvent(
        UUID osUuid,
        String recebidoPor,
        LocalDateTime occurredAt
) {

    public EntregaConfirmadaEvent {
        Objects.requireNonNull(osUuid, "osUuid não pode ser nulo");

        if (recebidoPor == null || recebidoPor.isBlank()) {
            throw new IllegalArgumentException("recebidoPor não pode ser nulo ou vazio");
        }

        recebidoPor = recebidoPor.strip();
        occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }

    public static EntregaConfirmadaEvent of(UUID osUuid, String recebidoPor) {
        return new EntregaConfirmadaEvent(
                osUuid,
                recebidoPor,
                LocalDateTime.now()
        );
    }
}