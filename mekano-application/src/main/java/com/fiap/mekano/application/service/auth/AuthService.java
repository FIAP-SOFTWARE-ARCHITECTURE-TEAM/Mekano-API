package com.fiap.mekano.application.service.auth;

import com.fiap.mekano.domain.model.Role;
import com.fiap.mekano.domain.port.in.AuthServicePort;
import com.fiap.mekano.domain.port.in.LoginCommand;
import com.fiap.mekano.domain.port.in.TokenPair;
import com.fiap.mekano.domain.port.out.PasswordHasherPort;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import com.fiap.mekano.domain.port.out.UserRoleRepositoryPort;
import com.fiap.mekano.shared.exception.AppException;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class AuthService implements AuthServicePort {

    private static final String ISSUER = "mekano-api";
    private static final long ACCESS_TOKEN_SECONDS = 900L;

    @Inject
    UserRepositoryPort userRepository;

    @Inject
    PasswordHasherPort passwordHasher;

    @Inject
    RefreshTokenService refreshTokenService;

    @Inject
    UserRoleRepositoryPort userRoleRepository;

    @Override
    @Transactional
    public TokenPair login(LoginCommand command) {
        var user = userRepository.findByEmail(command.email())
                .filter(foundUser -> foundUser.isActive())
                .orElseThrow(this::unauthorized);

        if (!passwordHasher.matches(command.password(), user.getPasswordHash())) {
            throw unauthorized();
        }

        Role role = userRoleRepository.findRoleByUserUuid(user.getId())
                .orElseThrow(this::unauthorized);

        String accessToken = issueAccessToken(user.getId(), user.getName(), role);
        String refreshToken = refreshTokenService.createToken(user.getId(), role);

        return new TokenPair(accessToken, refreshToken, ACCESS_TOKEN_SECONDS);
    }

    @Override
    @Transactional
    public TokenPair refresh(String refreshToken) {
        String tokenHash = RefreshTokenService.sha256(refreshToken);

        var rotated = refreshTokenService.rotate(tokenHash);

        var user = userRepository.findById(rotated.userUuid())
                .filter(foundUser -> foundUser.isActive())
                .orElseThrow(this::unauthorized);

        String accessToken = issueAccessToken(
                rotated.userUuid(),
                user.getName(),
                rotated.role()
        );

        return new TokenPair(
                accessToken,
                rotated.refreshToken(),
                ACCESS_TOKEN_SECONDS
        );
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        String tokenHash = RefreshTokenService.sha256(refreshToken);
        refreshTokenService.invalidateByUser(tokenHash);
    }

    private String issueAccessToken(UUID userUuid, String name, Role role) {
        Instant now = Instant.now();

        return Jwt.issuer(ISSUER)
                .subject(userUuid.toString())
                .claim("name", name)
                .groups(Set.of(role.name()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ACCESS_TOKEN_SECONDS))
                .sign();
    }

    private AppException unauthorized() {
        return new AppException(401, "Credenciais inválidas");
    }
}
