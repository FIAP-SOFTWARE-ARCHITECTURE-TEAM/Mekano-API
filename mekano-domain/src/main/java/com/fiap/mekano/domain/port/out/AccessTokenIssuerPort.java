package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.Role;

import java.util.UUID;

public interface AccessTokenIssuerPort {

    String issue(UUID userUuid, String name, Role role);
}