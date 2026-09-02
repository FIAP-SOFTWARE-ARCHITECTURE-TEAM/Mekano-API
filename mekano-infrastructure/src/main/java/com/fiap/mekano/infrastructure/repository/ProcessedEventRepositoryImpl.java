package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.port.out.ProcessedEventsRepositoryPort;
import com.fiap.mekano.infrastructure.entity.ProcessedEventEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class ProcessedEventRepositoryImpl implements ProcessedEventsRepositoryPort {

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

    @Override
    public boolean existsFor(String eventType, UUID aggregateUuid) {
        return eventoJaProcessado(eventType + ":" + aggregateUuid);
    }

    @Override
    @Transactional
    public void save(String eventType, UUID aggregateUuid) {
        String eventoId = eventType + ":" + aggregateUuid;
        if (eventoJaProcessado(eventoId)) {
            return;
        }
        ProcessedEventEntity entity = ProcessedEventEntity.builder()
                .eventoId(eventoId)
                .tipoEvento(eventType)
                .origemAgregado("ORDEM_DE_SERVICO")
                .uuidAgregado(aggregateUuid)
                .payload("{}")
                .build();
        panacheRepository.persist(entity);
    }
}