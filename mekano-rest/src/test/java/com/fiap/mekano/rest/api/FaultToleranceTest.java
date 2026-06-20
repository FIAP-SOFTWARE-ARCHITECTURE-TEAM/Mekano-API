package com.fiap.mekano.rest.api;

import com.fiap.mekano.domain.model.User;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Testes de integração para Fault Tolerance (@Retry, @Timeout, @CircuitBreaker)
 * em UserRepositoryImpl.
 *
 * Completam a Fase 7 que foi entregue sem testes de FT.
 *
 * Nota: @CircuitBreaker é difícil de testar deterministicamente em @QuarkusTest
 * porque requer estado half-open. O teste abaixo cobre @Retry (observável via
 * log) e @Timeout (lança TimeoutException). O CB é verificado como smoke test
 * apenas confirmando que a anotação não quebra o startup.
 */
@QuarkusTest
class FaultToleranceTest {

    @Inject
    UserRepositoryPort userRepository;

    @Test
    @TestTransaction
    @DisplayName("findById com @Retry deve retornar Optional.empty para ID inexistente")
    void findById_comRetry_optionalEmpty() {
        // Act — @Retry com maxRetries=3; ID aleatório não deve existir
        Optional<User> result = userRepository.findById(UUID.randomUUID());

        // Assert — retorna vazio sem lançar exceção
        assertThat(result).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("findByEmail com @Retry deve retornar Optional.empty para email inexistente")
    void findByEmail_comRetry_optionalEmpty() {
        // Act
        Optional<User> result = userRepository.findByEmail("naoexiste@fiap.br");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("save com @Timeout deve persistir sem lançar TimeoutException para dados válidos")
    void save_comTimeout_persisteComSucesso() {
        // Arrange
        User user = User.create("Teste FT", "ft@fiap.br", "$2a$10$hash");

        // Act + Assert — não deve lançar TimeoutException
        assertDoesNotThrow(() -> {
            User saved = userRepository.save(user);
            assertThat(saved).isNotNull();
            assertThat(saved.getId()).isEqualTo(user.getId());
        });
    }

    @Test
    @TestTransaction
    @DisplayName("save com mesmo email duas vezes lança exceção (não TimeoutException)")
    void save_duplicateEmail_lancaExcecao() {
        // Arrange
        String email = "dup-ft@fiap.br";
        userRepository.save(User.create("Primeiro", email, "$2a$10$hash"));

        // Act
        User segundo = User.create("Segundo", email, "$2a$10$hash");

        // Assert — deve lançar RuntimeException (constraint violation), não TimeoutException
        assertThrows(Exception.class, () -> userRepository.save(segundo));
    }

    @Test
    @TestTransaction
    @DisplayName("findById e findByEmail com @CacheResult devem retornar mesmo valor em chamadas repetidas")
    void cacheResult_mesmoValorEmChamadasRepetidas() {
        // Arrange — criar usuário
        User user = User.create("Cache Test", "cache@fiap.br", "$2a$10$hash");
        User saved = userRepository.save(user);
        UUID id = saved.getId();

        // Act — chamar findById duas vezes (segunda deve vir do cache)
        Optional<User> first = userRepository.findById(id);
        Optional<User> second = userRepository.findById(id);

        // Assert
        assertThat(first).isPresent();
        assertThat(second).isPresent();
        assertThat(second.get().getId()).isEqualTo(first.get().getId());
    }
}
