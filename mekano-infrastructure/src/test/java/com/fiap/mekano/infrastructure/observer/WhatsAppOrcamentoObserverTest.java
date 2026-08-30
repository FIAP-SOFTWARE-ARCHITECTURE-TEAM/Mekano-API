package com.fiap.mekano.infrastructure.observer;

import com.fiap.mekano.domain.event.DiagnosticoFinalizadoEvent;
import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.valueobject.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;
import com.fiap.mekano.domain.port.out.WhatsAppNotifierPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WhatsAppOrcamentoObserver")
class WhatsAppOrcamentoObserverTest {

    private final WhatsAppNotifierPort notifier = mock(WhatsAppNotifierPort.class);
    private final OrdemDeServicoRepositoryPort osRepository = mock(OrdemDeServicoRepositoryPort.class);
    private final ClienteRepositoryPort clienteRepository = mock(ClienteRepositoryPort.class);
    private final OrcamentoRepositoryPort orcamentoRepository = mock(OrcamentoRepositoryPort.class);
    private final VeiculoRepositoryPort veiculoRepository = mock(VeiculoRepositoryPort.class);

    private final WhatsAppOrcamentoObserver observer =
            new WhatsAppOrcamentoObserver(notifier, osRepository, clienteRepository, orcamentoRepository, veiculoRepository);

    private OrdemDeServico stubOs(UUID osUuid, UUID clienteUuid, UUID veiculoUuid) {
        return OrdemDeServico.reconstitute(osUuid, clienteUuid, veiculoUuid,
                "Problema no motor", null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, LocalDateTime.now(), 0L);
    }

    @Test
    @DisplayName("deve notificar WhatsApp com veículo quando cliente tem telefone")
    void deveNotificarQuandoClienteTemTelefone() {
        UUID osUuid = UUID.randomUUID();
        UUID clienteUuid = UUID.randomUUID();
        UUID veiculoUuid = UUID.randomUUID();
        UUID orcamentoUuid = UUID.randomUUID();

        OrdemDeServico os = stubOs(osUuid, clienteUuid, veiculoUuid);
        var cliente = Cliente.reconstitute(clienteUuid, "João", "12345678909",
                "joao@test.com", "11999999999",
                "Rua A", "100", "Centro", "São Paulo", "SP", "01001000", null);
        var orcamento = Orcamento.reconstitute(orcamentoUuid, "Diagnóstico completo",
                List.of(new ItemOrcamento("Troca óleo", 1L, BigDecimal.valueOf(150))),
                BigDecimal.valueOf(150), LocalDateTime.now());
        var veiculo = Veiculo.reconstitute(veiculoUuid, clienteUuid, "ABC1D23", "Fiat", "Uno", 2020,
                LocalDateTime.now());

        when(osRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(clienteRepository.findById(clienteUuid)).thenReturn(Optional.of(cliente));
        when(orcamentoRepository.findByOrdemServicoUuid(osUuid)).thenReturn(Optional.of(orcamento));
        when(veiculoRepository.findById(veiculoUuid)).thenReturn(Optional.of(veiculo));

        var event = DiagnosticoFinalizadoEvent.of(osUuid, "Diagnóstico completo",
                List.of(new ItemOrcamento("Troca óleo", 1L, BigDecimal.valueOf(150))));

        observer.aoFinalizarDiagnostico(event);

        verify(notifier).notificarOrcamento("11999999999", "João", "Fiat", "Uno", "ABC1D23",
                BigDecimal.valueOf(150),
                List.of(new ItemOrcamento("Troca óleo", 1L, BigDecimal.valueOf(150))));
    }

    @Test
    @DisplayName("não deve notificar quando cliente não tem telefone")
    void naoDeveNotificarQuandoClienteSemTelefone() {
        UUID osUuid = UUID.randomUUID();
        UUID clienteUuid = UUID.randomUUID();

        OrdemDeServico os = stubOs(osUuid, clienteUuid, UUID.randomUUID());
        var cliente = Cliente.reconstitute(clienteUuid, "João", "12345678909",
                "joao@test.com", null,
                "Rua A", "100", "Centro", "São Paulo", "SP", "01001000", null);

        when(osRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(clienteRepository.findById(clienteUuid)).thenReturn(Optional.of(cliente));

        var event = new DiagnosticoFinalizadoEvent(osUuid, "Diagnóstico", List.of(), LocalDateTime.now());

        observer.aoFinalizarDiagnostico(event);

        verify(notifier, never()).notificarOrcamento(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(BigDecimal.class), anyList());
    }

    @Test
    @DisplayName("lookup com erro não deve propagar exceção após commit (WR-05)")
    void erroNoLookup_naoDevePropagar() {
        UUID osUuid = UUID.randomUUID();
        UUID clienteUuid = UUID.randomUUID();

        OrdemDeServico os = stubOs(osUuid, clienteUuid, UUID.randomUUID());

        when(osRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(clienteRepository.findById(clienteUuid))
                .thenThrow(new com.fiap.mekano.domain.exception.AppException(404, "cliente não encontrado"));

        var event = new DiagnosticoFinalizadoEvent(osUuid, "Diagnóstico", List.of(), LocalDateTime.now());

        assertDoesNotThrow(() -> observer.aoFinalizarDiagnostico(event));

        verify(notifier, never()).notificarOrcamento(anyString(), anyString(), anyString(), anyString(),
                anyString(), any(BigDecimal.class), anyList());
    }
}
