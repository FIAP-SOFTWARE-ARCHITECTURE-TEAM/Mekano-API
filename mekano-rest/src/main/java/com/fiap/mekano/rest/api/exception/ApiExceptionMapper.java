package com.fiap.mekano.rest.api.exception;

import com.fiap.mekano.domain.exception.AppException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * ExceptionMapper único que trata todas as exceções da aplicação (D-09).
 *
 * <p>Fluxo de tratamento:
 * <ol>
 *   <li>{@link AppException} — lê status HTTP direto de {@code getStatus()}.</li>
 *   <li>Fallback — exceção não mapeada; 500 com log de stacktrace.</li>
 * </ol>
 *
 * <p>Respostas no formato RFC 7807 (application/problem+json).
 *
 * @see AppException
 * @see ProblemDetail
 */
@Provider
@ApplicationScoped
public class ApiExceptionMapper implements ExceptionMapper<Exception> {

    private static final String PROBLEM_JSON = "application/problem+json";

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exception) {

        if (exception instanceof AppException ex) {
            return build(ex.getStatus(), ex.getMessage());
        }

        Log.errorf(exception, "Unhandled exception: %s", exception.getMessage());
        return build(500, "Erro interno do servidor");
    }

    private Response build(int status, String detail) {
        String instance = uriInfo != null ? uriInfo.getRequestUri().toString() : null;
        return Response.status(status)
                .entity(ProblemDetail.of(status, detail, instance))
                .type(PROBLEM_JSON)
                .build();
    }
}
