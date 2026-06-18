package com.fiap.mekano.rest.api.exception;

import io.quarkus.logging.Log;
import io.quarkus.security.UnauthorizedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduz {@link UnauthorizedException} (Quarkus Security) para HTTP 401 com corpo
 * {@link ErrorResponse} JSON.
 *
 * <p><b>DEPRECATED:</b> Substituído por {@link GenericExceptionMapper} (D-09).
 * Mantido para rollback rápido — sem {@code @Provider} para não ser descoberto.
 *
 * @deprecated desde 10-02-PLAN. Usar {@link GenericExceptionMapper}.
 */
@Deprecated
public class UnauthorizedExceptionMapper implements ExceptionMapper<UnauthorizedException> {

    @Override
    public Response toResponse(UnauthorizedException exception) {
        // T-08-07: nunca ecoar mensagens internas do SmallRye/Quarkus para o cliente.
        Log.debugf(exception, "Unauthorized request");
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse("Unauthorized"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
