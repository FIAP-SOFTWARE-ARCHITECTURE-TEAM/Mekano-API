package com.fiap.mekano.domain.event;

import com.fiap.mekano.domain.os.StatusPagamento;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record CobrancaGeradaEvent(
        UUID osUuid,
        StatusPagamento statusPagamento,
        LocalDateTime occurredAt
) {

    public CobrancaGeradaEvent {
        Objects.requireNonNull(osUuid, "osUuid não pode ser nulo");
        Objects.requireNonNull(statusPagamento, "statusPagamento não pode ser nulo");
        occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }

    public static CobrancaGeradaEvent of(UUID osUuid) {
        return new CobrancaGeradaEvent(
                osUuid,
                StatusPagamento.AGUARDANDO_PAGAMENTO,
                LocalDateTime.now()
        );
    }
}