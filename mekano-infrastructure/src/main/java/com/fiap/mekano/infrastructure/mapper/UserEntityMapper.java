package com.fiap.mekano.infrastructure.mapper;

import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.infrastructure.entity.UserEntity;

public interface UserEntityMapper {

    UserEntity toEntity(User user);

    User toDomain(UserEntity entity);
}
