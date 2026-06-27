package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.ClienteEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ClientePanacheRepository implements PanacheRepositoryBase<ClienteEntity, Long> {
}
