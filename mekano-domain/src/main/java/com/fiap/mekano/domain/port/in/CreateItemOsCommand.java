package com.fiap.mekano.domain.port.in;

import java.util.UUID;

public record CreateItemOsCommand(UUID referenciaUuid, String tipo, Long quantidade) {}
