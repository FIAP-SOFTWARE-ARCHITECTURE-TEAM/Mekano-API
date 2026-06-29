package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.OrcamentoEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class OrcamentoPanacheRepository implements PanacheRepositoryBase<OrcamentoEntity, Long> {
}
