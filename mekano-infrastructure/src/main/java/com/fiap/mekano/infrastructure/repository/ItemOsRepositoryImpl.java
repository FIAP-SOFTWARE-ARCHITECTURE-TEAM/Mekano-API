package com.fiap.mekano.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fiap.mekano.domain.model.ItemOs;
import com.fiap.mekano.domain.port.out.ItemOsRepositoryPort;
import com.fiap.mekano.infrastructure.entity.ItemOsEntity;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ItemOsRepositoryImpl implements ItemOsRepositoryPort {

    private final ItemOsPanacheRepository panacheRepository;

    public ItemOsRepositoryImpl(ItemOsPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Override
    public ItemOs save(ItemOs itemOs) {
        ItemOsEntity entity = panacheRepository.find("uuid = ?1", itemOs.getId()).firstResult();
        if (entity == null) {
            entity = new ItemOsEntity();
        }

        entity.setUuid(itemOs.getId());
        entity.setOsUuid(itemOs.getOsUuid());
        entity.setReferenciaUuid(itemOs.getReferenciaUuid());
        entity.setTipo(itemOs.getTipo());
        entity.setDescricao(itemOs.getDescricao());
        entity.setQuantidade(itemOs.getQuantidade());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(itemOs.getCreatedAt());
        }
        entity.setDeletedAt(null);
        entity.setIsActive(true);

        if (entity.getId() == null) {
            panacheRepository.persist(entity);
        }

        return toDomain(entity);
    }

    @Override
    public List<ItemOs> findByOsUuid(UUID osUuid) {
        return panacheRepository.find("osUuid = ?1 and isActive = ?2", osUuid, true)
                .list()
                .stream()
                .map(ItemOsRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public void deleteByOsUuid(UUID osUuid) {
        panacheRepository.update("isActive = false, deletedAt = ?1 WHERE osUuid = ?2 AND isActive = true",
                LocalDateTime.now(), osUuid);
    }

    private static ItemOs toDomain(ItemOsEntity entity) {
        return ItemOs.reconstitute(
                entity.getUuid(),
                entity.getOsUuid(),
                entity.getReferenciaUuid(),
                entity.getTipo(),
                entity.getDescricao(),
                entity.getQuantidade(),
                entity.getCreatedAt() == null ? LocalDateTime.now() : entity.getCreatedAt(),
                entity.getIsActive()
        );
    }
}
