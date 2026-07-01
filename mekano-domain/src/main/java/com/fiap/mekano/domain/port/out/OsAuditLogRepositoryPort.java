package com.fiap.mekano.domain.port.out;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.fiap.mekano.domain.os.OsAuditAction;

public interface OsAuditLogRepositoryPort {

    void save(CreateOsAuditLogCommand command);

    List<OsAuditLogData> findByOsUuidOrderByCreatedAtDesc(UUID osUuid);

    record CreateOsAuditLogCommand(
            UUID osUuid,
            OsAuditAction acao,
            String usuarioEmail,
            String observacao,
            String metadataJson
    ) {}

    record OsAuditLogData(
            UUID uuid,
            UUID osUuid,
            OsAuditAction acao,
            String usuarioEmail,
            String observacao,
            String metadataJson,
            LocalDateTime createdAt
    ) {}
}