package com.fiap.mekano.rest.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record OsAuditLogResponse(
        UUID uuid,
        UUID osUuid,
        String acao,
        String usuarioEmail,
        String observacao,
        String metadataJson,
        LocalDateTime createdAt
) {}