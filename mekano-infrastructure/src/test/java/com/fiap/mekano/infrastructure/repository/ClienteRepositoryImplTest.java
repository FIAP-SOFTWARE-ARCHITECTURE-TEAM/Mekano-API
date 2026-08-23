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

    @Test
    @TestTransaction
    void update_deveAlterarClienteInativoPreservandoIsActive() {
        Cliente salvo = repository.create(novoCliente());
        repository.markAsDeleted(salvo.getId());

        Cliente atualizado = Cliente.reconstitute(
                salvo.getId(),
                "Cliente Inativo Atualizado",
                salvo.getCpf().getValue(),
                "inativo.novo@teste.com",
                null,
                "Rua B",
                "200",
                "Vila Nova",
                "Campinas",
                "SP",
                "02002000",
                salvo.getCreatedAt());

        Cliente resultado = repository.update(atualizado);

        assertThat(resultado.getId()).isEqualTo(salvo.getId());
        assertThat(resultado.getNome()).isEqualTo("Cliente Inativo Atualizado");
        assertThat(resultado.getEmail().getValue()).isEqualTo("inativo.novo@teste.com");
        assertThat(resultado.getIsActive()).isFalse();

        Optional<Cliente> encontrado = repository.findById(salvo.getId());
        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getIsActive()).isFalse();
    }

    @Test
    @TestTransaction
    void findAll_deveFiltrarPorIsActive() {
        Cliente ativo = repository.create(novoCliente());
        Cliente inativo = repository.create(Cliente.create(
                "Cliente Inativo",
                "11144477735",
                "inativo@teste.com",
                "11988887777",
                "Rua B",
                "200",
                "Centro",
                "São Paulo",
                "SP",
                "01001000"));
        repository.markAsDeleted(inativo.getId());

        assertThat(repository.findAll(0, 100, "nome,asc", null))
                .extracting(Cliente::getId)
                .contains(ativo.getId(), inativo.getId());
        assertThat(repository.findAll(0, 100, "nome,asc", true))
                .extracting(Cliente::getId)
                .contains(ativo.getId())
                .doesNotContain(inativo.getId());
        assertThat(repository.findAll(0, 100, "nome,asc", false))
                .extracting(Cliente::getId)
                .contains(inativo.getId())
                .doesNotContain(ativo.getId());
        assertThat(repository.countAll(null)).isGreaterThanOrEqualTo(2L);
        assertThat(repository.countAll(true)).isGreaterThanOrEqualTo(1L);
        assertThat(repository.countAll(false)).isGreaterThanOrEqualTo(1L);
    }
}