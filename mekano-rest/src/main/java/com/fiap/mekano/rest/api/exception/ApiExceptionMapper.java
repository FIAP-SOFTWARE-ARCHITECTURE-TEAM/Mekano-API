package com.fiap.mekano.rest.api.exception;

import com.fiap.mekano.domain.exception.AppException;
import io.quarkus.logging.Log;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

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
        if (exception instanceof WebApplicationException ex) {
            int status = ex.getResponse() != null ? ex.getResponse().getStatus() : 500;
            String detail = ex.getMessage();
            if (detail == null || detail.isBlank()) {
                detail = ex.getResponse() != null && ex.getResponse().getStatusInfo() != null
                        ? ex.getResponse().getStatusInfo().getReasonPhrase()
                        : "Erro interno do servidor";
            }
            return build(status, detail);
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
