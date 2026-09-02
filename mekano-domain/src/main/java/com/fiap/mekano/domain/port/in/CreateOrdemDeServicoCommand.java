package com.fiap.mekano.domain.port.in;

import java.util.List;
import java.util.UUID;

public record CreateOrdemDeServicoCommand(UUID clienteId, UUID veiculoId,
                                          String descricaoProblema,
                                          List<CreateItemOsCommand> itens) {}
