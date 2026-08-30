package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.MotivoRequisicao;
import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("RequisicaoCompraRepositoryImpl")
class RequisicaoCompraRepositoryImplTest {

    @Inject
    RequisicaoCompraRepositoryImpl repository;

    @Test
    @TestTransaction
    @DisplayName("save deve persistir nova requisição")
    void saveDevePersistirNovaRequisicao() {
        UUID pecaId = UUID.randomUUID();
        RequisicaoCompra requisicao = RequisicaoCompra.criarParaOrcamento(pecaId, 10L, MotivoRequisicao.ESTOQUE_MINIMO);

        RequisicaoCompra saved = repository.save(requisicao);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPecaId()).isEqualTo(pecaId);
        assertThat(saved.getQuantidade()).isEqualTo(10L);
        assertThat(saved.getStatus()).isEqualTo(StatusRequisicao.COMPRA_APROVADA);
        assertThat(saved.getMotivo()).isEqualTo(MotivoRequisicao.ESTOQUE_MINIMO);
    }

    @Test
    @TestTransaction
    @DisplayName("save deve atualizar requisição existente")
    void saveDeveAtualizarRequisicaoExistente() {
        UUID pecaId = UUID.randomUUID();
        RequisicaoCompra requisicao = RequisicaoCompra.criarParaMinimo(pecaId, 5L, MotivoRequisicao.ORDEM_SERVICO);
        RequisicaoCompra saved = repository.save(requisicao);

        RequisicaoCompra atualizada = RequisicaoCompra.reconstitute(
                saved.getId(), pecaId, 20L, StatusRequisicao.ENVIADA,
                MotivoRequisicao.ORDEM_SERVICO, saved.getCreatedAt());

        RequisicaoCompra result = repository.save(atualizada);

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getQuantidade()).isEqualTo(20L);
        assertThat(result.getStatus()).isEqualTo(StatusRequisicao.ENVIADA);
    }

    @Test
    @TestTransaction
    @DisplayName("findById deve retornar requisição when found")
    void findByIdDeveRetornarQuandoEncontrado() {
        UUID pecaId = UUID.randomUUID();
        RequisicaoCompra requisicao = RequisicaoCompra.criarParaOrcamento(pecaId, 10L, MotivoRequisicao.ESTOQUE_MINIMO);
        RequisicaoCompra saved = repository.save(requisicao);

        Optional<RequisicaoCompra> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getPecaId()).isEqualTo(pecaId);
    }

    @Test
    @TestTransaction
    @DisplayName("findById deve retornar vazio when not found")
    void findByIdDeveRetornarVazioQuandoNaoEncontrado() {
        Optional<RequisicaoCompra> found = repository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("atualizar deve delegar para save")
    void atualizarDeveDelegarParaSave() {
        UUID pecaId = UUID.randomUUID();
        RequisicaoCompra requisicao = RequisicaoCompra.criarParaMinimo(pecaId, 5L, MotivoRequisicao.ESTOQUE_MINIMO);
        RequisicaoCompra saved = repository.save(requisicao);

        RequisicaoCompra atualizada = RequisicaoCompra.reconstitute(
                saved.getId(), pecaId, 15L, StatusRequisicao.COMPRA_APROVADA,
                MotivoRequisicao.ESTOQUE_MINIMO, saved.getCreatedAt());

        RequisicaoCompra result = repository.atualizar(atualizada);

        assertThat(result.getQuantidade()).isEqualTo(15L);
        assertThat(result.getStatus()).isEqualTo(StatusRequisicao.COMPRA_APROVADA);
    }

    @Test
    @TestTransaction
    @DisplayName("findAll deve retornar pagina de requisições ativas")
    void findAllDeveRetornarPaginaDeRequisicoesAtivas() {
        UUID pecaId = UUID.randomUUID();
        repository.save(RequisicaoCompra.criarParaOrcamento(pecaId, 10L, MotivoRequisicao.ESTOQUE_MINIMO));
        repository.save(RequisicaoCompra.criarParaMinimo(pecaId, 5L, MotivoRequisicao.ORDEM_SERVICO));

        List<RequisicaoCompra> result = repository.findAll(0, 10);

        assertThat(result).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @TestTransaction
    @DisplayName("countAll deve retornar contagem de requisições ativas")
    void countAllDeveRetornarContagem() {
        long before = repository.countAll();

        UUID pecaId = UUID.randomUUID();
        repository.save(RequisicaoCompra.criarParaOrcamento(pecaId, 10L, MotivoRequisicao.ESTOQUE_MINIMO));

        long after = repository.countAll();

        assertThat(after).isEqualTo(before + 1);
    }
}
