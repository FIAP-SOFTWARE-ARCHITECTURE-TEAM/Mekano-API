package com.fiap.mekano.domain.exception;

/**
 * Exceção checked para violações de regras de negócio onde o caller
 * pode se recuperar (D-05).
 *
 * <p>Diferente de {@link DomainException} (unchecked, para erros de
 * validação dos quais o caller não pode se recuperar), esta exceção
 * força o tratamento explícito nos contratos:
 *
 * <ul>
 *   <li>{@link UserNotFoundException} — recurso não encontrado</li>
 *   <li>{@link UserAlreadyExistsException} — conflito de email</li>
 * </ul>
 *
 * <p>O caller (adapter/use case) é obrigado a declarar {@code throws}
 * ou capturar a exceção, garantindo que cenários de recuperação sejam
 * considerados em tempo de compilação.
 *
 * <p>Esta classe NÃO deve importar nenhuma classe de framework.
 */
public abstract class BusinessException extends Exception {

    protected BusinessException(String message) {
        super(message);
    }

    protected BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}
