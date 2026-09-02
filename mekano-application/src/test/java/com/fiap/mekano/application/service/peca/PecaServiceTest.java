package com.fiap.mekano.application.service.peca;

import com.fiap.mekano.domain.event.EstoqueMinimoAtingidoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.Peca;
import com.fiap.mekano.domain.port.in.CreatePecaCommand;
import com.fiap.mekano.domain.port.in.UpdatePecaCommand;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.PecaRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PecaService")
class PecaServiceTest {

    @Mock
    PecaRepositoryPort pecaRepository;

    @Mock
    EventPublisher eventPublisher;

    @Mock
    OrcamentoRepositoryPort orcamentoRepository;

    @InjectMocks
    PecaService pecaService;

    private UUID pecaId;

    @BeforeEach
    void setUp() {
        pecaId = UUID.randomUUID();
    }

    @Test
    @DisplayName("reservarSaldo deve delegar ao port e retornar true")
    void reservarSaldoDeveRetornarTrue() {
        when(pecaRepository.reservarSaldo(pecaId, 5)).thenReturn(true);

        boolean result = pecaService.reservarSaldo(pecaId, 5);

        assertTrue(result);
        verify(pecaRepository, times(1)).reservarSaldo(pecaId, 5);
    }

    @Test
    @DisplayName("reservarSaldo deve retornar false quando saldo insuficiente")
    void reservarSaldoDeveRetornarFalse() {
        when(pecaRepository.reservarSaldo(pecaId, 999)).thenReturn(false);

        boolean result = pecaService.reservarSaldo(pecaId, 999);

        assertFalse(result);
        verify(pecaRepository, times(1)).reservarSaldo(pecaId, 999);
    }

    @Test
    @DisplayName("debitarSaldoReservado deve delegar ao port e retornar true")
    void debitarSaldoReservadoDeveRetornarTrue() {
        when(pecaRepository.debitarSaldoReservado(pecaId, 5)).thenReturn(true);

        boolean result = pecaService.debitarSaldoReservado(pecaId, 5);

        assertTrue(result);
        verify(pecaRepository, times(1)).debitarSaldoReservado(pecaId, 5);
    }

    @Test
    @DisplayName("liberarReserva deve delegar ao port e retornar true")
    void liberarReservaDeveRetornarTrue() {
        when(pecaRepository.liberarReserva(pecaId, 5)).thenReturn(true);

        boolean result = pecaService.liberarReserva(pecaId, 5);

        assertTrue(result);
        verify(pecaRepository, times(1)).liberarReserva(pecaId, 5);
    }

    @Test
    @DisplayName("liberarReserva deve retornar false quando não há reserva suficiente")
    void liberarReservaDeveRetornarFalse() {
        when(pecaRepository.liberarReserva(pecaId, 999)).thenReturn(false);

        boolean result = pecaService.liberarReserva(pecaId, 999);

        assertFalse(result);
        verify(pecaRepository, times(1)).liberarReserva(pecaId, 999);
    }

    @Test
    @DisplayName("excluir deve delegar ao repository")
    void excluirDeveDelegarAoRepository() {
        pecaService.excluir(pecaId);

        verify(pecaRepository, times(1)).remover(pecaId);
    }

    @Test
    @DisplayName("reativar deve delegar ao repository")
    void reativarDeveDelegarAoRepository() {
        pecaService.reativar(pecaId);

        verify(pecaRepository, times(1)).reativar(pecaId);
    }

