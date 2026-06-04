package com.fiap.mekano.infrastructure.event;

import com.fiap.mekano.domain.port.out.EventPublisher;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;

/**
 * Implementação de EventPublisher usando CDI Events.
 * Publica eventos via jakarta.enterprise.event.Event.fire().
 */
@ApplicationScoped
public class CdiEventPublisher implements EventPublisher {

    @Inject
    Event<Object> eventBus;

    @Override
    public <T> void publish(T event) {
        eventBus.fire(event);
    }
}
