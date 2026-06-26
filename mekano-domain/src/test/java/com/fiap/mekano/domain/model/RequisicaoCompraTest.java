package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RequisicaoCompra Entity")
class RequisicaoCompraTest {

    @Test
    @DisplayName("deve criar requisição para orçamento com status COMPRADA")
    void deveCriarRequisicaoParaOrcamento() {
        UUID pecaId = UUID.randomUUID();

        RequisicaoCompra requisicao = RequisicaoCompra.criarParaOrcamento(pecaId, 50L, "Orçamento #123");

        assertThat(requisicao.getId()).isNotNull();
        assertThat(requisicao.getStatus()).isEqualTo(StatusRequisicao.COMPRADA);
        assertThat(requisicao.getPecaId()).isEqualTo(pecaId);
        assertThat(requisicao.getQuantidade()).isEqualTo(50L);
    }

    @Test
    @DisplayName("deve criar requisição para mínimo com status ABERTA")
    void deveCriarRequisicaoParaMinimo() {
        UUID pecaId = UUID.randomUUID();

        RequisicaoCompra requisicao = RequisicaoCompra.criarParaMinimo(pecaId, 100L, "Reposição de mínimo");

        assertThat(requisicao.getId()).isNotNull();
        assertThat(requisicao.getStatus()).isEqualTo(StatusRequisicao.ABERTA);
        assertThat(requisicao.getPecaId()).isEqualTo(pecaId);
        assertThat(requisicao.getQuantidade()).isEqualTo(100L);
    }

    @Test
    @DisplayName("deve verificar se pode ser enviada (status ABERTA)")
    void deveVerificarSePodeSerEnviada() {
        RequisicaoCompra requisicaoAberta = RequisicaoCompra.criarParaMinimo(UUID.randomUUID(), 50L, "Motivo");
        RequisicaoCompra requisicaoComprada = RequisicaoCompra.criarParaOrcamento(UUID.randomUUID(), 50L, "Motivo");

        assertThat(requisicaoAberta.podeSerEnviada()).isTrue();
        assertThat(requisicaoComprada.podeSerEnviada()).isFalse();
    }

    @Test
    @DisplayName("deve verificar se pode ser recebida (status ENVIADA ou COMPRADA)")
    void deveVerificarSePodeSerRecebida() {
        RequisicaoCompra requisicaoComprada = RequisicaoCompra.criarParaOrcamento(UUID.randomUUID(), 50L, "Motivo");
        RequisicaoCompra requisicaoAberta = RequisicaoCompra.criarParaMinimo(UUID.randomUUID(), 50L, "Motivo");

        assertThat(requisicaoComprada.podeSerRecebida()).isTrue();
        assertThat(requisicaoAberta.podeSerRecebida()).isFalse();
    }

    @Test
    @DisplayName("deve rejeitar quantidade <= 0")
    void deveRejeitarQuantidadeInvalida() {
        UUID pecaId = UUID.randomUUID();

        assertThatThrownBy(() -> RequisicaoCompra.criarParaMinimo(pecaId, 0L, "Motivo"))
                .isInstanceOf(AppException.class);

        assertThatThrownBy(() -> RequisicaoCompra.criarParaOrcamento(pecaId, -1L, "Motivo"))
                .isInstanceOf(AppException.class);
    }
}
