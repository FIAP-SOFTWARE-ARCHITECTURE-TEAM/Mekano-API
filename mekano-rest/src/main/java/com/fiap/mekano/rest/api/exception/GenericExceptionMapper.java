package com.fiap.mekano.rest.api.exception;

import com.fiap.mekano.domain.exception.InvalidEmailException;
import com.fiap.mekano.domain.exception.InvalidUserDataException;
import com.fiap.mekano.domain.exception.UserAlreadyExistsException;
import com.fiap.mekano.domain.exception.UserNotFoundException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * ExceptionMapper único que substitui todos os mappers individuais (D-09).
 *
 * <p>Mapeia exceções para HTTP status codes baseando-se no tipo da exceção.
 * Fallback: 500 Internal Server Error com log da stacktrace.
 *
 * <p>Os mappers antigos foram desabilitados ({@code @Deprecated} sem {@code @Provider})
 * mas mantidos no código-fonte para rollback rápido.
 *
 * @see <a href="https://docs.oracle.com/javaee/7/api/javax/ws/rs/ext/ExceptionMapper.html">JAX-RS ExceptionMapper</a>
 */
@Provider
@ApplicationScoped
public class GenericExceptionMapper implements ExceptionMapper<Exception> {

    private static final Map<Class<?>, Function<Exception, Response>> MAPPERS = new ConcurrentHashMap<>();

    static {
        // 400 — erros de validação
        MAPPERS.put(ConstraintViolationException.class, e -> {
            var ex = (ConstraintViolationException) e;
            String message = ex.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(java.util.stream.Collectors.joining("; "));
            return build(Response.Status.BAD_REQUEST, message);
        });
        MAPPERS.put(InvalidEmailException.class, e -> build(Response.Status.BAD_REQUEST, e.getMessage()));
        MAPPERS.put(InvalidUserDataException.class, e -> build(Response.Status.BAD_REQUEST, e.getMessage()));

        // 404 — não encontrado
        MAPPERS.put(UserNotFoundException.class, e -> build(Response.Status.NOT_FOUND, e.getMessage()));

        // 409 — conflito
        MAPPERS.put(UserAlreadyExistsException.class, e -> build(Response.Status.CONFLICT, e.getMessage()));
    }

    private static Response build(Response.Status status, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }

    @Override
    public Response toResponse(Exception exception) {
        Function<Exception, Response> mapper = MAPPERS.get(exception.getClass());
        if (mapper != null) {
            return mapper.apply(exception);
        }
        // Fallback para exceções não mapeadas
        Log.errorf(exception, "Unhandled exception: %s", exception.getMessage());
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(new ErrorResponse("Erro interno do servidor"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
