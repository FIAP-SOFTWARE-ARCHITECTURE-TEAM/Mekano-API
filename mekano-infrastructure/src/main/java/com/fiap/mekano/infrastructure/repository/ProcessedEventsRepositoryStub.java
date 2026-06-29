package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.port.out.ProcessedEventsRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.UUID;

// TODO(#33): substituir por implementação real com ProcessedEventEntity +
//            ProcessedEventRepository (Panache) + tabela processed_events
@ApplicationScoped
public class ProcessedEventsRepositoryStub implements ProcessedEventsRepositoryPort {

    @Override
    public boolean existsFor(String eventType, UUID aggregateUuid) {
        return false;
    }

    @Override
    public void save(String eventType, UUID aggregateUuid) {
    }
}