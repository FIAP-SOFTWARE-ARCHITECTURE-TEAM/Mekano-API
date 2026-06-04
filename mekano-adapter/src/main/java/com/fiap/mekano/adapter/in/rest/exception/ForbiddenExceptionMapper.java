package com.fiap.mekano.adapter.in.rest.exception;

import io.quarkus.logging.Log;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

/**
 * Traduz {@link io.quarkus.security.ForbiddenException} (Quarkus Security) para HTTP 403
 * com corpo {@link ErrorResponse} JSON.
 *
 * Phase 8 (WR-02 follow-up — Code Review 08): com
 * {@code quarkus.http.auth.proactive=false} (D-07), uma requisição com JWT de
 * assinatura válida cuja claim {@code groups} não inclui o role exigido em
 * {@code @RolesAllowed} dispara {@link io.quarkus.security.ForbiddenException}
 * no interceptor de segurança Quarkus. Esta classe NÃO é subclasse de
 * {@link jakarta.ws.rs.ForbiddenException} — é necessário mapear o tipo
 * Quarkus específico, caso contrário a resposta cai no fallback default
 * (body vazio / HTML), violando o D-06 (toda resposta de erro deve devolver
 * {@link ErrorResponse} canônico em JSON).
 *
 * <b>DEPRECATED:</b> Substituído por {@link GenericExceptionMapper} (D-09).
 * Mantido para rollback rápido — sem {@code @Provider} para não ser descoberto.
 *
 * @deprecated desde 10-02-PLAN. Usar {@link GenericExceptionMapper}.
 */
@Deprecated
public class ForbiddenExceptionMapper implements ExceptionMapper<io.quarkus.security.ForbiddenException> {

    @Override
    public Response toResponse(io.quarkus.security.ForbiddenException exception) {
        // T-08-07: nunca ecoar mensagens internas do interceptor de segurança.
        Log.debugf(exception, "Forbidden request");
        return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse("Forbidden"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
