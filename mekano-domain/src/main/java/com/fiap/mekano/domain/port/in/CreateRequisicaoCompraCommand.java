package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.MotivoRequisicao;

import java.util.UUID;

public record CreateRequisicaoCompraCommand(
    UUID pecaId,
    Integer quantidade,
    MotivoRequisicao motivo
) {}
