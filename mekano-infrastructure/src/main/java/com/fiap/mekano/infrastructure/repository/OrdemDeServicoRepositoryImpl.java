package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import com.fiap.mekano.infrastructure.entity.OrdemDeServicoEntity;
import com.fiap.mekano.infrastructure.mapper.OrdemDeServicoEntityMapper;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;
import org.eclipse.microprofile.faulttolerance.Retry;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class OrdemDeServicoRepositoryImpl implements OrdemDeServicoRepositoryPort {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "status");

    @Inject
    OrdemDeServicoPanacheRepository panacheRepository;

    @Inject
    OrdemDeServicoEntityMapper mapper;

    @Override
    @CacheInvalidate(cacheName = CacheNames.ORDENS_DE_SERVICO)
    public OrdemDeServico save(OrdemDeServico os) {
        OrdemDeServicoEntity entity = mapper.toEntity(os);

        Optional<OrdemDeServicoEntity> existing = panacheRepository
                .find("uuid = ?1", os.getId()).firstResultOptional();

        try {
            if (existing.isPresent()) {
                OrdemDeServicoEntity managed = existing.get();
                managed.setClienteId(entity.getClienteId());
                managed.setVeiculoId(entity.getVeiculoId());
                managed.setDescricaoProblema(entity.getDescricaoProblema());
                managed.setStatus(entity.getStatus());
                managed.setMotivoCancelamento(entity.getMotivoCancelamento());
                managed.setVersion(entity.getVersion());
                panacheRepository.flush();
                return mapper.toDomain(managed);
            }

            panacheRepository.persist(entity);
            panacheRepository.flush();
            return mapper.toDomain(entity);
        } catch (OptimisticLockException e) {
            throw new AppException(409, "Conflito de versão — a OS foi atualizada por outro usuário");
        }
    }

    @Override
    @Retry(maxRetries = 3)
    @CacheResult(cacheName = CacheNames.ORDENS_DE_SERVICO)
    public Optional<OrdemDeServico> findById(UUID id) {
        return panacheRepository.find("uuid = ?1 AND isActive = ?2", id, true)
                .firstResultOptional().map(mapper::toDomain);
    }

    @Override
    public List<OrdemDeServico> findAll(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            sortField = "createdAt";
        }
        boolean ascending = sortParts.length < 2 || "asc".equalsIgnoreCase(sortParts[1]);
        var direction = ascending
                ? io.quarkus.panache.common.Sort.Direction.Ascending
                : io.quarkus.panache.common.Sort.Direction.Descending;
        var query = panacheRepository.find("isActive = ?1",
                io.quarkus.panache.common.Sort.by(sortField).direction(direction), true);
        return query.page(io.quarkus.panache.common.Page.of(page, size)).list()
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countAll() {
        return panacheRepository.count("isActive = ?1", true);
    }
}
