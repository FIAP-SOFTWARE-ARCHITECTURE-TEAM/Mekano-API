package com.fiap.mekano.rest.dto.admin;

import com.fiap.mekano.domain.model.Role;
import com.fiap.mekano.domain.port.in.AdminCreatedUser;

import java.util.UUID;

public record AdminCreateUserResponse(
        UUID id,
        String name,
        String email,
        Role role,
        String senhaGerada
) {

    public static AdminCreateUserResponse from(AdminCreatedUser user) {
        return new AdminCreateUserResponse(
                user.id(),
                user.name(),
                user.email(),
                user.role(),
                user.generatedPassword()
        );
    }
}