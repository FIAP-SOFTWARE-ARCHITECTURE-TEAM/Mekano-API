package com.fiap.mekano.domain.event;

import com.fiap.mekano.domain.os.OsAuditAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OsTransitionedEventTest {

    @Test
    @DisplayName("Deve criar evento com metadata imutável")
    void deveCriarEventoComMetadataImutavel() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("statusAnterior", "ORCADA");
        metadata.put("statusAtual", "APROVADA");

        UUID osUuid = UUID.randomUUID();

        OsTransitionedEvent event = new OsTransitionedEvent(
                osUuid,
                OsAuditAction.APROVAR,
                "cliente@mekano.com",
                "Aprovado",
                metadata
        );

        metadata.put("statusAtual", "ALTERADO");

        assertEquals(osUuid, event.osUuid());
        assertEquals(OsAuditAction.APROVAR, event.acao());
        assertEquals("APROVADA", event.metadata().get("statusAtual"));
        assertThrows(UnsupportedOperationException.class, () -> event.metadata().put("x", "y"));
    }

    @Test
    @DisplayName("Deve converter metadata nula para Map vazio")
    void deveConverterMetadataNulaParaMapVazio() {
        OsTransitionedEvent event = new OsTransitionedEvent(
                UUID.randomUUID(),
                OsAuditAction.CRIAR,
                "atendente@mekano.com",
                "OS criada",
                null
        );

        assertNotNull(event.metadata());
        assertTrue(event.metadata().isEmpty());
    }

    @Test
    @DisplayName("Não deve aceitar osUuid nulo")
    void naoDeveAceitarOsUuidNulo() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new OsTransitionedEvent(
                        null,
                        OsAuditAction.CRIAR,
                        "atendente@mekano.com",
                        "OS criada",
                        Map.of()
                )
        );

        assertEquals("osUuid não pode ser nulo", exception.getMessage());
    }

    @Test
    @DisplayName("Não deve aceitar ação nula")
    void naoDeveAceitarAcaoNula() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new OsTransitionedEvent(
                        UUID.randomUUID(),
                        null,
                        "atendente@mekano.com",
                        "OS criada",
                        Map.of()
                )
        );

        assertEquals("acao não pode ser nula", exception.getMessage());
    }
}
