package com.fiap.mekano.application.service.auth;

import com.fiap.mekano.domain.model.Role;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.in.LoginCommand;
import com.fiap.mekano.domain.port.out.AccessTokenIssuerPort;
import com.fiap.mekano.domain.port.out.PasswordHasherPort;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import com.fiap.mekano.domain.port.out.UserRoleRepositoryPort;
import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceJwtTest {

    @Mock
    UserRepositoryPort userRepository;

    @Mock
    PasswordHasherPort passwordHasher;

    @Mock
    RefreshTokenService refreshTokenService;

    @Mock
    UserRoleRepositoryPort userRoleRepository;

    @Mock
    AccessTokenIssuerPort accessTokenIssuer;

    AuthService authService;

    UUID userUuid;
    User activeUser;

    @BeforeEach
    void setUp() {
        authService = new AuthService();

        authService.userRepository = userRepository;
        authService.passwordHasher = passwordHasher;
        authService.refreshTokenService = refreshTokenService;
        authService.userRoleRepository = userRoleRepository;
        authService.accessTokenIssuer = accessTokenIssuer;

        userUuid = UUID.randomUUID();

        activeUser = User.reconstitute(
                userUuid,
                "Admin Mekano",
                "admin@mekano.com",
                true,
                "$2a$10$hashfake",
                LocalDateTime.now()
        );
    }

    @Test
    void login_deveEmitirAccessTokenUsandoAccessTokenIssuerPort() {
        var command = new LoginCommand("admin@mekano.com", "123456");

        when(userRepository.findByEmail("admin@mekano.com"))
                .thenReturn(Optional.of(activeUser));

        when(passwordHasher.matches("123456", "$2a$10$hashfake"))
                .thenReturn(true);

        when(userRoleRepository.findRoleByUserUuid(userUuid))
                .thenReturn(Optional.of(Role.admin));

        when(accessTokenIssuer.issue(userUuid, "Admin Mekano", Role.admin))
                .thenReturn("access-token-fake");

        when(refreshTokenService.createToken(userUuid, Role.admin))
                .thenReturn("refresh-token-fake");

        var tokenPair = authService.login(command);

        assertEquals("access-token-fake", tokenPair.accessToken());
        assertEquals("refresh-token-fake", tokenPair.refreshToken());
        assertEquals(900L, tokenPair.expiresIn());

        verify(accessTokenIssuer).issue(userUuid, "Admin Mekano", Role.admin);
        verify(refreshTokenService).createToken(userUuid, Role.admin);
    }

    @Test
    void login_quandoSenhaInvalida_naoDeveEmitirAccessToken() {
        var command = new LoginCommand("admin@mekano.com", "senha-errada");

        when(userRepository.findByEmail("admin@mekano.com"))
                .thenReturn(Optional.of(activeUser));

        when(passwordHasher.matches("senha-errada", "$2a$10$hashfake"))
                .thenReturn(false);

        AppException exception = assertThrows(
                AppException.class,
                () -> authService.login(command)
        );

        assertEquals(401, exception.getStatus());
        

        verifyNoInteractions(accessTokenIssuer);
        verifyNoInteractions(refreshTokenService);
        verifyNoInteractions(userRoleRepository);
    }

    @Test
    void login_quandoUsuarioNaoTemRole_naoDeveEmitirAccessToken() {
        var command = new LoginCommand("admin@mekano.com", "123456");

        when(userRepository.findByEmail("admin@mekano.com"))
                .thenReturn(Optional.of(activeUser));

        when(passwordHasher.matches("123456", "$2a$10$hashfake"))
                .thenReturn(true);

        when(userRoleRepository.findRoleByUserUuid(userUuid))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> authService.login(command)
        );

        assertEquals(401, exception.getStatus());

        verifyNoInteractions(accessTokenIssuer);
        verifyNoInteractions(refreshTokenService);
    }

    @Test
    void refresh_deveRotacionarRefreshTokenEEmitirNovoAccessToken() {
        String oldRefreshToken = "jti-antigo:secret-antigo";
        String tokenHash = RefreshTokenService.sha256(oldRefreshToken);

        var rotated = new RefreshTokenService.RotatedRefreshToken(
                userUuid,
                Role.admin,
                "refresh-token-novo"
        );

        when(refreshTokenService.rotate(tokenHash))
                .thenReturn(rotated);

        when(userRepository.findById(userUuid))
                .thenReturn(Optional.of(activeUser));

        when(accessTokenIssuer.issue(userUuid, "Admin Mekano", Role.admin))
                .thenReturn("access-token-novo");

        var tokenPair = authService.refresh(oldRefreshToken);

        assertEquals("access-token-novo", tokenPair.accessToken());
        assertEquals("refresh-token-novo", tokenPair.refreshToken());
        assertEquals(900L, tokenPair.expiresIn());

        verify(refreshTokenService).rotate(tokenHash);
        verify(accessTokenIssuer).issue(userUuid, "Admin Mekano", Role.admin);
    }
    
    @Test
    void logout_deveInvalidarRefreshTokenPorHash() {
        String refreshToken = "jti:secret";
        String tokenHash = RefreshTokenService.sha256(refreshToken);

        authService.logout(refreshToken);

        verify(refreshTokenService).invalidateByUser(tokenHash);
    }
}