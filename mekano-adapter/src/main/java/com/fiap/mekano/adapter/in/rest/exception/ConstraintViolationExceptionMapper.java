package com.fiap.mekano.adapter.in.rest.exception;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.stream.Collectors;

/**
 * Traduz ConstraintViolationException (Bean Validation) para HTTP 400 Bad Request.
 *
 * CRÍTICO: implementa ExceptionMapper<jakarta.validation.ConstraintViolationException> (Bean Validation).
 * NÃO usar a versão do pacote ws.rs — são tipos diferentes e o mapper não funcionaria!
 *
 * Formato da mensagem: "create.request.name: não deve estar em branco; create.request.email: deve ser um endereço de e-mail bem formado"
 * Múltiplas violations são concatenadas com "; " (decisão D-02).
 */
@Provider
@ApplicationScoped
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorResponse(message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
