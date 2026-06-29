package com.fiap.mekano.infrastructure.repository;

import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.out.OsAuditLogRepositoryPort;
import com.fiap.mekano.infrastructure.entity.OsAuditLogEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OsAuditLogRepositoryImplTest {

    @Test
    @DisplayName("Deve persistir comando como entidade")
    void devePersistirComandoComoEntidade() {
        OsAuditLogPanacheRepository panacheRepository = mock(OsAuditLogPanacheRepository.class);

        OsAuditLogRepositoryImpl repository = new OsAuditLogRepositoryImpl();
        repository.panacheRepository = panacheRepository;

        UUID osUuid = UUID.randomUUID();

        repository.save(new OsAuditLogRepositoryPort.CreateOsAuditLogCommand(
                osUuid,
                OsAuditAction.APROVAR,
                "cliente@mekano.com",
                "Aprovado",
                "{\"statusAtual\":\"APROVADA\"}"
        ));

        ArgumentCaptor<OsAuditLogEntity> captor = ArgumentCaptor.forClass(OsAuditLogEntity.class);

        verify(panacheRepository).persist(captor.capture());

        OsAuditLogEntity entity = captor.getValue();

        assertNotNull(entity.uuid);
        assertEquals(osUuid, entity.osUuid);
        assertEquals("APROVAR", entity.acao);
        assertEquals("cliente@mekano.com", entity.usuarioEmail);
        assertEquals("Aprovado", entity.observacao);
        assertEquals("{\"statusAtual\":\"APROVADA\"}", entity.metadataJson);
    }

    @Test
    @DisplayName("Deve converter entidades em dados de auditoria")
    void deveConverterEntidadesEmDadosDeAuditoria() {
        OsAuditLogPanacheRepository panacheRepository = mock(OsAuditLogPanacheRepository.class);

        OsAuditLogRepositoryImpl repository = new OsAuditLogRepositoryImpl();
        repository.panacheRepository = panacheRepository;

        UUID auditUuid = UUID.randomUUID();
        UUID osUuid = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.now();

        OsAuditLogEntity entity = new OsAuditLogEntity();
        entity.uuid = auditUuid;
        entity.osUuid = osUuid;
        entity.acao = "EXECUTAR";
        entity.usuarioEmail = "mecanico@mekano.com";
        entity.observacao = "Execução iniciada";
        entity.metadataJson = "{\"statusAtual\":\"EM_EXECUCAO\"}";
        entity.createdAt = createdAt;

        when(panacheRepository.findByOsUuidOrderByCreatedAtDesc(osUuid)).thenReturn(List.of(entity));

        List<OsAuditLogRepositoryPort.OsAuditLogData> result =
                repository.findByOsUuidOrderByCreatedAtDesc(osUuid);

        assertEquals(1, result.size());

        OsAuditLogRepositoryPort.OsAuditLogData item = result.get(0);

        assertEquals(auditUuid, item.uuid());
        assertEquals(osUuid, item.osUuid());
        assertEquals(OsAuditAction.EXECUTAR, item.acao());
        assertEquals("mecanico@mekano.com", item.usuarioEmail());
        assertEquals("Execução iniciada", item.observacao());
        assertEquals("{\"statusAtual\":\"EM_EXECUCAO\"}", item.metadataJson());
        assertEquals(createdAt, item.createdAt());

        verify(panacheRepository).findByOsUuidOrderByCreatedAtDesc(osUuid);
    }
}
