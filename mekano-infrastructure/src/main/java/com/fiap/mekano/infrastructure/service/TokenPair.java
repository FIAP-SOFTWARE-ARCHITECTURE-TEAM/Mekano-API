package com.fiap.mekano.infrastructure.service;

import java.time.Instant;

/**
 * Par de dados retornado pela geração de um refresh token.
 *
 * @param token     o JWT compacto (assinado)
 * @param jti       identificador único do token
 * @param expiresAt momento de expiração do token
 */
public record TokenPair(
        String token,
        String jti,
        Instant expiresAt
) {
}
