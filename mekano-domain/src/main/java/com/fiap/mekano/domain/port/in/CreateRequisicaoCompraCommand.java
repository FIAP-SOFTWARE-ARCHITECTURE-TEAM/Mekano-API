package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.MotivoRequisicao;

import java.util.List;

public record CreateRequisicaoCompraCommand(
    List<ItemRequisicaoCompraCommand> itens,
    MotivoRequisicao motivo
) {}
