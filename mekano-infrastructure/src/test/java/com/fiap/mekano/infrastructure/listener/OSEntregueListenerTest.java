package com.fiap.mekano.infrastructure.listener;

import com.fiap.mekano.domain.event.EntregaConfirmadaEvent;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.out.OsAuditLogRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
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
    @DisplayName("deve registrar auditoria ao receber EntregaConfirmadaEvent")
    void deveRegistrarAuditoriaAoReceberEvento() {
        UUID osId = UUID.randomUUID();
        var event = new EntregaConfirmadaEvent(osId, "João Silva", LocalDateTime.now());

        listener.onEntregaConfirmada(event);

        ArgumentCaptor<OsAuditLogRepositoryPort.CreateOsAuditLogCommand> captor =
                ArgumentCaptor.forClass(OsAuditLogRepositoryPort.CreateOsAuditLogCommand.class);
        verify(auditRepository, times(1)).save(captor.capture());

        var command = captor.getValue();
        assertEquals(osId, command.osUuid());
        assertEquals(OsAuditAction.ENTREGA_REALIZADA, command.acao());
        assertEquals("sistema", command.usuarioEmail());
        assertEquals("João Silva", command.observacao());
        assertTrue(command.metadataJson().contains("dataEntrega"));
    }

    @Test
    @DisplayName("deve usar 'Entrega realizada' quando recebidoPor é null")
    void deveUsarObservacaoDefaultQuandoNull() {
        UUID osId = UUID.randomUUID();
        var event = new EntregaConfirmadaEvent(osId, "Entrega realizada", LocalDateTime.now());

        listener.onEntregaConfirmada(event);

        ArgumentCaptor<OsAuditLogRepositoryPort.CreateOsAuditLogCommand> captor =
                ArgumentCaptor.forClass(OsAuditLogRepositoryPort.CreateOsAuditLogCommand.class);
        verify(auditRepository).save(captor.capture());

        assertEquals("Entrega realizada", captor.getValue().observacao());
    }

    @Test
    @DisplayName("deve usar osUuid do evento de entrega confirmada")
    void deveUsarOsUuidCorreto() {
        UUID osId = UUID.randomUUID();
        var event = new EntregaConfirmadaEvent(osId, "Maria", LocalDateTime.now());

        listener.onEntregaConfirmada(event);

        ArgumentCaptor<OsAuditLogRepositoryPort.CreateOsAuditLogCommand> captor =
                ArgumentCaptor.forClass(OsAuditLogRepositoryPort.CreateOsAuditLogCommand.class);
        verify(auditRepository).save(captor.capture());

        assertEquals(osId, captor.getValue().osUuid());
    }
}