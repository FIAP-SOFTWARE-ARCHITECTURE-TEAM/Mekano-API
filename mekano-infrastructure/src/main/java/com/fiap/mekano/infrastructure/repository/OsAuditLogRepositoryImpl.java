package com.fiap.mekano.infrastructure.repository;


import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.out.OsAuditLogRepositoryPort;
import com.fiap.mekano.infrastructure.entity.OsAuditLogEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OsAuditLogRepositoryImpl implements OsAuditLogRepositoryPort {

    @Inject
    OsAuditLogPanacheRepository panacheRepository;

    @Override
    @Transactional
    public void save(CreateOsAuditLogCommand command) {
        OsAuditLogEntity entity = new OsAuditLogEntity();
        entity.uuid = UUID.randomUUID();
        entity.osUuid = command.osUuid();
        entity.acao = command.acao().name();
        entity.usuarioEmail = command.usuarioEmail();
        entity.observacao = command.observacao();
        entity.metadataJson = command.metadataJson();

        panacheRepository.persist(entity);
    }

    @Override
    public List<OsAuditLogData> findByOsUuidOrderByCreatedAtDesc(UUID osUuid) {
        return panacheRepository.findByOsUuidOrderByCreatedAtDesc(osUuid)
                .stream()
                .map(entity -> new OsAuditLogData(
                        entity.uuid,
                        entity.osUuid,
                        OsAuditAction.valueOf(entity.acao),
                        entity.usuarioEmail,
                        entity.observacao,
                        entity.metadataJson,
                        entity.createdAt
                ))
                .toList();
    }
}