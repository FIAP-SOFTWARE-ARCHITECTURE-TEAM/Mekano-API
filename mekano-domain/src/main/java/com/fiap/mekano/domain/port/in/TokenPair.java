package com.fiap.mekano.domain.port.in;

public record TokenPair(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
