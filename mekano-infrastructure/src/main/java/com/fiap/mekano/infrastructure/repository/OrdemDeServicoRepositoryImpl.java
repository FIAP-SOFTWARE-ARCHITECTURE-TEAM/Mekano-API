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
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Parameters;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class OrdemDeServicoRepositoryImpl implements OrdemDeServicoRepositoryPort {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("status", "createdAt", "descricaoProblema");

    @Inject
    OrdemDeServicoPanacheRepository panacheRepository;

    @Inject
    OrdemDeServicoEntityMapper mapper;

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.ORDENS_SERVICO)
    public OrdemDeServico save(OrdemDeServico ordemDeServico) {
        OrdemDeServicoEntity entity = mapper.toEntity(ordemDeServico);

        Optional<OrdemDeServicoEntity> existing = panacheRepository
                .find("uuid = ?1", ordemDeServico.getId()).firstResultOptional();

        if (existing.isPresent()) {
            OrdemDeServicoEntity managed = existing.get();
            managed.setStatus(entity.getStatus());
            managed.setMotivoCancelamento(entity.getMotivoCancelamento());
            managed.setOrcamentoUuid(entity.getOrcamentoUuid());
            managed.setMecanicoUuid(entity.getMecanicoUuid());
            managed.setExecucaoIniciadaEm(entity.getExecucaoIniciadaEm());
            managed.setExecucaoFinalizadaEm(entity.getExecucaoFinalizadaEm());
            managed.setObservacaoExecucao(entity.getObservacaoExecucao());
            managed.setDataAprovacao(entity.getDataAprovacao());
            panacheRepository.flush();
            return mapper.toDomain(managed);
        }

        panacheRepository.persist(entity);
        panacheRepository.flush();
        return mapper.toDomain(entity);
    }

    @Override
    @CacheResult(cacheName = CacheNames.ORDENS_SERVICO)
    public Optional<OrdemDeServico> findById(UUID id) {
        return panacheRepository.find("uuid = ?1 AND isActive = ?2", id, true)
                .firstResultOptional().map(mapper::toDomain);
    }

    @Override
    public List<OrdemDeServico> findAll(int page, int size, String sort) {
        String sortValue = sort == null || sort.isBlank() ? "createdAt,desc" : sort;
        String[] sortParts = sortValue.split(",", 2);
        String sortField = sortParts[0].strip();
        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            sortField = "createdAt";
        }
        boolean ascending = sortParts.length < 2 || !"desc".equalsIgnoreCase(sortParts[1].strip());
        var direction = ascending ? Sort.Direction.Ascending : Sort.Direction.Descending;
        var query = panacheRepository.find("isActive = ?1",
                Sort.by(sortField).direction(direction), true);
        return query.page(Page.of(Math.max(page, 0), normalizeSize(size))).list()
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countAll() {
        return panacheRepository.count("isActive = ?1", true);
    }

    @Override
    public List<OrdemDeServico> findAllWithFilters(String status, UUID clienteUuid, int page, int size) {
        StringBuilder query = new StringBuilder("isActive = :active");
        Map<String, Object> params = new HashMap<>();
        params.put("active", true);

        if (status != null && !status.isBlank()) {
            query.append(" AND status = :status");
            params.put("status", status);
        }
        if (clienteUuid != null) {
            query.append(" AND clienteUuid = :clienteUuid");
            params.put("clienteUuid", clienteUuid);
        }

        return panacheRepository
                .find(query.toString(), Sort.by("createdAt").descending(), params)
                .page(Page.of(Math.max(page, 0), normalizeSize(size)))
                .list().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<OrdemDeServico> findByIdWithItems(UUID id) {
        return findById(id);
    }

    @Override
    public Optional<Double> calcularTempoMedioExecucao() {
        Double result = panacheRepository
                .find("status = 'FINALIZADA' OR status = 'ENTREGUE'")
                .stream()
                .map(e -> {
                    if (e.getExecucaoIniciadaEm() != null && e.getExecucaoFinalizadaEm() != null) {
                        return (double) ChronoUnit.HOURS.between(e.getExecucaoIniciadaEm(), e.getExecucaoFinalizadaEm());
                    }
                    return null;
                })
                .filter(d -> d != null)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
        return result > 0 ? Optional.of(result) : Optional.empty();
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.ORDENS_SERVICO)
    public void markAsDeleted(UUID id) {
        OrdemDeServicoEntity entity = panacheRepository.find("uuid", id).firstResultOptional()
                .orElseThrow(() -> new AppException(404, Messages.get("os.not.found", id)));
        entity.setDeletedAt(LocalDateTime.now());
        entity.setIsActive(false);
    }

    private static int normalizeSize(int size) {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 100);
    }
}
