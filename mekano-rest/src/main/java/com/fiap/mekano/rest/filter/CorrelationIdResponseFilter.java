package com.fiap.mekano.rest.filter;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import java.io.IOException;

/**
 * Ecoa o correlation-id de {@link CorrelationIdRequestFilter} no header de resposta,
 * inclusive em respostas abortadas (429 do rate limit, 401/403 de segurança).
 */
@Provider
@ApplicationScoped
@Priority(Priorities.AUTHENTICATION - 100)
public class CorrelationIdResponseFilter implements ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext)
            throws IOException {
        Object correlationId = requestContext.getProperty(CorrelationIdRequestFilter.MDC_KEY);
        if (correlationId != null) {
            responseContext.getHeaders().putSingle(CorrelationIdRequestFilter.HEADER, correlationId);
        }
    }
}
