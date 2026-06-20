package com.fiap.mekano.domain.exception;

/**
 * Exceção padrão da aplicação — carrega o status HTTP e uma mensagem.
 *
 * <p>Use esta classe para lançar erros de qualquer camada com o status HTTP desejado:
 *
 * <pre>{@code
 * // Direto com código numérico
 * throw new AppException(404, "Usuário não encontrado");
 *
 * // Via factory methods (atalhos para os status mais comuns)
 * throw AppException.notFound("Usuário não encontrado");
 * throw AppException.conflict("Email já cadastrado");
 * throw AppException.badRequest("Nome é obrigatório");
 *
 * // Via jakarta.ws.rs.core.Response.Status (módulos que têm JAX-RS no classpath)
 * throw new AppException(Response.Status.NOT_FOUND.getStatusCode(), "Usuário não encontrado");
 * }</pre>
 *
 * <p>O {@code GenericExceptionMapper} captura qualquer {@code AppException} e converte
 * automaticamente para a resposta HTTP com o status e mensagem correspondentes —
 * sem necessidade de registrar novos mapeamentos.
 *
 * <p>Esta classe NÃO deve importar nenhuma classe de framework. O campo {@code status}
 * usa {@code int} para manter o domínio independente de {@code jakarta.ws.rs.core.Response.Status}.
 */
public class AppException extends RuntimeException {

    private final int status;

    /**
     * Cria uma AppException com o status HTTP e mensagem fornecidos.
     *
     * @param status  código de status HTTP (ex: 400, 404, 409, 500)
     * @param message mensagem legível para o cliente
     */
    public AppException(int status, String message) {
        super(message);
        this.status = status;
    }

    /**
     * Cria uma AppException com status HTTP, mensagem e causa original.
     *
     * @param status  código de status HTTP
     * @param message mensagem legível para o cliente
     * @param cause   exceção original encadeada
     */
    public AppException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /**
     * Retorna o código de status HTTP associado a esta exceção.
     *
     * @return status HTTP (ex: 400, 404, 409)
     */
    public int getStatus() {
        return status;
    }

    // -------------------------------------------------------------------------
    // Factory methods — atalhos para os status mais comuns
    // -------------------------------------------------------------------------

    /** 400 Bad Request — dados de entrada inválidos. */
    public static AppException badRequest(String message) {
        return new AppException(400, message);
    }

    /** 404 Not Found — recurso não encontrado. */
    public static AppException notFound(String message) {
        return new AppException(404, message);
    }

    /** 409 Conflict — conflito de estado (ex: email duplicado). */
    public static AppException conflict(String message) {
        return new AppException(409, message);
    }

    /** 422 Unprocessable Entity — dados sintaticamente válidos mas semanticamente inválidos. */
    public static AppException unprocessable(String message) {
        return new AppException(422, message);
    }

    /** 500 Internal Server Error — erro inesperado no servidor. */
    public static AppException internalError(String message) {
        return new AppException(500, message);
    }

    /**
     * Cria uma AppException com status HTTP arbitrário.
     *
     * @param status  código de status HTTP
     * @param message mensagem de erro
     */
    public static AppException of(int status, String message) {
        return new AppException(status, message);
    }
}
