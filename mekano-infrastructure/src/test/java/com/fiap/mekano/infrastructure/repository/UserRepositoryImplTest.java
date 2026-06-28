package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.User;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para {@link UserRepositoryImpl}.
 *
 * <p>Usa {@code @QuarkusTest}: sobe o container Quarkus completo com DevServices
 * (PostgreSQL via Testcontainers), executa migrations Flyway e valida o schema JPA.
 *
 * <p>Usa {@code @TestTransaction} em cada método: abre transação antes do teste e
 * faz rollback automático ao final — banco fica limpo para o próximo teste sem
 * necessidade de setup/teardown manual.
 *
 * <p>Pré-requisito: Docker deve estar disponível para DevServices subir o container PostgreSQL.
 */
@QuarkusTest
class UserRepositoryImplTest {

    @Inject
    UserRepositoryImpl repository;

    @Test
    @TestTransaction
    void save_devePersistirERetornarUserComEmailCorreto() {
        // Arrange
        User user = User.create("João Silva", "joao@fiap.br", true, "$2a$10$hashbcrypt");

        // Act
        User salvo = repository.save(user);

        // Assert — buscar no banco confirma persistência real
        Optional<User> encontrado = repository.findByEmail("joao@fiap.br");
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getEmail().getValue()).isEqualTo("joao@fiap.br");
        assertThat(salvo.getId()).isEqualTo(user.getId());
    }

    @Test
    @TestTransaction
    void findByEmail_deveNormalizarCaseEWhitespace() {
        User user = User.create("Carlos Souza", "carlos@fiap.br", true, "$2a$10$hashbcrypt");
        repository.save(user);

        Optional<User> encontrado = repository.findByEmail("  CARLOS@FIAP.BR  ");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getEmail().getValue()).isEqualTo("carlos@fiap.br");
    }

    @Test
    @TestTransaction
    void findAll_deveSanitizarPaginacaoESortInvalidos() {
        User user = User.create("Ana", "ana@fiap.br", true, "$2a$10$hashbcrypt");
        repository.save(user);

        var result = repository.findAll(-1, 0, " ");

        assertThat(result).isNotEmpty();
    }

    @Test
    @TestTransaction
    void existsByEmail_deveRetornarFalse_quandoEmailNaoExiste() {
        // Act
        boolean existe = repository.existsByEmail("naoexiste@fiap.br");

        // Assert
        assertThat(existe).isFalse();
    }

    @Test
    @TestTransaction
    void save_deveRetornarUserSemCamposNull_roundTrip() {
        // Arrange
        User original = User.create("Maria Oliveira", "maria@fiap.br", true, "$2a$10$hash");

        // Act
        User salvo = repository.save(original);

        // Assert — todos os campos devem estar presentes após round-trip entity↔domain
        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getId()).isEqualTo(original.getId());
        assertThat(salvo.getName()).isEqualTo("Maria Oliveira");
        assertThat(salvo.getEmail()).isNotNull();
        assertThat(salvo.getEmail().getValue()).isEqualTo("maria@fiap.br"); 
        assertThat(salvo.getPasswordHash()).isNotNull();
        assertThat(salvo.getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(salvo.getCreatedAt()).isNotNull();
        assertThat(salvo.getCreatedAt()).isEqualTo(original.getCreatedAt());
    }
}
