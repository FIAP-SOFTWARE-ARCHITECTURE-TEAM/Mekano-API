package com.fiap.mekano.domain.exception;

/**
 * Classe base para todas as exceções de domínio do Mekano.
 *
 * Regras:
 * - Esta classe NÃO deve importar nenhuma classe de framework (jakarta, quarkus, hibernate).
 * - A tradução para códigos HTTP é responsabilidade do ExceptionMapper no módulo adapter.
 * - Todas as exceções de regra de negócio devem estender esta classe.
 */
public abstract class DomainException extends RuntimeException {

    protected DomainException(String message) {
        super(message);
    }

    protected DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}
