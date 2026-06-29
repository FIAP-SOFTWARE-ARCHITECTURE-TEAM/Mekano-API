package com.fiap.mekano.application.service;

import com.fiap.mekano.domain.event.OSEntregueEvent;
import com.fiap.mekano.domain.exception.AppException;
import com.fiap.mekano.domain.exception.Messages;
import com.fiap.mekano.domain.port.out.EventPublisher;
import com.fiap.mekano.domain.port.out.OrdemDeServicoRepositoryPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.util.UUID;

@ApplicationScoped
public class EntregaService {

    private final OrdemDeServicoRepositoryPort ordemDeServicoRepository;
    private final EventPublisher eventPublisher;

    public EntregaService(OrdemDeServicoRepositoryPort ordemDeServicoRepository,
                          EventPublisher eventPublisher) {
        this.ordemDeServicoRepository = ordemDeServicoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void registrarEntrega(UUID osUuid, String observacao) {
        var os = ordemDeServicoRepository.findById(osUuid)
                .orElseThrow(() -> new AppException(404, Messages.get("os.not.found", osUuid)));

        if (!os.isPagamentoConfirmado()) {
            throw new AppException(409, "Não é possível entregar veículo sem pagamento confirmado");
        }

        os.entregar(observacao);
        ordemDeServicoRepository.save(os);
        eventPublisher.publish(OSEntregueEvent.of(osUuid, observacao));
    }
}