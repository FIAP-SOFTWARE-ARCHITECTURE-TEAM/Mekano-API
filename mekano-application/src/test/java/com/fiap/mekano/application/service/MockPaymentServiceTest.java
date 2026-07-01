package com.fiap.mekano.application.service;

import com.fiap.mekano.domain.event.PagamentoConfirmadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.os.StatusPagamento;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.ProcessedEventsRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MockPaymentService")
class MockPaymentServiceTest {

    @Mock OrdemDeServicoRepositoryPort ordemDeServicoRepository;
    @Mock ProcessedEventsRepositoryPort processedEventsRepository;
    @Mock EventPublisher eventPublisher;

    @InjectMocks
    MockPaymentService mockPaymentService;

    @Test
    @DisplayName("confirmarPagamento deve atualizar status e publicar evento")
    void confirmarPagamento_deveAtualizarStatusEPublicarEvento() {
        var os = criarOSComCobrancaPendente();
        var osUuid = os.getId();

        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(processedEventsRepository.existsFor("PAGAMENTO_CONFIRMADO", osUuid)).thenReturn(false);
        when(ordemDeServicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        mockPaymentService.confirmarPagamento(osUuid);

        assertEquals(StatusPagamento.CONFIRMADO, os.getStatusPagamento());
        verify(eventPublisher, times(1)).publish(any(PagamentoConfirmadoEvent.class));
        verify(processedEventsRepository, times(1)).save("PAGAMENTO_CONFIRMADO", osUuid);
    }

    @Test
    @DisplayName("confirmarPagamento deve lançar 409 se já CONFIRMADO")
    void confirmarPagamento_deveLancarExcecaoSeJaConfirmado() {
        var os = criarOSComCobrancaPendente();
        os.confirmarPagamento("PIX-123");
        var osUuid = os.getId();

        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));

        var ex = assertThrows(AppException.class, () -> mockPaymentService.confirmarPagamento(osUuid));
        assertEquals(409, ex.getStatus());
    }

    @Test
    @DisplayName("confirmarPagamento deve lançar 409 se não está AGUARDANDO_PAGAMENTO")
    void confirmarPagamento_deveLancarExcecaoSeNaoPendente() {
        var os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        var osUuid = os.getId();

        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));

        var ex = assertThrows(AppException.class, () -> mockPaymentService.confirmarPagamento(osUuid));
        assertEquals(409, ex.getStatus());
    }

    @Test
    @DisplayName("confirmarPagamento deve lançar 404 se OS não existe")
    void confirmarPagamento_deveLancar404SeOSNaoExiste() {
        var osUuid = UUID.randomUUID();

        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> mockPaymentService.confirmarPagamento(osUuid));
    }

    private static OrdemDeServico criarOSComCobrancaPendente() {
        var os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        os.aprovarOrcamento(UUID.randomUUID());
        os.finalizarExecucao(null);
        os.gerarCobranca();
        return os;
    }
}