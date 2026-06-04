package com.fiap.mekano.domain.exception;

/**
 * Lançada quando a criação de um usuário é tentada com um email já cadastrado.
 * Usada pelo caso de uso CreateUser na camada application.
 */
public class UserAlreadyExistsException extends BusinessException {

    public UserAlreadyExistsException(String email) {
        super("Usuário já existe com o email: " + email);
    }
}
