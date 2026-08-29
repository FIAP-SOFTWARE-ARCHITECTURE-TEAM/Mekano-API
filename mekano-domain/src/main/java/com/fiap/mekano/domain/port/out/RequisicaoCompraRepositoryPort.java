package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.RequisicaoCompra;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RequisicaoCompraRepositoryPort {
    RequisicaoCompra save(RequisicaoCompra requisicao);
    Optional<RequisicaoCompra> findById(UUID id);
    RequisicaoCompra atualizar(RequisicaoCompra requisicao);
    List<RequisicaoCompra> findAll(int page, int size);
    long countAll();
}
