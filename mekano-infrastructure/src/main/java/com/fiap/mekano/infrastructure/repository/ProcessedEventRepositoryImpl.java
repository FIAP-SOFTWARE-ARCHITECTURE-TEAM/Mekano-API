package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.ProcessedEventEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class ProcessedEventRepositoryImpl {

    private final ProcessedEventPanacheRepository panacheRepository;

    public ProcessedEventRepositoryImpl(ProcessedEventPanacheRepository panacheRepository) {
        this.panacheRepository = panacheRepository;
    }

    @Transactional
    public ProcessedEventEntity salvar(ProcessedEventEntity evento) {
        panacheRepository.persist(evento);
        return evento;
    }

    public Optional<ProcessedEventEntity> buscarPorEventoId(String eventoId) {
        return Optional.ofNullable(panacheRepository.buscarPorEventoId(eventoId));
    }

    public boolean eventoJaProcessado(String eventoId) {
        return panacheRepository.eventoJaProcessado(eventoId);
    }
}
