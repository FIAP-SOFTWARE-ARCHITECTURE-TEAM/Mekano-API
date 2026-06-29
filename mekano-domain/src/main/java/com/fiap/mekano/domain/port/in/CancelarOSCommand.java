package com.fiap.mekano.domain.port.in;

import java.util.UUID;

public record CancelarOSCommand(
        UUID osUuid,
        String motivo
) {}
