package com.fiap.mekano.infrastructure.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiap.mekano.domain.event.OsTransitionedEvent;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.out.OsAuditLogRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;


class OsAuditLogObserverTest {

    @Test
    @DisplayName("Deve persistir audit log ao observar evento")
    void devePersistirAuditLogAoObservarEvento() {
        OsAuditLogRepositoryPort repository = mock(OsAuditLogRepositoryPort.class);

        OsAuditLogObserver observer = new OsAuditLogObserver();
        observer.repository = repository;
        observer.objectMapper = new ObjectMapper();

        UUID osUuid = UUID.randomUUID();

        OsTransitionedEvent event = new OsTransitionedEvent(
                osUuid,
                OsAuditAction.FINALIZAR,
                "mecanico@mekano.com",
                "OS finalizada",
                Map.of("statusAnterior", "EM_EXECUCAO", "statusAtual", "FINALIZADA")
        );

        observer.onOsTransitioned(event);

        ArgumentCaptor<OsAuditLogRepositoryPort.CreateOsAuditLogCommand> captor =
                ArgumentCaptor.forClass(OsAuditLogRepositoryPort.CreateOsAuditLogCommand.class);

        verify(repository).save(captor.capture());

        OsAuditLogRepositoryPort.CreateOsAuditLogCommand command = captor.getValue();

        assertEquals(osUuid, command.osUuid());
        assertEquals(OsAuditAction.FINALIZAR, command.acao());
        assertEquals("mecanico@mekano.com", command.usuarioEmail());
        assertEquals("OS finalizada", command.observacao());
        assertTrue(command.metadataJson().contains("EM_EXECUCAO"));
        assertTrue(command.metadataJson().contains("FINALIZADA"));
    }

    @Test
    @DisplayName("Deve gravar erro quando metadata não puder ser serializado")
    void deveGravarErroQuandoMetadataNaoPuderSerSerializado() throws Exception {
        OsAuditLogRepositoryPort repository = mock(OsAuditLogRepositoryPort.class);
        ObjectMapper objectMapper = mock(ObjectMapper.class);

        when(objectMapper.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("erro") {});

        OsAuditLogObserver observer = new OsAuditLogObserver();
        observer.repository = repository;
        observer.objectMapper = objectMapper;

        OsTransitionedEvent event = new OsTransitionedEvent(
                UUID.randomUUID(),
                OsAuditAction.CANCELAR,
                "atendente@mekano.com",
                "OS cancelada",
                Map.of("statusAtual", "CANCELADA")
        );

        observer.onOsTransitioned(event);

        ArgumentCaptor<OsAuditLogRepositoryPort.CreateOsAuditLogCommand> captor =
                ArgumentCaptor.forClass(OsAuditLogRepositoryPort.CreateOsAuditLogCommand.class);

        verify(repository).save(captor.capture());

        assertEquals("{\"serializationError\":\"metadata inválido\"}", captor.getValue().metadataJson());
    }
}
