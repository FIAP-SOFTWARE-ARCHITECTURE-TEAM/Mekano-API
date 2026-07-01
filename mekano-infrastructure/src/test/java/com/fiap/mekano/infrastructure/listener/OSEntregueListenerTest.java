package com.fiap.mekano.infrastructure.listener;

import com.fiap.mekano.domain.event.OSEntregueEvent;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.out.OsAuditLogRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OSEntregueListener")
class OSEntregueListenerTest {

    @Mock
    OsAuditLogRepositoryPort auditRepository;

    @InjectMocks
    OSEntregueListener listener;

    @Test
    @DisplayName("deve registrar auditoria ao receber OSEntregueEvent")
    void deveRegistrarAuditoriaAoReceberEvento() {
        UUID osId = UUID.randomUUID();
        var event = OSEntregueEvent.of(osId, "Cliente retirou o veículo");

        listener.onOSEntregue(event);

        ArgumentCaptor<OsAuditLogRepositoryPort.CreateOsAuditLogCommand> captor =
                ArgumentCaptor.forClass(OsAuditLogRepositoryPort.CreateOsAuditLogCommand.class);
        verify(auditRepository, times(1)).save(captor.capture());

        var command = captor.getValue();
        assertEquals(osId, command.osUuid());
        assertEquals(OsAuditAction.ENTREGA_REALIZADA, command.acao());
        assertEquals("sistema", command.usuarioEmail());
        assertEquals("Cliente retirou o veículo", command.observacao());
        assertTrue(command.metadataJson().contains("dataEntrega"));
    }

    @Test
    @DisplayName("deve usar observação default quando null")
    void deveUsarObservacaoDefaultQuandoNull() {
        UUID osId = UUID.randomUUID();
        var event = OSEntregueEvent.of(osId, null);

        listener.onOSEntregue(event);

        ArgumentCaptor<OsAuditLogRepositoryPort.CreateOsAuditLogCommand> captor =
                ArgumentCaptor.forClass(OsAuditLogRepositoryPort.CreateOsAuditLogCommand.class);
        verify(auditRepository).save(captor.capture());

        assertEquals("Entrega realizada", captor.getValue().observacao());
    }
}
