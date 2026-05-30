package com.fiap.mekano.adapter.in.rest.exception;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

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
 * Mesmo padrão dos demais mappers desta camada (Pattern A — PATTERNS.md / G8):
 * {@code @Provider + @ApplicationScoped}, body via {@link ErrorResponse},
 * mensagem mascarada para o literal {@code "Forbidden"} (T-08-07 — não
 * vazar internals do pipeline de autorização).
 */
@Provider
@ApplicationScoped
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
