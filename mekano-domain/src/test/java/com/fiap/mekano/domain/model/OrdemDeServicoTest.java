package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.exception.AppException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrdemDeServico — máquina de estados (8×8 = 64 transições)")
class OrdemDeServicoTest {

    /**
     * Matriz esperada de transições válidas — espelho exato do enum StatusOS.
     */
    private static final Map<StatusOS, Set<StatusOS>> TRANSICOES_VALIDAS = Map.of(
            StatusOS.RECEBIDA, Set.of(StatusOS.EM_DIAGNOSTICO, StatusOS.CANCELADA),
            StatusOS.EM_DIAGNOSTICO, Set.of(StatusOS.AGUARDANDO_APROVACAO, StatusOS.CANCELADA),
            StatusOS.AGUARDANDO_APROVACAO, Set.of(StatusOS.AGUARDANDO_EXECUCAO, StatusOS.CANCELADA),
            StatusOS.AGUARDANDO_EXECUCAO, Set.of(StatusOS.EM_EXECUCAO, StatusOS.CANCELADA),
            StatusOS.EM_EXECUCAO, Set.of(StatusOS.FINALIZADA, StatusOS.CANCELADA),
            StatusOS.FINALIZADA, Set.of(StatusOS.ENTREGUE),
            StatusOS.ENTREGUE, Set.of(),
            StatusOS.CANCELADA, Set.of(StatusOS.ENTREGUE)
    );

    /**
     * Gera todas as 49 combinações (7 origens × 7 destinos).
     */
    static Stream<Arguments> todasTransicoes() {
        StatusOS[] todos = StatusOS.values();
        Stream.Builder<Arguments> builder = Stream.builder();
        for (StatusOS origem : todos) {
            for (StatusOS destino : todos) {
                boolean esperado = TRANSICOES_VALIDAS.get(origem).contains(destino);
                builder.accept(Arguments.of(origem, destino, esperado));
            }
        }
        return builder.build();
    }

    @ParameterizedTest(name = "{0} → {1} = {2}")
    @MethodSource("todasTransicoes")
    @DisplayName("Validar transição")
    void deveValidarTransicao(StatusOS origem, StatusOS destino, boolean esperado) {
        assertEquals(esperado, origem.podeTransicionarPara(destino),
                String.format("Transição %s → %s deveria ser %s", origem, destino, esperado));
    }

    // ─────────────── Testes de criação ───────────────

    @Test
    @DisplayName("create() retorna entidade com status RECEBIDA")
    void createDeveRetornarStatusRecebida() {
        OrdemDeServico os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Motor falhando");

        assertNotNull(os.getId());
        assertEquals(StatusOS.RECEBIDA, os.getStatus());
        assertEquals("Motor falhando", os.getDescricaoProblema());
        assertNotNull(os.getCreatedAt());
        assertNull(os.getMotivoCancelamento());
    }

    @Test
    @DisplayName("create() lança exceção se clienteId null")
    void createDeveLancarExcecaoSemCliente() {
        AppException ex = assertThrows(AppException.class,
                () -> OrdemDeServico.create(null, UUID.randomUUID(), "desc"));
        assertEquals(400, ex.getStatus());
    }

    @Test
    @DisplayName("create() lança exceção se veiculoId null")
    void createDeveLancarExcecaoSemVeiculo() {
        AppException ex = assertThrows(AppException.class,
                () -> OrdemDeServico.create(UUID.randomUUID(), null, "desc"));
        assertEquals(400, ex.getStatus());
    }

