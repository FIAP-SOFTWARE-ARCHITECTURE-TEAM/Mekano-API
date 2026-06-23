package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import com.fiap.mekano.infrastructure.entity.ClienteEntity;
import com.fiap.mekano.infrastructure.mapper.ClienteEntityMapper;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.hibernate.exception.ConstraintViolationException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class ClienteRepositoryImpl implements ClienteRepositoryPort {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("nome", "email", "cpf", "createdAt");

    @Inject
    ClientePanacheRepository panacheRepository;

    @Inject
    ClienteEntityMapper mapper;

    @Override
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CacheInvalidate(cacheName = CacheNames.CLIENTES)
    public Cliente save(Cliente cliente) {
        try {
            ClienteEntity entity = mapper.toEntity(cliente);
            panacheRepository.persist(entity);
            panacheRepository.flush();
            return mapper.toDomain(entity);
        } catch (PersistenceException e) {
            throw handleConstraintViolation(e, cliente.getCpf().getValue());
        }
    }

    private static AppException handleConstraintViolation(PersistenceException e, String cpf) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException) {
                throw new AppException(409, Messages.get("cliente.already.exists", cpf));
            }
            cause = cause.getCause();
        }
        throw e;
    }

    @Override
    @Retry(maxRetries = 3)
    @CacheResult(cacheName = CacheNames.CLIENTES)
    public Optional<Cliente> findById(UUID id) {
        return panacheRepository.find("uuid = ?1 AND isActive = ?2", id, true)
                .firstResultOptional().map(mapper::toDomain);
    }

    @Override
    @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
    public Optional<Cliente> findByCpf(String cpf) {
        return panacheRepository.find("cpf = ?1 AND isActive = ?2", cpf, true)
                .firstResultOptional().map(mapper::toDomain);
    }

    @Override
    public boolean existsByCpf(String cpf) {
        return panacheRepository.count("cpf = ?1 AND isActive = ?2", cpf, true) > 0;
    }

    @Override
    public List<Cliente> findAll(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            sortField = "nome";
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

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.CLIENTES)
    public void markAsDeleted(UUID id) {
        ClienteEntity entity = panacheRepository.find("uuid", id).firstResultOptional()
                .orElseThrow(() -> new AppException(404, Messages.get("cliente.not.found", id)));
        entity.setDeletedAt(LocalDateTime.now());
        entity.setIsActive(false);
    }
}
