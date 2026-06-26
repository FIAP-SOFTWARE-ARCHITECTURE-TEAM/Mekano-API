package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import com.fiap.mekano.infrastructure.entity.PecaEntity;
import com.fiap.mekano.infrastructure.mapper.PecaEntityMapper;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
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
    @Transactional(Transactional.TxType.MANDATORY)
    @CacheInvalidate(cacheName = CacheNames.PECAS)
    public Peca salvar(Peca peca) {
        var entity = mapper.toPecaEntity(peca);
        panacheRepository.persist(entity);
        em.flush();
        return mapper.toPeca(entity);
    }

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    @CacheResult(cacheName = CacheNames.PECAS)
    public Optional<Peca> buscarPorId(UUID uuid) {
        var entity = panacheRepository.find("uuid", uuid).firstResult();
        return Optional.ofNullable(entity).map(mapper::toPeca);
    }

    @Override
    @Transactional(Transactional.TxType.MANDATORY)
    public Optional<Peca> buscarPorDescricao(String descricao) {
        var entity = panacheRepository.find("descricao", descricao).firstResult();
        return Optional.ofNullable(entity).map(mapper::toPeca);
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public boolean debitarSaldo(UUID uuid, Integer quantidade) {
        var rowsUpdated = em.createNativeQuery(
            "UPDATE pecas SET saldo = saldo - :qtd WHERE uuid = :uuid AND saldo >= :qtd"
        )
        .setParameter("uuid", uuid)
        .setParameter("qtd", quantidade)
        .executeUpdate();

        return rowsUpdated > 0;
    }

    @Transactional(Transactional.TxType.MANDATORY)
    public void creditarSaldo(UUID uuid, Integer quantidade) {
        em.createNativeQuery(
            "UPDATE pecas SET saldo = saldo + :qtd WHERE uuid = :uuid"
        )
        .setParameter("uuid", uuid)
        .setParameter("qtd", quantidade)
        .executeUpdate();
    }
}
