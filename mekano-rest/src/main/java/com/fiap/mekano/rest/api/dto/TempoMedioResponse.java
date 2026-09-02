package com.fiap.mekano.rest.api.dto;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.Map;
import java.util.UUID;

@Schema(description = "Tempo médio de execução de OS")
public record TempoMedioResponse(
        @Schema(description = "Tempo médio em horas") Double tempoMedioHoras,
        @Schema(description = "Breakdown por mecânico (UUID → média em horas)") Map<UUID, Double> breakdownPorMecanico
) {
    public TempoMedioResponse(Double tempoMedioHoras) {
        this(tempoMedioHoras, Map.of());
    }
}
