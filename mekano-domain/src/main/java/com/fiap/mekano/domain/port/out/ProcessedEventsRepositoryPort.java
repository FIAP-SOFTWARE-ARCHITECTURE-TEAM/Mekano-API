package com.fiap.mekano.domain.port.out;

import com.fiap.mekano.domain.model.OrdemDeServico;

import java.util.Optional;
import java.util.UUID;

public interface ProcessedEventsRepositoryPort {
    boolean existsFor(String eventType, UUID aggregateUuid);
    void save(String eventType, UUID aggregateUuid);
}