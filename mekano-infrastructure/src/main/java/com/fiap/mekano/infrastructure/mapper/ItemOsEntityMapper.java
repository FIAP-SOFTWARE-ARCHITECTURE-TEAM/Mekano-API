package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.ItemOs;
import com.fiap.mekano.infrastructure.entity.ItemOsEntity;

public interface ItemOsEntityMapper {
    ItemOsEntity toEntity(ItemOs itemOs);
    ItemOs toDomain(ItemOsEntity entity);
}
