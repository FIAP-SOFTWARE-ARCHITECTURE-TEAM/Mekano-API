package com.fiap.mekano.infrastructure.repository;

import java.util.List;
import java.util.UUID;

import com.fiap.mekano.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.domain.port.out.ItemRequisicaoCompraRepositoryPort;
import com.fiap.mekano.infrastructure.entity.ItemRequisicaoCompraEntity;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ItemRequisicaoCompraRepositoryImpl implements ItemRequisicaoCompraRepositoryPort {

    private final ItemRequisicaoCompraPanacheRepository panacheRepository;

    public ItemRequisicaoCompraRepositoryImpl(ItemRequisicaoCompraPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public void saveAll(UUID requisicaoCompraId, List<ItemRequisicaoCompra> itens) {
        for (ItemRequisicaoCompra item : itens) {
            var entity = new ItemRequisicaoCompraEntity();
            entity.setUuid(UUID.randomUUID());
            entity.setRequisicaoCompraId(requisicaoCompraId);
            entity.setPecaId(item.getPecaId());
            entity.setQuantidade(item.getQuantidade().intValue());
            entity.setCreatedAt(java.time.LocalDateTime.now());
            entity.setIsActive(true);
            panacheRepository.persist(entity);
        }
    }

    @Override
    public List<ItemRequisicaoCompra> findByRequisicaoCompraId(UUID requisicaoCompraId) {
        return panacheRepository.find("requisicaoCompraId = ?1 and isActive = ?2", requisicaoCompraId, true)
                .list()
                .stream()
                .map(entity -> new ItemRequisicaoCompra(
                        entity.getPecaId(),
                        entity.getQuantidade() == null ? 0L : entity.getQuantidade().longValue()))
                .toList();
    }

    @Override
    public void deleteByRequisicaoCompraId(UUID requisicaoCompraId) {
        panacheRepository.update("isActive = false, deletedAt = java.time.LocalDateTime.now() WHERE requisicaoCompraId = ?1 and isActive = true",
                requisicaoCompraId);
    }
}
