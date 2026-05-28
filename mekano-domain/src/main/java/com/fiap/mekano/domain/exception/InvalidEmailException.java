package com.fiap.mekano.domain.exception;

/**
 * Lançada quando um valor fornecido não satisfaz o formato de email esperado.
 * Usada pelo Value Object {@code Email} no construtor.
 */
public class InvalidEmailException extends DomainException {

    public InvalidEmailException(String value) {
        super("Formato de email inválido: " + value);
    }
}
