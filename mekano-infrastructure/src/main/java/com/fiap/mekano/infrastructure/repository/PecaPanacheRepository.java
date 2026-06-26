package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.PecaEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PecaPanacheRepository implements PanacheRepository<PecaEntity> {}
