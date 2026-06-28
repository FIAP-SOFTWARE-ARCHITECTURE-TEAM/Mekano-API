package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.Role;
import com.fiap.mekano.domain.port.out.UserRoleRepositoryPort;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserRoleRepositoryImpl implements UserRoleRepositoryPort {

    @Inject
    UserRolePanacheRepository repository;

    @Override
    @CacheResult(cacheName = CacheNames.USER_ROLES)
    public Optional<Role> findRoleByUserUuid(@CacheKey UUID userUuid) {
        if (userUuid == null) {
            return Optional.empty();
        }

        return repository.findByUserUuid(userUuid)
                .map(entity -> entity.role);
    }
}
