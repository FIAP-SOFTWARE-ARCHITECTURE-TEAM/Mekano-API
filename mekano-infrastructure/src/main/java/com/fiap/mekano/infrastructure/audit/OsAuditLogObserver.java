package com.fiap.mekano.infrastructure.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.mekano.domain.event.OsTransitionedEvent;
import com.fiap.mekano.domain.port.out.OsAuditLogRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class OsAuditLogObserver {

    @Inject
    OsAuditLogRepositoryPort repository;

    @Inject
    ObjectMapper objectMapper;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onOsTransitioned(
            @Observes(during = TransactionPhase.AFTER_SUCCESS) OsTransitionedEvent event
    ) {
        repository.save(new OsAuditLogRepositoryPort.CreateOsAuditLogCommand(
                event.osUuid(),
                event.acao(),
                event.usuarioEmail(),
                event.observacao(),
                toJson(event.metadata())
        ));
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return "{\"serializationError\":\"metadata inválido\"}";
        }
    }
}