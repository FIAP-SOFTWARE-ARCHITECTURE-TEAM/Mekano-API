package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.Role;

import java.time.Instant;
import java.util.UUID;

public record RefreshTokenData(
        Long id,
        String jti,
        String tokenHash,
        UUID userUuid,
        Role role,
        Instant expiresAt,
        Instant rotatedAt
) {
    public boolean isExpired(Instant now) {
        return expiresAt != null && expiresAt.isBefore(now);
    }

    public boolean isRotated() {
        return rotatedAt != null;
    }

    public RefreshTokenData rotated(Instant rotatedAt) {
        return new RefreshTokenData(
                id,
                jti,
                tokenHash,
                userUuid,
                role,
                expiresAt,
                rotatedAt
        );
    }
}