    @Test
    @DisplayName("create() lança exceção se descricao null ou blank")
    void createDeveLancarExcecaoSemDescricao() {
        assertThrows(AppException.class,
                () -> OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), null));
        assertThrows(AppException.class,
                () -> OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "   "));
    }

    // ─────────────── Testes de transições explícitas ───────────────

    @Test
    @DisplayName("iniciarDiagnostico() transiciona RECEBIDA → EM_DIAGNOSTICO")
    void iniciarDiagnosticoDeveTransicionar() {
        OrdemDeServico os = criarOS();
        os.iniciarDiagnostico();
        assertEquals(StatusOS.EM_DIAGNOSTICO, os.getStatus());
    }

    @Test
    @DisplayName("finalizarDiagnostico() transiciona EM_DIAGNOSTICO → AGUARDANDO_APROVACAO")
    void finalizarDiagnosticoDeveTransicionar() {
        OrdemDeServico os = criarOS();
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        assertEquals(StatusOS.AGUARDANDO_APROVACAO, os.getStatus());
    }

    @Test
    @DisplayName("aprovarOrcamento() transiciona AGUARDANDO_APROVACAO → AGUARDANDO_EXECUCAO")
    void aprovarOrcamentoDeveTransicionar() {
        OrdemDeServico os = criarOS();
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        os.aprovarOrcamento(UUID.randomUUID());
        assertEquals(StatusOS.AGUARDANDO_EXECUCAO, os.getStatus());
    }

    @Test
    @DisplayName("reprovarOrcamento() transiciona AGUARDANDO_APROVACAO → CANCELADA")
    void reprovarOrcamentoDeveTransicionar() {
        OrdemDeServico os = criarOS();
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        os.reprovarOrcamento("Valor muito alto");
        assertEquals(StatusOS.CANCELADA, os.getStatus());
        assertEquals("Valor muito alto", os.getMotivoCancelamento());
    }

    @Test
    @DisplayName("reprovarOrcamento() lança exceção se motivo é blank")
    void reprovarOrcamentoDeveLancarExcecaoSemMotivo() {
        OrdemDeServico os = criarOS();
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        assertThrows(AppException.class, () -> os.reprovarOrcamento("  "));
    }

    @Test
    @DisplayName("cancelar() transiciona para CANCELADA com motivo")
    void cancelarDeveTransicionar() {
        OrdemDeServico os = criarOS();
        os.cancelar("Cliente desistiu");
        assertEquals(StatusOS.CANCELADA, os.getStatus());
        assertEquals("Cliente desistiu", os.getMotivoCancelamento());
    }

    @Test
    @DisplayName("cancelarPorSLA() transiciona para CANCELADA com motivo automático")
    void cancelarPorSLADeveTransicionar() {
        OrdemDeServico os = criarOS();
        os.cancelarPorSLA();
        assertEquals(StatusOS.CANCELADA, os.getStatus());
        assertEquals("SLA expirado", os.getMotivoCancelamento());
    }

    @Test
    @DisplayName("finalizar-execucao transiciona EM_EXECUCAO → FINALIZADA")
    void finalizarDeveTransicionar() {
        OrdemDeServico os = criarOS();
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        os.aprovarOrcamento(UUID.randomUUID());
        os.iniciarExecucao(UUID.randomUUID(), null);
        os.finalizarExecucao(null);
        assertEquals(StatusOS.FINALIZADA, os.getStatus());
    }

    @Test
    @DisplayName("entregar() transiciona FINALIZADA → ENTREGUE")
    void entregarDeveTransicionar() {
        OrdemDeServico os = criarOS();
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        os.aprovarOrcamento(UUID.randomUUID());
        os.iniciarExecucao(UUID.randomUUID(), null);
        os.finalizarExecucao(null);
        os.gerarCobranca();
        os.confirmarPagamento("PIX-123");
        os.entregar("Cliente");
        assertEquals(StatusOS.ENTREGUE, os.getStatus());
    }

    @Test
    @DisplayName("ENTREGUE é terminal — nenhuma transição de saída")
    void entregueDeveSerTerminal() {
        OrdemDeServico os = criarOS();
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        os.aprovarOrcamento(UUID.randomUUID());
        os.iniciarExecucao(UUID.randomUUID(), null);
        os.finalizarExecucao(null);
        os.gerarCobranca();
        os.confirmarPagamento("PIX-123");
        os.entregar("Cliente");

        assertThrows(AppException.class, os::iniciarDiagnostico);
        assertThrows(AppException.class, () -> os.cancelar("motivo"));
    }

    @Test
    @DisplayName("CANCELADA permite transição para ENTREGUE (devolução de veículo)")
    void canceladaDevePermitirEntrega() {
        OrdemDeServico os = criarOS();
        os.cancelar("motivo");

        assertThrows(AppException.class, os::iniciarDiagnostico);
        assertThrows(AppException.class, () -> os.finalizarExecucao(null));

        os.entregar("Cliente");
        assertEquals(StatusOS.ENTREGUE, os.getStatus());
    }

    @Test
    @DisplayName("transição inválida RECEBIDA → FINALIZADA lança 422")
    void transicaoInvalidaDeveLancar422() {
        OrdemDeServico os = criarOS();
        AppException ex = assertThrows(AppException.class, () -> os.finalizarExecucao(null));
        assertEquals(422, ex.getStatus());
    }

    @Test
    @DisplayName("reconstitute() preserva valores originais")
    void reconstituteDevePreservarValores() {
        UUID id = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 10, 0);

        OrdemDeServico os = OrdemDeServico.reconstitute(
                id, clienteId, veiculoId,
                "Barulho no motor",
                StatusOS.EM_EXECUCAO, null, null, null,
                null, null, null, null, null, null, null,
                null, null, null, null, null,
                null, null, null, createdAt, 3L
        );

        assertEquals(id, os.getId());
        assertEquals(clienteId, os.getClienteId());
        assertEquals(veiculoId, os.getVeiculoId());
        assertEquals(StatusOS.EM_EXECUCAO, os.getStatus());
        assertEquals(3L, os.getVersion());
        assertEquals(createdAt, os.getCreatedAt());
    }

    // ─────────────── Helper ───────────────

    private OrdemDeServico criarOS() {
        return OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Motor falhando");
    }
}
