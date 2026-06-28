package com.fiap.mekano.domain.port.in;

import java.util.UUID;

public record ReprovarOrcamentoCommand(UUID orcamentoUuid, String motivo) {}
