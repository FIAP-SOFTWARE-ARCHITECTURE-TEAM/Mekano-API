package com.fiap.mekano.application.service.os;

import com.fiap.mekano.domain.event.OsTransitionedEvent;
import com.fiap.mekano.domain.os.OsAuditAction;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OsAuditEventPublisherTest {

    @Test
    @DisplayName("Deve publicar evento OsTransitionedEvent")
    void devePublicarEvento() {
        Event<OsTransitionedEvent> events = mock(Event.class);

        OsAuditEventPublisher publisher = new OsAuditEventPublisher();
        publisher.events = events;

        UUID osUuid = UUID.randomUUID();

        publisher.publish(
                osUuid,
                OsAuditAction.APROVAR,
                "cliente@mekano.com",
                "Aprovado",
                Map.of("statusAnterior", "ORCADA", "statusAtual", "APROVADA")
        );

        ArgumentCaptor<OsTransitionedEvent> captor = ArgumentCaptor.forClass(OsTransitionedEvent.class);

        verify(events).fire(captor.capture());

        OsTransitionedEvent event = captor.getValue();

        assertEquals(osUuid, event.osUuid());
        assertEquals(OsAuditAction.APROVAR, event.acao());
        assertEquals("cliente@mekano.com", event.usuarioEmail());
        assertEquals("Aprovado", event.observacao());
        assertEquals("ORCADA", event.metadata().get("statusAnterior"));
        assertEquals("APROVADA", event.metadata().get("statusAtual"));
    }
}
