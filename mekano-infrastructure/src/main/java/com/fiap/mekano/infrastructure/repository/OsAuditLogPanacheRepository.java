package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.infrastructure.entity.OsAuditLogEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class OsAuditLogPanacheRepository implements PanacheRepository<OsAuditLogEntity> {

    public List<OsAuditLogEntity> findByOsUuidOrderByCreatedAtDesc(UUID osUuid) {
        return list("osUuid = ?1 order by createdAt desc", osUuid);
    }
}
