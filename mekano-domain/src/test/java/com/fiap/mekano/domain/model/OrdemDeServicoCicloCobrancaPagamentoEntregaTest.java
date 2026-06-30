package com.fiap.mekano.domain.model;

import com.fiap.mekano.domain.event.CobrancaGeradaEvent;
import com.fiap.mekano.domain.event.EntregaConfirmadaEvent;
import com.fiap.mekano.domain.event.PagamentoConfirmadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.os.StatusEntrega;
import com.fiap.mekano.domain.os.StatusPagamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OrdemDeServico - cobrança, pagamento e entrega")
class OrdemDeServicoCicloCobrancaPagamentoEntregaTest {

    @Test
    @DisplayName("deve criar OS com status inicial, pagamento não cobrado e entrega não liberada")
    void deveCriarOsComEstadosIniciais() {
        OrdemDeServico os = novaOS();

        assertNotNull(os.getId());
        assertEquals(StatusOS.RECEBIDA, os.getStatus());
        assertEquals(StatusPagamento.NAO_COBRADO, os.getStatusPagamento());
        assertEquals(StatusEntrega.NAO_LIBERADA, os.getStatusEntrega());
        assertNotNull(os.getCreatedAt());
        assertEquals(0L, os.getVersion());
    }

    @Test
    @DisplayName("deve executar ciclo completo: cobrança, pagamento confirmado e entrega")
    void deveExecutarCicloCompleto() {
        OrdemDeServico os = osFinalizada();

        CobrancaGeradaEvent cobranca = os.gerarCobranca();

        assertEquals(StatusPagamento.AGUARDANDO_PAGAMENTO, os.getStatusPagamento());
        assertEquals(StatusEntrega.NAO_LIBERADA, os.getStatusEntrega());
        assertNotNull(os.getCobrancaGeradaEm());
        assertEquals(os.getId(), cobranca.osUuid());

        PagamentoConfirmadoEvent pagamento = os.confirmarPagamento("PIX-123");

        assertEquals(StatusPagamento.CONFIRMADO, os.getStatusPagamento());
        assertEquals(StatusEntrega.LIBERADA_PARA_ENTREGA, os.getStatusEntrega());
        assertEquals("PIX-123", os.getReferenciaPagamento());
        assertNotNull(os.getPagamentoConfirmadoEm());
        assertEquals(os.getId(), pagamento.osUuid());

        EntregaConfirmadaEvent entrega = os.entregar("João Cliente");

        assertEquals(StatusOS.ENTREGUE, os.getStatus());
        assertEquals(StatusEntrega.ENTREGUE, os.getStatusEntrega());
        assertEquals("João Cliente", os.getRecebidoPor());
        assertNotNull(os.getEntregueEm());
        assertEquals(os.getId(), entrega.osUuid());
    }

    @Test
    @DisplayName("deve gerar cobrança vinculada ao orçamento")
    void deveGerarCobrancaComOrcamento() {
        OrdemDeServico os = osFinalizada();
        UUID orcamentoUuid = UUID.randomUUID();

        os.gerarCobranca(orcamentoUuid, BigDecimal.valueOf(1500.00));

        assertEquals(orcamentoUuid, os.getOrcamentoUuid());
        assertEquals(StatusPagamento.AGUARDANDO_PAGAMENTO, os.getStatusPagamento());
        assertNotNull(os.getCobrancaGeradaEm());
    }

    @Test
    @DisplayName("não deve gerar cobrança antes da OS estar finalizada")
    void naoDeveGerarCobrancaAntesDaOsFinalizada() {
        OrdemDeServico os = novaOS();

        AppException exception = assertThrows(
                AppException.class,
                os::gerarCobranca
        );

        assertTrue(exception.getMessage().contains("FINALIZADA"));
    }

    @Test
    @DisplayName("não deve confirmar pagamento sem cobrança gerada")
    void naoDeveConfirmarPagamentoSemCobranca() {
        OrdemDeServico os = osFinalizada();

        AppException exception = assertThrows(
                AppException.class,
                () -> os.confirmarPagamento("PIX-123")
        );

        assertTrue(exception.getMessage().contains("AGUARDANDO_PAGAMENTO"));
    }

    @Test
    @DisplayName("não deve confirmar pagamento com referência vazia")
    void naoDeveConfirmarPagamentoComReferenciaVazia() {
        OrdemDeServico os = osFinalizada();
        os.gerarCobranca();

        assertThrows(
                AppException.class,
                () -> os.confirmarPagamento(" ")
        );
    }

    @Test
    @DisplayName("não deve entregar sem pagamento confirmado")
    void naoDeveEntregarSemPagamentoConfirmado() {
        OrdemDeServico os = osFinalizada();
        os.gerarCobranca();

        AppException exception = assertThrows(
                AppException.class,
                () -> os.entregar("João Cliente")
        );

        assertTrue(exception.getMessage().contains("pagamento confirmado"));
    }

