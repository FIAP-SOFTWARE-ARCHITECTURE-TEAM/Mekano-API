package com.fiap.mekano.domain.event;

import com.fiap.mekano.domain.model.User;
import java.time.LocalDateTime;

/**
 * Evento de domínio disparado quando um novo usuário é criado.
 */
public record UserCreatedEvent(
    User user,
    LocalDateTime occurredAt
) {
    public static UserCreatedEvent of(User user) {
        return new UserCreatedEvent(user, LocalDateTime.now());
    }
}
