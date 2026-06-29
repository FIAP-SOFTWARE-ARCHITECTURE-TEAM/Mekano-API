package com.fiap.mekano.domain.event;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public record PagamentoConfirmadoEvent(
        UUID osUuid,
        String referenciaPagamento,
        LocalDateTime occurredAt
) {

    public PagamentoConfirmadoEvent {
        Objects.requireNonNull(osUuid, "osUuid não pode ser nulo");

        if (referenciaPagamento == null || referenciaPagamento.isBlank()) {
            throw new IllegalArgumentException("referenciaPagamento não pode ser nula ou vazia");
        }

        referenciaPagamento = referenciaPagamento.strip();
        occurredAt = occurredAt == null ? LocalDateTime.now() : occurredAt;
    }

    public static PagamentoConfirmadoEvent of(UUID osUuid, String referenciaPagamento) {
        return new PagamentoConfirmadoEvent(
                osUuid,
                referenciaPagamento,
                LocalDateTime.now()
        );
    }
}
