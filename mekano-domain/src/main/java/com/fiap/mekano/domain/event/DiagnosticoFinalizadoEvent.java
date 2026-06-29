package com.fiap.mekano.domain.event;

import com.fiap.mekano.domain.model.ItemOrcamento;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record DiagnosticoFinalizadoEvent(
        UUID osUuid,
        String descricao,
        List<ItemOrcamento> itens,
        LocalDateTime occurredAt
) {
    public static DiagnosticoFinalizadoEvent of(UUID osUuid, String descricao, List<ItemOrcamento> itens) {
        return new DiagnosticoFinalizadoEvent(osUuid, descricao, itens, LocalDateTime.now());
    }
}