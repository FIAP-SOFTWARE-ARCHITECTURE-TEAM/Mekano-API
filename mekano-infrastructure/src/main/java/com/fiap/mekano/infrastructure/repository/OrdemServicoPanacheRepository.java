package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.OrdemServicoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrdemServicoPanacheRepository implements PanacheRepository<OrdemServicoEntity> {

    public Optional<OrdemServicoEntity> findByUuid(UUID uuid) {
        return find("uuid", uuid).firstResultOptional();
    }
}