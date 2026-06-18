package com.fiap.mekano.rest.api.exception;

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
 * <p><b>DEPRECATED:</b> Substituído por {@link GenericExceptionMapper} (D-09).
 * Mantido para rollback rápido — sem {@code @Provider} para não ser descoberto.
 *
 * @deprecated desde 10-02-PLAN. Usar {@link GenericExceptionMapper}.
 */
@Deprecated
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
