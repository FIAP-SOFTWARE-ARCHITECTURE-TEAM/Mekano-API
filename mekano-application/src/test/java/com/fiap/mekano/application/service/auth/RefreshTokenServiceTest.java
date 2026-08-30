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

    @Test
    @DisplayName("createToken deve gerar token e persistir")
    void createTokenDeveGerarEPersistir() {
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        String plainToken = service.createToken(userUuid, Role.admin);

        assertNotNull(plainToken);
        assertTrue(plainToken.contains(":"));
        verify(repository, times(1)).save(any());
    }

    @Test
    @DisplayName("rotate deve invalidar token antigo e criar novo")
    void rotateDeveInvalidarECriarNovo() {
        String tokenHash = "hash-para-rotacionar";
        RefreshTokenData token = new RefreshTokenData(
                1L, "jti-123", tokenHash, userUuid, Role.admin,
                Instant.now().plusSeconds(3600), null);

        when(repository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.RotatedRefreshToken result = service.rotate(tokenHash);

        assertNotNull(result);
        assertEquals(userUuid, result.userUuid());
        assertEquals(Role.admin, result.role());
        assertNotNull(result.refreshToken());
        verify(repository, times(2)).save(any());
    }

    @Test
    @DisplayName("rotate com token expirado deve lancar 401")
    void rotateComTokenExpiradoDeveLancar401() {
        String tokenHash = "hash-expirado";
        RefreshTokenData token = new RefreshTokenData(
                1L, "jti-123", tokenHash, userUuid, Role.admin,
                Instant.now().minusSeconds(60), null);

        when(repository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        AppException ex = assertThrows(AppException.class, () -> service.rotate(tokenHash));
        assertEquals(401, ex.getStatus());
    }

    @Test
    @DisplayName("rotate com token ja rotacionado deve lancar 401")
    void rotateComTokenRotacionadoDeveLancar401() {
        String tokenHash = "hash-rotacionado";
        RefreshTokenData token = new RefreshTokenData(
                1L, "jti-123", tokenHash, userUuid, Role.admin,
                Instant.now().plusSeconds(3600), Instant.now());

        when(repository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        AppException ex = assertThrows(AppException.class, () -> service.rotate(tokenHash));
        assertEquals(401, ex.getStatus());
    }

    @Test
    @DisplayName("rotate com token inexistente deve lancar 401")
    void rotateComTokenInexistenteDeveLancar401() {
        when(repository.findByTokenHash("hash-fake")).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> service.rotate("hash-fake"));
        assertEquals(401, ex.getStatus());
    }

    @Test
    @DisplayName("sha256 deve retornar hash consistente")
    void sha256DeveRetornarHashConsistente() {
        String hash1 = RefreshTokenService.sha256("teste");
        String hash2 = RefreshTokenService.sha256("teste");

        assertNotNull(hash1);
        assertEquals(hash1, hash2);
    }

    @Test
    @DisplayName("sha256 com valores diferentes deve gerar hashes diferentes")
    void sha256ComValoresDiferentesDeveGerarHashesDiferentes() {
        String hash1 = RefreshTokenService.sha256("valor-a");
        String hash2 = RefreshTokenService.sha256("valor-b");

        assertNotEquals(hash1, hash2);
    }
}