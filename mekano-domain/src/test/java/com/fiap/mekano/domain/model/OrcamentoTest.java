package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Orcamento Entity")
class OrcamentoTest {

    @Test
    @DisplayName("deve criar orçamento e calcular valor total")
    void deveCriarOrcamentoComValorTotal() {
        ItemOrcamento item1 = new ItemOrcamento("Óleo", 2L, new BigDecimal("50.00"));
        ItemOrcamento item2 = new ItemOrcamento("Filtro", 1L, new BigDecimal("30.00"));

        Orcamento orcamento = Orcamento.create("Manutenção Preventiva", Arrays.asList(item1, item2));

        assertThat(orcamento.getId()).isNotNull();
        assertThat(orcamento.getDescricao()).isEqualTo("Manutenção Preventiva");
        assertThat(orcamento.getQuantidadeItens()).isEqualTo(2);
        assertThat(orcamento.getValorTotal()).isEqualTo(new BigDecimal("130.00"));
    }

    @Test
    @DisplayName("deve rejeitar orçamento sem itens")
    void deveRejeitarOrcamentoSemItens() {
        assertThatThrownBy(() -> Orcamento.create("Descrição", Arrays.asList()))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve validar valor total na reconstrução")
    void deveValidarValorTotalNaReconstrucao() {
        ItemOrcamento item = new ItemOrcamento("Óleo", 1L, new BigDecimal("50.00"));
        var itens = Arrays.asList(item);

        assertThatThrownBy(() -> Orcamento.reconstitute(
                java.util.UUID.randomUUID(),
                "Manutenção",
                itens,
                new BigDecimal("100.00"),
                java.time.LocalDateTime.now()
        )).isInstanceOf(AppException.class);
    }
}
