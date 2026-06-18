package com.fiap.mekano.rest.api.exception;

import com.fiap.mekano.domain.exception.InvalidRefreshTokenException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduz {@link InvalidRefreshTokenException} em HTTP 401 com corpo
 * {@link ErrorResponse} JSON ({@code {"message":"Invalid or expired refresh token"}}).
 *
 * <p><b>DEPRECATED:</b> Substituído por {@link GenericExceptionMapper} (D-09).
 * Mantido para rollback rápido — sem {@code @Provider} para não ser descoberto.
 *
 * @deprecated desde 10-02-PLAN. Usar {@link GenericExceptionMapper}.
 */
@Deprecated
public class InvalidRefreshTokenExceptionMapper implements ExceptionMapper<InvalidRefreshTokenException> {

    @Override
    public Response toResponse(InvalidRefreshTokenException exception) {
        Log.debugf(exception, "Invalid refresh token");
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse(exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
