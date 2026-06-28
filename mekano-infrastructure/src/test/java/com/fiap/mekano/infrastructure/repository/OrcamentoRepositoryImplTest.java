package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.StatusOrcamento;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class OrcamentoRepositoryImplTest {

    @Inject
    OrcamentoRepositoryImpl repository;

    @Test
    @TestTransaction
    void save_devePersistirERetornarOrcamento() {
        var itens = List.of(new ItemOrcamento("Óleo", 2L, new BigDecimal("50.00")));
        var orcamento = Orcamento.create("Preventiva", itens);

        var salvo = repository.save(orcamento);

        assertThat(salvo.getId()).isEqualTo(orcamento.getId());
        assertThat(salvo.getDescricao()).isEqualTo("Preventiva");
        assertThat(salvo.getStatus()).isEqualTo(StatusOrcamento.PENDENTE);
        assertThat(salvo.getValorTotal()).isEqualByComparingTo(new BigDecimal("100.00"));
    }

    @Test
    @TestTransaction
    void findByUuid_deveRetornarOrcamento() {
        var itens = List.of(new ItemOrcamento("Filtro", 1L, new BigDecimal("30.00")));
        var orcamento = Orcamento.create("Troca filtro", itens);
        repository.save(orcamento);

        var encontrado = repository.findByUuid(orcamento.getId());

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getDescricao()).isEqualTo("Troca filtro");
        assertThat(encontrado.get().getValorTotal()).isEqualByComparingTo(new BigDecimal("30.00"));
    }

    @Test
    @TestTransaction
    void findByUuid_deveRetornarEmpty_quandoNaoExiste() {
        var result = repository.findByUuid(UUID.randomUUID());
        assertThat(result).isEmpty();
    }

    @Test
    @TestTransaction
    void findByOrdemServicoUuid_deveRetornarOrcamento() {
        var osUuid = UUID.randomUUID();
        var itens = List.of(new ItemOrcamento("Item", 1L, BigDecimal.TEN));
        var orcamento = Orcamento.create("OS Orçamento", itens, osUuid);
        repository.save(orcamento);

        var encontrado = repository.findByOrdemServicoUuid(osUuid);

        assertThat(encontrado).isPresent();
        assertThat(encontrado.get().getOrdemServicoUuid()).isEqualTo(osUuid);
    }

    @Test
    @TestTransaction
    void save_deveAtualizarStatus() {
        var itens = List.of(new ItemOrcamento("Item", 1L, BigDecimal.TEN));
        var orcamento = Orcamento.create("Teste", itens);
        var salvo = repository.save(orcamento);

        salvo.aprovar();
        repository.save(salvo);

        var atualizado = repository.findByUuid(orcamento.getId());
        assertThat(atualizado).isPresent();
        assertThat(atualizado.get().getStatus()).isEqualTo(StatusOrcamento.APROVADO);
    }

    @Test
    @TestTransaction
    void findExpiradosPendentes_deveRetornarApenasOrcamentosVencidos() {
        var osUuid = UUID.randomUUID();
        var itens = List.of(new ItemOrcamento("Item", 1L, BigDecimal.TEN));
        var dataExpirada = LocalDateTime.now().minusHours(1);
        var orcamento = Orcamento.reconstitute(
                UUID.randomUUID(), "Expirado", itens, BigDecimal.TEN,
                LocalDateTime.now().minusHours(2),
                StatusOrcamento.PENDENTE, osUuid, dataExpirada);
        repository.save(orcamento);

        var expirados = repository.findExpiradosPendentes();

        assertThat(expirados).isNotEmpty();
        assertThat(expirados).anyMatch(o -> o.getId().equals(orcamento.getId()));
    }
}
