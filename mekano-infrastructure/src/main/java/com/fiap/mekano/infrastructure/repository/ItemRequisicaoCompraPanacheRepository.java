package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.ItemRequisicaoCompraEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ItemRequisicaoCompraPanacheRepository implements PanacheRepository<ItemRequisicaoCompraEntity> {
}
