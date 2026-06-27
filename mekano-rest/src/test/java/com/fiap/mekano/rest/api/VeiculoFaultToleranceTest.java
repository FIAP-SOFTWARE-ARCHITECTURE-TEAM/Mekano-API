package com.fiap.mekano.rest.api;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.port.out.UserRepositoryPort;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;

import io.quarkus.test.InjectMock;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
public class VeiculoFaultToleranceTest {

    @Inject
    VeiculoRepositoryPort repository;

    @InjectMock
    UserRepositoryPort userRepository;

    /** CENÁRIO 1 - Cache + Retry */
    @Test
    @TestTransaction
    void findById_retry_returnsEmpty() {

        Optional<Veiculo> result = repository.findById(UUID.randomUUID());

        assertThat(result).isEmpty();
    }

    /** CENÁRIO 2 - Timeout na operação de salvamento */
    @Test
    @TestTransaction
    void save_timeout_persistsSuccessfully() {

        Veiculo veiculo = Veiculo.create(
                UUID.randomUUID(),
                "ABC1234",
                "Toyota",
                "Corolla",
                2020);

        assertDoesNotThrow(() -> {

            Veiculo saved = repository.save(veiculo);

            assertThat(saved).isNotNull();
        });
    }

    /** CENÁRIO 3 - Cache do resultado */
    @Test
    @TestTransaction
    void cacheResult_sameVehicleRepeatedCalls() {

        Veiculo veiculo = repository.save(
                Veiculo.create(
                        UUID.randomUUID(),
                        "ABC1234",
                        "Toyota",
                        "Corolla",
                        2020));

        Optional<Veiculo> first = repository.findById(veiculo.getId());

        Optional<Veiculo> second = repository.findById(veiculo.getId());

        assertThat(first).isPresent();
        assertThat(second).isPresent();

        assertThat(
                first.get().getId()).isEqualTo(
                        second.get().getId());
    }

}
