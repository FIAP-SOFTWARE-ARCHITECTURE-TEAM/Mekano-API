package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.Cliente;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Testes de integração para {@link ClienteRepositoryImpl}.
 *
 * <p>Usa QuarkusTest + DevServices PostgreSQL + Flyway migrations.
 * Cada teste roda dentro de uma transação isolada e recebe rollback automático
 * ao final ({@code @TestTransaction}).
 */
@QuarkusTest
class ClienteRepositoryImplTest {

    @Inject
    ClienteRepositoryImpl repository;

    private Cliente novoCliente() {
        return Cliente.create(
                "Cliente Teste",
                "52998224725",
                "cliente@teste.com",
                "11999999999",
                "Rua A",
                "100",
                "Centro",
                "São Paulo",
                "SP",
                "01001000");
    }

    @Test
    @TestTransaction
    void create_devePersistirCliente() {
        Cliente salvo = repository.create(novoCliente());

        Optional<Cliente> encontrado = repository.findById(salvo.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getId()).isEqualTo(salvo.getId());
        assertThat(encontrado.get().getIsActive()).isTrue();
    }

    @Test
    @TestTransaction
    void markAsDeleted_deveRealizarSoftDelete() {
        Cliente salvo = repository.create(novoCliente());

        repository.markAsDeleted(salvo.getId());

        Optional<Cliente> encontrado = repository.findById(salvo.getId());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getIsActive()).isFalse();
    }

    @Test
    @TestTransaction
    void markAsDeleted_deveLancar404_quandoClienteNaoExiste() {
        assertThatThrownBy(() -> repository.markAsDeleted(java.util.UUID.randomUUID()))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getStatus()).isEqualTo(404));
    }

    @Test
    @TestTransaction
    void reactivate_deveReativarRegistroInativo() {
        Cliente salvo = repository.create(novoCliente());
        repository.markAsDeleted(salvo.getId());

        repository.reactivate(salvo.getId());

        Optional<Cliente> encontrado = repository.findById(salvo.getId());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getIsActive()).isTrue();
    }

    @Test
    @TestTransaction
    void reactivate_jaAtivo_naoAlteraEstado() {
        Cliente salvo = repository.create(novoCliente());

        repository.reactivate(salvo.getId());

        Optional<Cliente> encontrado = repository.findById(salvo.getId());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getIsActive()).isTrue();
    }

    @Test
    @TestTransaction
    void reactivate_deveLancar404_quandoClienteNaoExiste() {
        assertThatThrownBy(() -> repository.reactivate(java.util.UUID.randomUUID()))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getStatus()).isEqualTo(404));
    }
}