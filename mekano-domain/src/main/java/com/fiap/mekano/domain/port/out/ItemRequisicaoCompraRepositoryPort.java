package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.ItemRequisicaoCompra;

import java.util.List;
import java.util.UUID;

public interface ItemRequisicaoCompraRepositoryPort {
    void saveAll(UUID requisicaoCompraId, List<ItemRequisicaoCompra> itens);
    List<ItemRequisicaoCompra> findByRequisicaoCompraId(UUID requisicaoCompraId);
    void deleteByRequisicaoCompraId(UUID requisicaoCompraId);
}
