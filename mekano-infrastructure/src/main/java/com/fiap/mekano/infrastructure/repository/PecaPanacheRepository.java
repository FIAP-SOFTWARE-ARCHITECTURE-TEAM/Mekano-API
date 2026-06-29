package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.PecaEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PecaPanacheRepository implements PanacheRepositoryBase<PecaEntity, Long> {}
