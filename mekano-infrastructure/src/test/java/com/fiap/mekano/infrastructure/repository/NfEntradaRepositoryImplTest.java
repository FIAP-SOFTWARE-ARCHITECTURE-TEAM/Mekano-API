package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.NfEntrada;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
@DisplayName("NfEntradaRepositoryImpl")
class NfEntradaRepositoryImplTest {

    @Inject
    NfEntradaRepositoryImpl repository;

    private static final String CHAVE_ACESSO = "35240112345678901234567890123456789012345678";

    @Test
    @TestTransaction
    @DisplayName("save deve persistir nova NF de entrada")
    void saveDevePersistirNovaNfEntrada() {
        UUID reqId = UUID.randomUUID();
        NfEntrada nf = NfEntrada.create(CHAVE_ACESSO, new BigDecimal("1500.00"), reqId);

        NfEntrada saved = repository.save(nf);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getChaveAcesso()).isEqualTo(CHAVE_ACESSO);
        assertThat(saved.getValorTotal()).isEqualByComparingTo(new BigDecimal("1500.00"));
        assertThat(saved.getRequisicaoCompraId()).isEqualTo(reqId);
    }

    @Test
    @TestTransaction
    @DisplayName("save deve atualizar NF existente")
    void saveDeveAtualizarNfExistente() {
        UUID reqId = UUID.randomUUID();
        NfEntrada nf = NfEntrada.create(CHAVE_ACESSO, new BigDecimal("1500.00"), reqId);
        NfEntrada saved = repository.save(nf);

        NfEntrada atualizada = NfEntrada.reconstitute(
                saved.getId(), CHAVE_ACESSO, new BigDecimal("2000.00"),
                reqId, saved.getCreatedAt());

        NfEntrada result = repository.save(atualizada);

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getValorTotal()).isEqualByComparingTo(new BigDecimal("2000.00"));
    }

    @Test
    @TestTransaction
    @DisplayName("findById deve retornar NF when found")
    void findByIdDeveRetornarQuandoEncontrado() {
        UUID reqId = UUID.randomUUID();
        NfEntrada nf = NfEntrada.create(CHAVE_ACESSO, new BigDecimal("1500.00"), reqId);
        NfEntrada saved = repository.save(nf);

        Optional<NfEntrada> found = repository.findById(saved.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getChaveAcesso()).isEqualTo(CHAVE_ACESSO);
    }

    @Test
    @TestTransaction
    @DisplayName("findById deve retornar vazio when not found")
    void findByIdDeveRetornarVazioQuandoNaoEncontrado() {
        Optional<NfEntrada> found = repository.findById(UUID.randomUUID());

        assertThat(found).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("buscarPorChaveAcesso deve retornar NF when found")
    void buscarPorChaveAcessoDeveRetornarQuandoEncontrado() {
        UUID reqId = UUID.randomUUID();
        NfEntrada nf = NfEntrada.create(CHAVE_ACESSO, new BigDecimal("1500.00"), reqId);
        repository.save(nf);

        Optional<NfEntrada> found = repository.buscarPorChaveAcesso(CHAVE_ACESSO);

        assertThat(found).isPresent();
        assertThat(found.get().getRequisicaoCompraId()).isEqualTo(reqId);
    }

    @Test
    @TestTransaction
    @DisplayName("buscarPorChaveAcesso deve retornar vazio when not found")
    void buscarPorChaveAcessoDeveRetornarVazioQuandoNaoEncontrado() {
        Optional<NfEntrada> found = repository.buscarPorChaveAcesso("35240112345678901234567890123456789012349999");

        assertThat(found).isEmpty();
    }

    @Test
    @TestTransaction
    @DisplayName("findAll deve retornar pagina de NF ativas")
    void findAllDeveRetornarPaginaDeNfAtivas() {
        UUID reqId = UUID.randomUUID();
        repository.save(NfEntrada.create(CHAVE_ACESSO, new BigDecimal("1500.00"), reqId));

        List<NfEntrada> result = repository.findAll(0, 10);

        assertThat(result).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @TestTransaction
    @DisplayName("countAll deve retornar contagem de NF ativas")
    void countAllDeveRetornarContagem() {
        long before = repository.countAll();

        UUID reqId = UUID.randomUUID();
        repository.save(NfEntrada.create(CHAVE_ACESSO, new BigDecimal("1500.00"), reqId));

        long after = repository.countAll();

        assertThat(after).isEqualTo(before + 1);
    }
}
