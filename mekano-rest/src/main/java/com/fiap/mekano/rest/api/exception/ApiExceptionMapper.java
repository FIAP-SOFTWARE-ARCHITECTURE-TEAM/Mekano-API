package com.fiap.mekano.rest.api.exception;

import com.fiap.mekano.domain.exception.AppException;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.stream.Collectors;

/**
 * ExceptionMapper único que trata todas as exceções da aplicação (D-09).
 *
 * <p>Fluxo de tratamento:
 * <ol>
 *   <li>{@link ConstraintViolationException} — Bean Validation falhou; 400 com lista de violações.</li>
 *   <li>{@link AppException} — lê status HTTP direto de {@code getStatus()}.</li>
 *   <li>Fallback — exceção não mapeada; 500 com log de stacktrace.</li>
 * </ol>
 *
 * @see AppException
 */
@Provider
@ApplicationScoped
public class ApiExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {

        // Bean Validation — concatena todas as violações numa mensagem só
        if (exception instanceof ConstraintViolationException ex) {
            String message = ex.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            return build(400, message);
        }

        // AppException e qualquer subclasse — lê o status diretamente do objeto
        if (exception instanceof AppException ex) {
            return build(ex.getStatus(), ex.getMessage());
        }

        // Fallback — erro inesperado; loga a stacktrace completa
        Log.errorf(exception, "Unhandled exception: %s", exception.getMessage());
        return build(500, "Erro interno do servidor");
    }

    private static Response build(int status, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
