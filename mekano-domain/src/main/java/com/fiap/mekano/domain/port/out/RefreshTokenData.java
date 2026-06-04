package com.fiap.mekano.domain.port.out;

import java.time.Instant;
import java.util.UUID;

/**
 * Dados de um refresh token recuperado do banco.
 * Imutável — representa o estado persistido.
 */
public record RefreshTokenData(
    String jti,
    String tokenHash,
    UUID userId,
    Instant expiresAt,
    Instant rotatedAt,
    Instant createdAt
) {
    public boolean isRotated() { return rotatedAt != null; }
    public boolean isExpired(Instant now) { return now.isAfter(expiresAt); }
}
