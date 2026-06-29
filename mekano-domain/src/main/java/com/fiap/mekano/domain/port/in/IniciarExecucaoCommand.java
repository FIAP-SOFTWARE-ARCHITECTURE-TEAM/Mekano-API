package com.fiap.mekano.domain.port.in;

import java.util.UUID;

public record IniciarExecucaoCommand(
        UUID osUuid,
        UUID mecanicoUuid,
        String observacao
) {}
