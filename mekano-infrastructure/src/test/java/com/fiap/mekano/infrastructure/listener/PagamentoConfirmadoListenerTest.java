package com.fiap.mekano.infrastructure.listener;

import com.fiap.mekano.domain.event.PagamentoConfirmadoEvent;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.out.OsAuditLogRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagamentoConfirmadoListener")
class PagamentoConfirmadoListenerTest {

    @Mock
    OsAuditLogRepositoryPort auditRepository;

    @InjectMocks
    PagamentoConfirmadoListener listener;

    @Test
    @DisplayName("deve registrar auditoria ao receber PagamentoConfirmadoEvent")
    void deveRegistrarAuditoriaAoReceberEvento() {
        UUID osId = UUID.randomUUID();
        UUID transacaoId = UUID.randomUUID();
        var event = PagamentoConfirmadoEvent.of(osId, transacaoId, new BigDecimal("500.00"));

        listener.onPagamentoConfirmado(event);

        ArgumentCaptor<OsAuditLogRepositoryPort.CreateOsAuditLogCommand> captor =
                ArgumentCaptor.forClass(OsAuditLogRepositoryPort.CreateOsAuditLogCommand.class);
        verify(auditRepository, times(1)).save(captor.capture());

        var command = captor.getValue();
        assertEquals(osId, command.osUuid());
        assertEquals(OsAuditAction.PAGAMENTO_CONFIRMADO, command.acao());
        assertEquals("sistema", command.usuarioEmail());
        assertTrue(command.observacao().contains(transacaoId.toString()));
        assertTrue(command.metadataJson().contains("500.00"));
    }
}
