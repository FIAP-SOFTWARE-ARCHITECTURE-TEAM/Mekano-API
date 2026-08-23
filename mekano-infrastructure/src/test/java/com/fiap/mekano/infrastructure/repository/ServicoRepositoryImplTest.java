package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.Servico;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para {@link ServicoRepositoryImpl}.
 *
 * <p>Usa QuarkusTest + DevServices PostgreSQL + Flyway migrations.
 * Cada teste roda dentro de uma transação isolada com rollback automático.
 */
@QuarkusTest
class ServicoRepositoryImplTest {

    @Inject
    ServicoRepositoryImpl repository;

    @Test
    @TestTransaction
    void findAll_deveFiltrarPorIsActive() {
        Servico ativo = repository.save(
                Servico.create("Troca de Óleo", "Sintético 5W30", new BigDecimal("89.90")));
        Servico inativo = repository.save(
                Servico.create("Alinhamento", "Dianteiro e traseiro", new BigDecimal("120.00")));
        repository.markAsDeleted(inativo.getId());

        assertThat(repository.findAll(0, 100, "nome,asc", null))
                .extracting(Servico::getId)
                .contains(ativo.getId(), inativo.getId());
        assertThat(repository.findAll(0, 100, "nome,asc", true))
                .extracting(Servico::getId)
                .contains(ativo.getId())
                .doesNotContain(inativo.getId());
        assertThat(repository.findAll(0, 100, "nome,asc", false))
                .extracting(Servico::getId)
                .contains(inativo.getId())
                .doesNotContain(ativo.getId());
        assertThat(repository.countAll(null)).isGreaterThanOrEqualTo(2L);
        assertThat(repository.countAll(true)).isGreaterThanOrEqualTo(1L);
        assertThat(repository.countAll(false)).isGreaterThanOrEqualTo(1L);
    }

    @Test
    @TestTransaction
    void findAll_deveSanitizarSortInvalido() {
        repository.save(Servico.create("Balanceamento", "Rodas", new BigDecimal("80.00")));

        var resultado = repository.findAll(0, 100, "coluna-invalida", null);

        assertThat(resultado).isNotEmpty();
    }
}