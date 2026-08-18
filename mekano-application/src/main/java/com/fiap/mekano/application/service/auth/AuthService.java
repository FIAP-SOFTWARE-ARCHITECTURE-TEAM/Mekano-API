package com.fiap.mekano.application.service.auth;


import com.fiap.mekano.domain.model.Role;
import com.fiap.mekano.domain.port.in.AuthServicePort;
import com.fiap.mekano.domain.port.in.LoginCommand;
import com.fiap.mekano.domain.port.in.TokenPair;
import com.fiap.mekano.domain.port.out.AccessTokenIssuerPort;
import com.fiap.mekano.domain.port.out.PasswordHasherPort;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import com.fiap.mekano.domain.port.out.UserRoleRepositoryPort;
import com.fiap.mekano.domain.exception.AppException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class AuthService implements AuthServicePort {

    private static final long ACCESS_TOKEN_SECONDS = 900L;

    @Inject
    UserRepositoryPort userRepository;

    @Inject
    PasswordHasherPort passwordHasher;

    @Inject
    RefreshTokenService refreshTokenService;

    @Inject
    UserRoleRepositoryPort userRoleRepository;

    @Inject
    AccessTokenIssuerPort accessTokenIssuer;

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

        String accessToken = accessTokenIssuer.issue(
                user.getId(),
                user.getName(),
                role
        );

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

        String accessToken = accessTokenIssuer.issue(
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
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AppException(400, "refresh_token é obrigatório");
        }
        String tokenHash = RefreshTokenService.sha256(refreshToken);
        refreshTokenService.invalidateByUser(tokenHash);
    }
    
    private AppException unauthorized() {
        return new AppException(401, "Credenciais inválidas");
    }

   
}