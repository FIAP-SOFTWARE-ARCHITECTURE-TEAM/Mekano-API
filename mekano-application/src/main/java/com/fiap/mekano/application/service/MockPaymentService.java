package com.fiap.mekano.application.service;

import com.fiap.mekano.domain.event.PagamentoConfirmadoEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.os.StatusPagamento;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import com.fiap.mekano.domain.port.out.ProcessedEventsRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class MockPaymentService {

    private final OrdemDeServicoRepositoryPort ordemDeServicoRepository;
    private final ProcessedEventsRepositoryPort processedEventsRepository;
    private final EventPublisher eventPublisher;

    public MockPaymentService(OrdemDeServicoRepositoryPort ordemDeServicoRepository,
                              ProcessedEventsRepositoryPort processedEventsRepository,
                              EventPublisher eventPublisher) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.processedEventsRepository = processedEventsRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void confirmarPagamento(UUID osUuid) {
        var os = ordemDeServicoRepository.findById(osUuid)
                .orElseThrow(() -> new AppException(404, "OS não encontrada: " + osUuid));

        // D-06: idempotência — se já processado, retorna silenciosamente (200, não 409)
        if (processedEventsRepository.existsFor("PAGAMENTO_CONFIRMADO", osUuid)) {
            return;
        }

        if (os.getStatusPagamento() != StatusPagamento.AGUARDANDO_PAGAMENTO) {
            throw new AppException(409, "Pagamento não está pendente");
        }

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AppException(503, "Pagamento indisponível no momento, tente novamente mais tarde");
        }

        String referencia = "MOCK-" + UUID.randomUUID().toString().substring(0, 8);
        var pagamentoEvent = os.confirmarPagamento(referencia);
        ordemDeServicoRepository.save(os);
        processedEventsRepository.save("PAGAMENTO_CONFIRMADO", osUuid);
        eventPublisher.publish(pagamentoEvent);
    }
}