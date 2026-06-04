package com.fiap.mekano.domain.exception;

import java.util.UUID;

/**
 * Lançada quando um usuário não é encontrado pelo identificador fornecido (id ou email).
 * Usada pelos casos de uso que fazem lookup de usuário.
 */
public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(String identifier) {
        super("Usuário não encontrado: " + identifier);
    }

    /**
     * Construtor sobrecarregado para UUID de usuário.
     *
     * @param id UUID do usuário não encontrado
     */
    public UserNotFoundException(UUID id) {
        super("Usuário não encontrado: " + id);
    }
}
