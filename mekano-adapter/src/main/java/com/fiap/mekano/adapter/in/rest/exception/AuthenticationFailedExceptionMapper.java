package com.fiap.mekano.adapter.in.rest.exception;

import io.quarkus.security.AuthenticationFailedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduz {@link AuthenticationFailedException} (Quarkus Security) para HTTP 401 Unauthorized
 * com corpo {@link ErrorResponse} JSON.
 *
 * Papel: uniformizar o body das respostas 401 no mesmo formato dos demais erros
 * (400/404/409) já mapeados no resource — atende D-06 (consistência de payload de erro)
 * e replica literalmente o "Pattern A" do PATTERNS.md (template idêntico aos 4 mappers
 * existentes — Phase 5 / G8).
 *
 * @Provider + @ApplicationScoped: mantém consistência com os outros mappers da camada
 * (DuplicateUserExceptionMapper, UserNotFoundExceptionMapper, ConstraintViolationExceptionMapper,
 * IllegalArgumentExceptionMapper). JAX-RS descobre via Jandex CDI scan; Arc gerencia instância única.
 *
 * IMPORTANTE: este mapper só é invocado porque `quarkus.http.auth.proactive=false`
 * está ativo (D-07 / G9 / RESEARCH §D-07). Com `proactive=true` (default), a falha de
 * autenticação seria tratada pelo pipeline reativo do Vert.x antes do JAX-RS, ignorando
 * este provider e devolvendo um corpo HTML/texto não-uniforme. Ver 08-RESEARCH.md
 * para o racional completo da decisão.
 *
 * Null-guard: a mensagem de {@link AuthenticationFailedException} é frequentemente null
 * (token ausente / inválido sem detalhe). Quando null ou em branco, devolve a string
 * literal "Unauthorized" para evitar `"message": null` no JSON e não vazar detalhes
 * internos (T-08-07 / Information Disclosure).
 */
@Provider
@ApplicationScoped
public class AuthenticationFailedExceptionMapper implements ExceptionMapper<AuthenticationFailedException> {

    @Override
    public Response toResponse(AuthenticationFailedException exception) {
        String raw = exception.getMessage();
        String message = (raw == null || raw.isBlank()) ? "Unauthorized" : raw;
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse(message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
