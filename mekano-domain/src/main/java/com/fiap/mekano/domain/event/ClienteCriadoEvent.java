package com.fiap.mekano.domain.event;

import com.fiap.mekano.domain.model.Cliente;
import java.time.LocalDateTime;

/**
 * Evento de domínio disparado quando um novo cliente é criado.
 * Consumido por listeners na camada infrastructure para efeitos colaterais
 * (auditoria, notificações, etc.).
 */
public record ClienteCriadoEvent(
    Cliente cliente,
    LocalDateTime occurredAt
) {
    public static ClienteCriadoEvent of(Cliente cliente) {
        return new ClienteCriadoEvent(cliente, LocalDateTime.now());
    }
}