    @Test
    @DisplayName("não deve entregar com recebedor vazio")
    void naoDeveEntregarComRecebedorVazio() {
        OrdemDeServico os = osFinalizada();
        os.gerarCobranca();
        os.confirmarPagamento("PIX-123");

        assertThrows(
                AppException.class,
                () -> os.entregar(" ")
        );
    }

    @Test
    @DisplayName("não deve gerar cobrança duas vezes")
    void naoDeveGerarCobrancaDuasVezes() {
        OrdemDeServico os = osFinalizada();

        os.gerarCobranca();

        AppException exception = assertThrows(
                AppException.class,
                os::gerarCobranca
        );

        assertTrue(exception.getMessage().contains("AGUARDANDO_PAGAMENTO"));
    }

    @Test
    @DisplayName("não deve confirmar pagamento duas vezes")
    void naoDeveConfirmarPagamentoDuasVezes() {
        OrdemDeServico os = osFinalizada();

        os.gerarCobranca();
        os.confirmarPagamento("PIX-123");

        AppException exception = assertThrows(
                AppException.class,
                () -> os.confirmarPagamento("PIX-456")
        );

        assertTrue(exception.getMessage().contains("CONFIRMADO"));
    }

    @Test
    @DisplayName("não deve entregar duas vezes")
    void naoDeveEntregarDuasVezes() {
        OrdemDeServico os = osFinalizada();

        os.gerarCobranca();
        os.confirmarPagamento("PIX-123");
        os.entregar("João Cliente");

        AppException exception = assertThrows(
                AppException.class,
                () -> os.entregar("Maria Cliente")
        );

        assertTrue(exception.getMessage().contains("ENTREGUE"));
    }

    @Test
    @DisplayName("cancelamento antes do pagamento confirmado deve cancelar pagamento e bloquear entrega")
    void deveCancelarPagamentoEEntregaQuandoCancelarAntesDoPagamentoConfirmado() {
        OrdemDeServico os = novaOS();

        os.cancelar("Cliente desistiu");

        assertEquals(StatusOS.CANCELADA, os.getStatus());
        assertEquals("Cliente desistiu", os.getMotivoCancelamento());
        assertEquals(StatusPagamento.CANCELADO, os.getStatusPagamento());
        assertEquals(StatusEntrega.NAO_LIBERADA, os.getStatusEntrega());
    }

    @Test
    @DisplayName("reconstitute completo deve preservar campos de pagamento e entrega")
    void reconstituteCompletoDevePreservarPagamentoEEntrega() {
        UUID id = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        UUID veiculoId = UUID.randomUUID();
        UUID orcamentoUuid = UUID.randomUUID();
        UUID mecanicoUuid = UUID.randomUUID();

        LocalDateTime createdAt = LocalDateTime.now().minusDays(5);
        LocalDateTime cobrancaGeradaEm = LocalDateTime.now().minusDays(3);
        LocalDateTime pagamentoConfirmadoEm = LocalDateTime.now().minusDays(2);
        LocalDateTime entregueEm = LocalDateTime.now().minusDays(1);

        OrdemDeServico os = OrdemDeServico.reconstitute(
                id,
                clienteId,
                veiculoId,
                "Motor falhando",
                StatusOS.ENTREGUE,
                null,
                orcamentoUuid,
                mecanicoUuid,
                null,
                null,
                "Executado com sucesso",
                LocalDateTime.now().minusDays(4),
                StatusPagamento.CONFIRMADO,
                null,
                null,
                null,
                null,
                StatusEntrega.ENTREGUE,
                cobrancaGeradaEm,
                pagamentoConfirmadoEm,
                "PIX-999",
                entregueEm,
                "Maria Cliente",
                createdAt,
                20L
        );

        assertEquals(id, os.getId());
        assertEquals(StatusOS.ENTREGUE, os.getStatus());
        assertEquals(StatusPagamento.CONFIRMADO, os.getStatusPagamento());
        assertEquals(StatusEntrega.ENTREGUE, os.getStatusEntrega());
        assertEquals(cobrancaGeradaEm, os.getCobrancaGeradaEm());
        assertEquals(pagamentoConfirmadoEm, os.getPagamentoConfirmadoEm());
        assertEquals("PIX-999", os.getReferenciaPagamento());
        assertEquals(entregueEm, os.getEntregueEm());
        assertEquals("Maria Cliente", os.getRecebidoPor());
    }

    private OrdemDeServico novaOS() {
        return OrdemDeServico.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Motor falhando"
        );
    }

    private OrdemDeServico osFinalizada() {
        OrdemDeServico os = novaOS();

        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        os.aprovarOrcamento(UUID.randomUUID());
        os.iniciarExecucao(UUID.randomUUID(), "Início da execução");
        os.finalizarExecucao("Execução finalizada");

        assertEquals(StatusOS.FINALIZADA, os.getStatus());

        return os;
    }
}
