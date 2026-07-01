package com.fiap.mekano.domain.event;

import com.fiap.mekano.domain.os.OsAuditAction;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record OsTransitionedEvent(
        UUID osUuid,
        OsAuditAction acao,
        String usuarioEmail,
        String observacao,
        Map<String, Object> metadata
) {
    public OsTransitionedEvent {
        Objects.requireNonNull(osUuid, "osUuid não pode ser nulo");
        Objects.requireNonNull(acao, "acao não pode ser nula");
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}