package com.fiap.mekano.application.service.nfentrada;

import com.fiap.mekano.domain.event.EstoqueMinimoAtingidoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.ItemRequisicaoCompra;
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
import java.util.List;
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
    @DisplayName("registrar com múltiplos itens deve creditar cada peça e publicar eventos quando abaixo do mínimo")
    void registrar_comMultiplosItens_deveCreditarCadaPeca() {
        UUID pecaId1 = UUID.randomUUID();
        UUID pecaId2 = UUID.randomUUID();
        UUID requisicaoId = UUID.randomUUID();

        Peca peca1 = Peca.reconstitute(
                pecaId1, "PEA-001", "Óleo do Motor 5W30",
                new BigDecimal("45.90"), 3L, 10L, LocalDateTime.now(), 0L);
        Peca peca2 = Peca.reconstitute(
                pecaId2, "PEA-002", "Filtro de Ar",
                new BigDecimal("25.00"), 20L, 5L, LocalDateTime.now(), 0L);

        RequisicaoCompra requisicao = RequisicaoCompra.reconstitute(
                requisicaoId,
                List.of(new ItemRequisicaoCompra(pecaId1, 5L),
                        new ItemRequisicaoCompra(pecaId2, 10L)),
                StatusRequisicao.PRODUTO_RECEBIDO, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());

        NfEntrada nfEntrada = NfEntrada.create("35200612345678901234567890123456789012345678",
                new BigDecimal("354.50"), requisicaoId);

        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));
        when(nfRepository.save(any())).thenReturn(nfEntrada);
        when(pecaRepository.findById(pecaId1)).thenReturn(Optional.of(peca1));
        when(pecaRepository.findById(pecaId2)).thenReturn(Optional.of(peca2));

        var command = new CreateNfEntradaCommand("35200612345678901234567890123456789012345678",
                new BigDecimal("354.50"), requisicaoId);
        nfEntradaService.registrar(command);

        verify(pecaRepository).creditarSaldo(pecaId1, 5);
        verify(pecaRepository).creditarSaldo(pecaId2, 10);
        verify(eventPublisher, times(1)).publish(any(EstoqueMinimoAtingidoEvent.class));
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
                requisicaoId,
                List.of(new ItemRequisicaoCompra(pecaId, 5L)),
                StatusRequisicao.PRODUTO_RECEBIDO, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());

        NfEntrada nfEntrada = NfEntrada.create("35200612345678901234567890123456789012345678",
                new BigDecimal("229.50"), requisicaoId);

        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));
        when(nfRepository.save(any())).thenReturn(nfEntrada);
        when(pecaRepository.findById(pecaId)).thenReturn(Optional.of(peca));

        var command = new CreateNfEntradaCommand("35200612345678901234567890123456789012345678",
                new BigDecimal("229.50"), requisicaoId);
        nfEntradaService.registrar(command);

        verify(pecaRepository).creditarSaldo(pecaId, 5);
        verify(eventPublisher, never()).publish(any(EstoqueMinimoAtingidoEvent.class));
    }

    @Test
    @DisplayName("registrar com peça inexistente não deve lançar exceção (graceful degradation)")
    void registrar_comPecaInexistente_naoDeveLancarExcecao() {
        UUID pecaId = UUID.randomUUID();
        UUID requisicaoId = UUID.randomUUID();

        RequisicaoCompra requisicao = RequisicaoCompra.reconstitute(
                requisicaoId,
                List.of(new ItemRequisicaoCompra(pecaId, 5L)),
                StatusRequisicao.PRODUTO_RECEBIDO, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());

        NfEntrada nfEntrada = NfEntrada.create("35200612345678901234567890123456789012345678",
                new BigDecimal("229.50"), requisicaoId);

        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));
        when(nfRepository.save(any())).thenReturn(nfEntrada);
        when(pecaRepository.findById(pecaId)).thenReturn(Optional.empty());

        var command = new CreateNfEntradaCommand("35200612345678901234567890123456789012345678",
                new BigDecimal("229.50"), requisicaoId);
        assertDoesNotThrow(() -> nfEntradaService.registrar(command));

        verify(pecaRepository).creditarSaldo(pecaId, 5);
        verify(eventPublisher, never()).publish(any(EstoqueMinimoAtingidoEvent.class));
    }
}
