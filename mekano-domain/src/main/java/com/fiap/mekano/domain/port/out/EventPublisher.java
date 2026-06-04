package com.fiap.mekano.domain.port.out;

/**
 * Interface para publicação de eventos de domínio.
 * Implementação concreta: CdiEventPublisher em mekano-infrastructure.
 */
public interface EventPublisher {
    <T> void publish(T event);
}
