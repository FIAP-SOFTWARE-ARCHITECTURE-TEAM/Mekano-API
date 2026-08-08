package com.fiap.mekano.infrastructure.observer;

import com.fiap.mekano.domain.event.PagamentoConfirmadoEvent;
import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.model.Veiculo;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.VeiculoRepositoryPort;
import com.fiap.mekano.domain.port.out.WhatsAppNotifierPort;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class WhatsAppPagamentoObserverTest {

    @InjectMock
    WhatsAppNotifierPort notifier;

    @InjectMock
    OrdemDeServicoRepositoryPort osRepository;

    @InjectMock
    ClienteRepositoryPort clienteRepository;

    @InjectMock
    VeiculoRepositoryPort veiculoRepository;

    @Inject
    WhatsAppPagamentoObserver observer;

    @Test
    void aoConfirmarPagamento_comClienteComTelefone_notificaRetirada() {
        var osUuid = UUID.randomUUID();
        var clienteUuid = UUID.randomUUID();
        var veiculoUuid = UUID.randomUUID();

        var os = OrdemDeServico.reconstitute(osUuid, clienteUuid, veiculoUuid,
                "Problema no motor", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, 0L);
        var cliente = Cliente.reconstitute(clienteUuid, "Maria", "98765432100",
                "maria@test.com", "11988888888",
                "Rua B", "200", "Jardim", "São Paulo", "SP", "02002000", null);
        var veiculo = Veiculo.reconstitute(veiculoUuid, clienteUuid, "ABC1234",
                "Fiat", "Uno", 2020, null);

        when(osRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(clienteRepository.findById(clienteUuid)).thenReturn(Optional.of(cliente));
        when(veiculoRepository.findById(veiculoUuid)).thenReturn(Optional.of(veiculo));

        var event = PagamentoConfirmadoEvent.of(osUuid, "REF123");

        observer.aoConfirmarPagamento(event);

        verify(notifier).notificarRetirada("11988888888", "Maria", "ABC1234", osUuid);
    }

    @Test
    void aoConfirmarPagamento_clienteSemTelefone_naoNotifica() {
        var osUuid = UUID.randomUUID();
        var clienteUuid = UUID.randomUUID();
        var veiculoUuid = UUID.randomUUID();

        var os = OrdemDeServico.reconstitute(osUuid, clienteUuid, veiculoUuid,
                "Problema", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, 0L);
        var cliente = Cliente.reconstitute(clienteUuid, "Maria", "98765432100",
                "maria@test.com", null,
                "Rua B", "200", "Jardim", "São Paulo", "SP", "02002000", null);

        when(osRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(clienteRepository.findById(clienteUuid)).thenReturn(Optional.of(cliente));

        var event = PagamentoConfirmadoEvent.of(osUuid, "REF123");

        observer.aoConfirmarPagamento(event);

        verify(notifier, never()).notificarRetirada(any(), any(), any(), any());
    }
}