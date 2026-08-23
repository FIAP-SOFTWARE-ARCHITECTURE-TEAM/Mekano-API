package com.fiap.mekano.application.service.peca;

import com.fiap.mekano.domain.model.Peca;
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

        when(pecaRepository.buscarPorId(pecaId)).thenReturn(java.util.Optional.of(inativa));
        when(pecaRepository.salvar(any(Peca.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Peca resultado = pecaService.updatePeca(pecaId, new UpdatePecaCommand(
                pecaId, "PEA-001", "Óleo do Motor 5W40", new BigDecimal("55.90"), 10L));

        assertNotNull(resultado);
        assertEquals(false, resultado.getIsActive());
        assertEquals("Óleo do Motor 5W40", resultado.getDescricao());

        verify(pecaRepository, times(1)).salvar(any(Peca.class));
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
}