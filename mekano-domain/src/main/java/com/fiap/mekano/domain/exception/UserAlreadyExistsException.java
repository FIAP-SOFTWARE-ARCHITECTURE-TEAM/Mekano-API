package com.fiap.mekano.domain.exception;

/**
 * Lançada quando a criação de um usuário é tentada com um email já cadastrado.
 * Status HTTP: {@code 409 Conflict}.
 */
public class UserAlreadyExistsException extends BusinessException {

    public UserAlreadyExistsException(String email) {
        super(409, "Usuário já existe com o email: " + email);
    }
}
