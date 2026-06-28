package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.Role;

import java.util.Optional;
import java.util.UUID;

public interface UserRoleRepositoryPort {

    Optional<Role> findRoleByUserUuid(UUID userUuid); 
   
    void save(UUID userUuid, Role role);
}
