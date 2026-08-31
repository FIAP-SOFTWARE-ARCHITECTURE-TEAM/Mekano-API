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

    @Test
    @TestTransaction
    void findByCpf_deveRetornarClienteQuandoExiste() {
        repository.create(novoCliente());

        Optional<Cliente> encontrado = repository.findByCpf("52998224725");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getCpf().getValue()).isEqualTo("52998224725");
    }

    @Test
    @TestTransaction
    void findByCpf_comMascara_deveNormalizar() {
        repository.create(novoCliente());

        Optional<Cliente> encontrado = repository.findByCpf("529.982.247-25");

        assertThat(encontrado).isPresent();
    }

    @Test
    @TestTransaction
    void findByCpf_null_deveRetornarVazio() {
        Optional<Cliente> encontrado = repository.findByCpf(null);
        assertThat(encontrado).isEmpty();
    }

    @Test
    @TestTransaction
    void findByCpf_blank_deveRetornarVazio() {
        Optional<Cliente> encontrado = repository.findByCpf("   ");
        assertThat(encontrado).isEmpty();
    }

    @Test
    @TestTransaction
    void findByCpf_inexistente_deveRetornarVazio() {
        Optional<Cliente> encontrado = repository.findByCpf("00000000000");
        assertThat(encontrado).isEmpty();
    }

    @Test
    @TestTransaction
    void findByTelefone_exato_deveRetornarCliente() {
        repository.create(novoCliente());

        Optional<Cliente> encontrado = repository.findByTelefone("11999999999");

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getTelefone().getValue()).isEqualTo("11999999999");
    }

    @Test
    @TestTransaction
    void findByTelefone_comMascara_deveNormalizar() {
        repository.create(novoCliente());

        Optional<Cliente> encontrado = repository.findByTelefone("(11) 99999-9999");

        assertThat(encontrado).isPresent();
    }

    @Test
    @TestTransaction
    void findByTelefone_null_deveRetornarVazio() {
        Optional<Cliente> encontrado = repository.findByTelefone(null);
        assertThat(encontrado).isEmpty();
    }

    @Test
    @TestTransaction
    void findByTelefone_blank_deveRetornarVazio() {
        Optional<Cliente> encontrado = repository.findByTelefone("   ");
        assertThat(encontrado).isEmpty();
    }

    @Test
    @TestTransaction
    void existsByCpf_true_quandoExiste() {
        repository.create(novoCliente());

        boolean exists = repository.existsByCpf("52998224725");

        assertThat(exists).isTrue();
    }

    @Test
    @TestTransaction
    void existsByCpf_false_quandoNaoExiste() {
        boolean exists = repository.existsByCpf("00000000000");
        assertThat(exists).isFalse();
    }

    @Test
    @TestTransaction
    void existsByCpf_false_quandoNull() {
        boolean exists = repository.existsByCpf(null);
        assertThat(exists).isFalse();
    }

    @Test
    @TestTransaction
    void existsByCpf_false_quandoBlank() {
        boolean exists = repository.existsByCpf("   ");
        assertThat(exists).isFalse();
    }

    @Test
    @TestTransaction
    void update_deveAtualizarClienteExistente() {
        Cliente salvo = repository.create(novoCliente());

        Cliente atualizado = Cliente.reconstitute(
                salvo.getId(),
                "Nome Atualizado",
                salvo.getCpf().getValue(),
                "novo@email.com",
                "11988888888",
                "Rua C",
                "300",
                "Novo Bairro",
                "São Paulo",
                "SP",
                "02002000",
                salvo.getCreatedAt());

        Cliente resultado = repository.update(atualizado);

        assertThat(resultado.getNome()).isEqualTo("Nome Atualizado");
        assertThat(resultado.getEmail().getValue()).isEqualTo("novo@email.com");
    }

    @Test
    @TestTransaction
    void update_clienteInexistente_deveLancar404() {
        Cliente fantasma = Cliente.reconstitute(
                java.util.UUID.randomUUID(),
                "Fantasma",
                "52998224725",
                "fantasma@test.com",
                null,
                "Rua X", "1", "Bairro", "Cidade", "SP", "00000000",
                java.time.LocalDateTime.now());

        assertThatThrownBy(() -> repository.update(fantasma))
                .isInstanceOf(AppException.class)
                .satisfies(e -> assertThat(((AppException) e).getStatus()).isEqualTo(404));
    }
}