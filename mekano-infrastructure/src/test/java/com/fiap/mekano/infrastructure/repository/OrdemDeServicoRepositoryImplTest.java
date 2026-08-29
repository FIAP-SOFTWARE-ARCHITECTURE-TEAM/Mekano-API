package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOS;
import com.fiap.mekano.domain.os.StatusEntrega;
import com.fiap.mekano.domain.os.StatusPagamento;
import io.quarkus.test.TestTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes de integração para {@link OrdemDeServicoRepositoryImpl}.
 *
 * Usa QuarkusTest + H2 in-memory (MODE=PostgreSQL) + Hibernate drop-and-create.
 * Cada teste roda em transação isolada com rollback automático via @TestTransaction.
 */
@QuarkusTest
class OrdemDeServicoRepositoryImplTest {

    @Inject
    OrdemDeServicoRepositoryImpl repository;

    @Test
    @TestTransaction
    void findAllWithFilters_semFiltros_retornaTodas() {
        repository.save(criarOS("Problema A"));
        repository.save(criarOS("Problema B"));
        repository.save(criarOS("Problema C"));

        var resultado = repository.findAllWithFilters(null, null, null, null, null, 0, 100);

        assertThat(resultado).hasSize(3);
    }

    @Test
    @TestTransaction
    void findAllWithFilters_porStatus_retornaFiltradas() {
        var os1 = repository.save(criarOS("Problema A"));
        var os2 = repository.save(criarOS("Problema B"));
        os2.iniciarDiagnostico();
        repository.save(os2);

        var recebidas = repository.findAllWithFilters("RECEBIDA", null, null, null, null, 0, 100);
        var diagnosticos = repository.findAllWithFilters("EM_DIAGNOSTICO", null, null, null, null, 0, 100);

        assertThat(recebidas).hasSize(1);
        assertThat(recebidas.get(0).getId()).isEqualTo(os1.getId());
        assertThat(diagnosticos).hasSize(1);
        assertThat(diagnosticos.get(0).getId()).isEqualTo(os2.getId());
    }

    @Test
    @TestTransaction
    void findAllWithFilters_porClienteEVeiculo_retornaFiltradas() {
        UUID clienteX = UUID.randomUUID();
        UUID veiculoX = UUID.randomUUID();
        UUID clienteY = UUID.randomUUID();

        repository.save(criarOS(clienteX, veiculoX, "Problema X"));
        repository.save(criarOS(clienteY, UUID.randomUUID(), "Problema Y"));

        var porCliente = repository.findAllWithFilters(null, clienteX, null, null, null, 0, 100);
        var porVeiculo = repository.findAllWithFilters(null, null, veiculoX, null, null, 0, 100);
        var porAmbos = repository.findAllWithFilters(null, clienteX, veiculoX, null, null, 0, 100);

        assertThat(porCliente).hasSize(1);
        assertThat(porCliente.get(0).getClienteId()).isEqualTo(clienteX);
        assertThat(porVeiculo).hasSize(1);
        assertThat(porVeiculo.get(0).getVeiculoId()).isEqualTo(veiculoX);
        assertThat(porAmbos).hasSize(1);
    }

    @Test
    @TestTransaction
    void findAllWithFilters_combinacaoStatusECliente_retornaFiltradas() {
        UUID cliente = UUID.randomUUID();
        var os1 = repository.save(criarOS(cliente, UUID.randomUUID(), "Problema 1"));
        var os2 = repository.save(criarOS(cliente, UUID.randomUUID(), "Problema 2"));
        os2.iniciarDiagnostico();
        repository.save(os2);

        var recebidasCliente = repository.findAllWithFilters("RECEBIDA", cliente, null, null, null, 0, 100);

        assertThat(recebidasCliente).hasSize(1);
        assertThat(recebidasCliente.get(0).getId()).isEqualTo(os1.getId());
    }

    @Test
    @TestTransaction
    void findAllWithFilters_semResultados_retornaListaVazia() {
        var resultado = repository.findAllWithFilters("RECEBIDA", null, null, null, null, 0, 100);

        assertThat(resultado).isEmpty();
    }

    @Test
    @TestTransaction
    void calcularTempoMedioPorMecanico_agrupaCorretamente() {
        UUID mecanicoA = UUID.randomUUID();
        UUID mecanicoB = UUID.randomUUID();

        var os1 = criarOS("Problema A");
        avancarParaFinalizada(os1, mecanicoA);
        var os2 = criarOS("Problema B");
        avancarParaFinalizada(os2, mecanicoA);
        var os3 = criarOS("Problema C");
        avancarParaFinalizada(os3, mecanicoB);

        var breakdown = repository.calcularTempoMedioPorMecanico(null, null);

        assertThat(breakdown).containsOnlyKeys(mecanicoA, mecanicoB);
        // Valores podem ser 0.0 devido a timestamps próximos (ms de diferença)
        // O importante é que o agrupamento por mecânico funcione corretamente
    }

