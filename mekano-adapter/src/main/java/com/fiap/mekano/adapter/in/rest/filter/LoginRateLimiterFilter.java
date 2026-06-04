package com.fiap.mekano.adapter.in.rest.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.mekano.adapter.in.rest.exception.ErrorResponse;
import com.fiap.mekano.infrastructure.security.TokenBucketRateLimiter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Filtro CDI que aplica rate limit ao endpoint {@code POST /auth/login}.
 *
 * <p>Usa {@link TokenBucketRateLimiter} com chave composta {@code IP:email}
 * para permitir no máximo 10 tentativas por minuto (configurável). Quando o
 * limite é excedido, retorna HTTP 429 Too Many Requests com header
 * {@code Retry-After}.
 *
 * <p><b>Não</b> usa {@code @NameBinding} — a verificação de path é feita em
 * runtime para evitar intrusão anotando o resource existente. O filtro é
 * aplicado a todas as requisições, mas só age em {@code POST /auth/login}.
 *
 * <p><b>IMPORTANTE:</b> O body da requisição é lido e re-escrito no stream
 * ({@code setEntityStream}) para não consumi-lo para o resource downstream.
 */
@Provider
@ApplicationScoped
public class LoginRateLimiterFilter implements ContainerRequestFilter {

    @Inject
    TokenBucketRateLimiter rateLimiter;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        // Aplicar rate limit apenas a POST /auth/login
        if (!"/auth/login".equals(requestContext.getUriInfo().getPath()) ||
                !\"POST\".equals(requestContext.getMethod())) {
            return;
        }

        String ip = extractClientIp(requestContext);
        String email = extractEmailFromBody(requestContext);
        String key = ip + ":" + email;

        if (!rateLimiter.tryConsume(key)) {
            long retryAfter = rateLimiter.getRetryAfterSeconds(key);
            requestContext.abortWith(
                    Response.status(429)
                            .header("Retry-After", String.valueOf(retryAfter))
                            .entity(new ErrorResponse("Too many requests. Try again in " + retryAfter + " seconds."))
                            .type(MediaType.APPLICATION_JSON)
                            .build()
            );
        }
    }

    /**
     * Extrai o IP do cliente a partir do header {@code X-Forwarded-For}.
     *
     * <p>Se o header não estiver presente, usa o header {@code Host} como fallback.
     * Em produção atrás de proxy reverso, é obrigatório configurar
     * {@code X-Forwarded-For} confiável no proxy.
     */
    private String extractClientIp(ContainerRequestContext ctx) {
        String xff = ctx.getHeaderString("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return ctx.getHeaderString("Host"); // fallback
    }

    /**
     * Extrai o campo {@code email} do body JSON da requisição.
     *
     * <p>Lê o stream de bytes, parseia com Jackson e re-escreve o stream
     * ({@link ContainerRequestContext#setEntityStream}) para que o resource
     * downstream possa ler o body normalmente.
     *
     * @return o email extraído, ou {@code "unknown"} em caso de erro
     */
    private String extractEmailFromBody(ContainerRequestContext ctx) {
        try {
            byte[] bodyBytes = ctx.getEntityStream().readAllBytes();
            ctx.setEntityStream(new ByteArrayInputStream(bodyBytes)); // rewind
            JsonNode node = objectMapper.readTree(bodyBytes);
            JsonNode email = node.get("email");
            return email != null ? email.asText("unknown") : "unknown";
        } catch (Exception e) {
            return "unknown";
        }
    }
}
