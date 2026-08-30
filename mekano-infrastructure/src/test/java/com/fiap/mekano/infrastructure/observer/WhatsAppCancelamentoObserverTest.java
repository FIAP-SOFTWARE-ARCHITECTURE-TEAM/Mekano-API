package com.fiap.mekano.infrastructure.observer;

import com.fiap.mekano.domain.event.OSCanceladaEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;
import com.fiap.mekano.domain.port.out.WhatsAppNotifierPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("WhatsAppCancelamentoObserver")
class WhatsAppCancelamentoObserverTest {

    private final WhatsAppNotifierPort notifier = mock(WhatsAppNotifierPort.class);
    private final OrdemDeServicoRepositoryPort osRepository = mock(OrdemDeServicoRepositoryPort.class);
    private final ClienteRepositoryPort clienteRepository = mock(ClienteRepositoryPort.class);
    private final VeiculoRepositoryPort veiculoRepository = mock(VeiculoRepositoryPort.class);

    private final WhatsAppCancelamentoObserver observer =
            new WhatsAppCancelamentoObserver(notifier, osRepository, clienteRepository, veiculoRepository);

    private OrdemDeServico stubOs(UUID osUuid, UUID clienteUuid, UUID veiculoUuid) {
        return OrdemDeServico.reconstitute(osUuid, clienteUuid, veiculoUuid,
                "Problema no motor", null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, 0L);
    }

    @Test
    @DisplayName("deve notificar retirada via WhatsApp quando OS cancelada e cliente tem telefone")
    void deveNotificarQuandoOSCanceladaEClienteTemTelefone() {
        UUID osUuid = UUID.randomUUID();
        UUID clienteUuid = UUID.randomUUID();
        UUID veiculoUuid = UUID.randomUUID();

        OrdemDeServico os = stubOs(osUuid, clienteUuid, veiculoUuid);
        var cliente = Cliente.reconstitute(clienteUuid, "João", "12345678909",
                "joao@test.com", "11999999999",
                "Rua A", "100", "Centro", "São Paulo", "SP", "01001000", null);
        var veiculo = Veiculo.reconstitute(veiculoUuid, clienteUuid, "ABC1D23", "Fiat", "Uno", 2020,
                LocalDateTime.now());

        when(osRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(clienteRepository.findById(clienteUuid)).thenReturn(Optional.of(cliente));
        when(veiculoRepository.findById(veiculoUuid)).thenReturn(Optional.of(veiculo));

        var event = OSCanceladaEvent.of(osUuid, "Cliente desistiu");

        observer.aoCancelarOS(event);

        verify(notifier).notificarRetirada("11999999999", "João", "ABC1D23", osUuid);
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

        var event = OSCanceladaEvent.of(osUuid, "Cliente desistiu");

        observer.aoCancelarOS(event);

        verify(notifier, never()).notificarRetirada(anyString(), anyString(), anyString(), any(UUID.class));
    }

    @Test
    @DisplayName("lookup com erro não deve propagar exceção após commit (WR-05)")
    void erroNoLookup_naoDevePropagar() {
        UUID osUuid = UUID.randomUUID();
        UUID clienteUuid = UUID.randomUUID();

        OrdemDeServico os = stubOs(osUuid, clienteUuid, UUID.randomUUID());

        when(osRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(clienteRepository.findById(clienteUuid))
                .thenThrow(new AppException(404, "cliente não encontrado"));

        var event = OSCanceladaEvent.of(osUuid, "Cliente desistiu");

        assertDoesNotThrow(() -> observer.aoCancelarOS(event));

        verify(notifier, never()).notificarRetirada(anyString(), anyString(), anyString(), any(UUID.class));
    }
}