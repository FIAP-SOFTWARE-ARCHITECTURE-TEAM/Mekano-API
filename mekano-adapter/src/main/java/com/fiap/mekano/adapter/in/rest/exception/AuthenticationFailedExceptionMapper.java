package com.fiap.mekano.adapter.in.rest.exception;

import io.quarkus.logging.Log;
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
 * Mensagem mascarada (T-08-07 / Information Disclosure): a {@code message}
 * de {@link AuthenticationFailedException} é gerada por SmallRye-JWT/Quarkus
 * Security e frequentemente expõe internals do pipeline de verificação
 * (ex.: {@code SRJWT07000}, "Failed to verify a token", "No claim exists at
 * path …"). Para evitar vazar esses detalhes a callers anônimos, devolvemos
 * sempre o literal {@code "Unauthorized"} no body, e logamos a exceção
 * original em {@code DEBUG} para troubleshooting interno.
 */
@Provider
@ApplicationScoped
public class AuthenticationFailedExceptionMapper implements ExceptionMapper<AuthenticationFailedException> {

    @Override
    public Response toResponse(AuthenticationFailedException exception) {
        // T-08-07: nunca ecoar mensagens internas do SmallRye/Quarkus para o cliente.
        Log.debugf(exception, "Authentication failed");
        return Response.status(Response.Status.UNAUTHORIZED)
                .entity(new ErrorResponse("Unauthorized"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
