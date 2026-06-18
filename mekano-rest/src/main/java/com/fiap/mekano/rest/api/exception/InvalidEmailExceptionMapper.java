package com.fiap.mekano.rest.api.exception;

import com.fiap.mekano.domain.exception.InvalidEmailException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduz InvalidEmailException (domínio) para HTTP 400 Bad Request.
 *
 * <p><b>DEPRECATED:</b> Substituído por {@link GenericExceptionMapper} (D-09).
 * Mantido para rollback rápido — sem {@code @Provider} para não ser descoberto.
 *
 * @deprecated desde 10-02-PLAN. Usar {@link GenericExceptionMapper}.
 */
@Deprecated
public class InvalidEmailExceptionMapper implements ExceptionMapper<InvalidEmailException> {

    @Override
    public Response toResponse(InvalidEmailException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
