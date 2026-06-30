package com.fiap.mekano.infrastructure.listener;

import com.fiap.mekano.domain.event.PagamentoConfirmadoEvent;
import com.fiap.mekano.domain.os.OsAuditAction;
import com.fiap.mekano.domain.port.out.OsAuditLogRepositoryPort;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.TransactionPhase;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

/**
 * Listener para consumo de PagamentoConfirmadoEvent.
 * Registra na trilha de auditoria da OS para rastreabilidade operacional (D-19).
 */
@ApplicationScoped
public class PagamentoConfirmadoListener {

    @Inject
    OsAuditLogRepositoryPort auditRepository;

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public void onPagamentoConfirmado(
            @Observes(during = TransactionPhase.AFTER_SUCCESS) PagamentoConfirmadoEvent event
    ) {
        Log.infof("Pagamento confirmado para OS %s — transação %s, valor %s",
                event.osUuid(), event.transacaoId(), event.valor());

        auditRepository.save(new OsAuditLogRepositoryPort.CreateOsAuditLogCommand(
                event.osUuid(),
                OsAuditAction.PAGAMENTO_CONFIRMADO,
                "sistema",
                "Pagamento confirmado - transação " + event.transacaoId(),
                String.format("{\"transacaoId\":\"%s\",\"valor\":\"%s\"}", event.transacaoId(), event.valor())
        ));
    }
}
