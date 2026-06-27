package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.Servico;
import com.fiap.mekano.domain.port.out.ServicoRepositoryPort;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import com.fiap.mekano.infrastructure.entity.ServicoEntity;
import com.fiap.mekano.infrastructure.mapper.ServicoEntityMapper;
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

/**
 * Implementação concreta de {@link ServicoRepositoryPort} usando Quarkus Panache.
 *
 * <p>Segue o mesmo padrão do {@link UserRepositoryImpl}: duas classes para evitar
 * conflito de assinatura Panache.
 */
@ApplicationScoped
public class ServicoRepositoryImpl implements ServicoRepositoryPort {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("nome", "valor", "createdAt");

    @Inject
    ServicoPanacheRepository panacheRepository;

    @Inject
    ServicoEntityMapper mapper;

    @Override
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CacheInvalidate(cacheName = CacheNames.SERVICOS)
    public Servico save(Servico servico) {
        try {
            ServicoEntity entity = mapper.toEntity(servico);

            // Verifica se é update (entidade já persistida)
            Optional<ServicoEntity> existing = panacheRepository
                    .find("uuid = ?1", servico.getId()).firstResultOptional();

            if (existing.isPresent()) {
                ServicoEntity managed = existing.get();
                managed.setNome(entity.getNome());
                managed.setDescricao(entity.getDescricao());
                managed.setValor(entity.getValor());
                panacheRepository.flush();
                return mapper.toDomain(managed);
            }

            panacheRepository.persist(entity);
            panacheRepository.flush();
            return mapper.toDomain(entity);
        } catch (PersistenceException e) {
            throw handleConstraintViolation(e, servico.getNome());
        }
    }

    private static AppException handleConstraintViolation(PersistenceException e, String nome) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException) {
                throw new AppException(409, Messages.get("servico.already.exists", nome));
            }
            cause = cause.getCause();
        }
        throw e;
    }

    @Override
    @Retry(maxRetries = 3)
    @CacheResult(cacheName = CacheNames.SERVICOS)
    public Optional<Servico> findById(UUID id) {
        return panacheRepository.find("uuid = ?1 AND isActive = ?2", id, true)
                .firstResultOptional().map(mapper::toDomain);
    }

    @Override
    public boolean existsByNome(String nome) {
        return panacheRepository.count("nome = ?1 AND isActive = ?2", nome, true) > 0;
    }

    @Override
    public boolean existsByNomeAndIdNot(String nome, UUID id) {
        return panacheRepository.count("nome = ?1 AND uuid != ?2 AND isActive = ?3", nome, id, true) > 0;
    }

    @Override
    public List<Servico> findAll(int page, int size, String sort) {
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
    @CacheInvalidate(cacheName = CacheNames.SERVICOS)
    public void markAsDeleted(UUID id) {
        ServicoEntity entity = panacheRepository.find("uuid", id).firstResultOptional()
                .orElseThrow(() -> new AppException(404, Messages.get("servico.not.found", id)));
        entity.setDeletedAt(LocalDateTime.now());
        entity.setIsActive(false);
    }
}
