package com.fiap.mekano.domain.exception;

/**
 * Base para exceções de regras de negócio (unchecked).
 *
 * <p>Representa violações de regras de negócio recuperáveis — email duplicado,
 * recurso não encontrado, conflito de estado. O status HTTP é definido por cada
 * subclasse no construtor.
 *
 * <p>Esta classe NÃO deve importar nenhuma classe de framework.
 *
 * @see AppException
 */
public abstract class BusinessException extends AppException {

    protected BusinessException(int status, String message) {
        super(status, message);
    }

    protected BusinessException(int status, String message, Throwable cause) {
        super(status, message, cause);
    }
}
