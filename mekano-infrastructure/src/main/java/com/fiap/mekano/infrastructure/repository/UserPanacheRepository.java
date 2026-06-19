package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.UserEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Bean CDI Panache que expõe as operações de banco para {@link UserEntity}.
 *
 * <p>Separado de {@link UserRepositoryImpl} para evitar conflito de assinatura:
 * a PK interna é {@code Long} (auto-increment).
 * Todos os métodos Panache (persist, flush, find, count, etc.)
 * são herdados de {@code PanacheRepositoryBase} via bytecode enhancement do Quarkus.
 */
@ApplicationScoped
public class UserPanacheRepository implements PanacheRepositoryBase<UserEntity, Long> {
    // Todos os métodos Panache (persist, flush, findByIdOptional, find, count, etc.)
    // são herdados de PanacheRepositoryBase via bytecode enhancement do Quarkus.
}
