package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.valueobject.ItemOrcamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

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
        assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.PENDENTE);
        assertThat(orcamento.getOrdemServicoUuid()).isNull();
        assertThat(orcamento.getDataExpiracao()).isNull();
    }

    @Test
    @DisplayName("deve criar orçamento vinculado a OS com SLA 72h")
    void deveCriarOrcamentoVinculadoAOS() {
        var osUuid = UUID.randomUUID();
        var item = new ItemOrcamento("Serviço", 1L, new BigDecimal("100.00"));

        Orcamento orcamento = Orcamento.create("Orçamento OS", Arrays.asList(item), osUuid);

        assertThat(orcamento.getOrdemServicoUuid()).isEqualTo(osUuid);
        assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.PENDENTE);
        assertThat(orcamento.getDataExpiracao()).isNotNull();
        assertThat(orcamento.getDataExpiracao()).isAfter(LocalDateTime.now().plusHours(71));
        assertThat(orcamento.getDataExpiracao()).isBefore(LocalDateTime.now().plusHours(73));
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
                UUID.randomUUID(), "Manutenção", itens,
                new BigDecimal("100.00"), LocalDateTime.now()
        )).isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("aprovar() deve transicionar PENDENTE → APROVADO")
    void deveAprovarOrcamento() {
        var orcamento = Orcamento.create("Teste", Arrays.asList(
                new ItemOrcamento("Item", 1L, BigDecimal.TEN)));

        orcamento.aprovar();

        assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.APROVADO);
    }

    @Test
    @DisplayName("aprovar() deve lançar exceção se orçamento não está PENDENTE")
    void deveRejeitarAprovarOrcamentoNaoPendente() {
        var orcamento = Orcamento.create("Teste", Arrays.asList(
                new ItemOrcamento("Item", 1L, BigDecimal.TEN)));
        orcamento.reprovar();

        assertThatThrownBy(orcamento::aprovar)
                .isInstanceOf(AppException.class)
                .hasMessageContaining("aprovar");
    }

    @Test
    @DisplayName("reprovar() deve transicionar PENDENTE → REPROVADO")
    void deveReprovarOrcamento() {
        var orcamento = Orcamento.create("Teste", Arrays.asList(
                new ItemOrcamento("Item", 1L, BigDecimal.TEN)));

        orcamento.reprovar();

        assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.REPROVADO);
    }

    @Test
    @DisplayName("reprovar() deve lançar exceção se orçamento não está PENDENTE")
    void deveRejeitarReprovarOrcamentoNaoPendente() {
        var orcamento = Orcamento.create("Teste", Arrays.asList(
                new ItemOrcamento("Item", 1L, BigDecimal.TEN)));
        orcamento.aprovar();

        assertThatThrownBy(orcamento::reprovar)
                .isInstanceOf(AppException.class)
                .hasMessageContaining("reprovar");
    }

    @Test
    @DisplayName("expirar() deve transicionar PENDENTE → EXPIRADO")
    void deveExpirarOrcamento() {
        var orcamento = Orcamento.create("Teste", Arrays.asList(
                new ItemOrcamento("Item", 1L, BigDecimal.TEN)));

        orcamento.expirar();

        assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.EXPIRADO);
    }

    @Test
    @DisplayName("expirar() deve lançar exceção se orçamento não está PENDENTE")
    void deveRejeitarExpirarOrcamentoNaoPendente() {
        var orcamento = Orcamento.create("Teste", Arrays.asList(
                new ItemOrcamento("Item", 1L, BigDecimal.TEN)));
        orcamento.aprovar();

        assertThatThrownBy(orcamento::expirar)
                .isInstanceOf(AppException.class)
                .hasMessageContaining("expirar");
    }

    @Test
    @DisplayName("isExpirado() deve retornar true para orçamento vencido")
    void deveDetectarOrcamentoExpirado() {
        var dataExpirada = LocalDateTime.now().minusHours(1);
        var orcamento = Orcamento.reconstitute(
                UUID.randomUUID(), "Teste",
                Arrays.asList(new ItemOrcamento("Item", 1L, BigDecimal.TEN)),
                BigDecimal.TEN, LocalDateTime.now().minusHours(2),
                StatusOrcamento.PENDENTE, UUID.randomUUID(), dataExpirada);

        assertThat(orcamento.isExpirado()).isTrue();
    }

    @Test
    @DisplayName("isExpirado() deve retornar false para orçamento sem dataExpiracao")
    void deveRetornarFalsoQuandoSemDataExpiracao() {
        var orcamento = Orcamento.create("Teste", Arrays.asList(
                new ItemOrcamento("Item", 1L, BigDecimal.TEN)));

        assertThat(orcamento.isExpirado()).isFalse();
    }

    @Test
    @DisplayName("reconstitute() com overload novo deve preservar status e OS uuid")
    void deveReconstituirComStatusEOS() {
        var id = UUID.randomUUID();
        var osUuid = UUID.randomUUID();
        var createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);
        var dataExp = createdAt.plusHours(72);
        var itens = Arrays.asList(new ItemOrcamento("Item", 1L, BigDecimal.TEN));

        var orcamento = Orcamento.reconstitute(id, "Reconstituído", itens,
                BigDecimal.TEN, createdAt,
                StatusOrcamento.APROVADO, osUuid, dataExp);

        assertThat(orcamento.getId()).isEqualTo(id);
        assertThat(orcamento.getOrdemServicoUuid()).isEqualTo(osUuid);
        assertThat(orcamento.getStatus()).isEqualTo(StatusOrcamento.APROVADO);
        assertThat(orcamento.getDataExpiracao()).isEqualTo(dataExp);
    }
}
