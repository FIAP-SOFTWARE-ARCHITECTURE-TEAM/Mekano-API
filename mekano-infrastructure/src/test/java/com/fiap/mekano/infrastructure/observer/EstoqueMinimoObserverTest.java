package com.fiap.mekano.infrastructure.observer;

import com.fiap.mekano.domain.event.EstoqueMinimoAtingidoEvent;
import com.fiap.mekano.domain.model.MotivoRequisicao;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.domain.port.in.RequisicaoCompraServicePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("EstoqueMinimoObserver")
class EstoqueMinimoObserverTest {

    @Test
    @DisplayName("deve criar requisição com qtd calculada quando saldo abaixo do mínimo")
    void deveCriarRequisicaoComQtdCalculada() {
        RequisicaoCompraServicePort requisicaoService = mock(RequisicaoCompraServicePort.class);
        EstoqueMinimoObserver observer = new EstoqueMinimoObserver(requisicaoService);

        UUID pecaId = UUID.randomUUID();
        var event = new EstoqueMinimoAtingidoEvent(pecaId, 2, 5);

        observer.aoAtingirEstoqueMinimo(event);

        ArgumentCaptor<CreateRequisicaoCompraCommand> captor = ArgumentCaptor.forClass(CreateRequisicaoCompraCommand.class);
        verify(requisicaoService, times(1)).criar(captor.capture());

        CreateRequisicaoCompraCommand command = captor.getValue();
        assertEquals(1, command.itens().size());
        assertEquals(pecaId, command.itens().get(0).pecaId());
        assertEquals(3, command.itens().get(0).quantidade());
        assertEquals(MotivoRequisicao.ESTOQUE_MINIMO, command.motivo());
    }

    @Test
    @DisplayName("não deve criar requisição quando saldo maior ou igual ao mínimo")
    void naoDeveCriarRequisicaoQuandoSaldoSuficiente() {
        RequisicaoCompraServicePort requisicaoService = mock(RequisicaoCompraServicePort.class);
        EstoqueMinimoObserver observer = new EstoqueMinimoObserver(requisicaoService);

        UUID pecaId = UUID.randomUUID();
        var event = new EstoqueMinimoAtingidoEvent(pecaId, 5, 5);

        observer.aoAtingirEstoqueMinimo(event);

        verify(requisicaoService, never()).criar(any());
    }

    @Test
    @DisplayName("não deve criar requisição quando saldo acima do mínimo")
    void naoDeveCriarRequisicaoQuandoSaldoAcimaMinimo() {
        RequisicaoCompraServicePort requisicaoService = mock(RequisicaoCompraServicePort.class);
        EstoqueMinimoObserver observer = new EstoqueMinimoObserver(requisicaoService);

        UUID pecaId = UUID.randomUUID();
        var event = new EstoqueMinimoAtingidoEvent(pecaId, 10, 5);

        observer.aoAtingirEstoqueMinimo(event);

        verify(requisicaoService, never()).criar(any());
    }
}
