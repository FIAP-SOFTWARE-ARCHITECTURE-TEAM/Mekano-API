package com.fiap.mekano.application.service.auth;

import com.fiap.mekano.domain.model.Role;
import com.fiap.mekano.domain.port.out.RefreshTokenData;
import com.fiap.mekano.domain.port.out.RefreshTokenRepositoryPort;
import com.fiap.mekano.shared.exception.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    RefreshTokenRepositoryPort repository;

    RefreshTokenService service;

    UUID userUuid;

    @BeforeEach
    void setUp() {
        service = new RefreshTokenService();
        service.repository = repository;
        userUuid = UUID.randomUUID();
    }

    @Test
    @DisplayName("logout com token válido deve invalidar tokens do usuário")
    void logoutComTokenValidoDeveInvalidarTokensDoUsuario() {
        String tokenHash = "hash-valido";

        RefreshTokenData token = new RefreshTokenData(
                1L,
                "jti-123",
                tokenHash,
                userUuid,
                Role.admin,
                Instant.now().plusSeconds(3600),
                null
        );

        when(repository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(token));

        service.invalidateByUser(tokenHash);

        verify(repository).findByTokenHash(tokenHash);
        verify(repository).deleteByUser(userUuid);
    }

    @Test
    @DisplayName("segundo logout com mesmo token deve falhar com 401")
    void segundoLogoutComMesmoTokenDeveFalharCom401() {
        String tokenHash = "hash-ja-invalidado";

        when(repository.findByTokenHash(tokenHash))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> service.invalidateByUser(tokenHash)
        );

        assertEquals(401, exception.getStatus());
        assertEquals("Credenciais inválidas", exception.getMessage());

        verify(repository).findByTokenHash(tokenHash);
        verify(repository, never()).deleteByUser(any());
    }

    @Test
    @DisplayName("token inexistente deve falhar com 401")
    void tokenInexistenteDeveFalharCom401() {
        String tokenHash = "hash-inexistente";

        when(repository.findByTokenHash(tokenHash))
                .thenReturn(Optional.empty());

        AppException exception = assertThrows(
                AppException.class,
                () -> service.invalidateByUser(tokenHash)
        );

        assertEquals(401, exception.getStatus());

        verify(repository).findByTokenHash(tokenHash);
        verify(repository, never()).deleteByUser(any());
    }

    @Test
    @DisplayName("token já rotacionado deve falhar com 401")
    void tokenRotacionadoDeveFalharCom401() {
        String tokenHash = "hash-rotacionado";

        RefreshTokenData token = new RefreshTokenData(
                1L,
                "jti-123",
                tokenHash,
                userUuid,
                Role.admin,
                Instant.now().plusSeconds(3600),
                Instant.now()
        );

        when(repository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(token));

        AppException exception = assertThrows(
                AppException.class,
                () -> service.invalidateByUser(tokenHash)
        );

        assertEquals(401, exception.getStatus());

        verify(repository).findByTokenHash(tokenHash);
        verify(repository, never()).deleteByUser(any());
    }

    @Test
    @DisplayName("token expirado deve falhar com 401")
    void tokenExpiradoDeveFalharCom401() {
        String tokenHash = "hash-expirado";

        RefreshTokenData token = new RefreshTokenData(
                1L,
                "jti-123",
                tokenHash,
                userUuid,
                Role.admin,
                Instant.now().minusSeconds(60),
                null
        );

        when(repository.findByTokenHash(tokenHash))
                .thenReturn(Optional.of(token));

        AppException exception = assertThrows(
                AppException.class,
                () -> service.invalidateByUser(tokenHash)
        );

        assertEquals(401, exception.getStatus());

        verify(repository).findByTokenHash(tokenHash);
        verify(repository, never()).deleteByUser(any());
    }
}