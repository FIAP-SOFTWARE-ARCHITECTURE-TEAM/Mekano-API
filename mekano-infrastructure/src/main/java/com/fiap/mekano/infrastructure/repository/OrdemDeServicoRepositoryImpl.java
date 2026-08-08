package com.fiap.mekano.infrastructure.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.microprofile.faulttolerance.Retry;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import com.fiap.mekano.infrastructure.entity.OrdemDeServicoEntity;
import com.fiap.mekano.infrastructure.mapper.OrdemDeServicoEntityMapper;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.OptimisticLockException;

@ApplicationScoped
public class OrdemDeServicoRepositoryImpl implements OrdemDeServicoRepositoryPort {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("createdAt", "status", "descricaoProblema");

    @Inject
    OrdemDeServicoPanacheRepository panacheRepository;

    @Inject
    OrdemDeServicoEntityMapper mapper;

    @Override
    @CacheInvalidate(cacheName = CacheNames.ORDENS_SERVICO)
    public OrdemDeServico save(OrdemDeServico os) {
        OrdemDeServicoEntity entity = mapper.toEntity(os);

        Optional<OrdemDeServicoEntity> existing = panacheRepository
                .find("uuid = ?1", os.getId()).firstResultOptional();

        try {
            if (existing.isPresent()) {
                OrdemDeServicoEntity managed = existing.get();
                managed.setClienteUuid(entity.getClienteUuid());
                managed.setVeiculoUuid(entity.getVeiculoUuid());
                managed.setDescricaoProblema(entity.getDescricaoProblema());
                managed.setStatus(entity.getStatus());
                managed.setMotivoCancelamento(entity.getMotivoCancelamento());
                managed.setVersion(entity.getVersion());
                managed.setOrcamentoUuid(entity.getOrcamentoUuid());
                managed.setMecanicoUuid(entity.getMecanicoUuid());
                managed.setExecucaoIniciadaEm(entity.getExecucaoIniciadaEm());
                managed.setExecucaoFinalizadaEm(entity.getExecucaoFinalizadaEm());
                managed.setObservacaoExecucao(entity.getObservacaoExecucao());
                managed.setDataAprovacao(entity.getDataAprovacao());
                managed.setStatusPagamento(entity.getStatusPagamento());
                managed.setStatusEntrega(entity.getStatusEntrega());
                managed.setDataPagamento(entity.getDataPagamento());
                managed.setFormaPagamento(entity.getFormaPagamento());
                managed.setValorPago(entity.getValorPago());
                managed.setDataEntrega(entity.getDataEntrega());
                managed.setEnderecoEntrega(entity.getEnderecoEntrega());
                managed.setCobrancaGeradaEm(entity.getCobrancaGeradaEm());
                managed.setPagamentoConfirmadoEm(entity.getPagamentoConfirmadoEm());
                managed.setReferenciaPagamento(entity.getReferenciaPagamento());
                managed.setEntregueEm(entity.getEntregueEm());
                managed.setRecebidoPor(entity.getRecebidoPor());
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
        boolean ascending = sortParts.length < 2 || "asc".equalsIgnoreCase(sortParts[1].strip());
        var direction = ascending ? Sort.Direction.Ascending : Sort.Direction.Descending;

        return panacheRepository.find("isActive = ?1", Sort.by(sortField).direction(direction), true)
                .page(Page.of(page, size)).list()
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countAll() {
        return panacheRepository.count("isActive = ?1", true);
    }

    @Override
    public List<OrdemDeServico> findAllWithFilters(String status, UUID clienteUuid, UUID veiculoUuid,
                                                    LocalDateTime dataInicio, LocalDateTime dataFim,
                                                    int page, int size) {
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
        if (veiculoUuid != null) {
            query.append(" AND veiculoUuid = :veiculoUuid");
            params.put("veiculoUuid", veiculoUuid);
        }
        if (dataInicio != null) {
            query.append(" AND createdAt >= :dataInicio");
            params.put("dataInicio", dataInicio);
        }
        if (dataFim != null) {
            query.append(" AND createdAt <= :dataFim");
            params.put("dataFim", dataFim);
        }

        return panacheRepository.find(query.toString(), Sort.by("createdAt").descending(), params)
                .page(Page.of(page, size)).list()
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<OrdemDeServico> findByIdWithItems(UUID id) {
        // Por enquanto equivalente ao findById — será expandido quando houver itens de OS
        return findById(id);
    }

    @Override
    public Optional<Double> calcularTempoMedioExecucao(LocalDateTime dataInicio, LocalDateTime dataFim) {
        StringBuilder query = new StringBuilder("status = ?1 AND isActive = ?2");
        List<Object> params = new java.util.ArrayList<>();
        params.add("FINALIZADA");
        params.add(true);

        if (dataInicio != null) {
            query.append(" AND execucaoFinalizadaEm >= ?").append(params.size() + 1);
            params.add(dataInicio);
        }
        if (dataFim != null) {
            query.append(" AND execucaoFinalizadaEm <= ?").append(params.size() + 1);
            params.add(dataFim);
        }

        List<OrdemDeServicoEntity> finalizadas = panacheRepository
                .find(query.toString(), params.toArray()).list();

        if (finalizadas.isEmpty()) {
            return Optional.empty();
        }

        double media = finalizadas.stream()
                .filter(e -> e.getExecucaoIniciadaEm() != null && e.getExecucaoFinalizadaEm() != null)
                .mapToLong(e -> ChronoUnit.HOURS.between(e.getExecucaoIniciadaEm(), e.getExecucaoFinalizadaEm()))
                .average()
                .orElse(0.0);

        return Optional.of(media);
    }

    @Override
    public Map<UUID, Double> calcularTempoMedioPorMecanico(LocalDateTime dataInicio, LocalDateTime dataFim) {
        StringBuilder query = new StringBuilder("status = ?1 AND isActive = ?2 AND mecanicoUuid IS NOT NULL");
        List<Object> params = new java.util.ArrayList<>();
        params.add("FINALIZADA");
        params.add(true);

        if (dataInicio != null) {
            query.append(" AND execucaoFinalizadaEm >= ?").append(params.size() + 1);
            params.add(dataInicio);
        }
        if (dataFim != null) {
            query.append(" AND execucaoFinalizadaEm <= ?").append(params.size() + 1);
            params.add(dataFim);
        }

        List<OrdemDeServicoEntity> finalizadas = panacheRepository
                .find(query.toString(), params.toArray()).list();

        return finalizadas.stream()
                .filter(e -> e.getMecanicoUuid() != null
                        && e.getExecucaoIniciadaEm() != null
                        && e.getExecucaoFinalizadaEm() != null)
                .collect(Collectors.groupingBy(
                        OrdemDeServicoEntity::getMecanicoUuid,
                        LinkedHashMap::new,
                        Collectors.averagingDouble(e ->
                                ChronoUnit.HOURS.between(e.getExecucaoIniciadaEm(), e.getExecucaoFinalizadaEm()))));
    }

    @Override
    @CacheInvalidate(cacheName = CacheNames.ORDENS_SERVICO)
    public void markAsDeleted(UUID id) {
        panacheRepository.find("uuid = ?1", id).firstResultOptional().ifPresent(entity -> {
            entity.setIsActive(false);
            entity.setDeletedAt(LocalDateTime.now());
            panacheRepository.flush();
        });
    }

    @Override
    public boolean existsByClienteUuidAndStatusIn(UUID clienteUuid, List<String> statuses) {
        return panacheRepository
                .find("clienteUuid = ?1 AND status IN (?2) AND isActive = ?3",
                        clienteUuid, statuses, true)
                .count() > 0;
    }

    @Override
    public Optional<UUID> findOrcamentoUuidByOsId(UUID osId) {
        return panacheRepository.find("uuid = ?1 AND isActive = ?2", osId, true)
                .firstResultOptional()
                .map(OrdemDeServicoEntity::getOrcamentoUuid);
    }
}