    @Test
    @TestTransaction
    void calcularTempoMedioPorMecanico_semOSFinalizadas_retornaMapaVazio() {
        repository.save(criarOS("Problema"));

        var breakdown = repository.calcularTempoMedioPorMecanico(null, null);

        assertThat(breakdown).isEmpty();
    }

    @Test
    @TestTransaction
    void findAllWithFilters_excluiStatusTerminais() {
        // OS ativa não-terminal deve aparecer
        var ativa = repository.save(criarOS("OS Ativa"));

        // OS em status terminais NÃO devem aparecer
        repository.save(criarOSComStatus("OS Finalizada", StatusOS.FINALIZADA));
        repository.save(criarOSComStatus("OS Entregue", StatusOS.ENTREGUE));
        repository.save(criarOSComStatus("OS Cancelada", StatusOS.CANCELADA));

        var resultado = repository.findAllWithFilters(null, null, null, null, null, 0, 100);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getId()).isEqualTo(ativa.getId());
    }

    @Test
    @TestTransaction
    void findAllWithFilters_ordenaPorPrioridadeDeStatus() {
        // Cria OS em ordem "aleatória" de status; a listagem deve ordenar por prioridade
        var recebida = repository.save(criarOSComStatus("Recebida", StatusOS.RECEBIDA));
        var emExecucao = repository.save(criarOSComStatus("Em Execução", StatusOS.EM_EXECUCAO));
        var aguardandoExecucao = repository.save(criarOSComStatus("Aguardando Execução", StatusOS.AGUARDANDO_EXECUCAO));
        var aguardandoAprovacao = repository.save(criarOSComStatus("Aguardando Aprovação", StatusOS.AGUARDANDO_APROVACAO));
        var emDiagnostico = repository.save(criarOSComStatus("Em Diagnóstico", StatusOS.EM_DIAGNOSTICO));

        var resultado = repository.findAllWithFilters(null, null, null, null, null, 0, 100);

        // Ordem esperada: EM_EXECUCAO(0), AGUARDANDO_APROVACAO(1), EM_DIAGNOSTICO(2), RECEBIDA(3), AGUARDANDO_EXECUCAO(4)
        assertThat(resultado).extracting(OrdemDeServico::getId)
                .containsExactly(
                        emExecucao.getId(),
                        aguardandoAprovacao.getId(),
                        emDiagnostico.getId(),
                        recebida.getId(),
                        aguardandoExecucao.getId());
    }

    @Test
    @TestTransaction
    void findAllWithFilters_mesmoStatus_ordenaPorCreatedAtAsc() {
        var antiga = repository.save(
                criarOSComStatusECreatedAt("Antiga", StatusOS.RECEBIDA, LocalDateTime.now().minusDays(3)));
        var media = repository.save(
                criarOSComStatusECreatedAt("Média", StatusOS.RECEBIDA, LocalDateTime.now().minusDays(2)));
        var nova = repository.save(
                criarOSComStatusECreatedAt("Nova", StatusOS.RECEBIDA, LocalDateTime.now().minusDays(1)));

        var resultado = repository.findAllWithFilters("RECEBIDA", null, null, null, null, 0, 100);

        // Mais antigas primeiro (createdAt ASC)
        assertThat(resultado).extracting(OrdemDeServico::getId)
                .containsExactly(antiga.getId(), media.getId(), nova.getId());
    }

    // ─────────────── Helpers ───────────────

    private static OrdemDeServico criarOS(String descricao) {
        return OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), descricao);
    }

    private static OrdemDeServico criarOS(UUID clienteId, UUID veiculoId, String descricao) {
        return OrdemDeServico.create(clienteId, veiculoId, descricao);
    }

    private void avancarParaFinalizada(OrdemDeServico os, UUID mecanico) {
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        os.aprovarOrcamento(UUID.randomUUID());
        os.iniciarExecucao(mecanico, null);
        os.finalizarExecucao(null);
        repository.save(os);
    }

    private static OrdemDeServico criarOSComStatus(String descricao, StatusOS status) {
        return criarOSComStatusECreatedAt(descricao, status, LocalDateTime.now());
    }

    private static OrdemDeServico criarOSComStatusECreatedAt(String descricao, StatusOS status,
                                                             LocalDateTime createdAt) {
        return OrdemDeServico.reconstitute(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                descricao, status, null,
                null, null, null, null, null, null,
                StatusPagamento.NAO_COBRADO, null, null, null, null,
                StatusEntrega.NAO_LIBERADA, null, null, null, null, null,
                createdAt, 0L);
    }
}