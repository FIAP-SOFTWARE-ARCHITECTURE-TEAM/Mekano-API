package com.fiap.mekano.domain.port.in;

import com.fiap.mekano.domain.model.Role;

import java.util.UUID;

public record AdminCreatedUser(
        UUID id,
        String name,
        String email,
        Role role,
        String generatedPassword
) {
}