package com.fiap.mekano.domain.port.in;

public record LoginCommand(
        String email,
        String password
) {
}
