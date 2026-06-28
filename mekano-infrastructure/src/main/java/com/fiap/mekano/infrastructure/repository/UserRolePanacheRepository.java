package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.UserRoleEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRolePanacheRepository implements PanacheRepositoryBase<UserRoleEntity, Long> {

    public Optional<UserRoleEntity> findByUserUuid(UUID userUuid) {
        return find("userUuid", userUuid).firstResultOptional();
    }
}
