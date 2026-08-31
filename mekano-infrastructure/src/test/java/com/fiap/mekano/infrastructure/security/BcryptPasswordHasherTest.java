package com.fiap.mekano.infrastructure.security;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("BcryptPasswordHasher")
class BcryptPasswordHasherTest {

    @Inject
    BcryptPasswordHasher hasher;

    @Test
    @DisplayName("hash deve gerar hash da senha")
    void hashDeveGerarHash() {
        String hash = hasher.hash("senha123");

        assertThat(hash).isNotNull();
        assertThat(hash).isNotBlank();
        assertThat(hash).isNotEqualTo("senha123");
    }

    @Test
    @DisplayName("matches deve retornar true para senha correta")
    void matchesDeveRetornarTrueParaSenhaCorreta() {
        String hash = hasher.hash("minhaSenha");

        assertThat(hasher.matches("minhaSenha", hash)).isTrue();
    }

    @Test
    @DisplayName("matches deve retornar false para senha incorreta")
    void matchesDeveRetornarFalseParaSenhaIncorreta() {
        String hash = hasher.hash("minhaSenha");

        assertThat(hasher.matches("senhaErrada", hash)).isFalse();
    }

    @Test
    @DisplayName("hash de senhas diferentes deve gerar hashes diferentes")
    void hashDeveGerarHashesDiferentes() {
        String hash1 = hasher.hash("senha1");
        String hash2 = hasher.hash("senha2");

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("hash deve ser consistente para a mesma senha")
    void hashDeveSerConsistente() {
        String hash1 = hasher.hash("senha123");
        String hash2 = hasher.hash("senha123");

        assertThat(hasher.matches("senha123", hash1)).isTrue();
        assertThat(hasher.matches("senha123", hash2)).isTrue();
    }
}
