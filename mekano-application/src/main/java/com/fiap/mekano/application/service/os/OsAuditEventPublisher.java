package com.fiap.mekano.application.service.os;

import com.fiap.mekano.domain.event.OsTransitionedEvent;
import com.fiap.mekano.domain.os.OsAuditAction;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class OsAuditEventPublisher {

    @Inject
    Event<OsTransitionedEvent> events;

    public void publish(
            UUID osUuid,
            OsAuditAction acao,
            String usuarioEmail,
            String observacao,
            Map<String, Object> metadata
    ) {
        events.fire(new OsTransitionedEvent(
                osUuid,
                acao,
                usuarioEmail,
                observacao,
                metadata
        ));
    }
}