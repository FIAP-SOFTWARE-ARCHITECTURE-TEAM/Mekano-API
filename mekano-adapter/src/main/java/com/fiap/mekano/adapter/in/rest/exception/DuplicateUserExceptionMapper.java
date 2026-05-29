package com.fiap.mekano.adapter.in.rest.exception;

import com.fiap.mekano.domain.exception.UserAlreadyExistsException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Traduz UserAlreadyExistsException (domínio) para HTTP 409 Conflict.
 *
 * @Provider: JAX-RS descobre este mapper automaticamente via Jandex CDI scan.
 * @ApplicationScoped: garante instância única gerenciada pelo Arc.
 *
 * Nome do arquivo: DuplicateUserExceptionMapper (não UserAlreadyExistsExceptionMapper)
 * conforme decisão em CONTEXT.md "Files to Create".
 */
@Provider
@ApplicationScoped
public class DuplicateUserExceptionMapper implements ExceptionMapper<UserAlreadyExistsException> {

    @Override
    public Response toResponse(UserAlreadyExistsException exception) {
        return Response.status(Response.Status.CONFLICT)
                .entity(new ErrorResponse(exception.getMessage()))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
