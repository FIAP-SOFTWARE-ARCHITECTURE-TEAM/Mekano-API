package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.ItemOrcamento;

import java.util.List;
import java.util.UUID;

public record GerarOrcamentoCommand(
        UUID ordemServicoUuid,
        String descricao,
        List<ItemOrcamento> itens
) {}
