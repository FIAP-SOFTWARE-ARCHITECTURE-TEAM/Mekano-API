package com.fiap.mekano.domain.exception;

/**
 * Lançada quando o rate limit de uma requisição é excedido.
 *
 * <p>Traduzida pelo {@code RateLimitExceededExceptionMapper} na camada adapter
 * para HTTP 429 Too Many Requests com header {@code Retry-After}.
 *
 * <p>O valor {@code retryAfterSeconds} indica por quantos segundos o cliente
 * deve aguardar antes de tentar novamente.
 */
public class RateLimitExceededException extends BusinessException {

    private final long retryAfterSeconds;

    public RateLimitExceededException(long retryAfterSeconds) {
        super("Too many requests. Try again in " + retryAfterSeconds + " seconds.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    /**
     * Retorna o número de segundos que o cliente deve aguardar antes de
     * tentar novamente. Usado pelo ExceptionMapper para preencher o header
     * HTTP {@code Retry-After}.
     */
    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
