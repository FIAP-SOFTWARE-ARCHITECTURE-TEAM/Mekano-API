package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.ItemOs;

import java.util.List;
import java.util.UUID;

public interface ItemOsRepositoryPort {

    ItemOs save(ItemOs itemOs);

    List<ItemOs> findByOsUuid(UUID osUuid);

    void deleteByOsUuid(UUID osUuid);
}
