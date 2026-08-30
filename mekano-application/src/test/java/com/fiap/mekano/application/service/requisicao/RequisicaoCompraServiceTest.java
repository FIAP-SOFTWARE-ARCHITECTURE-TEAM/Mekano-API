package com.fiap.mekano.application.service.requisicao;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.MotivoRequisicao;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import com.fiap.mekano.domain.port.out.RequisicaoCompraRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RequisicaoCompraService")
class RequisicaoCompraServiceTest {

    @Mock
    RequisicaoCompraRepositoryPort requisicaoRepository;

    @Mock
    PecaRepositoryPort pecaRepository;

    @InjectMocks
    RequisicaoCompraService requisicaoService;

    private UUID pecaId;
    private UUID requisicaoId;
    private Peca mockPeca;

    @BeforeEach
    void setUp() {
        pecaId = UUID.randomUUID();
        requisicaoId = UUID.randomUUID();
        mockPeca = Peca.reconstitute(
                pecaId, "PEA-001", "Óleo do Motor 5W30",
                new BigDecimal("45.90"), 50L, 10L, LocalDateTime.now(), 0L);
    }

    @Test
    @DisplayName("criar deve retornar resposta quando peca existe")
    void criarDeveRetornarRespostaQuandoPecaExiste() {
        when(pecaRepository.findById(pecaId)).thenReturn(Optional.of(mockPeca));

        var requisicao = RequisicaoCompra.criarParaMinimo(pecaId, 10L, MotivoRequisicao.ESTOQUE_MINIMO);
        when(requisicaoRepository.save(any())).thenReturn(requisicao);

        var command = new CreateRequisicaoCompraCommand(pecaId, 10, MotivoRequisicao.ESTOQUE_MINIMO);
        var response = requisicaoService.criar(command);

        assertNotNull(response);
        assertEquals(pecaId, response.pecaId());
        assertEquals("ABERTA", response.status());
        assertEquals("ESTOQUE_MINIMO", response.motivo());
        verify(requisicaoRepository).save(any());
    }

    @Test
    @DisplayName("criar deve lancar excecao quando peca nao existe")
    void criarDeveLancarExcecaoQuandoPecaNaoExiste() {
        when(pecaRepository.findById(pecaId)).thenReturn(Optional.empty());

        var command = new CreateRequisicaoCompraCommand(pecaId, 10, MotivoRequisicao.ESTOQUE_MINIMO);

        AppException ex = assertThrows(AppException.class, () -> requisicaoService.criar(command));
        assertEquals(404, ex.getStatus());

        verify(requisicaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelar deve cancelar requisicao com motivo ESTOQUE_MINIMO")
    void cancelarDeveCancelarRequisicaoComMotivoEstoqueMinimo() {
        var requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, pecaId, 10L,
                StatusRequisicao.ABERTA, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());
        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));
        when(requisicaoRepository.atualizar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        requisicaoService.cancelar(requisicaoId);

        verify(requisicaoRepository).atualizar(any());
    }

    @Test
    @DisplayName("cancelar deve lancar excecao quando motivo e ORDEM_SERVICO")
    void cancelarDeveLancarExcecaoQuandoMotivoERdemServico() {
        var requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, pecaId, 10L,
                StatusRequisicao.ABERTA, MotivoRequisicao.ORDEM_SERVICO, LocalDateTime.now());
        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));

        AppException ex = assertThrows(AppException.class, () -> requisicaoService.cancelar(requisicaoId));
        assertEquals(409, ex.getStatus());

        verify(requisicaoRepository, never()).atualizar(any());
    }

    @Test
    @DisplayName("cancelar deve lancar excecao quando status nao e ABERTA")
    void cancelarDeveLancarExcecaoQuandoStatusNaoEAberta() {
        var requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, pecaId, 10L,
                StatusRequisicao.ENVIADA, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());
        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));

        AppException ex = assertThrows(AppException.class, () -> requisicaoService.cancelar(requisicaoId));
        assertEquals(409, ex.getStatus());

        verify(requisicaoRepository, never()).atualizar(any());
    }

    @Test
    @DisplayName("buscarPorId deve lancar 404 quando nao encontrado")
    void buscarPorIdDeveLancar404QuandoNaoEncontrado() {
        UUID idInexistente = UUID.randomUUID();
        when(requisicaoRepository.findById(idInexistente)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> requisicaoService.buscarPorId(idInexistente));
        assertEquals(404, ex.getStatus());
    }
}
