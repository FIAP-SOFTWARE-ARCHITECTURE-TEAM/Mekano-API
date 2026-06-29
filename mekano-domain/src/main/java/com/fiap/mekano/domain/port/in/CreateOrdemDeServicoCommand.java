package com.fiap.mekano.domain.port.in;

import java.util.UUID;

public record CreateOrdemDeServicoCommand(UUID clienteId, UUID veiculoId, String descricaoProblema) {}
