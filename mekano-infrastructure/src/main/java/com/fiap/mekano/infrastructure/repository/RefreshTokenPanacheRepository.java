package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.RefreshTokenEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RefreshTokenPanacheRepository implements PanacheRepositoryBase<RefreshTokenEntity, Long> {

    public Optional<RefreshTokenEntity> findByTokenHashForUpdate(String tokenHash) {
        return getEntityManager()
                .createQuery(
                        "SELECT r FROM RefreshTokenEntity r WHERE r.tokenHash = :tokenHash",
                        RefreshTokenEntity.class
                )
                .setParameter("tokenHash", tokenHash)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE)
                .getResultStream()
                .findFirst();
    }

    public void deleteByUserUuid(UUID userUuid) {
        delete("userUuid", userUuid);
    }
}
