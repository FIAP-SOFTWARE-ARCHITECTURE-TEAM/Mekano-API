package com.fiap.mekano.infrastructure.rest.auth;

public record LoginRequest(
        String email,
        String password
) {
}
