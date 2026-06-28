package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.Role;

public record CreateAdminUserCommand(
        String name,
        String email,
        Role role
) {
}