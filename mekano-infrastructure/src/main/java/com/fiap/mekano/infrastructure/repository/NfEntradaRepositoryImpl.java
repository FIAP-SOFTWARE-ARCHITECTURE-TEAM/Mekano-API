package com.fiap.mekano.infrastructure.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fiap.mekano.domain.model.NfEntrada;
import com.fiap.mekano.domain.port.out.NfEntradaRepositoryPort;
import com.fiap.mekano.infrastructure.entity.NfEntradaEntity;

import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;

@ApplicationScoped
public class NfEntradaRepositoryImpl implements NfEntradaRepositoryPort {

    private final NfEntradaPanacheRepository panacheRepository;
    private final EntityManager em;

    public NfEntradaRepositoryImpl(NfEntradaPanacheRepository panacheRepository, EntityManager em) {
        this.panacheRepository = panacheRepository;
        this.em = em;
    }

    @Override
    public NfEntrada save(NfEntrada nfEntrada) {
        NfEntradaEntity entity = panacheRepository.find("uuid = ?1", nfEntrada.getId()).firstResult();
        if (entity == null) {
            entity = new NfEntradaEntity();
        }

        entity.setUuid(nfEntrada.getId());
        entity.setChaveAcesso(nfEntrada.getChaveAcesso());
        entity.setValorTotal(nfEntrada.getValorTotal());
        entity.setPecaId(nfEntrada.getPecaId());
        entity.setRequisicaoCompraId(nfEntrada.getRequisicaoCompraId());
        entity.setDataRecebimento(nfEntrada.getCreatedAt());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(nfEntrada.getCreatedAt());
        }
        entity.setDeletedAt(null);
        entity.setIsActive(true);

        if (entity.getId() == null) {
            panacheRepository.persist(entity);
            em.flush();
        }

        return toDomain(entity);
    }

    @Override
    public Optional<NfEntrada> findById(UUID id) {
        return panacheRepository.find("uuid = ?1 and isActive = ?2", id, true)
                .firstResultOptional()
                .map(NfEntradaRepositoryImpl::toDomain);
    }

    @Override
    public Optional<NfEntrada> buscarPorChaveAcesso(String chaveAcesso) {
        return panacheRepository.find("chaveAcesso = ?1 and isActive = ?2", chaveAcesso, true)
                .firstResultOptional()
                .map(NfEntradaRepositoryImpl::toDomain);
    }

    @Override
    public List<NfEntrada> findAll(int page, int size) {
        return panacheRepository.find("isActive = ?1", Sort.by("id"), true)
                .page(Page.of(page, size))
                .list()
                .stream()
                .map(NfEntradaRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public long countAll() {
        return panacheRepository.count("isActive", true);
    }

    private static NfEntrada toDomain(NfEntradaEntity entity) {
        return NfEntrada.reconstitute(
                entity.getUuid(),
                entity.getChaveAcesso(),
                entity.getValorTotal(),
                entity.getPecaId(),
                entity.getRequisicaoCompraId(),
                entity.getCreatedAt() == null ? LocalDateTime.now() : entity.getCreatedAt()
        );
    }
}
