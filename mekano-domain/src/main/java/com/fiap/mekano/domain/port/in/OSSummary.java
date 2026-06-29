package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.StatusOS;

import java.time.LocalDateTime;
import java.util.UUID;

public record OSSummary(
        UUID id,
        UUID clienteId,
        UUID veiculoId,
        String descricaoProblema,
        StatusOS status,
        UUID mecanicoUuid,
        LocalDateTime createdAt
) {}
