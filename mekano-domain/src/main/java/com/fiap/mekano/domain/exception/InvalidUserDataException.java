package com.fiap.mekano.domain.exception;

/**
 * Lançada quando dados básicos do usuário são inválidos (ex: nome nulo ou vazio).
 * Usada pelo CreateUserUseCase (application) antes de chamar User.create().
 */
public class InvalidUserDataException extends DomainException {

    public InvalidUserDataException(String message) {
        super(message);
    }
}
