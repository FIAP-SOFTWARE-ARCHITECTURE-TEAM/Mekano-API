package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NfEntrada Entity")
class NfEntradaTest {

    @Test
    @DisplayName("deve criar nota fiscal com cálculo automático de valor total")
    void deveCriarNfEntradaComValorTotal() {
        LocalDateTime dataEmissao = LocalDateTime.now().minusDays(1);

        NfEntrada nf = NfEntrada.create(
                "123456",
                "1",
                "12345678000190",
                "Fornecedor LTDA",
                dataEmissao,
                new BigDecimal("1000.00"),
                new BigDecimal("100.00"),
                new BigDecimal("50.00"),
                new BigDecimal("25.00"),
                "12345678901234567890123456789012345678901234"
        );

        assertThat(nf.getId()).isNotNull();
        assertThat(nf.getNumero()).isEqualTo("123456");
        assertThat(nf.getValorTotal()).isEqualTo(new BigDecimal("1175.00"));
    }

    @Test
    @DisplayName("deve rejeitar data emissão no futuro")
    void deveRejeitarDataEmissaoFuturo() {
        LocalDateTime dataFutura = LocalDateTime.now().plusDays(1);

        assertThatThrownBy(() -> NfEntrada.create(
                "123456",
                "1",
                "12345678000190",
                "Fornecedor",
                dataFutura,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "12345678901234567890123456789012345678901234"
        )).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve rejeitar CNPJ inválido")
    void deveRejeitarCnpjInvalido() {
        LocalDateTime data = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> NfEntrada.create(
                "123456",
                "1",
                "123",
                "Fornecedor",
                data,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "12345678901234567890123456789012345678901234"
        )).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve rejeitar chave de acesso inválida")
    void deveRejeitarChaveAcessoInvalida() {
        LocalDateTime data = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> NfEntrada.create(
                "123456",
                "1",
                "12345678000190",
                "Fornecedor",
                data,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "1234"
        )).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve formatar chave de acesso corretamente")
    void deveFormatarChaveAcesso() {
        LocalDateTime data = LocalDateTime.now().minusDays(1);
        String chave = "12345678901234567890123456789012345678901234";

        NfEntrada nf = NfEntrada.create(
                "123456",
                "1",
                "12345678000190",
                "Fornecedor",
                data,
                new BigDecimal("1000.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                chave
        );

        assertThat(nf.chaveAcessoFormatada())
                .isEqualTo("12.3456.7890.1234.5678.9012.3456.7890.1234.5678901234");
    }

    @Test
    @DisplayName("deve rejeitar valor negativo de impostos")
    void deveRejeitarValorNegativoImpostos() {
        LocalDateTime data = LocalDateTime.now().minusDays(1);

        assertThatThrownBy(() -> NfEntrada.create(
                "123456",
                "1",
                "12345678000190",
                "Fornecedor",
                data,
                new BigDecimal("1000.00"),
                new BigDecimal("-100.00"),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                "12345678901234567890123456789012345678901244"
        )).isInstanceOf(AppException.class);
    }
}
