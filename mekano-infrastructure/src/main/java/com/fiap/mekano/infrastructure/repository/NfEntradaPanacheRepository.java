package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.NfEntradaEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class NfEntradaPanacheRepository implements PanacheRepositoryBase<NfEntradaEntity, Long> {}
