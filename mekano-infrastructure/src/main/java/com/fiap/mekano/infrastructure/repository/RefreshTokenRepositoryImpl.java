package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.port.out.RefreshTokenData;
import com.fiap.mekano.domain.port.out.RefreshTokenRepositoryPort;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import com.fiap.mekano.infrastructure.entity.RefreshTokenEntity;
import io.quarkus.cache.CacheInvalidate;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RefreshTokenRepositoryImpl implements RefreshTokenRepositoryPort {

    @Inject
    RefreshTokenPanacheRepository repository;

    @Override
    public Optional<RefreshTokenData> findByTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.isBlank()) {
            return Optional.empty();
        }

        return repository.findByTokenHashForUpdate(tokenHash)
                .map(this::toData);
    }

    @Override
    @CacheInvalidate(cacheName = CacheNames.REFRESH_TOKENS)
    public RefreshTokenData save(RefreshTokenData data) {
        RefreshTokenEntity entity;

        if (data.id() == null) {
            entity = new RefreshTokenEntity();
        } else {
            entity = repository.findById(data.id());
            if (entity == null) {
                entity = new RefreshTokenEntity();
                entity.id = data.id();
            }
        }

        entity.jti = data.jti();
        entity.tokenHash = data.tokenHash();
        entity.userUuid = data.userUuid();
        entity.role = data.role();
        entity.expiresAt = data.expiresAt();
        entity.rotatedAt = data.rotatedAt();

        if (entity.id == null) {
            repository.persist(entity);
            repository.flush();
            return toData(entity);
        }

        RefreshTokenEntity merged = repository.getEntityManager().merge(entity);
        repository.flush();

        return toData(merged);
    }

    @Override
    @CacheInvalidate(cacheName = CacheNames.REFRESH_TOKENS)
    public void deleteByUser(UUID userUuid) {
        if (userUuid != null) {
            repository.deleteByUserUuid(userUuid);
        }
    }

    private RefreshTokenData toData(RefreshTokenEntity entity) {
        return new RefreshTokenData(
                entity.id,
                entity.jti,
                entity.tokenHash,
                entity.userUuid,
                entity.role,
                entity.expiresAt,
                entity.rotatedAt
        );
    }
}
