package com.fiap.mekano.rest.api.exception;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.stream.Collectors;

/**
 * Traduz ConstraintViolationException (Bean Validation) para HTTP 400 Bad Request.
 *
 * <p><b>DEPRECATED:</b> Substituído por {@link GenericExceptionMapper} (D-09).
 * Mantido para rollback rápido — sem {@code @Provider} para não ser descoberto.
 *
 * @deprecated desde 10-02-PLAN. Usar {@link GenericExceptionMapper}.
 */
@Deprecated
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
