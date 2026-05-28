package com.fiap.mekano.domain.exception;

/**
 * Lançada quando um usuário não é encontrado pelo identificador fornecido (id ou email).
 * Usada pelos casos de uso que fazem lookup de usuário.
 */
public class UserNotFoundException extends DomainException {

    public UserNotFoundException(String identifier) {
        super("Usuário não encontrado: " + identifier);
    }
}
