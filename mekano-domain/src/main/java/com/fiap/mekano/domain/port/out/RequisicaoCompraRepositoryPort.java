package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.RequisicaoCompra;
import java.util.Optional;
import java.util.UUID;

public interface RequisicaoCompraRepositoryPort {
    RequisicaoCompra salvar(RequisicaoCompra requisicao);
    Optional<RequisicaoCompra> buscarPorId(UUID id);
    RequisicaoCompra atualizar(RequisicaoCompra requisicao);
}
