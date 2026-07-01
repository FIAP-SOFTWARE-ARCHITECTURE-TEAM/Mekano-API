package com.fiap.mekano.domain.port.in;

import java.util.List;
import java.util.UUID;

public record FinalizarDiagnosticoCommand(
        UUID osId,
        String descricao,
        List<ItemDiagnostico> itens
) {
    public record ItemDiagnostico(UUID referenciaUuid, String tipo, Long quantidade) {}
}