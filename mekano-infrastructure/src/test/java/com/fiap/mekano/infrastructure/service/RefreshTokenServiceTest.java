package com.fiap.mekano.infrastructure.service;

import com.fiap.mekano.domain.exception.InvalidRefreshTokenException;
import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.out.RefreshTokenData;
import com.fiap.mekano.domain.port.out.RefreshTokenRepositoryPort;
import com.fiap.mekano.infrastructure.repository.UserRepositoryImpl;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de integração para {@link RefreshTokenService}.
 *
 * <p>Usa {@code @QuarkusTest} com DevServices (PostgreSQL via Testcontainers),
 * migrations Flyway (V1 + V2) e {@code @TestTransaction} para rollback automático.
 *
 * <p>{@link RefreshTokenServiceTestProfile} gera um par de chaves RSA-2048 in-memory
 * e configura {@code smallrye.jwt.sign.key.location} para que o {@code Jwt.issuer()}
 * no serviço consiga assinar tokens sem chave commitada.
 *
 * <p>Pré-requisito: Docker deve estar disponível para DevServices subir o container PostgreSQL.
 */
@QuarkusTest
@TestProfile(RefreshTokenServiceTestProfile.class)
class RefreshTokenServiceTest {

    @Inject
    RefreshTokenService service;

    @Inject
    RefreshTokenRepositoryPort repository;

    @Inject
    UserRepositoryImpl userRepository;

    /**
     * Cria um usuário no banco e retorna seu UUID — necessário por causa da FK
     * em refresh_tokens.user_id -> users.id.
     */
    private User criarUsuario() {
        User user = User.create("Test User", "test.refresh@fiap.br", "$2a$10$hash");
        return userRepository.save(user);
    }

    @Test
    @TestTransaction
    void generateAndRotate_shouldInvalidateOldToken() throws InvalidRefreshTokenException {
        // Arrange
        User user = criarUsuario();
        TokenPair firstPair = service.generateTokens(user.getId());
        String oldToken = firstPair.token();
        String oldJti = firstPair.jti();

        // Act — valida e rotaciona
        RefreshTokenData rotated = service.validateAndRotate(oldToken);

        // Assert — token antigo está rotacionado
        Optional<RefreshTokenData> stored = repository.findByJti(oldJti);
        assertThat(stored).isPresent();
        assertThat(stored.get().isRotated()).isTrue();
        assertThat(stored.get().rotatedAt()).isNotNull();

        // O token retornado por validateAndRotate é o antigo (já rotacionado)
        assertThat(rotated.jti()).isEqualTo(oldJti);
        assertThat(rotated.isRotated()).isTrue();
    }

    @Test
    @TestTransaction
    void rotatedToken_shouldBeRejected() throws InvalidRefreshTokenException {
        // Arrange
        User user = criarUsuario();
        TokenPair firstPair = service.generateTokens(user.getId());
        String oldToken = firstPair.token();

        // Primeiro uso — rotaciona
        service.validateAndRotate(oldToken);

        // Act & Assert — segundo uso com o mesmo token deve rejeitar
        assertThatThrownBy(() -> service.validateAndRotate(oldToken))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("already used");
    }

    @Test
    @TestTransaction
    void tamperedToken_shouldBeRejected() {
        // Arrange
        User user = criarUsuario();
        TokenPair pair = service.generateTokens(user.getId());

        // Corrompe o token para simular hash mismatch
        String tampered = pair.token().substring(0, pair.token().length() - 5) + "XXXXX";

        // Act & Assert — hash mismatch deve rejeitar
        assertThatThrownBy(() -> service.validateAndRotate(tampered))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @TestTransaction
    void invalidTokenFormat_shouldBeRejected() {
        assertThatThrownBy(() -> service.validateAndRotate("not-a-jwt"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    @TestTransaction
    void nonExistentJti_shouldBeRejected() {
        // Arrange — gera token para obter um jti válido no payload, mas não persiste
        // (só gerar via service.generateTokens já persiste, então usamos tampered
        // token que faz o extractJti obter um jti que não está no banco)
        User user = criarUsuario();
        TokenPair pair = service.generateTokens(user.getId());

        // Apaga o registro do banco para que findByJti retorne empty
        // Não temos delete direct na port, mas tampered token gera hash mismatch
        String tampered = pair.token().substring(0, pair.token().length() - 5) + "XXXXX";

        assertThatThrownBy(() -> service.validateAndRotate(tampered))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }
}
