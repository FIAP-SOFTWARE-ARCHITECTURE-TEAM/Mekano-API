package com.fiap.mekano.infrastructure.observer;

import com.fiap.mekano.application.service.peca.PecaService;
import com.fiap.mekano.application.service.requisicao.RequisicaoCompraService;
import com.fiap.mekano.domain.event.OrcamentoAprovadoEvent;
import com.fiap.mekano.domain.model.MotivoRequisicao;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("PecaOrcamentoObserver")
class PecaOrcamentoObserverTest {

    @Test
    @DisplayName("reserva com disponivel suficiente não deve criar requisição de compra")
    void reservaOkNaoCriaRequisicao() {
        PecaService pecaService = mock(PecaService.class);
        RequisicaoCompraService requisicaoService = mock(RequisicaoCompraService.class);
        PecaOrcamentoObserver observer = new PecaOrcamentoObserver(pecaService, requisicaoService);

        UUID pecaId = UUID.randomUUID();
        Peca peca = Peca.reconstitute(
                pecaId, "PECA-001", "Parafuso", BigDecimal.TEN,
                20L, 5L, LocalDateTime.now(), 0L
        );
        when(pecaService.buscarPorId(pecaId)).thenReturn(peca);
        when(pecaService.reservarSaldo(pecaId, 10)).thenReturn(true);

        var event = new OrcamentoAprovadoEvent(
                UUID.randomUUID(),
                List.of(new OrcamentoAprovadoEvent.ItemOrcamento(pecaId, 10))
        );

        observer.aoOrcamentoAprovado(event);

        verify(pecaService).reservarSaldo(pecaId, 10);
        verify(requisicaoService, never()).criar(any());
    }

    @Test
    @DisplayName("reserva com disponivel insuficiente deve reservar e criar requisição com faltante")
    void reservaFalhaCriaRequisicaoComFaltante() {
        PecaService pecaService = mock(PecaService.class);
        RequisicaoCompraService requisicaoService = mock(RequisicaoCompraService.class);
        PecaOrcamentoObserver observer = new PecaOrcamentoObserver(pecaService, requisicaoService);

        UUID pecaId = UUID.randomUUID();
        Peca peca = Peca.reconstitute(
                pecaId, "PECA-001", "Parafuso", BigDecimal.TEN,
                10L, 5L, LocalDateTime.now(), 7L
        );
        when(pecaService.buscarPorId(pecaId)).thenReturn(peca);
        when(pecaService.reservarSaldo(pecaId, 10)).thenReturn(true);

        var event = new OrcamentoAprovadoEvent(
                UUID.randomUUID(),
                List.of(new OrcamentoAprovadoEvent.ItemOrcamento(pecaId, 10))
        );

        observer.aoOrcamentoAprovado(event);

        // disponivel = 10 - 7 = 3; 3 - 10 = -7 → faltante 7
        ArgumentCaptor<CreateRequisicaoCompraCommand> captor = ArgumentCaptor.forClass(CreateRequisicaoCompraCommand.class);
        verify(requisicaoService, times(1)).criar(captor.capture());

        CreateRequisicaoCompraCommand command = captor.getValue();
        assertEquals(pecaId, command.pecaId());
        assertEquals(7, command.quantidade());
        assertEquals(MotivoRequisicao.ORDEM_SERVICO, command.motivo());
    }

    @Test
    @DisplayName("reserva com disponivel exatamente igual à quantidade não deve criar requisição")
    void reservaDisponivelExatoNaoCriaRequisicao() {
        PecaService pecaService = mock(PecaService.class);
        RequisicaoCompraService requisicaoService = mock(RequisicaoCompraService.class);
        PecaOrcamentoObserver observer = new PecaOrcamentoObserver(pecaService, requisicaoService);

        UUID pecaId = UUID.randomUUID();
        Peca peca = Peca.reconstitute(
                pecaId, "PECA-001", "Parafuso", BigDecimal.TEN,
                15L, 5L, LocalDateTime.now(), 10L
        );
        when(pecaService.buscarPorId(pecaId)).thenReturn(peca);
        when(pecaService.reservarSaldo(pecaId, 5)).thenReturn(true);

        var event = new OrcamentoAprovadoEvent(
                UUID.randomUUID(),
                List.of(new OrcamentoAprovadoEvent.ItemOrcamento(pecaId, 5))
        );

        observer.aoOrcamentoAprovado(event);

        // disponivel = 15 - 10 = 5; 5 - 5 = 0 → não cria req
        verify(pecaService).reservarSaldo(pecaId, 5);
        verify(requisicaoService, never()).criar(any());
    }
}
