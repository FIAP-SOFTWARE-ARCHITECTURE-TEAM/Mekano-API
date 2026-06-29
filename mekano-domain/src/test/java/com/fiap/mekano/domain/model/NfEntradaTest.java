package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("NfEntrada Entity")
class NfEntradaTest {

    @Test
    @DisplayName("deve criar nota fiscal")
    void deveCriarNfEntrada() {
        UUID pecaId = UUID.randomUUID();
        UUID requisicaoId = UUID.randomUUID();

        NfEntrada nf = NfEntrada.create(
                "12345678901234567890123456789012345678901234",
                new BigDecimal("1875.00"),
                pecaId, requisicaoId
        );

        assertThat(nf.getId()).isNotNull();
        assertThat(nf.getChaveAcesso()).isEqualTo("12345678901234567890123456789012345678901234");
        assertThat(nf.getValorTotal()).isEqualTo(new BigDecimal("1875.00"));
        assertThat(nf.getPecaId()).isEqualTo(pecaId);
        assertThat(nf.getRequisicaoCompraId()).isEqualTo(requisicaoId);
    }

    @Test
    @DisplayName("deve rejeitar chave de acesso inválida")
    void deveRejeitarChaveAcessoInvalida() {
        assertThatThrownBy(() -> NfEntrada.create(
                "1234",
                BigDecimal.ONE,
                UUID.randomUUID(), UUID.randomUUID()
        )).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve rejeitar valor total zero ou negativo")
    void deveRejeitarValorTotalInvalido() {
        assertThatThrownBy(() -> NfEntrada.create(
                "12345678901234567890123456789012345678901234",
                BigDecimal.ZERO,
                UUID.randomUUID(), UUID.randomUUID()
        )).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve formatar chave de acesso corretamente")
    void deveFormatarChaveAcesso() {
        String chave = "12345678901234567890123456789012345678901234";

        NfEntrada nf = NfEntrada.create(
                chave, BigDecimal.ONE,
                UUID.randomUUID(), UUID.randomUUID()
        );

        assertThat(nf.chaveAcessoFormatada())
                .isEqualTo("12.3456.7890.1234.5678.9012.3456.7890.1234.5678901234");
    }

    @Test
    @DisplayName("deve rejeitar pecaId nulo")
    void deveRejeitarPecaIdNulo() {
        assertThatThrownBy(() -> NfEntrada.create(
                "12345678901234567890123456789012345678901234",
                BigDecimal.ONE,
                null, UUID.randomUUID()
        )).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve rejeitar requisicaoCompraId nulo")
    void deveRejeitarRequisicaoIdNulo() {
        assertThatThrownBy(() -> NfEntrada.create(
                "12345678901234567890123456789012345678901234",
                BigDecimal.ONE,
                UUID.randomUUID(), null
        )).isInstanceOf(AppException.class);
    }
}
