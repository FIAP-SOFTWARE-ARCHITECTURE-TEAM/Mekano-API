package com.fiap.mekano.domain.port.in;

import java.util.UUID;

public record FinalizarExecucaoCommand(
        UUID osUuid,
        String observacao
) {}
