package com.fiap.mekano.rest.filter;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

import org.jboss.logmanager.MDC;

import java.util.UUID;

/**
 * Correlation-id por request (docs/ARCHITECTURE-REVIEW-2026-10-17.md item 14).
 *
 * <p>Aceita {@code X-Request-ID} do cliente (proxy/API gateway costuma enviar) ou
 * gera um UUID; coloca no MDC — com {@code quarkus.log.console.json=true} toda
 * linha de log passa a carregar {@code correlationId}, viabilizando troubleshooting
 * multi-pod no Kubernetes.
 *
 * <p>Roda ANTES do {@link RateLimitFilter} para que respostas 429/401 já carreguem
 * a correlação. O header de resposta é adicionado por {@link CorrelationIdResponseFilter}.
 *
 * <p>Valor inválido (vazio, &gt; 64 chars ou fora de [A-Za-z0-9._-]) é descartado —
 * o header do cliente nunca vaza para os logs sem sanitização.
 */
@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION - 100)
public class CorrelationIdRequestFilter implements ContainerRequestFilter {

    public static final String HEADER = "X-Request-ID";
    public static final String MDC_KEY = "correlationId";

    @Override
    public void filter(ContainerRequestContext ctx) {
        String correlationId = sanitize(ctx.getHeaderString(HEADER));
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        ctx.setProperty(MDC_KEY, correlationId);
    }

    static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.isEmpty() || trimmed.length() > 64 || !trimmed.matches("[A-Za-z0-9._-]+")) {
            return null;
        }
        return trimmed;
    }
}
