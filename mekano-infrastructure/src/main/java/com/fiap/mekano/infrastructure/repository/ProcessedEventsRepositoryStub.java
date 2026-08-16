package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.port.out.ProcessedEventsRepositoryPort;

import java.util.UUID;

// Desativado — ProcessedEventRepositoryImpl implementa o port real
public class ProcessedEventsRepositoryStub implements ProcessedEventsRepositoryPort {

    @Override
    public boolean existsFor(String eventType, UUID aggregateUuid) {
        return false;
    }

    @Override
    public void save(String eventType, UUID aggregateUuid) {
    }
}