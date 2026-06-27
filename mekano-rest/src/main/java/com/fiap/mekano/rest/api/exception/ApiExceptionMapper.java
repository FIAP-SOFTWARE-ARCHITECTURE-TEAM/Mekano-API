package com.fiap.mekano.rest.api.exception;

import java.util.HashMap;
import java.util.Map;

import com.fiap.mekano.domain.exception.AppException;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
@ApplicationScoped
public class ApiExceptionMapper implements ExceptionMapper<AppException> {

    @Override
    public Response toResponse(AppException exception) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", exception.getStatus());
        body.put("message", exception.getMessage());

        return Response.status(exception.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(body)
                .build();
    }
}
