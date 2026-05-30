package com.fiap.mekano.adapter.in.rest.exception;

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
 * Phase 8 (08-05 deviation, Rule 1 — bug discovery durante UAT-1):
 *
 * O mapper {@link AuthenticationFailedExceptionMapper} (08-03) cobre
 * {@link io.quarkus.security.AuthenticationFailedException} — token presente
 * mas inválido (assinatura, expiração, issuer). Porém, com
 * `quarkus.http.auth.proactive=false` (D-07), a ausência total de token em
 * endpoint protegido faz o pipeline lançar {@link UnauthorizedException}
 * (subclasse separada de SecurityException), sem mapper → JAX-RS responde
 * 401 com body vazio (`content-length: 0`), violando UAT-1 (D-06: erro 401
 * deve ter body JSON com `message`).
 *
 * Este mapper completa a cobertura: missing-token e invalid-token retornam
 * o mesmo formato canonical de erro.
 *
 * Mesmo padrão (Pattern A — PATTERNS.md / G8):
 * @Provider + @ApplicationScoped, body via ErrorResponse, mensagem mascarada
 * para "Unauthorized" (T-08-07): exceções de Quarkus Security podem expor
 * internals do pipeline de auth, então nunca ecoamos {@code getMessage()}
 * para o cliente — apenas logamos em DEBUG.
 */
@Provider
@ApplicationScoped
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
