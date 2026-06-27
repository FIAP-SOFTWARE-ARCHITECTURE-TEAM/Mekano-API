package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.NfEntrada;
import com.fiap.mekano.domain.port.out.NfEntradaRepositoryPort;
import com.fiap.mekano.infrastructure.entity.NfEntradaEntity;
import com.fiap.mekano.infrastructure.mapper.NfEntradaEntityMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class NfEntradaRepositoryImpl implements NfEntradaRepositoryPort {

    @Inject
    EntityManager em;

    @Inject
    NfEntradaEntityMapper mapper;

    @Inject
    NfEntradaPanacheRepository panacheRepository;

    @Override
    @Transactional
    public NfEntrada salvar(NfEntrada nfEntrada) {
        var entity = mapper.toNfEntradaEntity(nfEntrada);
        panacheRepository.persist(entity);
        em.flush();
        return mapper.toNfEntrada(entity);
    }

    @Override
    public Optional<NfEntrada> buscarPorId(UUID uuid) {
        var entity = panacheRepository.find("uuid", uuid).firstResult();
        return Optional.ofNullable(entity).map(mapper::toNfEntrada);
    }
}
