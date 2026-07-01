package com.fiap.mekano.infrastructure.listener;

import com.fiap.mekano.domain.event.OSEntregueEvent;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.out.OsAuditLogRepositoryPort;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Listener para consumo de OSEntregueEvent.
 * Registra na trilha de auditoria para rastreabilidade operacional (D-20).
 */
@ApplicationScoped
public class OSEntregueListener {

    @Inject
    OsAuditLogRepositoryPort auditRepository;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onOSEntregue(
            @Observes(during = TransactionPhase.AFTER_SUCCESS) OSEntregueEvent event
    ) {
        Log.infof("OS %s entregue ao cliente — observação: %s",
                event.osUuid(), event.observacao());

        auditRepository.save(new OsAuditLogRepositoryPort.CreateOsAuditLogCommand(
                event.osUuid(),
                OsAuditAction.ENTREGA_REALIZADA,
                "sistema",
                event.observacao() != null ? event.observacao() : "Entrega realizada",
                String.format("{\"dataEntrega\":\"%s\"}", event.dataEntrega())
        ));
    }
}
