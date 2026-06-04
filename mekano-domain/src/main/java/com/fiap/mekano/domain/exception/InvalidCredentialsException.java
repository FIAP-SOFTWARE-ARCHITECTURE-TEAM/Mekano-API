package com.fiap.mekano.domain.exception;

/**
 * Lançada pelo caso de uso de autenticação quando email não existe OU
 * a senha não confere com o hash armazenado.
 *
 * <p>Importante (T-08-07 / Information Disclosure): a exceção <b>não
 * diferencia</b> "email inexistente" de "senha incorreta" para evitar
 * user enumeration. Os dois cenários colapsam em uma única mensagem
 * genérica ({@code "Invalid credentials"}) traduzida para HTTP 401
 * pelo {@code InvalidCredentialsExceptionMapper} no módulo adapter.
 */
public class InvalidCredentialsException extends BusinessException {

    public InvalidCredentialsException() {
        super("Invalid credentials");
    }
}
