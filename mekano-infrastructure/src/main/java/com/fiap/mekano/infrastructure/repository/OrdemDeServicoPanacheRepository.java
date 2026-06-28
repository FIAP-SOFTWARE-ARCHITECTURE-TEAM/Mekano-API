package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.OrdemDeServicoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrdemDeServicoPanacheRepository implements PanacheRepositoryBase<OrdemDeServicoEntity, Long> {
}
