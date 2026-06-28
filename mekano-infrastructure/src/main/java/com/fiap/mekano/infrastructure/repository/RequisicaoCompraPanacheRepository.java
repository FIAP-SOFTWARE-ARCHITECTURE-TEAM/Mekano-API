package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.RequisicaoCompraEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class RequisicaoCompraPanacheRepository implements PanacheRepositoryBase<RequisicaoCompraEntity, Long> {}
