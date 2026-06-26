package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.infrastructure.entity.PecaEntity;
import com.fiap.mekano.infrastructure.mapper.PecaEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PecaRepositoryImpl implements PecaRepositoryPort {

    @Inject
    EntityManager em;

    @Inject
    PecaEntityMapper mapper;

    @Inject
    PecaPanacheRepository panacheRepository;

    @Override
    @Transactional
    public Peca salvar(Peca peca) {
        var entity = mapper.toPecaEntity(peca);
        panacheRepository.persist(entity);
        em.flush();
        return mapper.toPeca(entity);
    }

    @Override
    public Optional<Peca> buscarPorId(UUID uuid) {
        var entity = panacheRepository.find("uuid", uuid).firstResult();
        return Optional.ofNullable(entity).map(mapper::toPeca);
    }

    @Override
    public Optional<Peca> buscarPorDescricao(String descricao) {
        var entity = panacheRepository.find("descricao", descricao).firstResult();
        return Optional.ofNullable(entity).map(mapper::toPeca);
    }
}
