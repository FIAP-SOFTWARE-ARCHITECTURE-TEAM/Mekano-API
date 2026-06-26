package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.Veiculo;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para {@link VeiculoRepositoryImpl}.
 *
 * Usa QuarkusTest + DevServices PostgreSQL + Flyway migrations.
 *
 * Cada teste roda dentro de uma transação isolada através do
 * 
 * @TestTransaction e recebe rollback automático ao final.
 */
@QuarkusTest
class VeiculoRepositoryImplTest {

    @Inject
    VeiculoRepositoryImpl repository;

    @Test
    @TestTransaction
    void save_devePersistirERetornarVeiculoComPlacaCorreta() {

        // Arrange
        UUID clienteUuid = UUID.randomUUID();

        Veiculo veiculo = Veiculo.create(
                clienteUuid,
                "ABC1234",
                "Toyota",
                "Corolla",
                2020);

        // Act
        Veiculo salvo = repository.save(veiculo);

        // Assert
        Optional<Veiculo> encontrado = repository.findByPlaca("ABC1234");

        assertThat(encontrado).isPresent();

        assertThat(
                encontrado.get()
                        .getPlaca()
                        .getValue())
                .isEqualTo("ABC1234");

        assertThat(
                salvo.getId())
                .isEqualTo(
                        veiculo.getId());
    }

    @Test
    @TestTransaction
    void existsByPlaca_deveRetornarFalse_quandoPlacaNaoExiste() {

        // Act
        boolean existe = repository.existsByPlaca("ZZZ9999");

        // Assert
        assertThat(existe).isFalse();
    }

    @Test
    @TestTransaction
    void save_deveRetornarVeiculoSemCamposNull_roundTrip() {

        // Arrange
        UUID clienteUuid = UUID.randomUUID();

        Veiculo original = Veiculo.create(
                clienteUuid,
                "DEF1234",
                "Honda",
                "Civic",
                2021);

        // Act
        Veiculo salvo = repository.save(original);

        // Assert
        assertThat(salvo.getId()).isNotNull();
        assertThat(salvo.getId()).isEqualTo(original.getId());

        assertThat(salvo.getClienteUuid())
                .isEqualTo(clienteUuid);

        assertThat(salvo.getPlaca()).isNotNull();
        assertThat(salvo.getPlaca().getValue())
                .isEqualTo("DEF1234");

        assertThat(salvo.getMarca())
                .isEqualTo("Honda");

        assertThat(salvo.getModelo())
                .isEqualTo("Civic");

        assertThat(salvo.getAno())
                .isEqualTo(2021);

        assertThat(salvo.getCreatedAt()).isNotNull();
        assertThat(salvo.getCreatedAt())
                .isEqualTo(original.getCreatedAt());
    }

    @Test
    @TestTransaction
    void markAsDeleted_deveRealizarSoftDelete() {

        // Arrange
        Veiculo veiculo = repository.save(
                Veiculo.create(
                        UUID.randomUUID(),
                        "GHI1234",
                        "Volkswagen",
                        "Golf",
                        2019));

        // Act
        repository.markAsDeleted(
                veiculo.getId());

        // Assert
        Optional<Veiculo> encontrado = repository.findById(
                veiculo.getId());

        assertThat(encontrado).isEmpty();
    }

    @Test
    @TestTransaction
    void findById_deveRetornarVeiculoQuandoExistir() {

        // Arrange
        Veiculo salvo = repository.save(
                Veiculo.create(
                        UUID.randomUUID(),
                        "JKL1234",
                        "Chevrolet",
                        "Onix",
                        2022));

        // Act
        Optional<Veiculo> encontrado = repository.findById(
                salvo.getId());

        // Assert
        assertThat(encontrado).isPresent();

        assertThat(
                encontrado.get().getId())
                .isEqualTo(
                        salvo.getId());
    }

    @Test
    @TestTransaction
    void findByPlaca_deveRetornarVeiculoQuandoExistir() {

        // Arrange
        repository.save(
                Veiculo.create(
                        UUID.randomUUID(),
                        "MNO1234",
                        "Fiat",
                        "Pulse",
                        2023));

        // Act
        Optional<Veiculo> encontrado = repository.findByPlaca(
                "MNO1234");

        // Assert
        assertThat(encontrado).isPresent();

        assertThat(
                encontrado.get()
                        .getMarca())
                .isEqualTo("Fiat");
    }
}