package com.fiap.mekano.domain.port.out;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Output port — contrato de persistência de refresh tokens.
 *
 * Operações: armazenar novo token, buscar por jti, invalidar (rotação).
 * O domínio não conhece JPA, Panache ou algoritmo de hash.
 */
public interface RefreshTokenRepositoryPort {

    /**
     * Armazena um novo refresh token.
     *
     * @param jti       identificador único do token (UUID v4)
     * @param tokenHash SHA-256 do token em hex
     * @param userId    UUID do usuário proprietário
     * @param expiresAt momento em que o token expira
     */
    void save(String jti, String tokenHash, UUID userId, Instant expiresAt);

    /**
     * Busca token armazenado pelo jti.
     *
     * @param jti identificador único do token
     * @return RefreshTokenData se encontrado e não rotacionado, ou empty
     */
    Optional<RefreshTokenData> findByJti(String jti);

    /**
     * Invalida o token — marca rotated_at com o timestamp atual.
     *
     * @param jti identificador do token a invalidar
     */
    void invalidate(String jti);

    /**
     * Remove tokens expirados (cleanup).
     */
    void deleteExpired();
}
