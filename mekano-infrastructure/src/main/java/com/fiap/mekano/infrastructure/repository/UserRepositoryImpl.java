package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import com.fiap.mekano.infrastructure.entity.UserEntity;
import com.fiap.mekano.infrastructure.mapper.UserEntityMapper;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheResult;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
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
public class UserRepositoryImpl implements UserRepositoryPort {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("name", "email", "createdAt");

    @Inject
    UserPanacheRepository panacheRepository;

    @Inject
    UserEntityMapper mapper;

    @Override
    @Timeout(value = 5, unit = ChronoUnit.SECONDS)
    @CacheInvalidate(cacheName = CacheNames.USERS)
    public User save(User user) {
        try {
            var entity = mapper.toEntity(user);
            panacheRepository.persist(entity);
            panacheRepository.flush();
            return mapper.toDomain(entity);
        } catch (PersistenceException e) {
            throw handleConstraintViolation(e, user.getEmail().getValue());
        }
    }

    private static AppException handleConstraintViolation(PersistenceException e, String email) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException) {
                throw new AppException(409, Messages.get("user.already.exists", email));
            }
            cause = cause.getCause();
        }
        throw e;
    }

    @Override
    @Retry(maxRetries = 3)
    @CacheResult(cacheName = CacheNames.USERS)
    public Optional<User> findById(UUID id) {
        return panacheRepository.find("uuid = ?1 AND isActive = ?2", id, true)
                .firstResultOptional()
                .map(mapper::toDomain);
    }

    @Override
    @Retry(maxRetries = 3, delay = 200, delayUnit = ChronoUnit.MILLIS)
    @CacheResult(cacheName = CacheNames.USERS)
    public Optional<User> findByEmail(String email) {
        return panacheRepository.find("email = ?1 AND isActive = ?2", email, true)
                .firstResultOptional()
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return panacheRepository.count("email = ?1 AND isActive = ?2", email, true) > 0;
    }

    @Override
    public List<User> findAll(int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        if (!ALLOWED_SORT_FIELDS.contains(sortField)) {
            sortField = "name";
        }
        boolean ascending = sortParts.length < 2 || "asc".equalsIgnoreCase(sortParts[1]);
        var direction = ascending ? Sort.Direction.Ascending : Sort.Direction.Descending;
        var query = panacheRepository.find("isActive = ?1", Sort.by(sortField).direction(direction), true);
        return query.page(Page.of(page, size)).list().stream().map(mapper::toDomain).toList();
    }

    @Override
    public long countAll() {
        return panacheRepository.count("isActive = ?1", true);
    }

    @Override
    @Transactional
    @CacheInvalidate(cacheName = CacheNames.USERS)
    public void markAsDeleted(UUID id) {
        UserEntity entity = panacheRepository.find("uuid", id)
                .firstResultOptional()
                .orElseThrow(() -> new AppException(404, Messages.get("user.not.found", id)));
        entity.setDeletedAt(LocalDateTime.now());
        entity.setIsActive(false);
    }
}
