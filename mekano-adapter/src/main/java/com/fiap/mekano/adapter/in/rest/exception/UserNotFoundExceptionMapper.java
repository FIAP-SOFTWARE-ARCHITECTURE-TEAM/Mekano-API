package com.fiap.mekano.adapter.in.rest.exception;

import com.fiap.mekano.domain.exception.UserNotFoundException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduz UserNotFoundException (domínio) para HTTP 404 Not Found.
 *
 * Requisito ADP-06: obrigatório para satisfazer comportamento 404 da API.
 *
 * @Provider @ApplicationScoped: padrão uniforme para todos os ExceptionMappers do projeto.
 */
@Provider
@ApplicationScoped
public class UserNotFoundExceptionMapper implements ExceptionMapper<UserNotFoundException> {

    @Override
    public Response toResponse(UserNotFoundException exception) {
        return Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse(exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
