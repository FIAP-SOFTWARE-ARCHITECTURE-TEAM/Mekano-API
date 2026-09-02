package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.ItemOsEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ItemOsPanacheRepository implements PanacheRepositoryBase<ItemOsEntity, Long> {}
