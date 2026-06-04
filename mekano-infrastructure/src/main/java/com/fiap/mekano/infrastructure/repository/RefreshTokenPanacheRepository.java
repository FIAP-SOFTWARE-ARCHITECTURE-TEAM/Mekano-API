package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.RefreshTokenEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

/**
 * Bean CDI Panache que expõe as operações de banco para {@link RefreshTokenEntity}.
 *
 * <p>Separado de {@link RefreshTokenRepositoryImpl} para evitar conflito de assinatura
 * (mesmo padrão de {@link UserPanacheRepository} / {@link UserRepositoryImpl}).
 *
 * <p>Todos os métodos Panache (persist, flush, findByIdOptional, find, delete, etc.)
 * são herdados de {@code PanacheRepositoryBase} via bytecode enhancement do Quarkus.
 */
@ApplicationScoped
public class RefreshTokenPanacheRepository implements PanacheRepositoryBase<RefreshTokenEntity, UUID> {
    // Herdados de PanacheRepositoryBase via bytecode enhancement.
}
