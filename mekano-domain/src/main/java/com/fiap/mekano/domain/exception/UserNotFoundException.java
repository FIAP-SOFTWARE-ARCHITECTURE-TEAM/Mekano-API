package com.fiap.mekano.domain.exception;

import java.util.UUID;

/**
 * Lançada quando um usuário não é encontrado pelo identificador fornecido (id ou email).
 * Status HTTP: {@code 404 Not Found}.
 */
public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(String identifier) {
        super(404, "Usuário não encontrado: " + identifier);
    }

    /**
     * Construtor sobrecarregado para UUID de usuário.
     *
     * @param id UUID do usuário não encontrado
     */
    public UserNotFoundException(UUID id) {
        super(404, "Usuário não encontrado: " + id);
    }
}
