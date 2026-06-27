package com.fiap.mekano.rest.api.exception;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Resposta de erro no formato RFC 7807 — Problem Details for HTTP APIs.
 *
 * <pre>{@code
 * {
 * "type": "about:blank",
 * "title": "Not Found",
 * "status": 404,
 * "detail": "Usuário não encontrado",
 * "instance": "/api/v1/users/123"
 * }
 * }</pre>
 *
 * @param type URI que identifica o tipo do problema (default "about:blank")
 * @param title Resumo legível do problema (derivado do status HTTP)
 * @param status Código de status HTTP
 * @param detail Explicação específica desta ocorrência
 * @param instance URI que identifica a ocorrência específica (opcional)
 */
@Schema(name = "ProblemDetail", description = "Resposta de erro no formato RFC 7807 Problem Details")
public record ProblemDetail(
        @Schema(description = "URI que identifica o tipo do problema (RFC 7807)", example = "about:blank") String type,

        @Schema(description = "Resumo legível do problema", example = "Not Found") String title,

        @Schema(description = "Código de status HTTP", example = "404") int status,

        @Schema(description = "Explicação específica desta ocorrência", example = "Usuário não encontrado") String detail,

        @Schema(description = "URI que identifica a ocorrência específica (opcional)", example = "/api/v1/users/123") String instance) {
    public static ProblemDetail of(int status, String detail) {
        return new ProblemDetail("about:blank", titleFor(status), status, detail, null);
    }

    public static ProblemDetail of(int status, String detail, String instance) {
        return new ProblemDetail("about:blank", titleFor(status), status, detail, instance);
    }

    private static String titleFor(int status) {
        return switch (status) {
            case 400 -> "Bad Request";
            case 401 -> "Unauthorized";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 409 -> "Conflict";
            case 422 -> "Unprocessable Entity";
            case 429 -> "Too Many Requests";
            case 500 -> "Internal Server Error";
            default -> "Unknown Error";
        };
    }
}