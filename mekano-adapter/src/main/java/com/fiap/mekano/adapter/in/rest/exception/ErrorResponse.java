package com.fiap.mekano.adapter.in.rest.exception;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * DTO de resposta de erro uniforme.
 *
 * Usado por todos os ExceptionMappers para garantir formato JSON consistente:
 * {"message": "descrição do erro"}
 *
 * Java record: imutável, serializado corretamente pelo Jackson.
 */
@Schema(description = "Resposta de erro da API")
public record ErrorResponse(
        @Schema(description = "Descrição do erro") String message
) {}