    @Test
    @DisplayName("updatePeca em peça inativa deve preservar isActive=false")
    void updatePecaEmPecaInativaPreservaIsActive() {
        Peca inativa = Peca.reconstitute(
                pecaId, "PEA-001", "Óleo do Motor 5W30",
                new BigDecimal("45.90"),
                50L, 10L, LocalDateTime.now().minusDays(30), 0L, false);

        when(pecaRepository.findById(pecaId)).thenReturn(java.util.Optional.of(inativa));
        when(pecaRepository.save(any(Peca.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Peca resultado = pecaService.updatePeca(pecaId, new UpdatePecaCommand(
                pecaId, "PEA-001", "Óleo do Motor 5W40", new BigDecimal("55.90"), 10L));

        assertNotNull(resultado);
        assertEquals(false, resultado.getIsActive());
        assertEquals("Óleo do Motor 5W40", resultado.getDescricao());

        verify(pecaRepository, times(1)).save(any(Peca.class));
    }

    @Test
    @DisplayName("findAll/countAll devem repassar filtro isActive")
    void findAllRepassaFiltroIsActive() {
        when(pecaRepository.findAll(0, 10, false)).thenReturn(java.util.List.of());
        when(pecaRepository.countAll(false)).thenReturn(4L);

        var resultado = pecaService.findAll(0, 10, false);
        long total = pecaService.countAll(false);

        assertNotNull(resultado);
        assertEquals(4L, total);
        verify(pecaRepository, times(1)).findAll(0, 10, false);
        verify(pecaRepository, times(1)).countAll(false);
    }

    @Test
    @DisplayName("criar deve persistir peca e retornar response")
    void criarDevePersistirPeca() {
        Peca peca = Peca.create("PEA-001", "Filtro de óleo", new BigDecimal("25.00"), 5L);
        when(pecaRepository.save(any(Peca.class))).thenReturn(peca);

        var command = new CreatePecaCommand("PEA-001", "Filtro de óleo", new BigDecimal("25.00"), 5L);
        var response = pecaService.criar(command);

        assertNotNull(response);
        assertEquals("PEA-001", response.codigo());
        verify(pecaRepository, times(1)).save(any(Peca.class));
    }

    @Test
    @DisplayName("debitarSaldo com sucesso deve publicar EstoqueMinimoAtingidoEvent quando estoque cai abaixo do minimo")
    void debitarSaldoDevePublicarEventoEstoqueMinimo() {
        Peca peca = Peca.reconstitute(
                pecaId, "PEA-001", "Óleo", new BigDecimal("45.90"),
                10L, 5L, LocalDateTime.now(), 0L);
        when(pecaRepository.findById(pecaId)).thenReturn(java.util.Optional.of(peca));
        when(pecaRepository.debitarSaldo(pecaId, 8)).thenReturn(true);

        boolean result = pecaService.debitarSaldo(pecaId, 8);

        assertTrue(result);
        verify(eventPublisher, times(1)).publish(any(EstoqueMinimoAtingidoEvent.class));
    }

    @Test
    @DisplayName("debitarSaldo com estoque acima do minimo nao deve publicar evento")
    void debitarSaldoNaoDevePublicarEventoQuandoEstoqueOk() {
        Peca peca = Peca.reconstitute(
                pecaId, "PEA-001", "Óleo", new BigDecimal("45.90"),
                50L, 5L, LocalDateTime.now(), 0L);
        when(pecaRepository.findById(pecaId)).thenReturn(java.util.Optional.of(peca));
        when(pecaRepository.debitarSaldo(pecaId, 2)).thenReturn(true);

        boolean result = pecaService.debitarSaldo(pecaId, 2);

        assertTrue(result);
        verify(eventPublisher, never()).publish(any(EstoqueMinimoAtingidoEvent.class));
    }

    @Test
    @DisplayName("debitarSaldo com falha nao deve publicar evento")
    void debitarSaldoComFalhaNaoDevePublicarEvento() {
        Peca peca = Peca.reconstitute(
                pecaId, "PEA-001", "Óleo", new BigDecimal("45.90"),
                10L, 5L, LocalDateTime.now(), 0L);
        when(pecaRepository.findById(pecaId)).thenReturn(java.util.Optional.of(peca));
        when(pecaRepository.debitarSaldo(pecaId, 999)).thenReturn(false);

        boolean result = pecaService.debitarSaldo(pecaId, 999);

        assertFalse(result);
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    @DisplayName("creditarSaldo deve delegar ao repository")
    void creditarSaldoDeveDelegarAoRepository() {
        pecaService.creditarSaldo(pecaId, 10);

        verify(pecaRepository, times(1)).creditarSaldo(pecaId, 10);
    }

    @Test
    @DisplayName("debitarSaldoReservado deve retornar false quando falha")
    void debitarSaldoReservadoDeveRetornarFalseQuandoFalha() {
        when(pecaRepository.debitarSaldoReservado(pecaId, 999)).thenReturn(false);

        boolean result = pecaService.debitarSaldoReservado(pecaId, 999);

        assertFalse(result);
    }

    @Test
    @DisplayName("buscarPorId deve lancar 404 quando nao encontrado")
    void buscarPorIdDeveLancar404QuandoNaoEncontrado() {
        when(pecaRepository.findById(pecaId)).thenReturn(java.util.Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> pecaService.buscarPorId(pecaId));
        assertEquals(404, ex.getStatus());
    }

    @Test
    @DisplayName("excluir deve lancar 409 quando peca vinculada a OS ativa")
    void excluirDeveLancar409QuandoVinculadaAOsAtiva() {
        when(orcamentoRepository.existsByPecaIdVinculadaAOrdemComStatus(
                pecaId, java.util.List.of("AGUARDANDO_APROVACAO", "EM_EXECUCAO")))
                .thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> pecaService.excluir(pecaId));
        assertEquals(409, ex.getStatus());

        verify(pecaRepository, never()).remover(any());
    }

    @Test
    @DisplayName("listarAbaixoEstoqueMinimo deve delegar ao repository")
    void listarAbaixoEstoqueMinimoDeveDelegarAoRepository() {
        when(pecaRepository.listarAbaixoEstoqueMinimo()).thenReturn(java.util.List.of());

        var result = pecaService.listarAbaixoEstoqueMinimo();

        assertNotNull(result);
        verify(pecaRepository, times(1)).listarAbaixoEstoqueMinimo();
    }
}