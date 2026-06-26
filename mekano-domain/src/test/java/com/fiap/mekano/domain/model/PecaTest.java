package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Peca Entity")
class PecaTest {

    @Test
    @DisplayName("deve criar peça com factory create()")
    void deveCriarPecaComCreate() {
        Peca peca = Peca.create(
                "PEA-001",
                "Óleo Motor 5W30",
                UnidadeMedida.LITRO,
                new BigDecimal("45.50"),
                100L
        );

        assertThat(peca.getId()).isNotNull();
        assertThat(peca.getCodigo()).isEqualTo("PEA-001");
        assertThat(peca.getDescricao()).isEqualTo("Óleo Motor 5W30");
        assertThat(peca.getSaldoAtual()).isEqualTo(0L);
        assertThat(peca.getEstoqueMinimo()).isEqualTo(100L);
        assertThat(peca.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("deve debitar saldo corretamente")
    void deveDebitarSaldoCorretamente() {
        Peca peca = Peca.reconstitute(
                UUID.randomUUID(),
                "PEA-001",
                "Óleo",
                UnidadeMedida.LITRO,
                new BigDecimal("45.50"),
                100L,
                null,
                LocalDateTime.now()
        );

        Long novoSaldo = peca.debitarSaldo(30L);

        assertThat(novoSaldo).isEqualTo(70L);
    }

    @Test
    @DisplayName("deve rejeitar débito que deixaria saldo negativo")
    void deveRejeitarDebitoComSaldoNegativo() {
        Peca peca = Peca.reconstitute(
                UUID.randomUUID(),
                "PEA-001",
                "Óleo",
                UnidadeMedida.LITRO,
                new BigDecimal("45.50"),
                50L,
                null,
                LocalDateTime.now()
        );

        assertThatThrownBy(() -> peca.debitarSaldo(100L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve creditar saldo corretamente")
    void deveCreditarSaldoCorretamente() {
        Peca peca = Peca.reconstitute(
                UUID.randomUUID(),
                "PEA-001",
                "Óleo",
                UnidadeMedida.LITRO,
                new BigDecimal("45.50"),
                50L,
                null,
                LocalDateTime.now()
        );

        Long novoSaldo = peca.creditarSaldo(30L);

        assertThat(novoSaldo).isEqualTo(80L);
    }

    @Test
    @DisplayName("deve verificar se estoque mínimo foi atingido")
    void deveVerificarEstoqueMinimoAtingido() {
        Peca peca = Peca.reconstitute(
                UUID.randomUUID(),
                "PEA-001",
                "Óleo",
                UnidadeMedida.LITRO,
                new BigDecimal("45.50"),
                50L,
                100L,
                LocalDateTime.now()
        );

        assertThat(peca.isEstoqueMinimoAtingido()).isTrue();
    }

    @Test
    @DisplayName("deve retornar false se não houver estoque mínimo configurado")
    void deveRetornarFalseSemEstoqueMinimo() {
        Peca peca = Peca.reconstitute(
                UUID.randomUUID(),
                "PEA-001",
                "Óleo",
                UnidadeMedida.LITRO,
                new BigDecimal("45.50"),
                50L,
                null,
                LocalDateTime.now()
        );

        assertThat(peca.isEstoqueMinimoAtingido()).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar código null ou blank")
    void deveRejeitarCodigoInvalido() {
        assertThatThrownBy(() -> Peca.create(null, "Descrição", UnidadeMedida.UNIDADE, BigDecimal.ONE, null))
                .isInstanceOf(AppException.class);
    }
}
