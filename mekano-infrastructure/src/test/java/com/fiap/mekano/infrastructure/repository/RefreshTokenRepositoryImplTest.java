package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.Role;
import com.fiap.mekano.domain.port.out.RefreshTokenData;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("RefreshTokenRepositoryImpl")
class RefreshTokenRepositoryImplTest {

    @Inject
    RefreshTokenRepositoryImpl repository;

    @Test
    @TestTransaction
    @DisplayName("save deve persistir novo refresh token")
    void saveDevePersistirNovoRefreshToken() {
        UUID userUuid = UUID.randomUUID();
        RefreshTokenData data = new RefreshTokenData(null, "jti-123", "hash-abc", userUuid,
                Role.cliente, Instant.now().plusSeconds(3600), null);

        RefreshTokenData saved = repository.save(data);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.jti()).isEqualTo("jti-123");
        assertThat(saved.tokenHash()).isEqualTo("hash-abc");
        assertThat(saved.userUuid()).isEqualTo(userUuid);
        assertThat(saved.role()).isEqualTo(Role.cliente);
    }

    @Test
    @TestTransaction
    @DisplayName("save deve atualizar refresh token existente")
    void saveDeveAtualizarRefreshTokenExistente() {
        UUID userUuid = UUID.randomUUID();
        RefreshTokenData data = new RefreshTokenData(null, "jti-123", "hash-abc", userUuid,
                Role.cliente, Instant.now().plusSeconds(3600), null);
        RefreshTokenData saved = repository.save(data);

        RefreshTokenData atualizado = new RefreshTokenData(saved.id(), "jti-456", "hash-def",
                userUuid, Role.cliente, Instant.now().plusSeconds(7200), Instant.now());

        RefreshTokenData result = repository.save(atualizado);

        assertThat(result.id()).isEqualTo(saved.id());
        assertThat(result.jti()).isEqualTo("jti-456");
        assertThat(result.tokenHash()).isEqualTo("hash-def");
        assertThat(result.rotatedAt()).isNotNull();
    }

    @Test
    @TestTransaction
    @DisplayName("findByTokenHash deve retornar token when found")
    void findByTokenHashDeveRetornarQuandoEncontrado() {
        UUID userUuid = UUID.randomUUID();
        RefreshTokenData data = new RefreshTokenData(null, "jti-123", "hash-abc", userUuid,
                Role.cliente, Instant.now().plusSeconds(3600), null);
        repository.save(data);

        Optional<RefreshTokenData> found = repository.findByTokenHash("hash-abc");

        assertThat(found).isPresent();
        assertThat(found.get().jti()).isEqualTo("jti-123");
    }

    @Test
    @TestTransaction
    @DisplayName("findByTokenHash deve retornar vazio when not found")
    void findByTokenHashDeveRetornarVazioQuandoNaoEncontrado() {
        Optional<RefreshTokenData> found = repository.findByTokenHash("hash-inexistente");

        assertThat(found).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("findByTokenHash deve retornar vazio for null hash")
    void findByTokenHashDeveRetornarVazioParaNull() {
        Optional<RefreshTokenData> found = repository.findByTokenHash(null);

        assertThat(found).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("findByTokenHash deve retornar vazio for blank hash")
    void findByTokenHashDeveRetornarVazioParaBlank() {
        Optional<RefreshTokenData> found = repository.findByTokenHash("   ");

        assertThat(found).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("deleteByUser deve remover tokens do usuário")
    void deleteByUserDeveRemoverTokensDoUsuario() {
        UUID userUuid = UUID.randomUUID();
        repository.save(new RefreshTokenData(null, "jti-1", "hash-1", userUuid,
                Role.cliente, Instant.now().plusSeconds(3600), null));
        repository.save(new RefreshTokenData(null, "jti-2", "hash-2", userUuid,
                Role.cliente, Instant.now().plusSeconds(3600), null));

        repository.deleteByUser(userUuid);

        Optional<RefreshTokenData> found1 = repository.findByTokenHash("hash-1");
        Optional<RefreshTokenData> found2 = repository.findByTokenHash("hash-2");
        assertThat(found1).isEmpty();
        assertThat(found2).isEmpty();
    }

}
