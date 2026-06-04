package com.fiap.mekano.adapter.in.rest.exception;

import com.fiap.mekano.domain.exception.RateLimitExceededException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduz {@link RateLimitExceededException} (domínio) para HTTP 429 Too Many Requests
 * com header {@code Retry-After}.
 *
 * <p>O header {@code Retry-After} informa ao cliente por quantos segundos deve
 * aguardar antes de tentar novamente, permitindo que implemente backoff
 * respeitando a política de rate limit do servidor.
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6585#section-4">RFC 6585 §4</a>
 */
@Provider
@ApplicationScoped
public class RateLimitExceededExceptionMapper implements ExceptionMapper<RateLimitExceededException> {

    @Override
    public Response toResponse(RateLimitExceededException exception) {
        return Response.status(429)
                .header("Retry-After", String.valueOf(exception.getRetryAfterSeconds()))
                .entity(new ErrorResponse(exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
