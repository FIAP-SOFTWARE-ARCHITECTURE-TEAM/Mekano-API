package com.fiap.mekano.domain.exception;

/**
 * Exceção padrão da aplicação — carrega o status HTTP e uma mensagem.
 *
 * <pre>{@code
 * // Direto com código numérico
 * throw new AppException(404, "Usuário não encontrado");
 *
 * // Via jakarta.ws.rs.core.Response.Status (módulos que têm JAX-RS no classpath)
 * throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), "Usuário não encontrado");
 * }</pre>
 *
 * <p>O {@code ApiExceptionMapper} converte automaticamente qualquer {@code AppException}
 * para a resposta HTTP correspondente — sem necessidade de registrar novos mapeamentos.
 *
 * <p>Esta classe NÃO deve importar nenhuma classe de framework. O campo {@code status}
 * usa {@code int} para manter o domínio independente de {@code jakarta.ws.rs.core.Response.Status}.
 */
public class AppException extends RuntimeException {

    private final int status;

    public AppException(int status, String message) {
        super(message);
        this.status = status;
    }

    public AppException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
