package com.fiap.mekano.domain.exception;

/**
 * Base para exceções de validação de domínio (unchecked).
 *
 * <p>Representa erros dos quais o caller não pode se recuperar — dados inválidos,
 * formato incorreto, regras estruturais violadas. Status HTTP padrão: {@code 400 Bad Request}.
 *
 * <p>Esta classe NÃO deve importar nenhuma classe de framework.
 *
 * @see AppException
 */
public abstract class DomainException extends AppException {

    protected DomainException(String message) {
        super(400, message);
    }

    protected DomainException(String message, Throwable cause) {
        super(400, message, cause);
    }
}
