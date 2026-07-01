package com.fiap.mekano.application.listener;

import com.fiap.mekano.domain.event.CobrancaGeradaEvent;
import com.fiap.mekano.domain.event.OSFinalizadaEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.StatusOrcamento;
import com.fiap.mekano.domain.os.StatusPagamento;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.ProcessedEventsRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CobrancaEmitidaListener")
class CobrancaEmitidaListenerTest {

    @Mock OrdemDeServicoRepositoryPort ordemDeServicoRepository;
    @Mock OrcamentoRepositoryPort orcamentoRepository;
    // TODO(#33): remover mock e usar implementação real quando #33 for feita
    @Mock ProcessedEventsRepositoryPort processedEventsRepository;
    @Mock EventPublisher eventPublisher;

    @InjectMocks
    CobrancaEmitidaListener listener;

    @Test
    @DisplayName("deve emitir cobrança ao finalizar OS com orçamento")
    void deveEmitirCobranca() {
        var os = criarOSFinalizada();
        var osUuid = os.getId();

        var orcamento = Orcamento.create("Teste",
                List.of(new ItemOrcamento("Item", 1L, BigDecimal.TEN)), osUuid);

        var event = OSFinalizadaEvent.of(osUuid);

        when(processedEventsRepository.existsFor("COBRANCA_EMITIDA", osUuid)).thenReturn(false);
        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(orcamentoRepository.findByOrdemServicoUuid(osUuid)).thenReturn(Optional.of(orcamento));
        when(ordemDeServicoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        listener.on(event);

        assertEquals(StatusPagamento.AGUARDANDO_PAGAMENTO, os.getStatusPagamento());
        verify(eventPublisher, times(1)).publish(any(CobrancaGeradaEvent.class));
        verify(processedEventsRepository, times(1)).save("COBRANCA_EMITIDA", osUuid);
    }

    @Test
    @DisplayName("deve ignorar evento já processado")
    void deveIgnorarEventoJaProcessado() {
        var osUuid = UUID.randomUUID();
        var event = OSFinalizadaEvent.of(osUuid);

        when(processedEventsRepository.existsFor("COBRANCA_EMITIDA", osUuid)).thenReturn(true);

        listener.on(event);

        verify(ordemDeServicoRepository, never()).findById(any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("deve lançar 404 se OS não existe")
    void deveLancar404SeOSNaoExiste() {
        var osUuid = UUID.randomUUID();
        var event = OSFinalizadaEvent.of(osUuid);

        when(processedEventsRepository.existsFor("COBRANCA_EMITIDA", osUuid)).thenReturn(false);
        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> listener.on(event));
    }

    @Test
    @DisplayName("deve lançar 404 se orçamento não existe")
    void deveLancar404SeOrcamentoNaoExiste() {
        var os = criarOSFinalizada();
        var osUuid = os.getId();
        var event = OSFinalizadaEvent.of(osUuid);

        when(processedEventsRepository.existsFor("COBRANCA_EMITIDA", osUuid)).thenReturn(false);
        when(ordemDeServicoRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(orcamentoRepository.findByOrdemServicoUuid(osUuid)).thenReturn(Optional.empty());

        assertThrows(AppException.class, () -> listener.on(event));
    }

    private static OrdemDeServico criarOSFinalizada() {
        var os = OrdemDeServico.create(UUID.randomUUID(), UUID.randomUUID(), "Problema");
        os.iniciarDiagnostico();
        os.finalizarDiagnostico();
        os.aprovarOrcamento(UUID.randomUUID());
        os.finalizarExecucao(null);
        return os;
    }
}