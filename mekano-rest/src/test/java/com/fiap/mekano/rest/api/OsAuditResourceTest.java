package com.fiap.mekano.rest.api;

import com.fiap.mekano.application.service.os.OsAuditQueryService;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.out.OsAuditLogRepositoryPort;
import com.fiap.mekano.rest.api.dto.OsAuditLogResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OsAuditResourceTest {

    @Test
    @DisplayName("Deve converter dados de auditoria para response REST")
    void deveConverterDadosDeAuditoriaParaResponseRest() {
        OsAuditQueryService service = mock(OsAuditQueryService.class);

        OsAuditResource resource = new OsAuditResource();
        resource.service = service;

        UUID auditUuid = UUID.randomUUID();
        UUID osUuid = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        OsAuditLogRepositoryPort.OsAuditLogData log = new OsAuditLogRepositoryPort.OsAuditLogData(
                auditUuid,
                osUuid,
                OsAuditAction.FINALIZAR,
                "mecanico@mekano.com",
                "OS finalizada",
                "{\"statusAtual\":\"FINALIZADA\"}",
                createdAt
        );

        when(service.findAudit(osUuid)).thenReturn(List.of(log));

        List<OsAuditLogResponse> response = resource.findAudit(osUuid);

        assertEquals(1, response.size());

        OsAuditLogResponse item = response.get(0);

        assertEquals(auditUuid, item.uuid());
        assertEquals(osUuid, item.osUuid());
        assertEquals("FINALIZAR", item.acao());
        assertEquals("mecanico@mekano.com", item.usuarioEmail());
        assertEquals("OS finalizada", item.observacao());
        assertEquals("{\"statusAtual\":\"FINALIZADA\"}", item.metadataJson());
        assertEquals(createdAt, item.createdAt());

        verify(service).findAudit(osUuid);
    }

    @Test
    @DisplayName("Deve retornar lista vazia quando OS não possuir auditoria")
    void deveRetornarListaVaziaQuandoOsNaoPossuirAuditoria() {
        OsAuditQueryService service = mock(OsAuditQueryService.class);

        OsAuditResource resource = new OsAuditResource();
        resource.service = service;

        UUID osUuid = UUID.randomUUID();

        when(service.findAudit(osUuid)).thenReturn(List.of());

        List<OsAuditLogResponse> response = resource.findAudit(osUuid);

        assertTrue(response.isEmpty());
        verify(service).findAudit(osUuid);
    }
}
