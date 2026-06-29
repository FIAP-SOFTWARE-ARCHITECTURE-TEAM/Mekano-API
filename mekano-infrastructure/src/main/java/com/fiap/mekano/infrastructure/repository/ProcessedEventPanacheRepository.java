package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.ProcessedEventEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProcessedEventPanacheRepository implements PanacheRepository<ProcessedEventEntity> {

    public boolean eventoJaProcessado(String eventoId) {
        return count("evento_id = ?1", eventoId) > 0;
    }

    public ProcessedEventEntity buscarPorEventoId(String eventoId) {
        return find("evento_id = ?1", eventoId).firstResult();
    }
}
