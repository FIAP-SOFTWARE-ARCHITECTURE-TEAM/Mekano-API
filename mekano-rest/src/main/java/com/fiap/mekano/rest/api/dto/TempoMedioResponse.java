package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Tempo médio de execução de OS")
public record TempoMedioResponse(
        @Schema(description = "Tempo médio em horas") Double tempoMedioHoras
) {}
