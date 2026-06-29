package com.fiap.mekano.application.service.os;

import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.out.OsAuditLogRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OsAuditQueryServiceTest {

    @Test
    @DisplayName("Deve consultar auditoria da OS pelo repositório")
    void deveConsultarAuditoriaDaOs() {
        OsAuditLogRepositoryPort repository = mock(OsAuditLogRepositoryPort.class);

        OsAuditQueryService service = new OsAuditQueryService();
        service.repository = repository;

        UUID osUuid = UUID.randomUUID();

        OsAuditLogRepositoryPort.OsAuditLogData log = new OsAuditLogRepositoryPort.OsAuditLogData(
                UUID.randomUUID(),
                osUuid,
                OsAuditAction.EXECUTAR,
                "mecanico@mekano.com",
                "Execução iniciada",
                "{\"statusAtual\":\"EM_EXECUCAO\"}",
                LocalDateTime.now()
        );

        when(repository.findByOsUuidOrderByCreatedAtDesc(osUuid)).thenReturn(List.of(log));

        List<OsAuditLogRepositoryPort.OsAuditLogData> result = service.findAudit(osUuid);

        assertEquals(1, result.size());
        assertEquals(OsAuditAction.EXECUTAR, result.get(0).acao());
        verify(repository).findByOsUuidOrderByCreatedAtDesc(osUuid);
    }
}
