package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.RefreshTokenEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Bean CDI Panache que expõe as operações de banco para {@link RefreshTokenEntity}.
 *
 * <p>Separado de {@link RefreshTokenRepositoryImpl} para evitar conflito de assinatura
 * (mesmo padrão de {@link UserPanacheRepository} / {@link UserRepositoryImpl}).
 * A PK interna é {@code Long} (auto-increment).
 */
@ApplicationScoped
public class RefreshTokenPanacheRepository implements PanacheRepositoryBase<RefreshTokenEntity, Long> {
    // Herdados de PanacheRepositoryBase via bytecode enhancement.
}
