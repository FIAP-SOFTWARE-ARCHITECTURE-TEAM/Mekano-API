package com.fiap.mekano.application.service.requisicao;

import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.ItemRequisicaoCompra;
import com.fiap.mekano.domain.model.MotivoRequisicao;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.model.RequisicaoCompra;
import com.fiap.mekano.domain.model.StatusRequisicao;
import com.fiap.mekano.domain.port.in.CreateRequisicaoCompraCommand;
import com.fiap.mekano.domain.port.in.ItemRequisicaoCompraCommand;
import com.fiap.mekano.domain.port.out.ItemRequisicaoCompraRepositoryPort;
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
import java.util.List;
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

    @Mock
    ItemRequisicaoCompraRepositoryPort itemRepository;

    @InjectMocks
    RequisicaoCompraService requisicaoService;

    private UUID pecaId1;
    private UUID pecaId2;
    private UUID requisicaoId;
    private Peca mockPeca1;
    private Peca mockPeca2;

    @BeforeEach
    void setUp() {
        pecaId1 = UUID.randomUUID();
        pecaId2 = UUID.randomUUID();
        requisicaoId = UUID.randomUUID();
        mockPeca1 = Peca.reconstitute(
                pecaId1, "PEA-001", "Óleo do Motor 5W30",
                new BigDecimal("45.90"), 50L, 10L, LocalDateTime.now(), 0L);
        mockPeca2 = Peca.reconstitute(
                pecaId2, "PEA-002", "Filtro de Ar",
                new BigDecimal("25.00"), 30L, 5L, LocalDateTime.now(), 0L);
    }

    @Test
    @DisplayName("criar deve retornar resposta com múltiplos itens quando peças existem")
    void criarDeveRetornarRespostaQuandoPecasExistem() {
        when(pecaRepository.findById(pecaId1)).thenReturn(Optional.of(mockPeca1));
        when(pecaRepository.findById(pecaId2)).thenReturn(Optional.of(mockPeca2));

        var itens = List.of(
                new ItemRequisicaoCompraCommand(pecaId1, 10),
                new ItemRequisicaoCompraCommand(pecaId2, 5));

        var requisicao = RequisicaoCompra.criarParaMinimo(
                List.of(new ItemRequisicaoCompra(pecaId1, 10L),
                        new ItemRequisicaoCompra(pecaId2, 5L)),
                MotivoRequisicao.ESTOQUE_MINIMO);
        when(requisicaoRepository.save(any())).thenReturn(requisicao);

        var command = new CreateRequisicaoCompraCommand(itens, MotivoRequisicao.ESTOQUE_MINIMO);
        var response = requisicaoService.criar(command);

        assertNotNull(response);
        assertEquals(2, response.itens().size());
        assertEquals("ABERTA", response.status());
        assertEquals("ESTOQUE_MINIMO", response.motivo());
        verify(requisicaoRepository).save(any());
        verify(itemRepository).saveAll(any(), any());
    }

    @Test
    @DisplayName("criar deve lancar excecao quando uma das peças não existe (rejeição parcial)")
    void criarDeveLancarExcecaoQuandoUmaPecaNaoExiste() {
        when(pecaRepository.findById(pecaId1)).thenReturn(Optional.of(mockPeca1));
        when(pecaRepository.findById(pecaId2)).thenReturn(Optional.empty());

        var itens = List.of(
                new ItemRequisicaoCompraCommand(pecaId1, 10),
                new ItemRequisicaoCompraCommand(pecaId2, 5));

        var command = new CreateRequisicaoCompraCommand(itens, MotivoRequisicao.ESTOQUE_MINIMO);

        AppException ex = assertThrows(AppException.class, () -> requisicaoService.criar(command));
        assertEquals(404, ex.getStatus());

        verify(requisicaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("cancelar deve cancelar requisicao com motivo ESTOQUE_MINIMO")
    void cancelarDeveCancelarRequisicaoComMotivoEstoqueMinimo() {
        var itens = List.of(new ItemRequisicaoCompra(pecaId1, 10L));
        var requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, itens,
                StatusRequisicao.ABERTA, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());
        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));
        when(requisicaoRepository.atualizar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        requisicaoService.cancelar(requisicaoId);

        verify(requisicaoRepository).atualizar(any());
    }

    @Test
    @DisplayName("cancelar deve lancar excecao quando motivo e ORDEM_SERVICO")
    void cancelarDeveLancarExcecaoQuandoMotivoERdemServico() {
        var itens = List.of(new ItemRequisicaoCompra(pecaId1, 10L));
        var requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, itens,
                StatusRequisicao.ABERTA, MotivoRequisicao.ORDEM_SERVICO, LocalDateTime.now());
        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));

        AppException ex = assertThrows(AppException.class, () -> requisicaoService.cancelar(requisicaoId));
        assertEquals(409, ex.getStatus());

        verify(requisicaoRepository, never()).atualizar(any());
    }

    @Test
    @DisplayName("cancelar deve lancar excecao quando status nao e ABERTA")
    void cancelarDeveLancarExcecaoQuandoStatusNaoEAberta() {
        var itens = List.of(new ItemRequisicaoCompra(pecaId1, 10L));
        var requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, itens,
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

    @Test
    @DisplayName("enviar deve transicionar de ABERTA para ENVIADA")
    void enviarDeveTransicionarParaEnviada() {
        var itens = List.of(new ItemRequisicaoCompra(pecaId1, 10L));
        var requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, itens,
                StatusRequisicao.ABERTA, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());
        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));
        when(requisicaoRepository.atualizar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        requisicaoService.enviar(requisicaoId);

        verify(requisicaoRepository).atualizar(any());
    }

    @Test
    @DisplayName("enviar deve lancar 409 quando status nao e ABERTA")
    void enviarDeveLancar409QuandoStatusNaoEAberta() {
        var itens = List.of(new ItemRequisicaoCompra(pecaId1, 10L));
        var requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, itens,
                StatusRequisicao.ENVIADA, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());
        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));

        AppException ex = assertThrows(AppException.class, () -> requisicaoService.enviar(requisicaoId));
        assertEquals(409, ex.getStatus());

        verify(requisicaoRepository, never()).atualizar(any());
    }

    @Test
    @DisplayName("marcarComoComprada deve transicionar de ABERTA para COMPRA_APROVADA")
    void marcarComoCompradaDeveTransicionar() {
        var itens = List.of(new ItemRequisicaoCompra(pecaId1, 10L));
        var requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, itens,
                StatusRequisicao.ABERTA, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());
        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));
        when(requisicaoRepository.atualizar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        requisicaoService.marcarComoComprada(requisicaoId);

        verify(requisicaoRepository).atualizar(any());
    }

    @Test
    @DisplayName("marcarComoComprada deve lancar 409 quando status e ENVIADA")
    void marcarComoCompradaDeveLancar409QuandoEnviada() {
        var itens = List.of(new ItemRequisicaoCompra(pecaId1, 10L));
        var requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, itens,
                StatusRequisicao.ENVIADA, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());
        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));

        AppException ex = assertThrows(AppException.class, () -> requisicaoService.marcarComoComprada(requisicaoId));
        assertEquals(409, ex.getStatus());

        verify(requisicaoRepository, never()).atualizar(any());
    }

    @Test
    @DisplayName("marcarComoRecebida deve transicionar de ENVIADA para PRODUTO_RECEBIDO")
    void marcarComoRecebidaDeveTransicionar() {
        var itens = List.of(new ItemRequisicaoCompra(pecaId1, 10L));
        var requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, itens,
                StatusRequisicao.ENVIADA, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());
        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));
        when(requisicaoRepository.atualizar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        requisicaoService.marcarComoRecebida(requisicaoId);

        verify(requisicaoRepository).atualizar(any());
    }

    @Test
    @DisplayName("marcarComoRecebida deve transicionar de COMPRA_APROVADA para PRODUTO_RECEBIDO")
    void marcarComoRecebidaDeveTransicionarDeComprada() {
        var itens = List.of(new ItemRequisicaoCompra(pecaId1, 10L));
        var requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, itens,
                StatusRequisicao.COMPRA_APROVADA, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());
        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));
        when(requisicaoRepository.atualizar(any())).thenAnswer(invocation -> invocation.getArgument(0));

        requisicaoService.marcarComoRecebida(requisicaoId);

        verify(requisicaoRepository).atualizar(any());
    }

    @Test
    @DisplayName("marcarComoRecebida deve lancar 409 quando status e ABERTA")
    void marcarComoRecebidaDeveLancar409QuandoAberta() {
        var itens = List.of(new ItemRequisicaoCompra(pecaId1, 10L));
        var requisicao = RequisicaoCompra.reconstitute(
                requisicaoId, itens,
                StatusRequisicao.ABERTA, MotivoRequisicao.ESTOQUE_MINIMO, LocalDateTime.now());
        when(requisicaoRepository.findById(requisicaoId)).thenReturn(Optional.of(requisicao));

        AppException ex = assertThrows(AppException.class, () -> requisicaoService.marcarComoRecebida(requisicaoId));
        assertEquals(409, ex.getStatus());

        verify(requisicaoRepository, never()).atualizar(any());
    }

    @Test
    @DisplayName("findAll deve delegar ao repository")
    void findAllDeveDelegarAoRepository() {
        when(requisicaoRepository.findAll(0, 10)).thenReturn(List.of());

        var result = requisicaoService.findAll(0, 10);

        assertNotNull(result);
        verify(requisicaoRepository).findAll(0, 10);
    }

    @Test
    @DisplayName("countAll deve delegar ao repository")
    void countAllDeveDelegarAoRepository() {
        when(requisicaoRepository.countAll()).thenReturn(5L);

        long result = requisicaoService.countAll();

        assertEquals(5L, result);
        verify(requisicaoRepository).countAll();
    }
}
