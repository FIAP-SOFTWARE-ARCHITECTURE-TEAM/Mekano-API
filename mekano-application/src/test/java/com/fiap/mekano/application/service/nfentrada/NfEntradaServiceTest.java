package com.fiap.mekano.application.service.nfentrada;

import com.fiap.mekano.domain.event.EstoqueMinimoAtingidoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.MotivoRequisicao;
import com.fiap.mekano.domain.model.NfEntrada;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import com.fiap.mekano.domain.port.in.CreateNfEntradaCommand;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.NfEntradaRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NfEntradaService")
class NfEntradaServiceTest {

    @Mock
    NfEntradaRepositoryPort nfRepository;

    @Mock
    PecaRepositoryPort pecaRepository;

    @Mock
    RequisicaoCompraRepositoryPort requisicaoRepository;

    @Mock
    EventPublisher eventPublisher;

    @InjectMocks
    NfEntradaService nfEntradaService;

    @Test
    @DisplayName("registrar com peça abaixo do mínimo deve publicar evento")
    void registrar_comPecaAbaixoMinimo_devePublicarEvento() {
        UUID pecaId = UUID.randomUUID();
        UUID requisicaoId = UUID.randomUUID();

        Peca peca = Peca.reconstitute(
                pecaId, "PEA-001", "Óleo do Motor 5W30",
                new BigDecimal("45.90"), 3L, 10L, LocalDateTime.now(), 0L);

        RequisicaoCompra requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, pecaId, 5L, StatusRequisicao.PRODUTO_RECEBIDO, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());

        NfEntrada nfEntrada = NfEntrada.create("35200612345678901234567890123456789012345678", new BigDecimal("229.50"), pecaId, requisicaoId);

        when(requisicaoRepository.buscarPorId(requisicaoId)).thenReturn(Optional.of(requisicao));
        when(nfRepository.salvar(any())).thenReturn(nfEntrada);
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));

        var command = new CreateNfEntradaCommand("35200612345678901234567890123456789012345678", new BigDecimal("229.50"), requisicaoId);
        nfEntradaService.registrar(command);

        verify(pecaRepository).creditarSaldo(pecaId, 5);
        verify(eventPublisher).publish(any(EstoqueMinimoAtingidoEvent.class));

        ArgumentCaptor<EstoqueMinimoAtingidoEvent> captor = ArgumentCaptor.forClass(EstoqueMinimoAtingidoEvent.class);
        verify(eventPublisher).publish(captor.capture());
        EstoqueMinimoAtingidoEvent event = captor.getValue();
        assertEquals(pecaId, event.pecaId());
        assertEquals(3, event.saldoAtual());
        assertEquals(10, event.estoqueMinimo());
    }

    @Test
    @DisplayName("registrar com peça acima do mínimo não deve publicar evento")
    void registrar_comPecaAcimaMinimo_naoDevePublicarEvento() {
        UUID pecaId = UUID.randomUUID();
        UUID requisicaoId = UUID.randomUUID();

        Peca peca = Peca.reconstitute(
                pecaId, "PEA-001", "Óleo do Motor 5W30",
                new BigDecimal("45.90"), 20L, 10L, LocalDateTime.now(), 0L);

        RequisicaoCompra requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, pecaId, 5L, StatusRequisicao.PRODUTO_RECEBIDO, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());

        NfEntrada nfEntrada = NfEntrada.create("35200612345678901234567890123456789012345678", new BigDecimal("229.50"), pecaId, requisicaoId);

        when(requisicaoRepository.buscarPorId(requisicaoId)).thenReturn(Optional.of(requisicao));
        when(nfRepository.salvar(any())).thenReturn(nfEntrada);
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.of(peca));

        var command = new CreateNfEntradaCommand("35200612345678901234567890123456789012345678", new BigDecimal("229.50"), requisicaoId);
        nfEntradaService.registrar(command);

        verify(pecaRepository).creditarSaldo(pecaId, 5);
        verify(eventPublisher, never()).publish(any(EstoqueMinimoAtingidoEvent.class));
    }

    @Test
    @DisplayName("registrar com peça inexistente não deve lançar exceção")
    void registrar_comPecaInexistente_naoDeveLancarExcecao() {
        UUID pecaId = UUID.randomUUID();
        UUID requisicaoId = UUID.randomUUID();

        RequisicaoCompra requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, pecaId, 5L, StatusRequisicao.PRODUTO_RECEBIDO, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());

        NfEntrada nfEntrada = NfEntrada.create("35200612345678901234567890123456789012345678", new BigDecimal("229.50"), pecaId, requisicaoId);

        when(requisicaoRepository.buscarPorId(requisicaoId)).thenReturn(Optional.of(requisicao));
        when(nfRepository.salvar(any())).thenReturn(nfEntrada);
        when(pecaRepository.buscarPorId(pecaId)).thenReturn(Optional.empty());

        var command = new CreateNfEntradaCommand("35200612345678901234567890123456789012345678", new BigDecimal("229.50"), requisicaoId);
        assertDoesNotThrow(() -> nfEntradaService.registrar(command));

        verify(pecaRepository).creditarSaldo(pecaId, 5);
        verify(eventPublisher, never()).publish(any(EstoqueMinimoAtingidoEvent.class));
    }
}