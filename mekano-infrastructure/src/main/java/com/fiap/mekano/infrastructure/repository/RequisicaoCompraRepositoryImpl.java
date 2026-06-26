package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import com.fiap.mekano.infrastructure.entity.RequisicaoCompraEntity;
import com.fiap.mekano.infrastructure.mapper.RequisicaoCompraEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RequisicaoCompraRepositoryImpl implements RequisicaoCompraRepositoryPort {

    @Inject
    EntityManager em;

    @Inject
    RequisicaoCompraEntityMapper mapper;

    @Inject
    RequisicaoCompraPanacheRepository panacheRepository;

    @Override
    @Transactional
    public RequisicaoCompra salvar(RequisicaoCompra requisicao) {
        var entity = mapper.toRequisicaoCompraEntity(requisicao);
        panacheRepository.persist(entity);
        em.flush();
        return mapper.toRequisicaoCompra(entity);
    }

    @Override
    public Optional<RequisicaoCompra> buscarPorId(UUID uuid) {
        var entity = panacheRepository.find("uuid", uuid).firstResult();
        return Optional.ofNullable(entity).map(mapper::toRequisicaoCompra);
    }

    @Override
    @Transactional
    public RequisicaoCompra atualizar(RequisicaoCompra requisicao) {
        var entity = mapper.toRequisicaoCompraEntity(requisicao);
        entity = em.merge(entity);
        em.flush();
        return mapper.toRequisicaoCompra(entity);
    }
}
