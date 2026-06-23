package com.fiap.mekano.domain.event;

import com.fiap.mekano.domain.model.Cliente;
import java.time.LocalDateTime;

public record ClienteCriadoEvent(
    Cliente cliente,
    LocalDateTime occurredAt
) {
    public static ClienteCriadoEvent of(Cliente cliente) {
        return new ClienteCriadoEvent(cliente, LocalDateTime.now());
    }
}
