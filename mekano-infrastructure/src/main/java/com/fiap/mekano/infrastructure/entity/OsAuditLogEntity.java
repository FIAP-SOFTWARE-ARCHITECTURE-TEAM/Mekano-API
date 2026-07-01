package com.fiap.mekano.infrastructure.entity;


import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "os_audit_logs")
public class OsAuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true)
    public UUID uuid;

    @Column(name = "os_uuid", nullable = false)
    public UUID osUuid;

    @Column(nullable = false, length = 40)
    public String acao;

    @Column(name = "usuario_email", length = 180)
    public String usuarioEmail;

    @Column(length = 1000)
    public String observacao;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    public String metadataJson;

    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @PrePersist
    void prePersist() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}