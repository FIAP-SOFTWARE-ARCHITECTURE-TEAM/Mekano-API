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
                new BigDecimal("45.50"),
                100L,
                null,
                LocalDateTime.now(),
                0L
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
                new BigDecimal("45.50"),
                50L,
                null,
                LocalDateTime.now(),
                0L
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
                new BigDecimal("45.50"),
                50L,
                null,
                LocalDateTime.now(),
                0L
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
                new BigDecimal("45.50"),
                50L,
                100L,
                LocalDateTime.now(),
                0L
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
                new BigDecimal("45.50"),
                50L,
                null,
                LocalDateTime.now(),
                0L
        );

        assertThat(peca.isEstoqueMinimoAtingido()).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar código null ou blank")
    void deveRejeitarCodigoInvalido() {
        assertThatThrownBy(() -> Peca.create(null, "Descrição", BigDecimal.ONE, null))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve reconstitute com saldoReservado zero se null")
    void deveReconstituteComSaldoReservadoNull() {
        Peca peca = Peca.reconstitute(
                UUID.randomUUID(), "PEA-001", "Óleo",
                new BigDecimal("45.50"), 100L, null, LocalDateTime.now(), null);

        assertThat(peca.getSaldoReservado()).isZero();
    }

    @Test
    @DisplayName("deve rejeitar reconstitute com saldoReservado negativo")
    void deveRejeitarReconstituteComSaldoReservadoNegativo() {
        assertThatThrownBy(() -> Peca.reconstitute(
                UUID.randomUUID(), "PEA-001", "Óleo",
                new BigDecimal("45.50"), 100L, null, LocalDateTime.now(), -5L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("disponivel deve retornar saldoAtual - saldoReservado")
    void disponivelDeveRetornarSaldoAtualMenosReservado() {
        Peca peca = Peca.reconstitute(
                UUID.randomUUID(), "PEA-001", "Óleo",
                new BigDecimal("45.50"), 100L, null, LocalDateTime.now(), 30L);

        assertThat(peca.disponivel()).isEqualTo(70L);
    }

    @Test
    @DisplayName("isEstoqueMinimoAtingido deve considerar saldo disponível")
    void isEstoqueMinimoAtingidoDeveConsiderarDisponivel() {
        // saldo=10, reservado=8 → disponivel=2 < minimo=5 → true
        Peca pecaComReserva = Peca.reconstitute(
                UUID.randomUUID(), "PEA-001", "Óleo",
                new BigDecimal("45.50"), 10L, 5L, LocalDateTime.now(), 8L);

        assertThat(pecaComReserva.disponivel()).isEqualTo(2L);
        assertThat(pecaComReserva.isEstoqueMinimoAtingido()).isTrue();

        // saldo=10, reservado=2 → disponivel=8 >= minimo=5 → false
        Peca pecaSemReserva = Peca.reconstitute(
                UUID.randomUUID(), "PEA-001", "Óleo",
                new BigDecimal("45.50"), 10L, 5L, LocalDateTime.now(), 2L);

        assertThat(pecaSemReserva.disponivel()).isEqualTo(8L);
        assertThat(pecaSemReserva.isEstoqueMinimoAtingido()).isFalse();
    }

    @Test
    @DisplayName("create() deve definir saldoReservado como 0")
    void createDeveDefinirSaldoReservadoComoZero() {
        Peca peca = Peca.create("PEA-001", "Óleo", new BigDecimal("45.50"), 10L);

        assertThat(peca.getSaldoReservado()).isZero();
    }
}
