package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.port.out.RefreshTokenData;
import com.fiap.mekano.domain.port.out.RefreshTokenRepositoryPort;
import com.fiap.mekano.infrastructure.entity.RefreshTokenEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementação concreta de {@link RefreshTokenRepositoryPort} usando Quarkus Panache.
 *
 * <p>Segue o mesmo padrão de {@link UserRepositoryImpl}: delega operações Panache para
 * {@link RefreshTokenPanacheRepository} e implementa apenas o contrato de domínio.
 *
 * <p>Operações de escrita ({@code save}, {@code invalidate}, {@code deleteExpired}) são
 * {@code @Transactional} — mesmo padrão de {@code UserRepositoryImpl.save()}.
 */
@ApplicationScoped
public class RefreshTokenRepositoryImpl implements RefreshTokenRepositoryPort {

    @Inject
    RefreshTokenPanacheRepository panacheRepository;

    @Override
    @Transactional
    public void save(String jti, String tokenHash, UUID userId, Instant expiresAt) {
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setId(UUID.randomUUID());
        entity.setJti(jti);
        entity.setTokenHash(tokenHash);
        entity.setUserId(userId);
        entity.setExpiresAt(expiresAt);
        entity.setCreatedAt(Instant.now());
        panacheRepository.persist(entity);
    }

    @Override
    public Optional<RefreshTokenData> findByJti(String jti) {
        return panacheRepository.find("jti", jti).firstResultOptional()
                .map(e -> new RefreshTokenData(e.getJti(), e.getTokenHash(), e.getUserId(),
                        e.getExpiresAt(), e.getRotatedAt(), e.getCreatedAt()));
    }

    @Override
    @Transactional
    public void invalidate(String jti) {
        panacheRepository.update("rotatedAt = ?1 WHERE jti = ?2", Instant.now(), jti);
    }

    @Override
    @Transactional
    public void deleteExpired() {
        panacheRepository.delete("expiresAt < ?1", Instant.now());
    }
}
