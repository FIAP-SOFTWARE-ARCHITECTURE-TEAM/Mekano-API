package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import com.fiap.mekano.infrastructure.entity.OrcamentoEntity;
import com.fiap.mekano.infrastructure.mapper.OrcamentoEntityMapper;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class OrcamentoRepositoryImpl implements OrcamentoRepositoryPort {

    @Inject
    OrcamentoPanacheRepository panacheRepository;

    @Inject
    OrcamentoEntityMapper mapper;

    @Override
    @Transactional
    @CacheInvalidateAll(cacheName = CacheNames.ORCAMENTOS)
    public Orcamento save(Orcamento orcamento) {
        OrcamentoEntity entity = mapper.toEntity(orcamento);

        Optional<OrcamentoEntity> existing = panacheRepository
                .find("uuid = ?1", orcamento.getId()).firstResultOptional();

        if (existing.isPresent()) {
            OrcamentoEntity managed = existing.get();
            managed.setStatus(orcamento.getStatus().name());
            managed.setDescricao(entity.getDescricao());
            managed.setValorTotal(entity.getValorTotal());
            managed.setOrdemServicoUuid(entity.getOrdemServicoUuid());
            managed.setDataExpiracao(entity.getDataExpiracao());
            managed.setItensJson(entity.getItensJson());
            panacheRepository.flush();
            return mapper.toDomain(managed);
        }

        panacheRepository.persist(entity);
        panacheRepository.flush();
        return mapper.toDomain(entity);
    }

    @Override
    @CacheResult(cacheName = CacheNames.ORCAMENTOS)
    public Optional<Orcamento> findByUuid(UUID uuid) {
        return panacheRepository.find("uuid = ?1 AND isActive = ?2", uuid, true)
                .firstResultOptional().map(mapper::toDomain);
    }

    @Override
    @CacheResult(cacheName = CacheNames.ORCAMENTOS)
    public Optional<Orcamento> findByOrdemServicoUuid(UUID ordemServicoUuid) {
        return panacheRepository.find("ordemServicoUuid = ?1 AND isActive = ?2", ordemServicoUuid, true)
                .firstResultOptional().map(mapper::toDomain);
    }

    @Override
    public List<Orcamento> findExpiradosPendentes() {
        return panacheRepository
                .find("status = 'PENDENTE' AND dataExpiracao < ?1 AND isActive = ?2",
                        LocalDateTime.now(), true)
                .list().stream().map(mapper::toDomain).toList();
    }
}
