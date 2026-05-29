package com.fiap.mekano.adapter.in.rest.exception;

import com.fiap.mekano.domain.exception.InvalidEmailException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduz InvalidEmailException (domínio) para HTTP 400 Bad Request.
 *
 * <p>O VO {@code Email} aplica validação mais restritiva que o {@code @Email} do
 * Bean Validation (ex.: rejeita {@code user@localhost}). Sem este mapper, falhas
 * do VO escalam para HTTP 500. Este mapper garante que erros de formato de email
 * sempre retornem 400 ao cliente.
 */
@Provider
@ApplicationScoped
public class InvalidEmailExceptionMapper implements ExceptionMapper<InvalidEmailException> {

    @Override
    public Response toResponse(InvalidEmailException exception) {
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
