package com.fiap.mekano.domain.event;

import java.util.List;
import java.util.UUID;

public record OrcamentoAprovadoEvent(
    UUID orcamentoId,
    List<ItemOrcamento> itens
) {
    public record ItemOrcamento(UUID pecaId, Integer quantidade) {}
}
