package com.fiap.mekano.infrastructure.listener;

import com.fiap.mekano.domain.event.EntregaConfirmadaEvent;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.out.OsAuditLogRepositoryPort;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Listener para consumo de EntregaConfirmadaEvent (evento REALMENTE publicado por OrdemDeServicoService.entregar).
 * Registra ENTREGA_REALIZADA na trilha de auditoria.
 */
@ApplicationScoped
public class OSEntregueListener {

    @Inject
    OsAuditLogRepositoryPort auditRepository;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onEntregaConfirmada(
            @Observes(during = TransactionPhase.AFTER_SUCCESS) EntregaConfirmadaEvent event
    ) {
        Log.infof("OS %s entregue ao cliente — recebido por: %s",
                event.osUuid(), event.recebidoPor());

        String dataEntrega = event.occurredAt() != null
                ? event.occurredAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                : java.time.LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        auditRepository.save(new OsAuditLogRepositoryPort.CreateOsAuditLogCommand(
                event.osUuid(),
                OsAuditAction.ENTREGA_REALIZADA,
                "sistema",
                event.recebidoPor() != null ? event.recebidoPor() : "Entrega realizada",
                String.format("{\"dataEntrega\":\"%s\"}", dataEntrega)
        ));
    }
}
