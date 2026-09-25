package com.fiap.mekano.rest.api.exception;

import com.fiap.mekano.domain.exception.DomainException;
import com.fiap.mekano.domain.exception.TransicaoInvalidaException;
import com.fiap.mekano.domain.port.out.OSMetricsPort;
import io.quarkus.logging.Log;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * ExceptionMapper para DomainException (e subclasses como TransicaoInvalidaException).
 *
 * <p>Mapeia exceções de domínio para HTTP 422 e registra métricas de falha.
 * Esta é a camada onde domínio encontra transporte — o domain não sabe de HTTP.
 */
@Provider
@ApplicationScoped
public class DomainExceptionMapper implements ExceptionMapper<DomainException> {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final int HTTP_UNPROCESSABLE_ENTITY = 422;

    private final OSMetricsPort osMetrics;

    @Context
    UriInfo uriInfo;

    @Inject
    public DomainExceptionMapper(OSMetricsPort osMetrics) {
        this.osMetrics = osMetrics;
    }

    @Override
    public Response toResponse(DomainException exception) {
        if (exception instanceof TransicaoInvalidaException t) {
            Log.debugf("Transição inválida: %s → %s", t.getStatusAtual(), t.getStatusTentado());
            osMetrics.registrarFalhaTransicao(t.getStatusAtual(), t.getStatusTentado());
            return build(HTTP_UNPROCESSABLE_ENTITY, t.getMessage());
        }

        Log.debugf("Regra de negócio violada: %s", exception.getMessage());
        return build(HTTP_UNPROCESSABLE_ENTITY, exception.getMessage());
    }

    private Response build(int status, String detail) {
        String instance = uriInfo != null ? uriInfo.getRequestUri().toString() : null;
        return Response.status(status)
                .entity(ProblemDetail.of(status, detail, instance))
                .type(PROBLEM_JSON)
                .build();
    }
}
