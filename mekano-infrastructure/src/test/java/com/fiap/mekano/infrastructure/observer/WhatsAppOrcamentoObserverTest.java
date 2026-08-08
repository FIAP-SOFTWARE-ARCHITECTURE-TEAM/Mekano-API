package com.fiap.mekano.infrastructure.observer;

import com.fiap.mekano.domain.event.DiagnosticoFinalizadoEvent;
import com.fiap.mekano.domain.model.Cliente;
import com.fiap.mekano.domain.model.ItemOrcamento;
import com.fiap.mekano.domain.model.Orcamento;
import com.fiap.mekano.domain.model.OrdemDeServico;
import com.fiap.mekano.domain.port.out.ClienteRepositoryPort;
import com.fiap.mekano.domain.port.out.OrcamentoRepositoryPort;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.WhatsAppNotifierPort;
import com.fiap.mekano.domain.valueobject.Telefone;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@QuarkusTest
class WhatsAppOrcamentoObserverTest {

    @InjectMock
    WhatsAppNotifierPort notifier;

    @InjectMock
    OrdemDeServicoRepositoryPort osRepository;

    @InjectMock
    ClienteRepositoryPort clienteRepository;

    @InjectMock
    OrcamentoRepositoryPort orcamentoRepository;

    @Inject
    WhatsAppOrcamentoObserver observer;

    @Test
    void aoFinalizarDiagnostico_comClienteComTelefone_notificaWhatsApp() {
        var osUuid = UUID.randomUUID();
        var clienteUuid = UUID.randomUUID();
        var orcamentoUuid = UUID.randomUUID();

        var os = OrdemDeServico.reconstitute(osUuid, clienteUuid, UUID.randomUUID(),
                "Problema no motor", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, 0L);
        var cliente = Cliente.reconstitute(clienteUuid, "João", "12345678901",
                "joao@test.com", "11999999999",
                "Rua A", "100", "Centro", "São Paulo", "SP", "01001000", null);
        var orcamento = Orcamento.reconstitute(orcamentoUuid, "Diagnóstico completo",
                List.of(new ItemOrcamento("Troca óleo", 1, BigDecimal.valueOf(150))),
                BigDecimal.valueOf(150), null);

        when(osRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(clienteRepository.findById(clienteUuid)).thenReturn(Optional.of(cliente));
        when(orcamentoRepository.findById(orcamentoUuid)).thenReturn(Optional.of(orcamento));

        var event = DiagnosticoFinalizadoEvent.of(osUuid, "Diagnóstico completo",
                List.of(new ItemOrcamento("Troca óleo", 1, BigDecimal.valueOf(150))));

        observer.aoFinalizarDiagnostico(event);

        verify(notifier).notificarOrcamento("11999999999", "João", orcamentoUuid, BigDecimal.valueOf(150));
    }

    @Test
    void aoFinalizarDiagnostico_clienteSemTelefone_naoNotifica() {
        var osUuid = UUID.randomUUID();
        var clienteUuid = UUID.randomUUID();

        var os = OrdemDeServico.reconstitute(osUuid, clienteUuid, UUID.randomUUID(),
                "Problema", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, 0L);
        var cliente = Cliente.reconstitute(clienteUuid, "João", "12345678901",
                "joao@test.com", null,
                "Rua A", "100", "Centro", "São Paulo", "SP", "01001000", null);

        when(osRepository.findById(osUuid)).thenReturn(Optional.of(os));
        when(clienteRepository.findById(clienteUuid)).thenReturn(Optional.of(cliente));

        var event = DiagnosticoFinalizadoEvent.of(osUuid, "Problema",
                List.of(new ItemOrcamento("Troca óleo", 1, BigDecimal.valueOf(150))));

        observer.aoFinalizarDiagnostico(event);

        verify(notifier, never()).notificarOrcamento(any(), any(), any(), any());
    }
}