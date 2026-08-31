package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static com.fiap.mekano.domain.model.MotivoRequisicao.ESTOQUE_MINIMO;
import static com.fiap.mekano.domain.model.MotivoRequisicao.ORDEM_SERVICO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RequisicaoCompra Entity")
class RequisicaoCompraTest {

    @Test
    @DisplayName("deve criar requisição para orçamento com múltiplos itens e status COMPRA APROVADA")
    void deveCriarRequisicaoParaOrcamento() {
        UUID peca1 = UUID.randomUUID();
        UUID peca2 = UUID.randomUUID();

        List<ItemRequisicaoCompra> itens = List.of(
                new ItemRequisicaoCompra(peca1, 50L),
                new ItemRequisicaoCompra(peca2, 30L));

        RequisicaoCompra requisicao = RequisicaoCompra.criarParaOrcamento(itens, ORDEM_SERVICO);

        assertThat(requisicao.getId()).isNotNull();
        assertThat(requisicao.getStatus()).isEqualTo(StatusRequisicao.COMPRA_APROVADA);
        assertThat(requisicao.getItens()).hasSize(2);
        assertThat(requisicao.getItens().get(0).getPecaId()).isEqualTo(peca1);
        assertThat(requisicao.getItens().get(0).getQuantidade()).isEqualTo(50L);
        assertThat(requisicao.getItens().get(1).getPecaId()).isEqualTo(peca2);
        assertThat(requisicao.getItens().get(1).getQuantidade()).isEqualTo(30L);
    }

    @Test
    @DisplayName("deve criar requisição para mínimo com múltiplos itens e status ABERTA")
    void deveCriarRequisicaoParaMinimo() {
        UUID peca1 = UUID.randomUUID();
        UUID peca2 = UUID.randomUUID();

        List<ItemRequisicaoCompra> itens = List.of(
                new ItemRequisicaoCompra(peca1, 100L),
                new ItemRequisicaoCompra(peca2, 200L));

        RequisicaoCompra requisicao = RequisicaoCompra.criarParaMinimo(itens, ESTOQUE_MINIMO);

        assertThat(requisicao.getId()).isNotNull();
        assertThat(requisicao.getStatus()).isEqualTo(StatusRequisicao.ABERTA);
        assertThat(requisicao.getItens()).hasSize(2);
        assertThat(requisicao.getItens().get(0).getPecaId()).isEqualTo(peca1);
        assertThat(requisicao.getItens().get(0).getQuantidade()).isEqualTo(100L);
    }

    @Test
    @DisplayName("deve verificar se pode ser enviada (status ABERTA)")
    void deveVerificarSePodeSerEnviada() {
        List<ItemRequisicaoCompra> itens = List.of(new ItemRequisicaoCompra(UUID.randomUUID(), 50L));

        RequisicaoCompra requisicaoAberta = RequisicaoCompra.criarParaMinimo(itens, ESTOQUE_MINIMO);
        RequisicaoCompra requisicaoComprada = RequisicaoCompra.criarParaOrcamento(itens, ORDEM_SERVICO);

        assertThat(requisicaoAberta.podeSerEnviada()).isTrue();
        assertThat(requisicaoComprada.podeSerEnviada()).isFalse();
    }

    @Test
    @DisplayName("deve verificar se pode ser recebida (status ENVIADA ou COMPRADA)")
    void deveVerificarSePodeSerRecebida() {
        List<ItemRequisicaoCompra> itens = List.of(new ItemRequisicaoCompra(UUID.randomUUID(), 50L));

        RequisicaoCompra requisicaoComprada = RequisicaoCompra.criarParaOrcamento(itens, ORDEM_SERVICO);
        RequisicaoCompra requisicaoAberta = RequisicaoCompra.criarParaMinimo(itens, ESTOQUE_MINIMO);

        assertThat(requisicaoComprada.podeSerRecebida()).isTrue();
        assertThat(requisicaoAberta.podeSerRecebida()).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar lista de itens vazia")
    void deveRejeitarItensVazios() {
        assertThatThrownBy(() -> RequisicaoCompra.criarParaMinimo(List.of(), ESTOQUE_MINIMO))
                .isInstanceOf(AppException.class);

        assertThatThrownBy(() -> RequisicaoCompra.criarParaOrcamento(null, ORDEM_SERVICO))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve rejeitar item com quantidade <= 0")
    void deveRejeitarItemComQuantidadeInvalida() {
        assertThatThrownBy(() -> new ItemRequisicaoCompra(UUID.randomUUID(), 0L))
                .isInstanceOf(AppException.class);

        assertThatThrownBy(() -> new ItemRequisicaoCompra(UUID.randomUUID(), -1L))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("deve rejeitar item com pecaId nulo")
    void deveRejeitarItemComPecaIdNulo() {
        assertThatThrownBy(() -> new ItemRequisicaoCompra(null, 10L))
                .isInstanceOf(AppException.class);
    }
}
