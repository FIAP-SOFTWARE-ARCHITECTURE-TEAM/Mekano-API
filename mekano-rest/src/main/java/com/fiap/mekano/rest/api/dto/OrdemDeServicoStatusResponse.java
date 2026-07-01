package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO reduzido para consulta pública de status (D-16, AUTH-03).
 * Apenas uuid, status e data de entrada — sem dados sensíveis.
 */
@Schema(description = "Status resumido da OS (endpoint público)")
public record OrdemDeServicoStatusResponse(
        @Schema(description = "UUID da OS") UUID id,
        @Schema(description = "Status atual da OS") String status,
        @Schema(description = "Data de entrada da OS") LocalDateTime dataEntrada
) {}
