package com.fiap.mekano.infrastructure.repository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import com.fiap.mekano.infrastructure.entity.VeiculoEntity;
import com.fiap.mekano.infrastructure.mapper.VeiculoEntityMapper;

import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class VeiculoRepositoryImpl
        implements VeiculoRepositoryPort {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "placa",
            "marca",
            "modelo",
            "ano",
            "createdAt");

    @Inject
    VeiculoPanacheRepository panacheRepository;

    @Inject
    VeiculoEntityMapper mapper;

    @Override
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CacheInvalidate(cacheName = CacheNames.VEHICLES)
    public Veiculo save(Veiculo veiculo) {

        Optional<VeiculoEntity> existing = panacheRepository.find(
                "uuid",
                veiculo.getId())
                .firstResultOptional();

        VeiculoEntity entity;

        /** Se for Update */
        if (existing.isPresent()) {
            entity = existing.get();

            entity.setClienteUuid(
                    veiculo.getClienteUuid());

            entity.setPlaca(
                    veiculo.getPlaca().getValue());

            entity.setMarca(
                    veiculo.getMarca());

            entity.setModelo(
                    veiculo.getModelo());

            entity.setAno(
                    veiculo.getAno());

            /** Se for Create */
        } else {
            entity = mapper.toEntity(veiculo);
            panacheRepository.persist(entity);
        }

        panacheRepository.flush();

        return mapper.toDomain(entity);
    }

    @Override
    @Retry(maxRetries = 3)
    @CacheResult(cacheName = CacheNames.VEHICLES)
    public Optional<Veiculo> findById(UUID id) {
        return panacheRepository
                .find(
                        "uuid = ?1 AND isActive = ?2",
                        id,
                        true)
                .firstResultOptional()
                .map(mapper::toDomain);
    }

    @Override
    @Retry(maxRetries = 3)
    @CacheResult(cacheName = CacheNames.VEHICLES)
    public Optional<Veiculo> findByPlaca(String placa) {
        return panacheRepository
                .find(
                        "placa = ?1 AND isActive = ?2",
                        placa,
                        true)
                .firstResultOptional()
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByPlaca(String placa) {
        return panacheRepository.count(
                "placa = ?1 AND isActive = ?2",
                placa,
                true) > 0;
    }

    /**
     * Retorna todos os veículos ativos de forma paginada e ordenada.
     *
     * <p>
     * Usa {@code Sort.by()} do Panache para ordenação no banco.
     * Filtra {@code isActive = true} para excluir registros deletados logicamente.
     *
     * @param page número da página (0-based)
     * @param size tamanho da página
     * @param sort campo e direção (ex: "placa,asc")
     * @return lista de veículos da página
     */
    @Override
    public List<Veiculo> findAll(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            sortField = "placa";
        }
        boolean ascending = sortParts.length < 2 || "asc".equalsIgnoreCase(sortParts[1]);
        var direction = ascending ? io.quarkus.panache.common.Sort.Direction.Ascending
                : io.quarkus.panache.common.Sort.Direction.Descending;
        var query = panacheRepository.find("isActive = ?1",
                io.quarkus.panache.common.Sort.by(sortField).direction(direction), true);
        return query.page(io.quarkus.panache.common.Page.of(page, size)).list()
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countAll() {
        return panacheRepository.count(
                "isActive = ?1",
                true);
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.VEHICLES)
    public void markAsDeleted(UUID id) {

        VeiculoEntity entity = panacheRepository
                .find("uuid", id)
                .firstResultOptional()
                .orElseThrow(
                        () -> new AppException(
                                404,
                                "Veículo não encontrado"));

        entity.setDeletedAt(LocalDateTime.now());
        entity.setIsActive(false);
    }
}
