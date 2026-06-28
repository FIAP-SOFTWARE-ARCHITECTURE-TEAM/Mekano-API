package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.Role;
import com.fiap.mekano.domain.port.out.UserRoleRepositoryPort;
import com.fiap.mekano.infrastructure.cache.CacheNames;
import com.fiap.mekano.infrastructure.entity.UserRoleEntity;

import io.quarkus.cache.CacheKey;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
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

    @Override
    @Transactional
    @CacheInvalidateAll(cacheName = CacheNames.USER_ROLES)
    public void save(UUID userUuid, Role role) {
        boolean alreadyExists = repository
                .count("userUuid = ?1 and role = ?2 and isActive = true", userUuid, role) > 0;

        if (alreadyExists) {
            return;
        }

        UserRoleEntity entity = new UserRoleEntity();  
        entity.uuid = UUID.randomUUID();
        entity.userUuid = userUuid;
        entity.role = role;        
        entity.setIsActive(true);
        entity.setCreatedAt(LocalDateTime.now());

        repository.persist(entity);
    }
}
