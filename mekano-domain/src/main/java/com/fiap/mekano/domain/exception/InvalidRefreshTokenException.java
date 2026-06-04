package com.fiap.mekano.domain.exception;

/**
 * Lançada quando um refresh token é inválido, expirado ou já foi rotacionado.
 *
 * <p>Traduzida para HTTP 401 pelo {@code InvalidRefreshTokenExceptionMapper}
 * no módulo adapter (mesmo Pattern A de {@link InvalidCredentialsException}).
 */
public class InvalidRefreshTokenException extends DomainException {

    public InvalidRefreshTokenException() {
        super("Invalid or expired refresh token");
    }

    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
