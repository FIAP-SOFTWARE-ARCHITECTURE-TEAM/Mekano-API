package com.fiap.mekano.application.service.auth;

import com.fiap.mekano.domain.model.Role;
import com.fiap.mekano.domain.port.out.RefreshTokenData;
import com.fiap.mekano.domain.port.out.RefreshTokenRepositoryPort;
import com.fiap.mekano.shared.exception.AppException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;

@ApplicationScoped
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final long REFRESH_TOKEN_DAYS = 30;

    @Inject
    RefreshTokenRepositoryPort repository;

    @Transactional
    public String createToken(UUID userUuid, Role role) {
        String jti = randomBase64Url(24);
        String secret = randomBase64Url(48);
        String plainToken = jti + ":" + secret;

        RefreshTokenData data = new RefreshTokenData(
                null,
                jti,
                sha256(plainToken),
                userUuid,
                role,
                Instant.now().plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS),
                null
        );

        repository.save(data);

        return plainToken;
    }

    @Transactional
    public RotatedRefreshToken rotate(String tokenHash) {
        Instant now = Instant.now();

        RefreshTokenData current = repository.findByTokenHash(tokenHash)
                .orElseThrow(this::unauthorized);

        if (current.isRotated() || current.isExpired(now)) {
            throw unauthorized();
        }

        repository.save(current.rotated(now));

        String newRefreshToken = createToken(current.userUuid(), current.role());

        return new RotatedRefreshToken(
                current.userUuid(),
                current.role(),
                newRefreshToken
        );
    }

    @Transactional
    public void invalidateByUser(String tokenHash) {
        Instant now = Instant.now();

        RefreshTokenData token = repository.findByTokenHash(tokenHash)
                .orElseThrow(this::unauthorized);

        if (token.isRotated() || token.isExpired(now)) {
            throw unauthorized();
        }

        repository.deleteByUser(token.userUuid());
    }

    public static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));

            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Erro ao gerar SHA-256", ex);
        }
    }

    private static String randomBase64Url(int bytes) {
        byte[] buffer = new byte[bytes];
        SECURE_RANDOM.nextBytes(buffer);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(buffer);
    }

    private AppException unauthorized() {
        return new AppException(401, "Credenciais inválidas");
    }

    public record RotatedRefreshToken(
            UUID userUuid,
            Role role,
            String refreshToken
    ) {
    }
}
