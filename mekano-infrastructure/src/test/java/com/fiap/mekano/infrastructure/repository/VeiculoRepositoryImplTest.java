package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.Veiculo;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    void create_devePersistirERetornarVeiculoComPlacaCorreta() {

        // Arrange
        UUID clienteUuid = UUID.randomUUID();

        Veiculo veiculo = Veiculo.create(
                clienteUuid,
                "ABC1234",
                "Toyota",
                "Corolla",
                2020);

        // Act
        Veiculo salvo = repository.create(veiculo);

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
    void create_deveRetornarVeiculoSemCamposNull_roundTrip() {

        // Arrange
        UUID clienteUuid = UUID.randomUUID();

        Veiculo original = Veiculo.create(
                clienteUuid,
                "DEF1234",
                "Honda",
                "Civic",
                2021);

        // Act
        Veiculo salvo = repository.create(original);

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
        Veiculo veiculo = repository.create(
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

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getIsActive()).isFalse();
    }

    @Test
    @TestTransaction
    void findById_deveRetornarVeiculoQuandoExistir() {

        // Arrange
        Veiculo salvo = repository.create(
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
        repository.create(
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

    @Test
    @TestTransaction
    void update_deveAlterarCamposMutaveisEPreservarPlaca() {

        // Arrange
        Veiculo salvo = repository.create(
                Veiculo.create(
                        UUID.randomUUID(),
                        "PQR1234",
                        "Toyota",
                        "Corolla",
                        2020));

        Veiculo atualizado = Veiculo.reconstitute(
                salvo.getId(),
                salvo.getClienteUuid(),
                "PQR1234",
                "Toyota",
                "Yaris",
                2022,
                salvo.getCreatedAt());

        // Act
        Veiculo resultado = repository.update(atualizado);

        // Assert
        assertThat(resultado.getId()).isEqualTo(salvo.getId());

        assertThat(resultado.getPlaca().getValue())
                .isEqualTo("PQR1234");

        assertThat(resultado.getMarca())
                .isEqualTo("Toyota");

        assertThat(resultado.getModelo())
                .isEqualTo("Yaris");

        assertThat(resultado.getAno())
                .isEqualTo(2022);
    }

    @Test
    @TestTransaction
    void update_deveLancarAppException_quandoVeiculoNaoExiste() {

        // Arrange
        Veiculo veiculo = Veiculo.create(
                UUID.randomUUID(),
                "QRS1234",
                "Honda",
                "Fit",
                2021);

        // Act & Assert
        assertThatThrownBy(() -> repository.update(veiculo))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getStatus()).isEqualTo(404));
    }
}