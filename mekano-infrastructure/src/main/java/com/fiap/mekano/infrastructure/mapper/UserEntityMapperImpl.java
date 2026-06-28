package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.infrastructure.entity.UserEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class UserEntityMapperImpl implements UserEntityMapper {

    @Inject
    EmailMapper emailMapper;

    @Override
    public UserEntity toEntity(User user) {
        if (user == null) {
            return null;
        }
        UserEntity entity = new UserEntity();
        entity.setUuid(user.getId());
        entity.setName(user.getName());
        entity.setEmail(emailMapper.emailToString(user.getEmail()));
        entity.setIsActive(user.isActive());
        entity.setPasswordHash(user.getPasswordHash());
        entity.setCreatedAt(user.getCreatedAt());
        return entity;
    }

    @Override
    public User toDomain(UserEntity entity) {
        if (entity == null) {
            return null;
        }
        return User.reconstitute(
                entity.getUuid(),
                entity.getName(),
                entity.getEmail(),
                entity.getIsActive(),
                entity.getPasswordHash(),
                entity.getCreatedAt()
        );
    }
}
