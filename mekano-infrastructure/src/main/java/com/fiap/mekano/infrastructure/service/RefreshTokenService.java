package com.fiap.mekano.infrastructure.service;

import com.fiap.mekano.domain.exception.InvalidRefreshTokenException;
import com.fiap.mekano.domain.port.out.RefreshTokenData;
import com.fiap.mekano.domain.port.out.RefreshTokenRepositoryPort;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Serviço de infraestrutura para geração, validação e rotação de refresh tokens.
 *
 * <p>Gera JWTs assinados com claims mínimos (jti, sub, exp), armazena o hash SHA-256
 * do token no banco e implementa rotação completa: ao usar um token existente, o anterior
 * é invalidado (rotatedAt) e um novo par é gerado.
 *
 * <p>Este serviço não expõe endpoints públicos — é consumido internamente pelo fluxo
 * de autenticação (v3).
 */
@ApplicationScoped
public class RefreshTokenService {

    @Inject
    RefreshTokenRepositoryPort refreshTokenRepository;

    @ConfigProperty(name = "mekano.auth.refresh.expiration-hours", defaultValue = "24")
    long expirationHours;

    @ConfigProperty(name = "mp.jwt.verify.issuer", defaultValue = "https://mekano.fiap.com.br/refresh")
    String issuer;

    /**
     * Gera um novo par de refresh token: JWT assinado + hash armazenado no banco.
     *
     * @param userId UUID do usuário proprietário
     * @return TokenPair com o token JWT, jti e expiresAt
     */
    public TokenPair generateTokens(UUID userId) {
        String jti = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plus(Duration.ofHours(expirationHours));

        String token = Jwt.issuer(issuer)
                .subject(userId.toString())
                .claim("jti", jti)
                .expiresIn(Duration.ofHours(expirationHours))
                .sign();

        String tokenHash = sha256(token);
        refreshTokenRepository.save(jti, tokenHash, userId, expiresAt);

        return new TokenPair(token, jti, expiresAt);
    }

    /**
     * Valida um refresh token e o rotaciona (invalida o anterior, gera novo).
     *
     * <p>Extrai o jti do payload do JWT (sem verificar assinatura — o token é validado
     * por hash SHA-256), busca o registro no banco, verifica se não foi rotacionado
     * nem expirou, recalcula o hash e compara.
     *
     * @param refreshToken o JWT do refresh token a validar e rotacionar
     * @return os dados do token antigo (para referência)
     * @throws InvalidRefreshTokenException se o token for inválido, expirado ou já rotacionado
     */
    public RefreshTokenData validateAndRotate(String refreshToken) {
        // Extrai jti do payload JWT (sem verificar assinatura)
        String jti = extractJti(refreshToken);

        // Busca no banco
        RefreshTokenData stored = refreshTokenRepository.findByJti(jti)
                .orElseThrow(() -> new InvalidRefreshTokenException("Refresh token not found"));

        // Verifica se já foi rotacionado (token reuse attack)
        if (stored.isRotated()) {
            throw new InvalidRefreshTokenException("Refresh token already used (rotation)");
        }

        // Verifica expiração
        if (stored.isExpired(Instant.now())) {
            throw new InvalidRefreshTokenException("Refresh token expired");
        }

        // Recalcula hash e compara
        String computedHash = sha256(refreshToken);
        if (!computedHash.equalsIgnoreCase(stored.tokenHash())) {
            throw new InvalidRefreshTokenException("Refresh token hash mismatch");
        }

        // Invalida o token antigo (rotação)
        refreshTokenRepository.invalidate(jti);

        // Gera novo par (chamada recursiva para gerar e persistir)
        generateTokens(stored.userId());

        return stored;
    }

    /**
     * Extrai o claim {@code jti} do payload de um JWT sem verificar assinatura.
     *
     * @param token JWT compacto (header.payload.signature)
     * @return valor do claim jti
     */
    private String extractJti(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new InvalidRefreshTokenException("Invalid JWT format");
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            String json = new String(payload, StandardCharsets.UTF_8);
            // Extrai o valor de "jti" do JSON — parsing simples sem dependência
            String searchKey = "\"jti\":\"";
            int start = json.indexOf(searchKey);
            if (start < 0) {
                throw new InvalidRefreshTokenException("JWT missing jti claim");
            }
            start += searchKey.length();
            int end = json.indexOf("\"", start);
            return json.substring(start, end);
        } catch (InvalidRefreshTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidRefreshTokenException("Failed to parse JWT payload");
        }
    }

    /**
     * Calcula o hash SHA-256 de uma string e retorna em hexadecimal minúsculo.
     */
    private String sha256(String token) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
