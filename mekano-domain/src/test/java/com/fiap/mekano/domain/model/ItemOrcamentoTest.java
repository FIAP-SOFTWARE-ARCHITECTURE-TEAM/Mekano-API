package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ItemOrcamento Value Object")
class ItemOrcamentoTest {

    @Test
    @DisplayName("deve criar item e calcular subtotal")
    void deveCriarItemComSubtotal() {
        ItemOrcamento item = new ItemOrcamento("Óleo Motor", 2L, new BigDecimal("50.00"));

        assertThat(item.getDescricao()).isEqualTo("Óleo Motor");
        assertThat(item.getQuantidade()).isEqualTo(2L);
        assertThat(item.getValorUnitario()).isEqualTo(new BigDecimal("50.00"));
        assertThat(item.calcularSubtotal()).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("deve rejeitar descrição null ou blank")
    void deveRejeitarDescricaoInvalida() {
        assertThatThrownBy(() -> new ItemOrcamento(null, 1L, new BigDecimal("50.00")))
                .isInstanceOf(AppException.class);

        assertThatThrownBy(() -> new ItemOrcamento("   ", 1L, new BigDecimal("50.00")))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve rejeitar quantidade <= 0")
    void deveRejeitarQuantidadeInvalida() {
        assertThatThrownBy(() -> new ItemOrcamento("Descrição", 0L, new BigDecimal("50.00")))
                .isInstanceOf(AppException.class);

        assertThatThrownBy(() -> new ItemOrcamento("Descrição", -1L, new BigDecimal("50.00")))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve rejeitar valor unitário negativo")
    void deveRejeitarValorUnitarioNegativo() {
        assertThatThrownBy(() -> new ItemOrcamento("Descrição", 1L, new BigDecimal("-50.00")))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve verificar igualdade por valor")
    void deveVerificarIgualdadePorValor() {
        ItemOrcamento item1 = new ItemOrcamento("Óleo", 2L, new BigDecimal("50.00"));
        ItemOrcamento item2 = new ItemOrcamento("Óleo", 2L, new BigDecimal("50.00"));

        assertThat(item1).isEqualTo(item2);
    }
}
