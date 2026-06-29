package com.fiap.mekano.application.service.os;

import com.fiap.mekano.domain.port.out.OsAuditLogRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OsAuditQueryService {

    @Inject
    OsAuditLogRepositoryPort repository;

    public List<OsAuditLogRepositoryPort.OsAuditLogData> findAudit(UUID osUuid) {
        return repository.findByOsUuidOrderByCreatedAtDesc(osUuid);
    }
}